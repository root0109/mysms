package com.mysms.identity.iam;

import com.mysms.identity.IdentityApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.identity.iam.business.PasswordHasher;
import com.mysms.identity.iam.business.Totp;
import com.mysms.identity.iam.config.IamDatabaseConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Second factors and credentials end to end over HTTP against real PostgreSQL (no mocks): TOTP enrollment that
 * counts only after a code proves it, sign-in waiting for the second factor, one-time recovery codes, no code
 * replay, MFA failures in the sign-in lockout, disabling and replacing factors only with a current code, the
 * secret stored only encrypted; changing one's password (current one required, other sessions signed out), an
 * administrator setting another account's credentials without escalation, and accepting the terms.
 */
@SpringBootTest(classes = IdentityApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IamAccountSecurityComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final PasswordHasher HASHER = new PasswordHasher();
    private static final String TENANT_CODE = "secure";
    private static final String ADMIN_PASSWORD = "bootstrap-admin-secret-2";
    private static final String PASSWORD = "first-strong-pass-1";
    private static final String NEW_PASSWORD = "second-strong-pass-2";
    private static final String[] SELF_SERVICE = {"iam.mfa_credential.enroll", "iam.mfa_credential.read",
        "iam.mfa_credential.disable", "iam.mfa_credential.regenerate_recovery_codes",
        "iam.user_account.change_password", "iam.user_account.accept_terms"};

    private static UUID owner;
    private static String secret;
    private static long verifiedAt;
    private static List<String> recovery;
    private static String credential;
    private static String passwordOnly;

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(IamDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "iam", "people");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.iam.token.signing-key", TOKENS::privateKeyPem);
        registry.add("mysms.iam.bootstrap.tenant-code", () -> TENANT_CODE);
        registry.add("mysms.iam.bootstrap.admin-username", () -> "admin");
        registry.add("mysms.iam.bootstrap.admin-password", () -> ADMIN_PASSWORD);
    }

    private record Reply(int status, JsonNode body, String text) {
    }

    private Reply call(String method, String path, String token, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode json = response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body());
        return new Reply(response.statusCode(), json, method + " " + path + " -> " + response.body());
    }

    private Reply signIn(String username, String password) throws Exception {
        return call("POST", "/v1/iam/auth:login", null, "{\"tenantCode\":\"" + TENANT_CODE + "\",\"identifier\":\""
                + username + "\",\"password\":\"" + password + "\"}");
    }

    private Reply complete(String mfaToken, String code) throws Exception {
        return call("POST", "/v1/iam/mfa:complete", null, "{\"mfaToken\":\"" + mfaToken + "\",\"code\":\"" + code
                + "\"}");
    }

    private static String access(Reply login) {
        return login.body().path("tokens").path("accessToken").asString();
    }

    private static JsonNode claims(String jwt) {
        return JSON.readTree(new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), StandardCharsets.UTF_8));
    }

    private static String now(long offsetSeconds) {
        return Totp.codeAt(secret, Instant.now().getEpochSecond() + offsetSeconds);
    }

    /** A well-formed code that is valid for none of the steps around now (so never accepted by chance). */
    private static String wrong(String base32) {
        long t = Instant.now().getEpochSecond();
        List<String> valid = new ArrayList<>();
        for (long d = -90; d <= 90; d += 30) {
            valid.add(Totp.codeAt(base32, t + d));
        }
        for (int candidate = 0; ; candidate++) {
            String code = String.format(Locale.ROOT, "%06d", candidate);
            if (!valid.contains(code)) {
                return code;
            }
        }
    }

    private <T> T as(UUID tenant, Supplier<T> work) {
        CallContext context = CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736").withIdentity(tenant, null);
        return CallContextHolder.callAs(context, work);
    }

    private UUID tenant() {
        return as(null, () -> TestTransactions.of(database).readWithoutTenant(() -> database.jdbc()
                .sql("SELECT tenant_id FROM iam.tenant_directory WHERE tenant_code = :code").param("code", TENANT_CODE)
                .query(UUID.class).single()));
    }

    private <T> T sql(Supplier<T> work) {
        return as(tenant(), () -> TestTransactions.of(database).write(work::get));
    }

    /** An active account with a password and a tenant-wide role holding the given permissions. */
    private UUID user(String username, String... permissions) {
        UUID tenant = tenant();
        UUID id = UUID.randomUUID();
        UUID role = UUID.randomUUID();
        sql(() -> {
            database.jdbc().sql("""
                    INSERT INTO iam.user_account (id, tenant_id, username, password_hash, status, user_type)
                    VALUES (:id, :tenant, :username, :hash, 'active', 'teacher')
                    """).param("id", id).param("tenant", tenant).param("username", username)
                    .param("hash", HASHER.hash(PASSWORD)).update();
            database.jdbc().sql("INSERT INTO iam.role (id, tenant_id, code) VALUES (:id, :tenant, :code)")
                    .param("id", role).param("tenant", tenant).param("code", username + "-role").update();
            for (String permission : permissions) {
                database.jdbc().sql("""
                        INSERT INTO iam.role_permission (id, tenant_id, role_id, permission_id)
                        SELECT :rp, :tenant, :role, pm.id FROM iam.permission pm WHERE pm.code = :code
                        """).param("rp", UUID.randomUUID()).param("tenant", tenant).param("role", role)
                        .param("code", permission).update();
            }
            database.jdbc().sql("""
                    INSERT INTO iam.user_role (id, tenant_id, user_id, role_id, scope_type, is_primary)
                    VALUES (:id, :tenant, :user, :role, 'tenant', true)
                    """).param("id", UUID.randomUUID()).param("tenant", tenant).param("user", id).param("role", role)
                    .update();
            return null;
        });
        return id;
    }

    @Test
    @Order(1)
    void anAuthenticatorCountsOnlyAfterACodeProvesIt() throws Exception {
        owner = user("owner", SELF_SERVICE);
        Reply first = signIn("owner", PASSWORD);
        assertThat(first.status()).as(first.text()).isEqualTo(200);
        String token = access(first);
        passwordOnly = token;
        assertThat(claims(token).path("amr").toString()).isEqualTo("[\"pwd\"]");

        Reply sms = call("POST", "/v1/iam/mfa:enroll", token, "{\"credentialType\":\"sms\",\"target\":\"+919800000000\"}");
        assertThat(sms.status()).as("SMS needs message delivery").isEqualTo(501);

        Reply enrolled = call("POST", "/v1/iam/mfa:enroll", token, "{\"credentialType\":\"totp\",\"label\":\"Phone\"}");
        assertThat(enrolled.status()).as(enrolled.text()).isEqualTo(200);
        secret = enrolled.body().path("secret").asString();
        credential = enrolled.body().path("mfaCredentialId").asString();
        assertThat(secret).matches("[A-Z2-7]{32}");
        assertThat(enrolled.body().path("otpauthUrl").asString())
                .startsWith("otpauth://totp/MySMS:owner?secret=" + secret + "&issuer=MySMS");
        recovery = new ArrayList<>();
        enrolled.body().path("recoveryCodes").forEach(c -> recovery.add(c.asString()));
        assertThat(recovery).hasSize(10).allMatch(c -> c.matches("[a-z2-7]{5}-[a-z2-7]{5}"));
        String stored = sql(() -> database.jdbc().sql("SELECT string_agg(secret_enc, '|') FROM iam.mfa_credential "
                + "WHERE user_id = :user").param("user", owner).query(String.class).single());
        assertThat(stored).as("secrets stored only as envelopes").startsWith("v1.test.").doesNotContain(secret)
                .doesNotContain(recovery.get(0));

        Reply list = call("GET", "/v1/iam/mfa", token, null);
        assertThat(list.status()).as(list.text()).isEqualTo(200);
        assertThat(list.text()).doesNotContain(secret).doesNotContain("v1.test.");
        assertThat(signIn("owner", PASSWORD).body().path("mfaRequired").asBoolean())
                .as("an unverified factor does not count").isFalse();

        String enrollToken = enrolled.body().path("mfaToken").asString();
        assertThat(complete(enrollToken, wrong(secret)).status()).isEqualTo(401);
        verifiedAt = Instant.now().getEpochSecond();
        Reply verified = complete(enrollToken, Totp.codeAt(secret, verifiedAt));
        assertThat(verified.status()).as(verified.text()).isEqualTo(200);
        assertThat(verified.body().has("tokens")).as("verifying a factor never mints a new session").isFalse();
        assertThat(verified.body().path("mfaRequired").asBoolean()).isFalse();
        assertThat(complete(enrollToken, now(30)).status()).as("the step token works once").isEqualTo(401);
        JsonNode items = call("GET", "/v1/iam/mfa", token, null).body().path("items");
        assertThat(items).hasSize(2);
        items.forEach(item -> assertThat(item.hasNonNull("verifiedAt")).isTrue());
    }

    @Test
    @Order(2)
    void signInWaitsForTheSecondFactorAndNoCodeWorksTwice() throws Exception {
        Reply waiting = signIn("owner", PASSWORD);
        assertThat(waiting.body().path("mfaRequired").asBoolean()).isTrue();
        assertThat(waiting.body().has("tokens")).isFalse();
        assertThat(complete(waiting.body().path("mfaToken").asString(), Totp.codeAt(secret, verifiedAt)).status())
                .as("the code of a step already used is refused").isEqualTo(401);
        Reply next = signIn("owner", PASSWORD);
        assertThat(complete(waiting.body().path("mfaToken").asString(), now(30)).status())
                .as("a newer sign-in step replaces the older one").isEqualTo(401);
        Reply done = complete(next.body().path("mfaToken").asString(), now(30));
        assertThat(done.status()).as(done.text()).isEqualTo(200);
        assertThat(claims(access(done)).path("amr").toString()).isEqualTo("[\"pwd\",\"mfa\"]");
        Reply backdoor = call("POST", "/v1/iam/mfa:enroll", passwordOnly, "{\"credentialType\":\"totp\"}");
        assertThat(backdoor.status()).as("a password-only session cannot add a factor: " + backdoor.text())
                .isEqualTo(403);
        Reply another = call("POST", "/v1/iam/mfa:enroll", access(done), "{\"credentialType\":\"totp\"}");
        assertThat(another.status()).as("a session signed in with the factor can: " + another.text()).isEqualTo(200);
        assertThat(another.body().path("recoveryCodes").isEmpty()).as("existing recovery codes stay").isTrue();

        Reply viaRecovery = complete(signIn("owner", PASSWORD).body().path("mfaToken").asString(),
                recovery.get(0).toUpperCase(Locale.ROOT));
        assertThat(viaRecovery.status()).as("recovery codes are case-insensitive: " + viaRecovery.text())
                .isEqualTo(200);
        assertThat(complete(signIn("owner", PASSWORD).body().path("mfaToken").asString(), recovery.get(0)).status())
                .as("a recovery code works once").isEqualTo(401);

        String expiring = signIn("owner", PASSWORD).body().path("mfaToken").asString();
        sql(() -> database.jdbc().sql("UPDATE iam.auth_token SET expires_at = now() - interval '1 second' "
                + "WHERE user_id = :user AND token_type = 'one_time_login' AND revoked_at IS NULL")
                .param("user", owner).update());
        assertThat(complete(expiring, recovery.get(1)).status()).as("the step token expires").isEqualTo(401);
        assertThat(complete("not-a-token", "123456").status()).isEqualTo(401);
    }

    @Test
    @Order(3)
    void factorsChangeOnlyWithACurrentCode() throws Exception {
        String token = access(complete(signIn("owner", PASSWORD).body().path("mfaToken").asString(), recovery.get(2)));
        Reply wrong = call("POST", "/v1/iam/mfa:regenerateRecoveryCodes", token, "{\"code\":\"" + wrong(secret) + "\"}");
        assertThat(wrong.status()).as(wrong.text()).isEqualTo(422);
        assertThat(wrong.body().path("errors").get(0).path("field").asString()).isEqualTo("code");
        Reply fresh = call("POST", "/v1/iam/mfa:regenerateRecoveryCodes", token,
                "{\"code\":\"" + recovery.get(3) + "\"}");
        assertThat(fresh.status()).as(fresh.text()).isEqualTo(200);
        List<String> replaced = new ArrayList<>();
        fresh.body().path("recoveryCodes").forEach(c -> replaced.add(c.asString()));
        assertThat(replaced).hasSize(10).doesNotContainAnyElementsOf(recovery);
        assertThat(complete(signIn("owner", PASSWORD).body().path("mfaToken").asString(), recovery.get(4)).status())
                .as("earlier recovery codes are gone").isEqualTo(401);
        recovery = replaced;

        Reply refused = call("POST", "/v1/iam/mfa:disable", token,
                "{\"mfaCredentialId\":\"" + credential + "\",\"code\":\"" + wrong(secret) + "\"}");
        assertThat(refused.status()).isEqualTo(422);
        Reply other = call("POST", "/v1/iam/mfa:disable", token,
                "{\"mfaCredentialId\":\"" + UUID.randomUUID() + "\",\"code\":\"" + recovery.get(0) + "\"}");
        assertThat(other.status()).as("only the caller's own factors").isEqualTo(404);
        Reply disabled = call("POST", "/v1/iam/mfa:disable", token,
                "{\"mfaCredentialId\":\"" + credential + "\",\"code\":\"" + recovery.get(1) + "\"}");
        assertThat(disabled.status()).as(disabled.text()).isEqualTo(204);
        Reply plain = signIn("owner", PASSWORD);
        assertThat(plain.body().path("mfaRequired").asBoolean()).as("password-only again").isFalse();
        JsonNode left = call("GET", "/v1/iam/mfa", access(plain), null).body().path("items");
        left.forEach(item -> {
            assertThat(item.path("credentialType").asString()).as("the recovery codes went with the last authenticator")
                    .isEqualTo("totp");
            assertThat(item.hasNonNull("verifiedAt")).as("only the unfinished enrollment is left").isFalse();
        });
        // the failed codes above count towards the lockout; a success clears the count
        sql(() -> database.jdbc().sql("UPDATE iam.user_account SET failed_login_count = 0 WHERE id = :id")
                .param("id", owner).update());
    }

    @Test
    @Order(4)
    void wrongCodesLockSignInLikeWrongPasswords() throws Exception {
        UUID user = user("locked", SELF_SERVICE);
        String token = access(signIn("locked", PASSWORD));
        Reply enrolled = call("POST", "/v1/iam/mfa:enroll", token, "{\"credentialType\":\"totp\"}");
        String own = enrolled.body().path("secret").asString();
        assertThat(complete(enrolled.body().path("mfaToken").asString(),
                Totp.codeAt(own, Instant.now().getEpochSecond())).status()).isEqualTo(200);
        String step = signIn("locked", PASSWORD).body().path("mfaToken").asString();
        for (int i = 0; i < 5; i++) {
            assertThat(complete(step, wrong(own)).status()).isEqualTo(401);
        }
        String code = Totp.codeAt(own, Instant.now().getEpochSecond() + 30);
        assertThat(complete(step, code).status()).as("locked out: even the right code is refused").isEqualTo(401);
        assertThat(signIn("locked", PASSWORD).status()).as("password sign-in is locked too").isEqualTo(401);
        assertThat(call("POST", "/v1/iam/mfa:regenerateRecoveryCodes", token, "{\"code\":\"" + code + "\"}").status())
                .as("a locked account cannot try codes through a session either").isEqualTo(403);
        assertThat(call("POST", "/v1/iam/auth:changePassword", token, "{\"currentPassword\":\"" + PASSWORD
                + "\",\"newPassword\":\"" + NEW_PASSWORD + "\"}").status()).isEqualTo(403);
        Integer failures = sql(() -> database.jdbc().sql("SELECT failed_login_count FROM iam.user_account WHERE id = :id")
                .param("id", user).query(Integer.class).single());
        assertThat(failures).isGreaterThanOrEqualTo(5);
    }

    @Test
    @Order(5)
    void changingOnesPasswordNeedsTheCurrentOneAndSignsOtherSessionsOut() throws Exception {
        user("changer", SELF_SERVICE);
        Reply here = signIn("changer", PASSWORD);
        Reply elsewhere = signIn("changer", PASSWORD);
        String token = access(here);
        Reply wrong = call("POST", "/v1/iam/auth:changePassword", token,
                "{\"currentPassword\":\"not-the-password\",\"newPassword\":\"" + NEW_PASSWORD + "\"}");
        assertThat(wrong.status()).isEqualTo(422);
        assertThat(wrong.body().path("errors").get(0).path("field").asString()).isEqualTo("currentPassword");
        Reply weak = call("POST", "/v1/iam/auth:changePassword", token,
                "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"short\"}");
        assertThat(weak.status()).isEqualTo(422);
        assertThat(weak.body().path("errors").get(0).path("field").asString()).isEqualTo("newPassword");
        Reply changed = call("POST", "/v1/iam/auth:changePassword", token,
                "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"" + NEW_PASSWORD + "\"}");
        assertThat(changed.status()).as(changed.text()).isEqualTo(204);
        assertThat(signIn("changer", PASSWORD).status()).isEqualTo(401);
        assertThat(signIn("changer", NEW_PASSWORD).status()).isEqualTo(200);
        assertThat(call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\""
                + elsewhere.body().path("tokens").path("refreshToken").asString() + "\"}").status())
                .as("other sessions are signed out").isEqualTo(401);
        assertThat(call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\""
                + here.body().path("tokens").path("refreshToken").asString() + "\"}").status())
                .as("the session the change was made from stays").isEqualTo(200);
    }

    @Test
    @Order(6)
    void administratorsSetCredentialsOnlyOfAccountsTheyOutrank() throws Exception {
        UUID clerk = user("clerk", SELF_SERVICE);
        user("taken");
        Reply session = signIn("clerk", PASSWORD);
        String admin = access(signIn("admin", ADMIN_PASSWORD));
        String body = "{\"username\":\"clerk2\",\"temporaryPassword\":\"temporary-pass-123\"}";
        Reply set = call("POST", "/v1/iam/users/" + clerk + ":setCredentials", admin, body);
        assertThat(set.status()).as(set.text()).isEqualTo(200);
        assertThat(set.body().path("username").asString()).isEqualTo("clerk2");
        assertThat(set.body().path("mustResetPassword").asBoolean()).isTrue();
        assertThat(set.body().has("passwordHash")).isFalse();
        assertThat(signIn("clerk2", "temporary-pass-123").status()).isEqualTo(200);
        assertThat(call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\""
                + session.body().path("tokens").path("refreshToken").asString() + "\"}").status())
                .as("the account is signed out everywhere").isEqualTo(401);
        assertThat(call("POST", "/v1/iam/users/" + clerk + ":setCredentials", admin,
                "{\"username\":\"TAKEN\",\"temporaryPassword\":\"temporary-pass-123\"}").status()).isEqualTo(409);
        assertThat(call("POST", "/v1/iam/users/" + clerk + ":setCredentials", admin,
                "{\"temporaryPassword\":\"short\"}").status()).isEqualTo(422);
        sql(() -> database.jdbc().sql("UPDATE iam.user_account SET email = 'boss@example.org' WHERE username = 'taken'")
                .update());
        assertThat(call("POST", "/v1/iam/users/" + clerk + ":setCredentials", admin,
                "{\"username\":\"BOSS@example.org\",\"temporaryPassword\":\"temporary-pass-123\"}").status())
                .as("a username may not be another account's e-mail").isEqualTo(409);
        assertThat(call("POST", "/v1/iam/users", admin, "{\"username\":\"boss@example.org\",\"userType\":\"staff\"}")
                .status()).as("nor through the standard create").isEqualTo(409);

        // a helpdesk role that may set credentials but holds fewer permissions than the target
        UUID helpdesk = user("helpdesk", "iam.user_account.set_user_credentials");
        String help = access(signIn("helpdesk", PASSWORD));
        Reply escalate = call("POST", "/v1/iam/users/" + clerk + ":setCredentials", help,
                "{\"temporaryPassword\":\"temporary-pass-456\"}");
        assertThat(escalate.status()).as("no takeover of a more privileged account: " + escalate.text())
                .isEqualTo(403);
        Reply self = call("POST", "/v1/iam/users/" + helpdesk + ":setCredentials", help,
                "{\"temporaryPassword\":\"temporary-pass-456\"}");
        assertThat(self.status()).as("one's own password changes only with iam.ChangePassword").isEqualTo(403);
        assertThat(call("POST", "/v1/iam/users/" + UUID.randomUUID() + ":setCredentials", admin, body).status())
                .isEqualTo(404);
    }

    @Test
    @Order(7)
    void acceptingTheTermsIsRecorded() throws Exception {
        user("reader", SELF_SERVICE);
        String token = access(signIn("reader", PASSWORD));
        Reply accepted = call("POST", "/v1/iam/me:acceptTerms", token, "{\"termsVersion\":\"2026-11\"}");
        assertThat(accepted.status()).as(accepted.text()).isEqualTo(200);
        assertThat(accepted.body().hasNonNull("termsAcceptedAt")).isTrue();
        List<String> events = sql(() -> database.jdbc().sql("SELECT event_type FROM iam.outbox_event ORDER BY id")
                .query(String.class).list());
        assertThat(events).contains("iam.user_account.terms_accepted", "iam.user_account.mfa_enrolled",
                "iam.user_account.mfa_disabled", "iam.user_account.recovery_codes_regenerated",
                "iam.user_account.password_changed", "iam.user_account.credentials_set");
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", UUID.randomUUID().toString());
        claims.put("tid", tenant().toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of());
        assertThat(call("POST", "/v1/iam/me:acceptTerms", TOKENS.custom(claims, Instant.now().plusSeconds(60)),
                "{\"termsVersion\":\"2026-11\"}").status())
                .as("a self-service code every tenant user holds: an unknown account is simply not found").isEqualTo(404);
    }
}
