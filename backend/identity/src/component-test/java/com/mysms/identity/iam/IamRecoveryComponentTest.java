package com.mysms.identity.iam;

import com.mysms.identity.IdentityApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.identity.iam.IamHttp.Reply;
import com.mysms.identity.iam.business.RefreshTokens;
import com.mysms.identity.iam.config.IamDatabaseConfig;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Mailed one-time secrets end to end over HTTP, against real PostgreSQL and a real SMTP server (GreenMail): password
 * reset (the same answer for every identifier, single use, 30-minute expiry on the injected clock, a new password
 * signs out everywhere and proves the address, at most three codes in 30 minutes), an administrator requiring a reset
 * without escalation, e-mail verification (single use, 24-hour expiry, 429 after three codes, no SMS provider), and
 * self-service sign-up's own rules (closed unless the tenant opens it; the school is checked in org, so the rest runs in
 * the monolith's component test).
 */
@SpringBootTest(classes = IdentityApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(IamRecoveryComponentTest.MovableTime.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IamRecoveryComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final GreenMail SMTP = new GreenMail(ServerSetupTest.SMTP.dynamicPort());
    private static final IamHttp.MovableClock CLOCK = new IamHttp.MovableClock();
    private static final String TENANT_CODE = "recover";
    private static final String ADMIN_PASSWORD = "bootstrap-admin-secret-3";
    private static final String NEW_PASSWORD = "second-strong-pass-2";
    private static final String MAIL_WAIT = "a mail arrives";

    static {
        SMTP.start();
    }

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(IamDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    private IamHttp iam;

    @TestConfiguration
    static class MovableTime {

        @Bean
        Clock clock() {
            return CLOCK;
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "iam", "people");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.iam.token.signing-key", TOKENS::privateKeyPem);
        registry.add("mysms.iam.bootstrap.tenant-code", () -> TENANT_CODE);
        registry.add("mysms.iam.bootstrap.admin-username", () -> "admin");
        registry.add("mysms.iam.bootstrap.admin-password", () -> ADMIN_PASSWORD);
        registry.add("mysms.iam.smtp.host", () -> "127.0.0.1");
        registry.add("mysms.iam.smtp.port", () -> SMTP.getSmtp().getPort());
        registry.add("mysms.iam.smtp.from", () -> "no-reply@mysms.test");
        registry.add("mysms.iam.smtp.starttls-required", () -> "false");
        registry.add("mysms.iam.mail.app-url", () -> "https://app.mysms.test/");
    }

    @BeforeEach
    void connect() throws Exception {
        iam = new IamHttp(port, database, TENANT_CODE);
        SMTP.purgeEmailFromAllMailboxes();
    }

    @AfterEach
    void realTime() {
        CLOCK.reset();
    }

    @AfterAll
    static void stopSmtp() {
        SMTP.stop();
    }

    private Reply requestReset(String identifier) throws Exception {
        return iam.call("POST", "/v1/iam/auth:requestPasswordReset", null, "{\"tenantCode\":\"" + TENANT_CODE
                + "\",\"identifier\":\"" + identifier + "\"}");
    }

    private Reply reset(String token, String password) throws Exception {
        return iam.call("POST", "/v1/iam/auth:resetPassword", null, "{\"token\":\"" + token + "\",\"newPassword\":\""
                + password + "\"}");
    }

    /** Waits for exactly {@code count} mails in total and answers the last one. */
    private static MimeMessage mails(int count) throws Exception {
        assertThat(SMTP.waitForIncomingEmail(10_000, count)).as(MAIL_WAIT).isTrue();
        MimeMessage[] received = SMTP.getReceivedMessages();
        assertThat(received).hasSize(count);
        return received[count - 1];
    }

    /** After a moment, still no more than {@code count} mails (a background send would have arrived by then). */
    private static void noMoreThan(int count) throws Exception {
        SMTP.waitForIncomingEmail(1_500, count + 1);
        assertThat(SMTP.getReceivedMessages()).hasSize(count);
    }

    private static String to(MimeMessage mail) throws Exception {
        return mail.getRecipients(Message.RecipientType.TO)[0].toString();
    }

    @Test
    @Order(1)
    void aResetCodeIsMailedOnceForAKnownAccountAndWorksOnce() throws Exception {
        UUID reader = iam.user("reader", "reader@school.test");
        String refresh = iam.signIn("reader", IamHttp.PASSWORD).body().path("tokens").path("refreshToken").asString();

        Reply unknown = requestReset("nobody@school.test");
        Reply known = requestReset("READER@school.test");
        assertThat(unknown.status()).as(unknown.text()).isEqualTo(204);
        assertThat(known.status()).as("the same answer for a known account").isEqualTo(204);
        assertThat(requestReset("reader").status()).isEqualTo(204);
        assertThat(iam.call("POST", "/v1/iam/auth:requestPasswordReset", null,
                "{\"tenantCode\":\"no-such-tenant\",\"identifier\":\"reader\"}").status()).isEqualTo(204);
        MimeMessage mail = mails(2);
        noMoreThan(2);
        assertThat(to(mail)).isEqualTo("reader@school.test");
        assertThat(mail.getSubject()).isEqualTo("Reset your MySMS password");
        String body = GreenMailUtil.getBody(mail);
        String token = IamHttp.secretIn(body);
        assertThat(body).contains("https://app.mysms.test/app/auth/reset?token=").contains("30 minutes")
                .doesNotContain("reader\n");
        String first = IamHttp.secretIn(GreenMailUtil.getBody(SMTP.getReceivedMessages()[0]));

        String stored = iam.sql(() -> iam.jdbc().sql("SELECT string_agg(token_hash, '|') FROM iam.auth_token "
                + "WHERE user_id = :user AND token_type = 'password_reset'").param("user", reader)
                .query(String.class).single());
        assertThat(stored).as("only hashes are stored").contains(RefreshTokens.hash(token)).doesNotContain(token);

        Reply weak = reset(token, "short");
        assertThat(weak.status()).as(weak.text()).isEqualTo(422);
        assertThat(weak.firstErrorCode()).isEqualTo("PASSWORD_POLICY");
        Reply done = reset(token, NEW_PASSWORD);
        assertThat(done.status()).as("a refused password did not use the code up: " + done.text()).isEqualTo(204);
        Reply again = reset(token, NEW_PASSWORD + "x");
        assertThat(again.status()).as("single use").isEqualTo(422);
        assertThat(again.firstErrorCode()).isEqualTo("INVALID_CODE");
        assertThat(reset(first, NEW_PASSWORD + "y").status()).as("a new password ends every other open reset")
                .isEqualTo(422);
        assertThat(reset("garbage", NEW_PASSWORD).firstErrorCode()).isEqualTo("INVALID_CODE");

        assertThat(iam.signIn("reader", IamHttp.PASSWORD).status()).isEqualTo(401);
        assertThat(iam.signIn("reader", NEW_PASSWORD).status()).isEqualTo(200);
        assertThat(iam.call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + refresh + "\"}").status())
                .as("the reset signed the account out everywhere").isEqualTo(401);
        Boolean verified = iam.sql(() -> iam.jdbc().sql("SELECT email_verified_at IS NOT NULL FROM iam.user_account "
                + "WHERE id = :id").param("id", reader).query(Boolean.class).single());
        assertThat(verified).as("a code mailed to the address proves it").isTrue();
    }

    @Test
    @Order(2)
    void aResetCodeExpiresAfterThirtyMinutes() throws Exception {
        iam.user("late", "late@school.test");
        assertThat(requestReset("late").status()).isEqualTo(204);
        String token = IamHttp.secretIn(GreenMailUtil.getBody(mails(1)));
        CLOCK.advance(Duration.ofMinutes(31));
        assertThat(reset(token, NEW_PASSWORD).firstErrorCode()).isEqualTo("INVALID_CODE");
        CLOCK.reset();
        assertThat(reset(token, NEW_PASSWORD).status()).as("within the 30 minutes it works").isEqualTo(204);
    }

    @Test
    @Order(3)
    void atMostThreeCodesAreMailedInThirtyMinutes() throws Exception {
        iam.user("eager", "eager@school.test");
        for (int i = 0; i < 4; i++) {
            assertThat(requestReset("eager").status()).as("always the same answer").isEqualTo(204);
        }
        mails(3);
        noMoreThan(3);
        CLOCK.advance(Duration.ofMinutes(31));
        assertThat(requestReset("eager").status()).isEqualTo(204);
        mails(4);
    }

    @Test
    @Order(4)
    void anAdministratorRequiresAResetWithoutEscalation() throws Exception {
        String admin = iam.token("admin", ADMIN_PASSWORD);
        UUID forced = iam.user("forced", "forced@school.test", "iam.user_account.read");
        Reply login = iam.signIn("forced", IamHttp.PASSWORD);
        String refresh = login.body().path("tokens").path("refreshToken").asString();
        String etag = iam.call("GET", "/v1/iam/users/" + forced, admin, null).body().path("etag").asString();

        Reply stale = iam.call("POST", "/v1/iam/users/" + forced + ":forcePasswordReset", admin,
                "{\"etag\":\"W/\\\"99\\\"\"}");
        assertThat(stale.status()).as(stale.text()).isEqualTo(412);
        Reply done = iam.call("POST", "/v1/iam/users/" + forced + ":forcePasswordReset", admin,
                "{\"etag\":" + IamHttp.JSON.writeValueAsString(etag) + "}");
        assertThat(done.status()).as(done.text()).isEqualTo(200);
        assertThat(done.body().path("mustResetPassword").asBoolean()).isTrue();
        MimeMessage mail = mails(1);
        assertThat(to(mail)).isEqualTo("forced@school.test");
        assertThat(mail.getSubject()).isEqualTo("Choose a new MySMS password");
        assertThat(iam.call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + refresh + "\"}").status())
                .as("signed out everywhere").isEqualTo(401);
        assertThat(reset(IamHttp.secretIn(GreenMailUtil.getBody(mail)), NEW_PASSWORD).status()).isEqualTo(204);
        Boolean cleared = iam.sql(() -> iam.jdbc().sql("SELECT must_reset_password FROM iam.user_account "
                + "WHERE id = :id").param("id", forced).query(Boolean.class).single());
        assertThat(cleared).as("the new password clears the requirement").isFalse();

        UUID self = iam.admin();
        assertThat(iam.call("POST", "/v1/iam/users/" + self + ":forcePasswordReset", admin, "{}").status())
                .as("never one's own account").isEqualTo(403);
        UUID helpdesk = iam.user("helpdesk", null, "iam.user_account.force_password_reset", "iam.user_account.read");
        UUID richer = iam.user("richer", "richer@school.test", "iam.user_account.delete");
        String desk = iam.token("helpdesk", IamHttp.PASSWORD);
        Reply escalation = iam.call("POST", "/v1/iam/users/" + richer + ":forcePasswordReset", desk, "{}");
        assertThat(escalation.status()).as("an account with rights the caller lacks: " + escalation.text())
                .isEqualTo(403);
        UUID mute = iam.user("mute", null);
        Reply noAddress = iam.call("POST", "/v1/iam/users/" + mute + ":forcePasswordReset", admin, "{}");
        assertThat(noAddress.status()).isEqualTo(422);
        assertThat(noAddress.firstErrorCode()).isEqualTo("NO_EMAIL");
        assertThat(iam.call("POST", "/v1/iam/users/" + UUID.randomUUID() + ":forcePasswordReset", admin, "{}")
                .status()).isEqualTo(404);
        assertThat(helpdesk).isNotNull();
        noMoreThan(1);
    }

    @Test
    @Order(5)
    void anEmailAddressIsVerifiedOnceWithAMailedCode() throws Exception {
        UUID verifier = iam.user("verifier", "verifier@school.test");
        String token = iam.token("verifier", IamHttp.PASSWORD);
        Reply mobile = iam.call("POST", "/v1/iam/auth:requestVerification", token, "{\"channel\":\"mobile\"}");
        assertThat(mobile.status()).as(mobile.text()).isEqualTo(503);
        assertThat(mobile.code()).isEqualTo("NOT_CONFIGURED");
        Reply sent = iam.call("POST", "/v1/iam/auth:requestVerification", token, "{\"channel\":\"email\"}");
        assertThat(sent.status()).as(sent.text()).isEqualTo(204);
        MimeMessage mail = mails(1);
        assertThat(mail.getSubject()).isEqualTo("Confirm your e-mail address");
        String code = IamHttp.secretIn(GreenMailUtil.getBody(mail));
        assertThat(GreenMailUtil.getBody(mail)).contains("https://app.mysms.test/app/auth/verify?channel=email&code=");

        Reply wrongChannel = iam.call("POST", "/v1/iam/auth:confirmVerification", null,
                "{\"channel\":\"mobile\",\"code\":\"" + code + "\"}");
        assertThat(wrongChannel.status()).isEqualTo(503);
        Reply confirmed = iam.call("POST", "/v1/iam/auth:confirmVerification", null,
                "{\"channel\":\"email\",\"code\":\"" + code + "\"}");
        assertThat(confirmed.status()).as(confirmed.text()).isEqualTo(200);
        assertThat(confirmed.body().path("id").asString()).isEqualTo(verifier.toString());
        assertThat(confirmed.body().hasNonNull("emailVerifiedAt")).isTrue();
        assertThat(iam.call("POST", "/v1/iam/auth:confirmVerification", null,
                "{\"channel\":\"email\",\"code\":\"" + code + "\"}").firstErrorCode()).as("single use")
                .isEqualTo("INVALID_CODE");
        assertThat(iam.call("POST", "/v1/iam/auth:requestVerification", token, "{\"channel\":\"email\"}").status())
                .as("already verified").isEqualTo(409);
        String auditFound = iam.sql(() -> iam.jdbc().sql("SELECT count(*) FROM iam.outbox_event WHERE event_type = "
                + "'iam.user_account.email_verified' AND aggregate_id = :id").param("id", verifier)
                .query(String.class).single());
        assertThat(auditFound).isEqualTo("1");

        iam.user("patient", "patient@school.test");
        String patient = iam.token("patient", IamHttp.PASSWORD);
        for (int i = 0; i < 3; i++) {
            assertThat(iam.call("POST", "/v1/iam/auth:requestVerification", patient, "{\"channel\":\"email\"}")
                    .status()).isEqualTo(204);
        }
        Reply limited = iam.call("POST", "/v1/iam/auth:requestVerification", patient, "{\"channel\":\"email\"}");
        assertThat(limited.status()).as(limited.text()).isEqualTo(429);
        assertThat(limited.code()).isEqualTo("RATE_LIMITED");
        String latest = IamHttp.secretIn(GreenMailUtil.getBody(mails(4)));
        String earlier = IamHttp.secretIn(GreenMailUtil.getBody(SMTP.getReceivedMessages()[2]));
        assertThat(iam.call("POST", "/v1/iam/auth:confirmVerification", null,
                "{\"channel\":\"email\",\"code\":\"" + earlier + "\"}").status()).as("a newer code replaces it")
                .isEqualTo(422);
        CLOCK.advance(Duration.ofHours(25));
        assertThat(iam.call("POST", "/v1/iam/auth:confirmVerification", null,
                "{\"channel\":\"email\",\"code\":\"" + latest + "\"}").firstErrorCode()).as("24-hour expiry")
                .isEqualTo("INVALID_CODE");
    }

    /**
     * Sign-up's own rules, before any school is checked; the school and the account need org (see the monolith's
     * component test): here org is not running, so a valid form answers 503 and creates nothing.
     */
    @Test
    @Order(6)
    void signUpIsClosedUntilTheTenantOpensItAndNeedsOrgForTheSchool() throws Exception {
        String form = "{\"tenantCode\":\"" + TENANT_CODE + "\",\"institutionId\":\"" + UUID.randomUUID()
                + "\",\"email\":\"Learner@School.test\",\"fullName\":\"Asha Learner\",\"acceptTerms\":true,"
                + "\"personaCategory\":\"student\"}";
        String schools = "/v1/iam/tenant-codes/" + TENANT_CODE + "/sign-up-institutions";
        Reply closed = iam.call("POST", "/v1/iam/auth:signUp", null, form);
        assertThat(closed.status()).as(closed.text()).isEqualTo(403);
        assertThat(iam.call("POST", "/v1/iam/auth:signUp", null, form.replace(TENANT_CODE, "elsewhere")).status())
                .as("an unknown tenant answers the same").isEqualTo(403);
        assertThat(iam.call("GET", schools, null, null).status()).as("no school list while closed").isEqualTo(403);
        assertThat(iam.call("GET", "/v1/iam/tenant-codes/elsewhere/sign-up-institutions", null, null).status())
                .isEqualTo(403);

        UUID tenant = iam.tenant();
        iam.sql(() -> iam.jdbc().sql("""
                INSERT INTO iam.setting_value (id, tenant_id, scope_type, setting_key, value)
                VALUES (:a, :tenant, 'tenant', 'security.selfSignUp', 'true')
                """).param("a", UUID.randomUUID()).param("tenant", tenant).update());

        Reply terms = iam.call("POST", "/v1/iam/auth:signUp", null, form.replace("\"acceptTerms\":true",
                "\"acceptTerms\":false"));
        assertThat(terms.status()).isEqualTo(422);
        assertThat(terms.firstErrorCode()).isEqualTo("TERMS_REQUIRED");
        assertThat(iam.call("POST", "/v1/iam/auth:signUp", null, form.replace("\"student\"", "\"teacher\""))
                .status()).as("only students sign up themselves").isEqualTo(422);
        Reply noSchool = iam.call("POST", "/v1/iam/auth:signUp", null,
                form.replaceAll("\"institutionId\":\"[^\"]+\",", ""));
        assertThat(noSchool.status()).as(noSchool.text()).isEqualTo(422);
        assertThat(noSchool.body().path("errors").path(0).path("field").asString()).isEqualTo("institutionId");

        Reply unchecked = iam.call("POST", "/v1/iam/auth:signUp", null, form);
        assertThat(unchecked.status()).as(unchecked.text()).isEqualTo(503);
        assertThat(unchecked.code()).isEqualTo("UNAVAILABLE");
        Integer accounts = iam.sql(() -> iam.jdbc().sql("SELECT count(*) FROM iam.user_account WHERE email = "
                + "'Learner@School.test'").query(Integer.class).single());
        assertThat(accounts).as("no account at a school nobody checked").isZero();
        noMoreThan(0);
        assertThat(iam.call("GET", schools, null, null).status()).isEqualTo(503);
    }

    @Test
    @Order(7)
    void aResetCodeEndsWithTheAddressOrCredentialsItWasMailedForAndAResetEndsApiTokens() throws Exception {
        String admin = iam.token("admin", ADMIN_PASSWORD);
        UUID mover = iam.user("mover", "mover@school.test");
        assertThat(requestReset("mover").status()).isEqualTo(204);
        String moverCode = IamHttp.secretIn(GreenMailUtil.getBody(mails(1)));
        String etag = iam.call("GET", "/v1/iam/users/" + mover, admin, null).body().path("etag").asString();
        Reply moved = iam.call("PATCH", "/v1/iam/users/" + mover, admin, "{\"email\":\"moved@school.test\"}", etag);
        assertThat(moved.status()).as(moved.text()).isEqualTo(200);
        assertThat(reset(moverCode, NEW_PASSWORD).firstErrorCode())
                .as("the code went to an address the account no longer has").isEqualTo("INVALID_CODE");

        UUID quiet = iam.user("quiet", "quiet@school.test");
        assertThat(requestReset("quiet").status()).isEqualTo(204);
        String quietCode = IamHttp.secretIn(GreenMailUtil.getBody(mails(2)));
        iam.sql(() -> iam.jdbc().sql("UPDATE iam.user_account SET email = 'elsewhere@school.test' WHERE id = :id")
                .param("id", quiet).update());
        assertThat(reset(quietCode, NEW_PASSWORD).firstErrorCode())
                .as("however the address changed, the code is refused").isEqualTo("INVALID_CODE");

        UUID helped = iam.user("helped", "helped@school.test");
        assertThat(requestReset("helped").status()).isEqualTo(204);
        String helpedCode = IamHttp.secretIn(GreenMailUtil.getBody(mails(3)));
        Reply set = iam.call("POST", "/v1/iam/users/" + helped + ":setCredentials", admin,
                "{\"temporaryPassword\":\"temporary-pass-321\",\"mustResetPassword\":false}");
        assertThat(set.status()).as(set.text()).isEqualTo(200);
        assertThat(reset(helpedCode, NEW_PASSWORD).firstErrorCode())
                .as("an administrator's new credentials end the open reset").isEqualTo("INVALID_CODE");
        assertThat(iam.signIn("helped", "temporary-pass-321").status()).isEqualTo(200);

        UUID scripted = iam.user("scripted", "scripted@school.test", "iam.user_account.create_api_token");
        Reply created = iam.call("POST", "/v1/iam/users/" + scripted + ":createApiToken",
                iam.token("scripted", IamHttp.PASSWORD), "{\"name\":\"CI\"}");
        assertThat(created.status()).as(created.text()).isEqualTo(200);
        String apiToken = created.body().path("token").asString();
        assertThat(requestReset("scripted").status()).isEqualTo(204);
        assertThat(reset(IamHttp.secretIn(GreenMailUtil.getBody(mails(4))), NEW_PASSWORD).status()).isEqualTo(204);
        assertThat(iam.call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + apiToken + "\"}")
                .status()).as("a reset signs out everywhere, API tokens included").isEqualTo(401);
    }
}
