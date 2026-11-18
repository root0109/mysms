package com.mysms.payroll;

import com.mysms.authclient.testing.CrudContract;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.payroll.config.PayrollDatabaseConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Runs the {@link CrudContract} checks for each resource of crud-spec.json over HTTP, against real PostgreSQL as the
 * non-owner role (row-level security on).
 */
@SpringBootTest(classes = PayrollApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PayrollCrudComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(PayrollDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "payroll");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @TestFactory
    List<DynamicNode> standardOperations() {
        return new CrudContract(TOKENS, () -> port, this::fixtures).tests("crud-spec.json");
    }

    @TestFactory
    List<DynamicNode> longRunningOperations() {
        return new CrudContract(TOKENS, () -> port, this::fixtures).operationTests("payroll");
    }

    /** Fixture rows, inserted as the application role inside a transaction bound to the tenant. */
    private void fixtures(UUID tenant, List<String> statements) {
        CallContext context = CallContext.anonymous(TRACE).withIdentity(tenant, null);
        CallContextHolder.callAs(context, () -> TestTransactions.of(database).write(() -> {
            statements.forEach(statement -> database.jdbc().sql(statement).param("tenant", tenant).update());
            return null;
        }));
    }
}
