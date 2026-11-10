package com.mysms.core.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.json.JsonMapper;

/** Real PostgreSQL, real Flyway, a non-superuser role: tenancy, rollback and outbox behave as in production. */
class ModuleDatabaseComponentTest {

    private static final UUID TENANT_A = UUID.fromString("0190b0a0-0000-7000-8000-00000000000a");
    private static final UUID TENANT_B = UUID.fromString("0190b0a0-0000-7000-8000-00000000000b");
    private static final UUID TENANT_C = UUID.fromString("0190b0a0-0000-7000-8000-00000000000c");
    private static final UUID USER = UUID.fromString("0190b0a0-0000-7000-8000-0000000000ee");

    private static TestDatabase database;
    private static ModuleDatabase db;

    @BeforeAll
    static void open() {
        database = TestDatabase.create();
        MockEnvironment environment = new MockEnvironment()
                .withProperty("mysms.coretest.datasource.url", database.jdbcUrl())
                .withProperty("mysms.coretest.datasource.username", database.username())
                .withProperty("mysms.coretest.datasource.password", database.password())
                .withProperty("mysms.coretest.datasource.migrate-username", database.migrateUsername())
                .withProperty("mysms.coretest.datasource.migrate-password", database.migratePassword())
                .withProperty("mysms.coretest.datasource.transaction-timeout", "10s");
        db = ModuleDatabase.open("coretest", environment, true);
    }

    @AfterAll
    static void close() {
        db.close();
        database.close();
    }

    @AfterEach
    void clearContext() {
        CallContextHolder.clear();
    }

    private static void actAs(UUID tenant) {
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736").withIdentity(tenant, USER));
    }

    private static void insert(String name) {
        db.jdbc().sql("INSERT INTO coretest.item (id, tenant_id, name) VALUES (:id, :tenant, :name)")
                .param("id", UUID.randomUUID())
                .param("tenant", CallContextHolder.require().tenantId().orElseThrow())
                .param("name", name)
                .update();
    }

    private static List<String> names() {
        return db.jdbc().sql("SELECT name FROM coretest.item ORDER BY name").query(String.class).list();
    }

    @Test
    void migratesItsOwnSchemaOnceWithItsOwnHistory() {
        assertThat(db.module()).isEqualTo("coretest");
        assertThat(db.dataSource()).isNotNull();
        assertThat(db.transactionManager()).isNotNull();
        Integer tables = db.jdbc().sql("SELECT count(*) FROM information_schema.tables WHERE table_schema = 'coretest'")
                .query(Integer.class).single();
        assertThat(tables).isEqualTo(4); // item, outbox_event, idempotency_key, inbox_event
        ModuleDatabaseSettings owner = new ModuleDatabaseSettings(database.jdbcUrl(), database.username(),
                database.password(), null, null, null, database.migrateUsername(), database.migratePassword(), null,
                null, null, null, null, null);
        assertThat(ModuleDatabase.migrate("coretest", db.dataSource(), owner).migrationsExecuted).isZero();
        assertThat(new ModuleDatabaseHealthIndicator(db).health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void theApplicationRoleOwnsNothingAndCannotDisableRowLevelSecurity() {
        assertThatThrownBy(() -> db.jdbc().sql("ALTER TABLE coretest.item NO FORCE ROW LEVEL SECURITY").update())
                .rootCause().hasMessageContaining("must be owner");
        assertThatThrownBy(() -> db.jdbc().sql("SELECT count(*) FROM coretest.flyway_schema_history")
                .query(Integer.class).single()).rootCause().hasMessageContaining("permission denied");
        String owner = db.jdbc().sql("SELECT pg_get_userbyid(nspowner) FROM pg_namespace WHERE nspname = 'coretest'")
                .query(String.class).single();
        assertThat(owner).isEqualTo(database.migrateUsername()).isNotEqualTo(database.username());
    }

    @Test
    void rowLevelSecurityIsolatesTenants() {
        actAs(TENANT_A);
        TestTransactions.of(db).write(() -> {
            insert("a-1");
            return null;
        });
        actAs(TENANT_B);
        TestTransactions.of(db).write(() -> {
            insert("b-1");
            return null;
        });
        assertThat(TestTransactions.of(db).read(ModuleDatabaseComponentTest::names)).containsExactly("b-1");
        actAs(TENANT_A);
        assertThat(TestTransactions.of(db).read(ModuleDatabaseComponentTest::names)).containsExactly("a-1");
        // writing another tenant's row is rejected by the policy, whatever the SQL says
        assertThatThrownBy(() -> TestTransactions.of(db).write(() -> db.jdbc()
                .sql("INSERT INTO coretest.item (id, tenant_id, name) VALUES (:id, :tenant, 'x')")
                .param("id", UUID.randomUUID()).param("tenant", TENANT_B).update()))
                .rootCause().hasMessageContaining("row-level security");
        // without bindTenant() no tenant is bound: the read fails closed (no rows, or an error on a pooled
        // connection whose transaction-local tenant was reset to '') — never another tenant's rows
        Throwable failure = org.assertj.core.api.Assertions.catchThrowable(() -> assertThat(
                db.jdbc().sql("SELECT count(*) FROM coretest.item").query(Integer.class).single()).isZero());
        if (failure != null) {
            assertThat(failure).hasMessageContaining("invalid input syntax for type uuid");
        }
    }

    @Test
    void statementsFilterTheBoundTenantThemselves() {
        actAs(TENANT_A);
        TestTransactions.of(db).write(() -> {
            insert("explicit-a");
            return null;
        });
        String sql = "SELECT name FROM coretest.item WHERE tenant_id = :tenantId AND name LIKE 'explicit%'";
        assertThat(TestTransactions.of(db).read(() -> db.jdbc().sql(sql).query(String.class).list()))
                .containsExactly("explicit-a");
        // the call's tenant is what gets bound, so a statement can only ever filter its own tenant
        actAs(TENANT_B);
        assertThat(TestTransactions.of(db).read(() -> db.jdbc().sql(sql).query(String.class).list())).isEmpty();
        // with no tenant bound the parameter stays unbound: the statement fails instead of running unfiltered
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736"));
        assertThatThrownBy(() -> db.jdbc().sql(sql).query(String.class).list()).hasMessageContaining("tenantId");
        assertThat(TenantJdbcClient.currentTenant()).isEmpty();
    }

    @Test
    void rollsBackOnFailureAndRejectsWritesInReadOnlyTransactions() {
        actAs(UUID.randomUUID());
        assertThatThrownBy(() -> TestTransactions.of(db).write(() -> {
            insert("lost");
            throw new IllegalStateException("business rule failed");
        })).hasMessage("business rule failed");
        assertThat(TestTransactions.of(db).read(ModuleDatabaseComponentTest::names)).isEmpty();
        assertThatThrownBy(() -> TestTransactions.of(db).read(() -> {
            insert("read-only");
            return null;
        })).hasMessageContaining("read-only");
        // nested calls join the outer transaction
        TestTransactions.of(db).write(() -> TestTransactions.of(db).write(() -> {
            insert("nested");
            return null;
        }));
        assertThat(TestTransactions.of(db).read(ModuleDatabaseComponentTest::names)).containsExactly("nested");
    }

    @Test
    void refusesTenantScopedWorkWithoutATenant() {
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736"));
        assertThatThrownBy(() -> TestTransactions.of(db).read(() -> 1)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("without a tenant");
    }

    @Test
    void readsWithoutATenantSeeNoTenantRowsAndCannotWrite() {
        actAs(TENANT_C);
        TestTransactions.of(db).write(() -> {
            insert("visible-only-to-c");
            return null;
        });
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736"));
        // fails closed: a connection that never had a tenant sees no rows; one whose earlier transaction set a tenant
        // has an empty app.tenant_id, which the policy cannot cast, so the read errors — never another tenant's rows
        List<String> seen;
        try {
            seen = TestTransactions.of(db).readWithoutTenant(ModuleDatabaseComponentTest::names);
        } catch (org.springframework.dao.DataAccessException e) {
            seen = List.of();
        }
        assertThat(seen).isEmpty();
        assertThat(TestTransactions.of(db).readWithoutTenant(() -> db.jdbc().sql("SELECT 1").query(Integer.class)
                .single())).isEqualTo(1);
        assertThatThrownBy(() -> TestTransactions.of(db).readWithoutTenant(() -> {
            insert("x");
            return null;
        })).isInstanceOf(RuntimeException.class);
    }

    @Test
    void writesWithoutATenantFailClosedOnTenantTables() {
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736"));
        assertThat(TestTransactions.of(db).writeWithoutTenant(() -> db.jdbc().sql("SELECT 2").query(Integer.class)
                .single())).isEqualTo(2);
        assertThatThrownBy(() -> TestTransactions.of(db).writeWithoutTenant(() -> {
            insert("no-tenant");
            return null;
        })).isInstanceOf(RuntimeException.class);

        // a tenant caller's tenantless write runs without the tenant (so its events are global), then gets it back
        UUID caller = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736").withIdentity(caller, user));
        CallContext inside = TestTransactions.of(db).writeWithoutTenant(CallContextHolder::require);
        assertThat(inside.tenantId()).isEmpty();
        assertThat(inside.userId()).contains(user);
        assertThat(inside.traceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(CallContextHolder.require().tenantId()).contains(caller);
    }

    @Test
    void callAsBindsAndRestoresTheContext() {
        CallContext outer = CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736");
        CallContextHolder.set(outer);
        UUID inner = CallContextHolder.callAs(outer.withIdentity(TENANT_B, USER),
                () -> CallContextHolder.require().tenantId().orElseThrow());
        assertThat(inner).isEqualTo(TENANT_B);
        assertThat(CallContextHolder.require()).isSameAs(outer);
        CallContextHolder.clear();
        assertThatThrownBy(() -> CallContextHolder.callAs(outer, () -> {
            throw new IllegalStateException("fails");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(CallContextHolder.current()).isEmpty();
    }

    @Test
    void outboxAndAuditRowsCommitWithTheChange() {
        actAs(UUID.randomUUID());
        OutboxWriter outbox = new OutboxWriter("coretest", db.jdbc(), JsonMapper.builder().build());
        AuditWriter audit = new AuditWriter(outbox, Clock.systemUTC());
        UUID id = UUID.randomUUID();
        TestTransactions.of(db).write(() -> {
            outbox.publish("coretest.item.created", "coretest.item", id, Map.of("id", id.toString()));
            audit.recordCreate("coretest.item", id);
            audit.recordUpdate("coretest.item", id, List.of("name"));
            audit.recordTransition("coretest.item", id, "cancel", List.of("status"), "duplicate entry");
            audit.recordDelete("coretest.item", id);
            return null;
        });
        List<String> types = TestTransactions.of(db).read(() -> db.jdbc()
                .sql("SELECT event_type FROM coretest.outbox_event WHERE aggregate_id = :id ORDER BY id")
                .param("id", id).query(String.class).list());
        assertThat(types).containsExactly("coretest.item.created", "audit.recorded", "audit.recorded", "audit.recorded",
                "audit.recorded");
        String step = TestTransactions.of(db).read(() -> db.jdbc()
                .sql("SELECT payload FROM coretest.outbox_event WHERE aggregate_id = :id AND payload LIKE '%cancel%'")
                .param("id", id).query(String.class).single());
        assertThat(step).contains("\"action\":\"cancel\"", "\"reason\":\"duplicate entry\"",
                "\"changedFields\":[\"status\"]");
        String payload = TestTransactions.of(db).read(() -> db.jdbc()
                .sql("SELECT payload FROM coretest.outbox_event WHERE aggregate_id = :id AND payload LIKE '%update%'")
                .param("id", id).query(String.class).single());
        assertThat(payload).contains("\"changedFields\":[\"name\"]", "\"actorUserId\":\"" + USER + "\"");
        assertThatThrownBy(() -> new OutboxWriter("bad;schema", db.jdbc(), JsonMapper.builder().build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** A service of the module, as the modules write them: it only hands its work to the base class. */
    private static final class Work extends AppTransactionManager {

        Work(ModuleDatabase database) {
            super(database, database.transactionManager());
        }

        String readOn(ModuleDatabase database) {
            return doInReadTransaction(() -> session(database));
        }

        String primaryReadOn(ModuleDatabase database) {
            return doInPrimaryReadTransaction(() -> session(database));
        }

        String writeOn(ModuleDatabase database) {
            return doInWriteTransaction(() -> session(database));
        }

        /** A write the read-only transaction carries out: the replica's connection refuses it. */
        int writeInsideReadOn(ModuleDatabase database) {
            return doInReadTransaction(() -> database.jdbc().sql(
                    "INSERT INTO coretest.item (id, tenant_id, name) VALUES (gen_random_uuid(), :tenantId, 'x')")
                    .update());
        }

        /** A read inside a write joins it and stays on the primary. */
        String readInsideWriteOn(ModuleDatabase database) {
            return doInWriteTransaction(() -> readOn(database));
        }

        private static String session(ModuleDatabase database) {
            return database.jdbc().sql("SELECT current_setting('application_name')").query(String.class).single();
        }
    }

    @Test
    void readOnlyTransactionsReadFromTheReplicaAndEveryWriteGoesToThePrimary() {
        // the replica here is the same database behind a pool of its own: what is checked is which pool answers
        MockEnvironment environment = new MockEnvironment()
                .withProperty("mysms.coretest.datasource.url", database.jdbcUrl())
                .withProperty("mysms.coretest.datasource.username", database.username())
                .withProperty("mysms.coretest.datasource.password", database.password())
                .withProperty("mysms.coretest.datasource.migrate", "false")
                .withProperty("mysms.coretest.datasource.replica-url", database.jdbcUrl());
        try (ModuleDatabase routed = ModuleDatabase.open("coretest", environment, true)) {
            actAs(TENANT_A);
            Work work = new Work(routed);
            assertThat(routed.hasReplica()).isTrue();
            assertThat(work.readOn(routed)).isEqualTo("coretest-replica");
            assertThat(work.writeOn(routed)).isEqualTo("coretest");
            assertThat(work.readInsideWriteOn(routed)).as("joined, on the primary").isEqualTo("coretest");
            assertThat(work.primaryReadOn(routed)).as("a read that must see the latest state").isEqualTo("coretest");
            assertThat(DatabaseRoleHolder.current()).as("nothing is left on the thread").isEmpty();
            assertThat(routed.isReachable(DatabaseRole.PRIMARY)).isTrue();
            assertThat(routed.isReachable(DatabaseRole.REPLICA)).isTrue();
            assertThat(new ModuleDatabaseHealthIndicator(routed).health().getDetails()).containsEntry("replica", "UP");
            // the replica's connections refuse writes themselves, whatever the transaction says
            assertThatThrownBy(() -> work.writeInsideReadOn(routed)).hasMessageContaining("read-only");
        }
        // without a replica every transaction is on the primary
        actAs(TENANT_A);
        assertThat(db.hasReplica()).isFalse();
        assertThat(new Work(db).readOn(db)).isEqualTo("coretest");
        // a replica that does not answer neither stops the module nor its reads: the primary serves them
        environment.setProperty("mysms.coretest.datasource.replica-url", "jdbc:postgresql://127.0.0.1:1/none");
        try (ModuleDatabase unreachable = ModuleDatabase.open("coretest", environment, true)) {
            actAs(TENANT_A);
            assertThat(new Work(unreachable).readOn(unreachable)).isEqualTo("coretest");
            assertThat(new ModuleDatabaseHealthIndicator(unreachable).health().getStatus().getCode()).isEqualTo("UP");
            assertThat(new ModuleDatabaseHealthIndicator(unreachable).health().getDetails())
                    .containsEntry("replica", "DOWN");
        }
    }

    @Test
    void settingsValidateAndDefault() {
        ModuleDatabaseSettings settings = new ModuleDatabaseSettings("jdbc:x", "u", null, null, null, null, " ", null,
                " ", null, null, null, null, null);
        assertThat(settings.relayUsername()).as("a blank relay role is none").isNull();
        assertThat(settings.migrateUsername()).isNull();
        assertThat(settings.separateMigrationRole()).isFalse();
        assertThat(new ModuleDatabaseSettings("jdbc:x", "u", null, null, null, null, "u", null, null, null, null, null,
                null, null).separateMigrationRole()).isFalse();
        assertThat(settings.maximumPoolSize()).isEqualTo(10);
        assertThat(settings.hasReplica()).as("no replica unless one is named").isFalse();
        ModuleDatabaseSettings replicated = new ModuleDatabaseSettings("jdbc:x", "u", "p", null, null, null, null,
                null, null, null, null, "jdbc:r", " ", null);
        assertThat(replicated.hasReplica()).isTrue();
        assertThat(replicated.replicaUsername()).as("the application role by default").isEqualTo("u");
        assertThat(replicated.replicaPassword()).isEqualTo("p");
        assertThat(settings.transactionTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(settings.slowStatement()).isEqualTo(Duration.ofMillis(200));
        assertThat(settings.migrate()).isTrue();
        assertThatThrownBy(() -> new ModuleDatabaseSettings(" ", "u", null, null, null, null, null, null, null, null,
                null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ModuleDatabaseSettings("jdbc:x", null, null, null, null, null, null, null, null,
                null, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ModuleDatabase.open("Bad", settings, true)).isInstanceOf(IllegalArgumentException.class);
        ModuleDatabaseSettings badRole = new ModuleDatabaseSettings(database.jdbcUrl(), "bad;role", "x", null, null,
                null, database.migrateUsername(), database.migratePassword(), null, null, null, null, null, null);
        assertThatThrownBy(() -> ModuleDatabase.migrate("coretest", db.dataSource(), badRole))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ModuleDatabase.open("nothing", new MockEnvironment(), true))
                .isInstanceOf(IllegalStateException.class);
    }
}
