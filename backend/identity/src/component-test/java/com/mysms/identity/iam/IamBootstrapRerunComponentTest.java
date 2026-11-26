package com.mysms.identity.iam;

import com.mysms.identity.IdentityApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.identity.iam.business.IamBootstrap;
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
 * A bootstrap that runs again over a tenant it already provisioned. The administrator role already holds every
 * permission, so no permission is added; but when the administrator account itself is missing — a previous run that
 * stopped after the role, or an account removed since — the re-run grants the role again, and that grant must raise
 * the tenant's authorisation version like any other role grant. Without it a permissions cache built on the version
 * (ROADMAP SEC-0) would never learn about the new administrator.
 */
@SpringBootTest(classes = IdentityApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IamBootstrapRerunComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String TENANT_CODE = "rerun";
    private static final String ADMIN_PASSWORD = "bootstrap-admin-secret-5";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(IamDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private IamBootstrap bootstrap;

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
    }

    @BeforeEach
    void connect() {
        iam = new IamHttp(port, database, TENANT_CODE);
    }

    private long version(UUID tenant) {
        return iam.sql(() -> iam.jdbc()
                .sql("SELECT coalesce(max(authz_version), 0) FROM iam.tenant_authz_version WHERE tenant_id = :id")
                .param("id", tenant).query(Long.class).single());
    }

    private int adminGrants() {
        return iam.sql(() -> iam.jdbc().sql("""
                SELECT count(*) FROM iam.user_role ur
                  JOIN iam.user_account ua ON ua.id = ur.user_id
                 WHERE ua.username = 'admin' AND ur.deleted_at IS NULL
                """).query(Integer.class).single());
    }

    @Test
    void aReRunThatOnlyCreatesTheAdministratorStillRaisesTheAuthorisationVersion() {
        UUID tenant = iam.tenant();
        assertThat(adminGrants()).as("the first run granted the role").isEqualTo(1);

        // the state of a run that stopped after the role: every permission is already on the role, so the re-run
        // adds none, and only the administrator and its grant are created again
        iam.sql(() -> iam.jdbc().sql("DELETE FROM iam.user_role ur USING iam.user_account ua "
                + "WHERE ua.id = ur.user_id AND ua.username = 'admin'").update());
        iam.sql(() -> iam.jdbc().sql("DELETE FROM iam.user_account WHERE username = 'admin'").update());
        assertThat(adminGrants()).isZero();
        long before = version(tenant);

        bootstrap.run(null);

        assertThat(adminGrants()).as("the administrator holds the role again").isEqualTo(1);
        assertThat(version(tenant)).as("the new role grant was published").isEqualTo(before + 1);
    }
}
