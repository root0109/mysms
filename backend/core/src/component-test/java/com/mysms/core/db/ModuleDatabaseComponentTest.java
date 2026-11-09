package com.mysms.core.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.testkit.TestDatabase;
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
                null);
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
        db.tx().write(() -> {
            insert("a-1");
            return null;
        });
        actAs(TENANT_B);
        db.tx().write(() -> {
            insert("b-1");
            return null;
        });
        assertThat(db.tx().read(ModuleDatabaseComponentTest::names)).containsExactly("b-1");
        actAs(TENANT_A);
        assertThat(db.tx().read(ModuleDatabaseComponentTest::names)).containsExactly("a-1");
        // writing another tenant's row is rejected by the policy, whatever the SQL says
        assertThatThrownBy(() -> db.tx().write(() -> db.jdbc()
                .sql("INSERT INTO coretest.item (id, tenant_id, name) VALUES (:id, :tenant, 'x')")
                .param("id", UUID.randomUUID()).param("tenant", TENANT_B).update()))
                .rootCause().hasMessageContaining("row-level security");
        // outside the executor no tenant is bound: the read fails closed (no rows, or an error on a pooled
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
        db.tx().write(() -> {
            insert("explicit-a");
            return null;
        });
        String sql = "SELECT name FROM coretest.item WHERE tenant_id = :tenantId AND name LIKE 'explicit%'";
        assertThat(db.tx().read(() -> db.jdbc().sql(sql).query(String.class).list())).containsExactly("explicit-a");
        // the call's tenant is what gets bound, so a statement can only ever filter its own tenant
        actAs(TENANT_B);
        assertThat(db.tx().read(() -> db.jdbc().sql(sql).query(String.class).list())).isEmpty();
        // with no tenant bound the parameter stays unbound: the statement fails instead of running unfiltered
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736"));
        assertThatThrownBy(() -> db.jdbc().sql(sql).query(String.class).list()).hasMessageContaining("tenantId");
        assertThat(TenantJdbcClient.currentTenant()).isEmpty();
    }

    @Test
    void rollsBackOnFailureAndRejectsWritesInReadOnlyTransactions() {
        actAs(UUID.randomUUID());
        assertThatThrownBy(() -> db.tx().write(() -> {
            insert("lost");
            throw new IllegalStateException("business rule failed");
        })).hasMessage("business rule failed");
        assertThat(db.tx().read(ModuleDatabaseComponentTest::names)).isEmpty();
        assertThatThrownBy(() -> db.tx().read(() -> {
            insert("read-only");
            return null;
        })).hasMessageContaining("read-only");
        // nested calls join the outer transaction
        db.tx().write(() -> db.tx().write(() -> {
            insert("nested");
            return null;
        }));
        assertThat(db.tx().read(ModuleDatabaseComponentTest::names)).containsExactly("nested");
    }

    @Test
    void refusesTenantScopedWorkWithoutATenant() {
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736"));
        assertThatThrownBy(() -> db.tx().read(() -> 1)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("without a tenant");
    }

    @Test
    void readsWithoutATenantSeeNoTenantRowsAndCannotWrite() {
        actAs(TENANT_C);
        db.tx().write(() -> {
            insert("visible-only-to-c");
            return null;
        });
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736"));
        // fails closed: a connection that never had a tenant sees no rows; one whose earlier transaction set a tenant
        // has an empty app.tenant_id, which the policy cannot cast, so the read errors — never another tenant's rows
        List<String> seen;
        try {
            seen = db.tx().readWithoutTenant(ModuleDatabaseComponentTest::names);
        } catch (org.springframework.dao.DataAccessException e) {
            seen = List.of();
        }
        assertThat(seen).isEmpty();
        assertThat(db.tx().readWithoutTenant(() -> db.jdbc().sql("SELECT 1").query(Integer.class).single())).isEqualTo(1);
        assertThatThrownBy(() -> db.tx().readWithoutTenant(() -> {
            insert("x");
            return null;
        })).isInstanceOf(RuntimeException.class);
    }

    @Test
    void writesWithoutATenantFailClosedOnTenantTables() {
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736"));
        assertThat(db.tx().writeWithoutTenant(() -> db.jdbc().sql("SELECT 2").query(Integer.class).single())).isEqualTo(2);
        assertThatThrownBy(() -> db.tx().writeWithoutTenant(() -> {
            insert("no-tenant");
            return null;
        })).isInstanceOf(RuntimeException.class);

        // a tenant caller's tenantless write runs without the tenant (so its events are global), then gets it back
        UUID caller = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736").withIdentity(caller, user));
        CallContext inside = db.tx().writeWithoutTenant(CallContextHolder::require);
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
        db.tx().write(() -> {
            outbox.publish("coretest.item.created", "coretest.item", id, Map.of("id", id.toString()));
            audit.recordCreate("coretest.item", id);
            audit.recordUpdate("coretest.item", id, List.of("name"));
            audit.recordTransition("coretest.item", id, "cancel", List.of("status"), "duplicate entry");
            audit.recordDelete("coretest.item", id);
            return null;
        });
        List<String> types = db.tx().read(() -> db.jdbc()
                .sql("SELECT event_type FROM coretest.outbox_event WHERE aggregate_id = :id ORDER BY id")
                .param("id", id).query(String.class).list());
        assertThat(types).containsExactly("coretest.item.created", "audit.recorded", "audit.recorded", "audit.recorded",
                "audit.recorded");
        String step = db.tx().read(() -> db.jdbc()
                .sql("SELECT payload FROM coretest.outbox_event WHERE aggregate_id = :id AND payload LIKE '%cancel%'")
                .param("id", id).query(String.class).single());
        assertThat(step).contains("\"action\":\"cancel\"", "\"reason\":\"duplicate entry\"",
                "\"changedFields\":[\"status\"]");
        String payload = db.tx().read(() -> db.jdbc()
                .sql("SELECT payload FROM coretest.outbox_event WHERE aggregate_id = :id AND payload LIKE '%update%'")
                .param("id", id).query(String.class).single());
        assertThat(payload).contains("\"changedFields\":[\"name\"]", "\"actorUserId\":\"" + USER + "\"");
        assertThatThrownBy(() -> new OutboxWriter("bad;schema", db.jdbc(), JsonMapper.builder().build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void settingsValidateAndDefault() {
        ModuleDatabaseSettings settings = new ModuleDatabaseSettings("jdbc:x", "u", null, null, null, null, " ", null,
                " ", null);
        assertThat(settings.relayUsername()).as("a blank relay role is none").isNull();
        assertThat(settings.migrateUsername()).isNull();
        assertThat(settings.separateMigrationRole()).isFalse();
        assertThat(new ModuleDatabaseSettings("jdbc:x", "u", null, null, null, null, "u", null, null, null)
                .separateMigrationRole()).isFalse();
        assertThat(settings.maximumPoolSize()).isEqualTo(10);
        assertThat(settings.transactionTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(settings.migrate()).isTrue();
        assertThatThrownBy(() -> new ModuleDatabaseSettings(" ", "u", null, null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ModuleDatabaseSettings("jdbc:x", null, null, null, null, null, null, null, null,
                null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ModuleDatabase.open("Bad", settings, true)).isInstanceOf(IllegalArgumentException.class);
        ModuleDatabaseSettings badRole = new ModuleDatabaseSettings(database.jdbcUrl(), "bad;role", "x", null, null,
                null, database.migrateUsername(), database.migratePassword(), null, null);
        assertThatThrownBy(() -> ModuleDatabase.migrate("coretest", db.dataSource(), badRole))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ModuleDatabase.open("nothing", new MockEnvironment(), true))
                .isInstanceOf(IllegalStateException.class);
    }
}
