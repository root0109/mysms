package com.mysms.identity.iam;

import com.mysms.identity.IdentityApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.identity.iam.IamHttp.Reply;
import com.mysms.identity.iam.config.IamDatabaseConfig;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * A deployment without SMTP settings: the operations that must send mail answer 503 NOT_CONFIGURED and write nothing,
 * while a password-reset request answers 204 as always (no account enumeration) and only logs the problem.
 */
@SpringBootTest(classes = IdentityApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IamNoMailComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String TENANT_CODE = "nomail";
    private static final String ADMIN_PASSWORD = "bootstrap-admin-secret-4";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(IamDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    private IamHttp iam;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "iam", "people");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.iam.token.signing-key", TOKENS::privateKeyPem);
        registry.add("mysms.iam.bootstrap.tenant-code", () -> TENANT_CODE);
        registry.add("mysms.iam.bootstrap.admin-username", () -> "admin");
        registry.add("mysms.iam.bootstrap.admin-password", () -> ADMIN_PASSWORD);
        registry.add("mysms.iam.smtp.host", () -> "");
    }

    @BeforeEach
    void connect() {
        iam = new IamHttp(port, database, TENANT_CODE);
    }

    private int secrets(UUID user) {
        return iam.sql(() -> iam.jdbc().sql("SELECT count(*) FROM iam.auth_token WHERE user_id = :user "
                + "AND token_type IN ('password_reset', 'email_verification')").param("user", user)
                .query(Integer.class).single());
    }

    @Test
    void mailingOperationsAnswerNotConfiguredAndAResetRequestAnswersAsAlways() throws Exception {
        UUID user = iam.user("plain", "plain@school.test");
        Reply reset = iam.call("POST", "/v1/iam/auth:requestPasswordReset", null,
                "{\"tenantCode\":\"" + TENANT_CODE + "\",\"identifier\":\"plain\"}");
        assertThat(reset.status()).as(reset.text()).isEqualTo(204);
        assertThat(secrets(user)).as("no code that could never arrive").isZero();

        String admin = iam.token("admin", ADMIN_PASSWORD);
        Reply forced = iam.call("POST", "/v1/iam/users/" + user + ":forcePasswordReset", admin, "{}");
        assertThat(forced.status()).as(forced.text()).isEqualTo(503);
        assertThat(forced.code()).isEqualTo("NOT_CONFIGURED");
        Boolean flagged = iam.sql(() -> iam.jdbc().sql("SELECT must_reset_password FROM iam.user_account WHERE id = :id")
                .param("id", user).query(Boolean.class).single());
        assertThat(flagged).as("nothing was changed").isFalse();

        String own = iam.token("plain", IamHttp.PASSWORD);
        Reply verify = iam.call("POST", "/v1/iam/auth:requestVerification", own, "{\"channel\":\"email\"}");
        assertThat(verify.status()).isEqualTo(503);
        assertThat(verify.code()).isEqualTo("NOT_CONFIGURED");
        assertThat(secrets(user)).isZero();

        UUID tenant = iam.tenant();
        iam.sql(() -> iam.jdbc().sql("INSERT INTO iam.setting_value (id, tenant_id, scope_type, setting_key, value) "
                + "VALUES (:id, :tenant, 'tenant', 'security.selfSignUp', 'true')").param("id", UUID.randomUUID())
                .param("tenant", tenant).update());
        Reply signUp = iam.call("POST", "/v1/iam/auth:signUp", null, "{\"tenantCode\":\"" + TENANT_CODE
                + "\",\"institutionId\":\"" + UUID.randomUUID() + "\",\"email\":\"new@school.test\","
                + "\"fullName\":\"New Student\",\"acceptTerms\":true}");
        assertThat(signUp.status()).as(signUp.text()).isEqualTo(503);
        Integer accounts = iam.sql(() -> iam.jdbc().sql("SELECT count(*) FROM iam.user_account WHERE email = "
                + "'new@school.test'").query(Integer.class).single());
        assertThat(accounts).as("no account without its code").isZero();
    }
}
