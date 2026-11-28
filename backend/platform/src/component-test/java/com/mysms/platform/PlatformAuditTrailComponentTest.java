package com.mysms.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.InboxProcessor;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.messaging.inprocess.InProcessEventBus;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.events.PlatformAuditTrail;
import com.mysms.platform.events.PlatformReferenceCleanup;
import com.mysms.platform.events.PlatformTenantSync;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Real PostgreSQL: the platform turns the modules' `audit.recorded` events into platform.audit_log rows of the
 * event's tenant, once, and its own changes reach the trail through its outbox, relay and inbox. It registers the
 * tenant iam's installation bootstrap made, and an audit record of a tenant it does not know is dead-lettered after the
 * bus's delivery limit instead of coming back forever.
 */
@SpringBootTest(classes = PlatformApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlatformAuditTrailComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.fromString("0190b0a0-0000-7000-8000-0000000a0d11");
    private static final UUID OTHER = UUID.fromString("0190b0a0-0000-7000-8000-0000000a0d12");

    @Autowired
    @Qualifier(PlatformDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private PlatformAuditTrail trail;

    @Autowired
    private PlatformReferenceCleanup cleanup;

    @Autowired
    private PlatformTenantSync registration;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "platform", "master", "org");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private <T> T as(UUID tenant, Supplier<T> work) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).read(work::get));
    }

    @Test
    void anAuditEventBecomesOneRowOfItsTenantsTrail() {
        PlatformTenants.register(database, TENANT);
        PlatformTenants.register(database, OTHER);
        UUID actor = UUID.randomUUID();
        UUID subject = UUID.randomUUID();
        EventEnvelope event = new EventEnvelope(UUID.randomUUID(), "sis", "audit.recorded", Optional.of(TENANT),
                "sis.student", subject, Instant.parse("2026-11-07T09:00:00Z"),
                "{\"action\":\"update\",\"entityType\":\"sis.student\",\"entityId\":\"" + subject + "\","
                        + "\"actorUserId\":\"" + actor + "\",\"changedFields\":[\"status\"],\"reason\":\"left\","
                        + "\"traceId\":\"" + TRACE + "\",\"occurredAt\":\"2026-11-07T08:59:59Z\"}",
                Optional.of(TRACE));
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        assertThat(inbox.deliver(event, trail::record)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
        assertThat(inbox.deliver(event, trail::record)).isEqualTo(InboxProcessor.Outcome.DUPLICATE);
        Map<String, Object> row = as(TENANT, () -> database.jdbc().sql("""
                SELECT log_name, event, subject_type, subject_id, causer_type, causer_id,
                       properties ->> 'reason' AS reason, properties -> 'changedFields' ->> 0 AS field
                  FROM platform.audit_log
                 WHERE tenant_id = :tenantId AND subject_id = :subject
                """).param("subject", subject).query().singleRow());
        assertThat(row).containsEntry("log_name", "sis").containsEntry("event", "update")
                .containsEntry("subject_type", "sis.student").containsEntry("causer_type", "user")
                .containsEntry("causer_id", actor).containsEntry("reason", "left").containsEntry("field", "status");
        assertThat(as(OTHER, () -> database.jdbc().sql("SELECT count(*) FROM platform.audit_log "
                + "WHERE tenant_id = :tenantId AND subject_id = :subject").param("subject", subject)
                .query(Integer.class).single())).as("only in its own tenant").isZero();

        // a module audits only its own entities
        EventEnvelope forged = new EventEnvelope(UUID.randomUUID(), "hr", "audit.recorded", Optional.of(TENANT),
                "hr.staff", subject, Instant.now(), "{\"action\":\"delete\",\"entityType\":\"sis.student\"}",
                Optional.empty());
        assertThatThrownBy(() -> trail.record(forged)).isInstanceOf(RejectedEventException.class);

        // global data (no tenant): stored once without a tenant, invisible to every tenant session
        UUID country = UUID.randomUUID();
        EventEnvelope global = new EventEnvelope(UUID.randomUUID(), "master", "audit.recorded", Optional.empty(),
                "master.country", country, Instant.now(),
                "{\"action\":\"create\",\"entityType\":\"master.country\"}", Optional.empty());
        trail.record(global);
        trail.record(global); // tenantless events bypass the inbox: the insert itself is idempotent
        JdbcClient bypass = DATABASE.adminJdbc();
        assertThat(bypass.sql("SELECT count(*) FROM platform.audit_log WHERE tenant_id IS NULL AND subject_id = :id")
                .param("id", country).query(Integer.class).single()).isOne();
        assertThat(as(TENANT, () -> database.jdbc().sql("SELECT count(*) FROM platform.audit_log "
                + "WHERE subject_id = :id").param("id", country).query(Integer.class).single()))
                .as("no tenant sees platform staff's audit").isZero();
        assertThat(trail.module()).isEqualTo("platform");
        assertThat(trail.handlers()).containsOnlyKeys("audit.recorded");
        assertThat(cleanup.module()).as("the platform's reference cleanup is a second subscription, merged")
                .isEqualTo(trail.module());
        assertThat(cleanup.handlers()).doesNotContainKey("audit.recorded");
    }

    private static EventEnvelope audit(UUID tenant, UUID subject) {
        return new EventEnvelope(UUID.randomUUID(), "iam", "audit.recorded", Optional.of(tenant), "iam.role", subject,
                Instant.now(), "{\"action\":\"create\",\"entityType\":\"iam.role\"}", Optional.of(TRACE));
    }

    private static EventEnvelope bootstrapped(UUID tenant, UUID named, String code) {
        return new EventEnvelope(UUID.randomUUID(), "iam", "iam.tenant_directory.bootstrapped", Optional.of(tenant),
                "iam.tenant_directory", named, Instant.now(), "{\"id\":\"" + named + "\",\"code\":\"" + code + "\"}",
                Optional.of(TRACE));
    }

    @Test
    void theBootstrapTenantIsRegisteredOnceSoItsAuditRecordsLand() {
        UUID tenant = UUID.randomUUID();
        UUID role = UUID.randomUUID();
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        assertThat(registration.handlers()).containsOnlyKeys("iam.tenant_directory.bootstrapped");

        assertThat(inbox.deliver(bootstrapped(tenant, tenant, "boot-camp"), registration::register))
                .isEqualTo(InboxProcessor.Outcome.PROCESSED);
        // a restart announces it again: nothing changes
        inbox.deliver(bootstrapped(tenant, tenant, "boot-camp"), registration::register);
        JdbcClient bypass = DATABASE.adminJdbc();
        Map<String, Object> row = bypass.sql("SELECT code, name, status, row_version FROM platform.tenant WHERE id = :id")
                .param("id", tenant).query().singleRow();
        assertThat(row).containsEntry("code", "boot-camp").containsEntry("name", "boot-camp")
                .containsEntry("status", "active").containsEntry("row_version", 1);
        assertThat(bypass.sql("SELECT event_type FROM platform.outbox_event WHERE tenant_id = :tenant "
                + "AND aggregate_id = :tenant ORDER BY id").param("tenant", tenant).query(String.class).list())
                .as("audited, and announced so iam's directory follows the platform row")
                .containsExactly("audit.recorded", "platform.tenant.created");

        assertThat(inbox.deliver(audit(tenant, role), trail::record)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
        assertThat(as(tenant, () -> database.jdbc().sql("SELECT count(*) FROM platform.audit_log "
                + "WHERE tenant_id = :tenantId AND subject_id = :id").param("id", role).query(Integer.class).single()))
                .isOne();

        // events that can never succeed are rejected, not retried
        UUID other = UUID.randomUUID();
        assertThatThrownBy(() -> inbox.deliver(bootstrapped(other, other, "boot-camp"), registration::register))
                .as("a code another tenant holds").isInstanceOf(RejectedEventException.class);
        assertThatThrownBy(() -> inbox.deliver(bootstrapped(other, tenant, "boot-camp"), registration::register))
                .as("another tenant's id").isInstanceOf(RejectedEventException.class);
        assertThatThrownBy(() -> inbox.deliver(bootstrapped(other, other, "Not A Code"), registration::register))
                .isInstanceOf(RejectedEventException.class);
        assertThat(bypass.sql("SELECT count(*) FROM platform.tenant WHERE id = :id").param("id", other)
                .query(Integer.class).single()).isZero();
    }

    @Test
    void anAuditRecordOfAnUnknownTenantIsDeadLetteredAfterTheDeliveryLimit() {
        UUID unknown = UUID.randomUUID();
        EventEnvelope event = audit(unknown, UUID.randomUUID());
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        AtomicInteger tries = new AtomicInteger();
        try (InProcessEventBus bus = new InProcessEventBus(3)) {
            bus.subscribe("platform", Set.of("audit.recorded"), e -> {
                tries.incrementAndGet();
                inbox.deliver(e, trail::record);
            });
            assertThatThrownBy(() -> bus.publish(event)).as("fk_audit_log_tenant_id: delivered again")
                    .hasMessageContaining("fk_audit_log_tenant_id");
            assertThatThrownBy(() -> bus.publish(event)).hasMessageContaining("fk_audit_log_tenant_id");
            bus.publish(event); // the third failure reaches the limit: parked, the relay goes on
            bus.publish(event);
        }
        assertThat(tries).as("never delivered past the limit").hasValue(3);
        assertThat(DATABASE.adminJdbc().sql("SELECT processing_error FROM platform.inbox_event WHERE event_id = :id")
                .param("id", event.id()).query(String.class).single())
                .as("the inbox keeps why").contains("fk_audit_log_tenant_id");
    }
}
