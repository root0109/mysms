package com.mysms.iam;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.iam.business.PasswordHasher;
import com.mysms.iam.config.IamDatabaseConfig;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
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
 * iam end to end over HTTP against real PostgreSQL and the real migrations — no mocks: the installation bootstrap,
 * password sign-in (constant answer, progressive lockout, inactive/expired accounts, the MFA step), the issued tokens against
 * the edge's own verifier, refresh rotation with reuse detection, sign-out, GetMe, the setting catalogue, administrators'
 * setting values with scope authorization, and the caller's effective settings through the whole hierarchy.
 */
@SpringBootTest(classes = IamApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IamApiComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final PasswordHasher HASHER = new PasswordHasher();
    private static final String TENANT_CODE = "acme";
    private static final String ADMIN_PASSWORD = "bootstrap-admin-secret-1";
    private static final String PASSWORD = "teacher-secret-pass-1";
    private static final UUID INSTITUTION = UUID.randomUUID();
    private static final UUID DEPARTMENT = UUID.randomUUID();
    private static final UUID OTHER_DEPARTMENT = UUID.randomUUID();
    private static final UUID PERSON = UUID.randomUUID();

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(IamDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "iam");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.iam.token.signing-key", TOKENS::privateKeyPem);
        registry.add("mysms.iam.bootstrap.tenant-code", () -> TENANT_CODE);
        registry.add("mysms.iam.bootstrap.admin-username", () -> "admin");
        registry.add("mysms.iam.bootstrap.admin-password", () -> ADMIN_PASSWORD);
    }

    private record Reply(int status, JsonNode body, HttpResponse<String> raw) {
        String etag() {
            return raw.headers().firstValue("ETag").orElse(null);
        }

        String code() {
            return body.path("code").asString();
        }
    }

    private Reply call(String method, String path, String token, String body, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode json = response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body());
        return new Reply(response.statusCode(), json, response);
    }

    private static String login(String identifier, String password) {
        return "{\"tenantCode\":\"" + TENANT_CODE + "\",\"identifier\":\"" + identifier + "\",\"password\":\"" + password
                + "\"}";
    }

    private Reply signIn(String identifier, String password) throws Exception {
        return call("POST", "/v1/iam/auth:login", null, login(identifier, password));
    }

    private String accessToken(String identifier, String password) throws Exception {
        Reply reply = signIn(identifier, password);
        assertThat(reply.status()).as(reply.raw().body()).isEqualTo(200);
        return reply.body().path("tokens").path("accessToken").asString();
    }

    private static JsonNode claims(String jwt) {
        return JSON.readTree(new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), StandardCharsets.UTF_8));
    }

    private static String q(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    // ---- fixtures through the application role (RLS on)

    private UUID tenant() {
        return as(null, () -> TestTransactions.of(database).readWithoutTenant(() -> database.jdbc()
                .sql("SELECT tenant_id FROM iam.tenant_directory WHERE tenant_code = :code").param("code", TENANT_CODE)
                .query(UUID.class).single()));
    }

    private <T> T as(UUID tenant, Supplier<T> work) {
        CallContext context = CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736").withIdentity(tenant, null);
        return CallContextHolder.callAs(context, work);
    }

    private UUID user(String username, String status, UUID person) {
        UUID tenant = tenant();
        UUID id = UUID.randomUUID();
        as(tenant, () -> TestTransactions.of(database).write(() -> database.jdbc().sql("""
                INSERT INTO iam.user_account (id, tenant_id, person_id, username, password_hash, status, user_type)
                VALUES (:id, :tenant, :person, :username, :hash, :status, 'teacher')
                """).param("id", id).param("tenant", tenant).param("person", person).param("username", username)
                .param("hash", HASHER.hash(PASSWORD)).param("status", status).update()));
        return id;
    }

    private UUID role(String code, String... permissions) {
        UUID tenant = tenant();
        UUID id = UUID.randomUUID();
        as(tenant, () -> TestTransactions.of(database).write(() -> {
            database.jdbc().sql("INSERT INTO iam.role (id, tenant_id, code) VALUES (:id, :tenant, :code)")
                    .param("id", id).param("tenant", tenant).param("code", code).update();
            for (String permission : permissions) {
                database.jdbc().sql("""
                        INSERT INTO iam.role_permission (id, tenant_id, role_id, permission_id)
                        SELECT :rp, :tenant, :role, pm.id FROM iam.permission pm WHERE pm.code = :code
                        """).param("rp", UUID.randomUUID()).param("tenant", tenant).param("role", id)
                        .param("code", permission).update();
            }
            return null;
        }));
        return id;
    }

    private void grant(UUID user, UUID role, String scopeType, UUID scopeId, boolean primary) {
        UUID tenant = tenant();
        as(tenant, () -> TestTransactions.of(database).write(() -> database.jdbc().sql("""
                INSERT INTO iam.user_role (id, tenant_id, user_id, role_id, scope_type, scope_id, is_primary)
                VALUES (:id, :tenant, :user, :role, :type, :scope, :primary)
                """).param("id", UUID.randomUUID()).param("tenant", tenant).param("user", user).param("role", role)
                .param("type", scopeType).param("scope", scopeId).param("primary", primary).update()));
    }

    private void sql(String statement, Object... params) {
        UUID tenant = tenant();
        as(tenant, () -> TestTransactions.of(database).write(() -> {
            var spec = database.jdbc().sql(statement);
            for (int i = 0; i < params.length; i += 2) {
                spec = spec.param((String) params[i], params[i + 1]);
            }
            return spec.update();
        }));
    }

    private List<String> outboxTypes() {
        UUID tenant = tenant();
        return as(tenant, () -> TestTransactions.of(database).read(() -> database.jdbc()
                .sql("SELECT event_type FROM iam.outbox_event ORDER BY id").query(String.class).list()));
    }

    private static UUID teacher;
    private static UUID departmentHead;

    // ---- tests

    @Test
    @Order(1)
    void bootstrapCreatesTheTenantAndAnAdministratorWithEveryPermission() throws Exception {
        String admin = accessToken("admin", ADMIN_PASSWORD);
        JsonNode claims = claims(admin);
        assertThat(claims.has("perms")).as("roles, never a permission list (SEC-0)").isFalse();
        assertThat(claims.path("rgr")).as("one tenant-wide role").hasSize(1);
        assertThat(claims.path("rgr").get(0).asString()).endsWith("@tenant");
        assertThat(claims.path("roles").get(0).asString()).isEqualTo("administrator@tenant");
        assertThat(claims.path("tid").asString()).isEqualTo(tenant().toString());
        assertThat(claims.path("ins").get(0).asString()).as("a tenant-wide grant covers every institution").isEqualTo("*");
        assertThat(claims.has("tperms")).isFalse();
        assertThat(claims.has("pid")).isFalse();
        Reply me = call("GET", "/v1/iam/me", admin, null);
        assertThat(me.status()).isEqualTo(200);
        assertThat(me.body().path("permissions").size()).isGreaterThan(1600);
        assertThat(me.body().path("user").path("userType").asString()).isEqualTo("admin");
        assertThat(me.body().path("user").has("passwordHash")).isFalse();
        // the platform learns the tenant before the bootstrap's audit records reach it
        assertThat(outboxTypes()).first().isEqualTo("iam.tenant_directory.bootstrapped");
        String announced = as(tenant(), () -> TestTransactions.of(database).read(() -> database.jdbc().sql(
                "SELECT payload FROM iam.outbox_event WHERE event_type = 'iam.tenant_directory.bootstrapped'")
                .query(String.class).single()));
        assertThat(JSON.readTree(announced).path("code").asString()).isEqualTo(TENANT_CODE);
        assertThat(JSON.readTree(announced).path("id").asString()).isEqualTo(tenant().toString());
    }

    @Test
    @Order(2)
    void signInGivesOneAnswerForEveryWrongDetail() throws Exception {
        teacher = user("teacher1", "active", PERSON);
        List<Reply> refusals = new ArrayList<>();
        refusals.add(call("POST", "/v1/iam/auth:login", null,
                "{\"tenantCode\":\"nope\",\"identifier\":\"teacher1\",\"password\":\"" + PASSWORD + "\"}"));
        refusals.add(signIn("nobody", PASSWORD));
        refusals.add(signIn("teacher1", "wrong-password"));
        refusals.add(signIn("teacher1", "x".repeat(1025)));
        for (Reply refusal : refusals) {
            assertThat(refusal.status()).isEqualTo(401);
            assertThat(refusal.code()).isEqualTo("INVALID_CREDENTIALS");
            assertThat(refusal.body().path("detail").asString()).isEqualTo(refusals.get(0).body().path("detail").asString());
        }
        Reply incomplete = call("POST", "/v1/iam/auth:login", null, "{\"tenantCode\":\"acme\"}");
        assertThat(incomplete.status()).isEqualTo(422);
        // case-insensitive login name and tenant code
        Reply ok = call("POST", "/v1/iam/auth:login", null,
                "{\"tenantCode\":\"ACME\",\"identifier\":\"TEACHER1\",\"password\":\"" + PASSWORD + "\"}");
        assertThat(ok.status()).isEqualTo(200);
    }

    @Test
    @Order(3)
    void theIssuedTokensWorkAtTheEdgeAndCarryTheClaims() throws Exception {
        UUID teacherRole = role("teacher", "iam.role.read");
        grant(teacher, teacherRole, "institution", INSTITUTION, true);
        grant(teacher, teacherRole, "department", DEPARTMENT, true);
        grant(teacher, teacherRole, "department", OTHER_DEPARTMENT, false);
        Reply reply = signIn("teacher1", PASSWORD);
        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.raw().headers().firstValue("Cache-Control").orElse("")).contains("no-store");
        assertThat(reply.body().path("mfaRequired").asBoolean()).isFalse();
        assertThat(reply.body().path("user").path("username").asString()).isEqualTo("teacher1");
        assertThat(reply.body().path("user").path("failedLoginCount").asInt()).isZero();
        JsonNode tokens = reply.body().path("tokens");
        assertThat(tokens.path("tokenType").asString()).isEqualTo("Bearer");
        assertThat(tokens.path("expiresInSeconds").asInt()).isEqualTo(900);
        assertThat(tokens.path("refreshToken").asString()).startsWith(tenant() + ".");
        JsonNode claims = claims(tokens.path("accessToken").asString());
        assertThat(claims.path("sub").asString()).isEqualTo(teacher.toString());
        assertThat(claims.path("pid").asString()).isEqualTo(PERSON.toString());
        assertThat(claims.path("aud").toString()).contains("mysms");
        assertThat(claims.path("ins").get(0).asString()).isEqualTo(INSTITUTION.toString());
        assertThat(claims.path("rgr")).as("no tenant-wide grant: nothing reaches every institution").hasSize(1);
        assertThat(claims.path("rgr").get(0).asString()).doesNotContain("@");
        assertThat(claims.has("perms") || claims.has("tperms")).isFalse();
        assertThat(claims.path("roles").toString()).contains("teacher@department:" + DEPARTMENT);

        String header = new String(Base64.getUrlDecoder().decode(tokens.path("accessToken").asString().split("\\.")[0]),
                StandardCharsets.UTF_8);
        Reply jwks = call("GET", "/oauth2/jwks", null, null);
        assertThat(jwks.status()).isEqualTo(200);
        assertThat(jwks.raw().headers().firstValue("Content-Type").orElse("")).startsWith("application/jwk-set+json");
        String kid = jwks.body().path("keys").get(0).path("kid").asString();
        assertThat(header).contains(kid);
        assertThat(jwks.body().path("keys").get(0).has("d")).isFalse();

        Reply me = call("GET", "/v1/iam/me", tokens.path("accessToken").asString(), null);
        assertThat(me.status()).isEqualTo(200);
        assertThat(me.body().path("user").path("id").asString()).isEqualTo(teacher.toString());
        assertThat(me.body().path("roles")).hasSize(3);
        assertThat(me.body().path("permissions").toString()).contains("iam.role.read");
        assertThat(me.body().has("person")).as("people is unreachable: Me comes without the person").isFalse();
    }

    @Test
    @Order(4)
    void refreshRotatesAndAReusedTokenRevokesTheWholeSession() throws Exception {
        Reply first = signIn("teacher1", PASSWORD);
        String refresh = first.body().path("tokens").path("refreshToken").asString();
        Reply rotated = call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + refresh + "\"}");
        assertThat(rotated.status()).isEqualTo(200);
        assertThat(rotated.raw().headers().firstValue("Cache-Control").orElse("")).contains("no-store");
        String next = rotated.body().path("refreshToken").asString();
        assertThat(next).isNotEqualTo(refresh);
        assertThat(claims(rotated.body().path("accessToken").asString()).path("sid").asString())
                .isEqualTo(claims(first.body().path("tokens").path("accessToken").asString()).path("sid").asString());
        Reply reused = call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + refresh + "\"}");
        assertThat(reused.status()).isEqualTo(401);
        assertThat(reused.code()).isEqualTo("INVALID_CREDENTIALS");
        Reply afterReuse = call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + next + "\"}");
        assertThat(afterReuse.status()).as("the whole session is revoked").isEqualTo(401);
        assertThat(outboxTypes()).contains("iam.user_account.refresh_reuse_detected");
        for (String bad : List.of("garbage", tenant() + "." + "A".repeat(43), UUID.randomUUID() + "." + "B".repeat(43))) {
            assertThat(call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + bad + "\"}").status())
                    .isEqualTo(401);
        }
    }

    @Test
    @Order(5)
    void logoutEndsTheSessionOrAllOfThem() throws Exception {
        Reply one = signIn("teacher1", PASSWORD);
        Reply two = signIn("teacher1", PASSWORD);
        String access = one.body().path("tokens").path("accessToken").asString();
        assertThat(call("POST", "/v1/iam/auth:logout", access, "{}").status()).isEqualTo(204);
        assertThat(call("POST", "/v1/iam/auth:refresh", null,
                "{\"refreshToken\":\"" + one.body().path("tokens").path("refreshToken").asString() + "\"}").status())
                .isEqualTo(401);
        String secondRefresh = two.body().path("tokens").path("refreshToken").asString();
        Reply stillOpen = call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + secondRefresh + "\"}");
        assertThat(stillOpen.status()).as("another session is untouched").isEqualTo(200);
        UUID device = UUID.randomUUID();
        sql("""
                INSERT INTO iam.user_device (id, tenant_id, user_id, device_identifier, platform, push_token)
                VALUES (:id, :tenant, :user, 'phone-1', 'android', 'push-1')
                """, "id", device, "tenant", tenant(), "user", teacher);
        assertThat(call("POST", "/v1/iam/auth:logout", stillOpen.body().path("accessToken").asString(),
                "{\"allDevices\":true}").status()).isEqualTo(204);
        Boolean deviceActive = as(tenant(), () -> TestTransactions.of(database).read(() -> database.jdbc()
                .sql("SELECT is_active FROM iam.user_device WHERE id = :id").param("id", device).query(Boolean.class)
                .single()));
        assertThat(deviceActive).as("sign-out everywhere unregisters push devices").isFalse();
        assertThat(call("POST", "/v1/iam/auth:refresh", null,
                "{\"refreshToken\":\"" + stillOpen.body().path("refreshToken").asString() + "\"}").status()).isEqualTo(401);
        assertThat(call("POST", "/v1/iam/auth:logout", null, "{}").status()).isEqualTo(401);
    }

    @Test
    @Order(6)
    void repeatedFailuresLockSignInProgressively() throws Exception {
        UUID locky = user("locky", "active", null);
        for (int i = 0; i < 5; i++) {
            assertThat(signIn("locky", "wrong-" + i).status()).isEqualTo(401);
        }
        assertThat(signIn("locky", PASSWORD).status()).as("locked out, even with the right password").isEqualTo(401);
        OffsetDateTime lockedUntil = as(tenant(), () -> TestTransactions.of(database).read(() -> database.jdbc()
                .sql("SELECT locked_until FROM iam.user_account WHERE id = :id").param("id", locky)
                .query(OffsetDateTime.class).single()));
        assertThat(lockedUntil).isAfter(OffsetDateTime.now()).isBefore(OffsetDateTime.now().plusMinutes(2));
        sql("UPDATE iam.user_account SET locked_until = now() - interval '1 second' WHERE id = :id", "id", locky);
        Reply ok = signIn("locky", PASSWORD);
        assertThat(ok.status()).isEqualTo(200);
        assertThat(ok.body().path("user").path("failedLoginCount").asInt()).isZero();
    }

    @Test
    @Order(6)
    void failedAttemptsByOthersNeverEndAnOwnersSessionAndSignOutIsNoTheftAlarm() throws Exception {
        user("victim", "active", null);
        Reply session = signIn("victim", PASSWORD);
        String refresh = session.body().path("tokens").path("refreshToken").asString();
        for (int i = 0; i < 6; i++) {
            assertThat(signIn("victim", "attacker-guess-" + i).status()).isEqualTo(401);
        }
        Reply stillSignedIn = call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + refresh + "\"}");
        assertThat(stillSignedIn.status()).as("the lockout gates password sign-in only").isEqualTo(200);
        int alarmsBefore = (int) outboxTypes().stream().filter("iam.user_account.refresh_reuse_detected"::equals).count();
        String next = stillSignedIn.body().path("refreshToken").asString();
        assertThat(call("POST", "/v1/iam/auth:logout", stillSignedIn.body().path("accessToken").asString(), "{}").status())
                .isEqualTo(204);
        assertThat(call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + next + "\"}").status())
                .isEqualTo(401);
        assertThat(outboxTypes().stream().filter("iam.user_account.refresh_reuse_detected"::equals).count())
                .as("a token revoked by sign-out is refused, not reported as theft").isEqualTo(alarmsBefore);
    }

    @Test
    @Order(7)
    void inactiveAndExpiredAccountsCannotSignInAndSecondFactorAccountsWaitForTheirCode() throws Exception {
        user("gone", "inactive", null);
        assertThat(signIn("gone", PASSWORD).status()).isEqualTo(401);
        UUID expired = user("expired", "active", null);
        sql("UPDATE iam.user_account SET account_expires_at = now() - interval '1 day' WHERE id = :id", "id", expired);
        assertThat(signIn("expired", PASSWORD).status()).isEqualTo(401);
        UUID mfa = user("twofactor", "active", null);
        sql("""
                INSERT INTO iam.mfa_credential (id, tenant_id, user_id, credential_type, secret_enc, verified_at)
                VALUES (:id, :tenant, :user, 'totp', 'ciphertext', now())
                """, "id", UUID.randomUUID(), "tenant", tenant(), "user", mfa);
        Reply waiting = signIn("twofactor", PASSWORD);
        assertThat(waiting.status()).as("the password is right: the second factor is next").isEqualTo(200);
        assertThat(waiting.body().path("mfaRequired").asBoolean()).isTrue();
        assertThat(waiting.body().path("mfaToken").asString()).isNotBlank();
        assertThat(waiting.body().has("tokens")).as("no tokens before the second factor").isFalse();
        assertThat(waiting.body().has("user")).isFalse();
        assertThat(signIn("twofactor", "wrong").status()).as("a wrong password never reveals the second factor")
                .isEqualTo(401);
    }

    @Test
    @Order(8)
    void theSettingCatalogueIsReadable() throws Exception {
        String token = accessToken("admin", ADMIN_PASSWORD);
        Reply all = call("GET", "/v1/iam/setting-definitions", token, null);
        assertThat(all.status()).isEqualTo(200);
        assertThat(all.body().path("data")).hasSize(12);
        assertThat(all.body().path("paging").path("size").asInt()).isEqualTo(12);
        assertThat(all.body().path("data").get(0).path("category").asString()).isEqualTo("appearance");
        Reply appearance = call("GET", "/v1/iam/setting-definitions?filter=" + q("category = \"appearance\""), token, null);
        assertThat(appearance.body().path("data")).hasSize(4);
        JsonNode theme = appearance.body().path("data").get(0);
        assertThat(theme.path("settingKey").asString()).isEqualTo("ui.theme");
        assertThat(theme.path("allowedValues").toString()).contains("high_contrast");
        assertThat(theme.path("allowedScopes")).hasSize(5);
        Reply one = call("GET", "/v1/iam/setting-definitions/" + theme.path("id").asString(), token, null);
        assertThat(one.status()).isEqualTo(200);
        assertThat(one.etag()).isEqualTo("\"0\"");
        Reply paged = call("GET", "/v1/iam/setting-definitions?size=3", token, null);
        assertThat(paged.body().path("data")).hasSize(3);
        Reply page2 = call("GET", paged.body().path("paging").path("next").asString(), token, null);
        assertThat(page2.body().path("paging").path("page").asInt()).isEqualTo(2);
        assertThat(page2.body().path("data").get(0).path("settingKey").asString())
                .isNotEqualTo(paged.body().path("data").get(2).path("settingKey").asString());
        assertThat(call("GET", "/v1/iam/setting-definitions/" + UUID.randomUUID(), token, null).status()).isEqualTo(404);
    }

    @Test
    @Order(9)
    void effectiveSettingsFollowTheHierarchyAndLocks() throws Exception {
        String admin = accessToken("admin", ADMIN_PASSWORD);
        String me = accessToken("teacher1", PASSWORD);
        Reply defaults = call("GET", "/v1/iam/me/settings", me, null);
        assertThat(defaults.status()).isEqualTo(200);
        assertThat(defaults.body().path("items")).as("administrator-only settings are not even listed").hasSize(9);
        assertThat(defaults.raw().body()).doesNotContain("security.sessionTimeoutMinutes");
        Reply catalogue = call("GET", "/v1/iam/setting-definitions", me, null);
        assertThat(catalogue.body().path("data")).hasSize(9);
        assertThat(catalogue.body().path("paging").has("next")).isFalse();
        String timeoutId = adminDefinitionId(admin, "security.sessionTimeoutMinutes");
        assertThat(call("GET", "/v1/iam/setting-definitions/" + timeoutId, me, null).status()).isEqualTo(404);
        assertThat(call("GET", "/v1/iam/setting-definitions/" + timeoutId, admin, null).status()).isEqualTo(200);
        assertThat(call("GET", "/v1/iam/me/settings", admin, null).body().path("items")).hasSize(12);
        assertThat(effective(defaults, "ui.theme").path("value").asString()).isEqualTo("system");
        assertThat(effective(defaults, "ui.theme").path("sourceScope").asString()).isEqualTo("default");

        assertThat(createValue(admin, "tenant", null, "ui.theme", "dark", false).status()).isEqualTo(201);
        assertThat(effective(call("GET", "/v1/iam/me/settings", me, null), "ui.theme").path("value").asString())
                .isEqualTo("dark");
        assertThat(createValue(admin, "department", OTHER_DEPARTMENT, "ui.theme", "system", false).status()).isEqualTo(201);
        assertThat(createValue(admin, "department", DEPARTMENT, "ui.theme", "light", false).status()).isEqualTo(201);
        JsonNode theme = effective(call("GET", "/v1/iam/me/settings", me, null), "ui.theme");
        assertThat(theme.path("value").asString()).as("the primary department wins").isEqualTo("light");
        assertThat(theme.path("sourceScope").asString()).isEqualTo("department");
        assertThat(theme.has("sourceScopeId")).as("which unit set it is administration data").isFalse();

        Reply mine = call("PUT", "/v1/iam/me/settings/ui.theme", me, "{\"value\":\"high_contrast\"}");
        assertThat(mine.status()).isEqualTo(200);
        assertThat(mine.body().path("value").asString()).isEqualTo("high_contrast");
        assertThat(mine.body().path("sourceScope").asString()).isEqualTo("user");
        assertThat(call("PUT", "/v1/iam/me/settings/ui.theme", me, "{\"value\":\"dark\"}").body().path("value")
                .asString()).isEqualTo("dark");

        assertThat(createValue(admin, "tenant", null, "ui.density", "compact", true).status()).isEqualTo(201);
        JsonNode density = effective(call("GET", "/v1/iam/me/settings", me, null), "ui.density");
        assertThat(density.path("locked").asBoolean()).isTrue();
        assertThat(density.path("value").asString()).isEqualTo("compact");
        Reply locked = call("PUT", "/v1/iam/me/settings/ui.density", me, "{\"value\":\"comfortable\"}");
        assertThat(locked.status()).isEqualTo(409);
        assertThat(locked.code()).isEqualTo("SETTING_LOCKED");

        Reply invalid = call("PUT", "/v1/iam/me/settings/ui.textScale", me, "{\"value\":\"500\"}");
        assertThat(invalid.status()).isEqualTo(422);
        assertThat(invalid.body().path("errors").get(0).path("field").asString()).isEqualTo("value");
        Reply adminOnly = call("PUT", "/v1/iam/me/settings/security.sessionTimeoutMinutes", me, "{\"value\":\"30\"}");
        assertThat(adminOnly.status()).as("invisible to users: as if it did not exist").isEqualTo(404);
        assertThat(call("DELETE", "/v1/iam/me/settings/security.sessionTimeoutMinutes", me, null).status()).isEqualTo(404);
        Reply notForAdminsEither = call("PUT", "/v1/iam/me/settings/security.sessionTimeoutMinutes", admin,
                "{\"value\":\"30\"}");
        assertThat(notForAdminsEither.status()).as("visible to administrators, but set per scope, not per user")
                .isEqualTo(422);
        assertThat(call("PUT", "/v1/iam/me/settings/no.such", me, "{\"value\":\"x\"}").status()).isEqualTo(404);
        assertThat(call("PUT", "/v1/iam/me/settings/ui.theme", me, "{}").status()).isEqualTo(422);

        assertThat(call("DELETE", "/v1/iam/me/settings/ui.theme", me, null).status()).isEqualTo(204);
        assertThat(effective(call("GET", "/v1/iam/me/settings", me, null), "ui.theme").path("value").asString())
                .isEqualTo("light");
        assertThat(call("DELETE", "/v1/iam/me/settings/ui.theme", me, null).status()).as("idempotent").isEqualTo(204);
        assertThat(call("DELETE", "/v1/iam/me/settings/no.such", me, null).status()).isEqualTo(404);

        // the institution selected with X-Institution-Id joins the chain
        assertThat(createValue(admin, "institution", INSTITUTION, "ui.locale", "hi", false).status()).isEqualTo(201);
        Reply withInstitution = call("GET", "/v1/iam/me/settings", me, null, "X-Institution-Id", INSTITUTION.toString());
        assertThat(effective(withInstitution, "ui.locale").path("value").asString()).isEqualTo("hi");
        assertThat(call("GET", "/v1/iam/me/settings", me, null, "X-Institution-Id", UUID.randomUUID().toString()).status())
                .isEqualTo(403);
    }

    private String adminDefinitionId(String admin, String key) throws Exception {
        for (JsonNode item : call("GET", "/v1/iam/setting-definitions", admin, null).body().path("data")) {
            if (key.equals(item.path("settingKey").asString())) {
                return item.path("id").asString();
            }
        }
        throw new AssertionError("no definition " + key);
    }

    private static JsonNode effective(Reply list, String key) {
        for (JsonNode item : list.body().path("items")) {
            if (key.equals(item.path("settingKey").asString())) {
                return item;
            }
        }
        throw new AssertionError("no setting " + key + " in " + list.raw().body());
    }

    private Reply createValue(String token, String scope, UUID unit, String key, String value, boolean locked)
            throws Exception {
        String body = "{\"scopeType\":\"" + scope + "\"," + (unit == null ? "" : "\"scopeId\":\"" + unit + "\",")
                + "\"settingKey\":\"" + key + "\",\"value\":\"" + value + "\",\"isLocked\":" + locked + "}";
        return call("POST", "/v1/iam/setting-values", token, body);
    }

    @Test
    @Order(10)
    void administratorsManageValuesWithTheCatalogueRulesAndETags() throws Exception {
        String admin = accessToken("admin", ADMIN_PASSWORD);
        for (String bad : List.of(
                "{\"scopeType\":\"tenant\",\"scopeId\":\"" + UUID.randomUUID()
                        + "\",\"settingKey\":\"ui.theme\",\"value\":\"dark\"}",
                "{\"scopeType\":\"department\",\"settingKey\":\"ui.theme\",\"value\":\"dark\"}",
                "{\"scopeType\":\"user\",\"scopeId\":\"" + teacher + "\",\"settingKey\":\"ui.theme\",\"value\":\"dark\","
                        + "\"isLocked\":true}",
                "{\"scopeType\":\"tenant\",\"settingKey\":\"no.such\",\"value\":\"x\"}",
                "{\"scopeType\":\"department\",\"scopeId\":\"" + DEPARTMENT + "\","
                        + "\"settingKey\":\"security.sessionTimeoutMinutes\",\"value\":\"30\"}",
                "{\"scopeType\":\"tenant\",\"settingKey\":\"ui.textScale\",\"value\":\"99\"}")) {
            Reply refused = call("POST", "/v1/iam/setting-values", admin, bad);
            assertThat(refused.status()).as(bad).isEqualTo(422);
            assertThat(refused.code()).isEqualTo("VALIDATION_FAILED");
        }
        assertThat(createValue(admin, "tenant", null, "ui.theme", "light", false).status()).as("duplicate").isEqualTo(409);
        Reply dryRun = call("POST", "/v1/iam/setting-values?validateOnly=true", admin,
                "{\"scopeType\":\"tenant\",\"settingKey\":\"ui.textScale\",\"value\":\"120\"}");
        assertThat(dryRun.status()).isEqualTo(200);
        assertThat(call("GET", "/v1/iam/setting-values/" + dryRun.body().path("id").asString(), admin, null).status())
                .isEqualTo(404);

        Reply created = createValue(admin, "tenant", null, "ui.textScale", "110", false);
        assertThat(created.status()).isEqualTo(201);
        String id = created.body().path("id").asString();
        assertThat(UUID.fromString(id).version()).isEqualTo(7);
        assertThat(created.raw().headers().firstValue("Location")).contains("/v1/iam/setting-values/" + id);
        assertThat(created.etag()).isEqualTo("\"1\"");
        String path = "/v1/iam/setting-values/" + id;
        assertThat(call("PATCH", path, admin, "{\"value\":\"120\"}").status()).isEqualTo(428);
        assertThat(call("PATCH", path, admin, "{\"value\":\"120\"}", "If-Match", "\"7\"").status()).isEqualTo(412);
        assertThat(call("PATCH", path, admin, "{\"value\":\"999\"}", "If-Match", "\"1\"").status()).isEqualTo(422);
        Reply updated = call("PATCH", path + "?updateMask=value", admin, "{\"value\":\"130\",\"isLocked\":true}",
                "If-Match", "\"1\"");
        assertThat(updated.status()).isEqualTo(200);
        assertThat(updated.etag()).isEqualTo("\"2\"");
        assertThat(updated.body().path("value").asString()).isEqualTo("130");
        assertThat(updated.body().path("isLocked").asBoolean()).as("not in the mask").isFalse();
        Reply masked = call("GET", path + "?fields=" + q("value,settingKey"), admin, null);
        assertThat(masked.body().has("scopeType")).isFalse();
        assertThat(masked.body().path("value").asString()).isEqualTo("130");
        Reply list = call("GET", "/v1/iam/setting-values?filter=" + q("settingKey = \"ui.textScale\""), admin, null);
        assertThat(list.body().path("data")).hasSize(1);
        assertThat(call("DELETE", path, admin, null, "If-Match", "\"1\"").status()).isEqualTo(412);
        assertThat(call("DELETE", path, admin, null, "If-Match", "\"2\"").status()).isEqualTo(204);
        assertThat(call("GET", path, admin, null).status()).isEqualTo(404);
        assertThat(createValue(admin, "tenant", null, "ui.textScale", "110", false).status())
                .as("a deleted value frees its scope").isEqualTo(201);
    }

    @Test
    @Order(11)
    void scopedAdministratorsActOnlyOnTheirOwnUnits() throws Exception {
        departmentHead = user("depthead", "active", null);
        UUID headRole = role("department_head", "iam.setting_value.read", "iam.setting_value.create",
                "iam.setting_value.update", "iam.setting_value.delete");
        grant(departmentHead, headRole, "department", DEPARTMENT, true);
        String head = accessToken("depthead", PASSWORD);
        Reply headSettings = call("GET", "/v1/iam/me/settings", head, null);
        assertThat(headSettings.raw().body()).as("set only by tenant or institution administrators")
                .doesNotContain("security.sessionTimeoutMinutes");
        assertThat(call("GET", "/v1/iam/setting-definitions", head, null).body().path("data")).hasSize(9);
        Reply own = createValue(head, "department", DEPARTMENT, "ui.density", "comfortable", false);
        assertThat(own.status()).as(own.raw().body()).isEqualTo(201);
        assertThat(createValue(head, "department", OTHER_DEPARTMENT, "ui.density", "compact", false).status())
                .isEqualTo(403);
        assertThat(createValue(head, "tenant", null, "ui.dateFormat", "yyyy-MM-dd", false).status()).isEqualTo(403);
        Reply visible = call("GET", "/v1/iam/setting-values", head, null);
        assertThat(visible.status()).isEqualTo(200);
        for (JsonNode item : visible.body().path("data")) {
            assertThat(item.path("scopeId").asString()).isEqualTo(DEPARTMENT.toString());
        }
        assertThat(visible.body().path("data")).hasSize(2);
        String admin = accessToken("admin", ADMIN_PASSWORD);
        Reply other = call("GET", "/v1/iam/setting-values?filter=" + q("scopeId = \"" + OTHER_DEPARTMENT + "\""), admin,
                null);
        String otherId = other.body().path("data").get(0).path("id").asString();
        assertThat(call("GET", "/v1/iam/setting-values/" + otherId, head, null).status()).isEqualTo(404);
        assertThat(call("DELETE", "/v1/iam/setting-values/" + otherId, head, null, "If-Match", "\"1\"").status())
                .isEqualTo(404);
        String teacherToken = accessToken("teacher1", PASSWORD);
        assertThat(call("GET", "/v1/iam/setting-values", teacherToken, null).status()).as("no permission at the edge")
                .isEqualTo(403);
    }

    @Test
    @Order(12)
    void pathsOutsideTheContractAreNotFound() throws Exception {
        String admin = accessToken("admin", ADMIN_PASSWORD);
        assertThat(call("GET", "/v1/iam/no-such-thing", admin, null).status()).isEqualTo(404);
    }

    @Test
    @Order(14)
    void aSuspendedTenantKeepsNobodySignedIn() throws Exception {
        Reply session = signIn("teacher1", PASSWORD);
        String refresh = session.body().path("tokens").path("refreshToken").asString();
        UUID tenant = tenant();
        as(tenant, () -> TestTransactions.of(database).write(() -> database.jdbc()
                .sql("UPDATE iam.tenant_directory SET status = 'suspended' WHERE tenant_code = :code")
                .param("code", TENANT_CODE).update()));
        try {
            assertThat(call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + refresh + "\"}").status())
                    .isEqualTo(401);
            assertThat(signIn("teacher1", PASSWORD).status()).isEqualTo(401);
        } finally {
            as(tenant, () -> TestTransactions.of(database).write(() -> database.jdbc()
                    .sql("UPDATE iam.tenant_directory SET status = 'active' WHERE tenant_code = :code")
                    .param("code", TENANT_CODE).update()));
        }
    }

    @Test
    @Order(13)
    void signInAndSettingChangesLeaveEventsWithoutPersonalData() {
        List<String> types = outboxTypes();
        assertThat(types).contains("iam.user_account.signed_in", "iam.user_account.sign_in_failed",
                "iam.user_account.signed_out", "iam.setting_value.created", "iam.setting_value.updated",
                "iam.setting_value.deleted", "audit.recorded");
        String payloads = as(tenant(), () -> TestTransactions.of(database).read(() -> String.join("\n", database.jdbc()
                .sql("SELECT payload FROM iam.outbox_event").query(String.class).list())));
        assertThat(payloads).doesNotContain("teacher1").doesNotContain(PASSWORD).doesNotContain("wrong-password")
                .doesNotContain(ADMIN_PASSWORD);
    }
}
