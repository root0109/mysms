package com.mysms.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.InboxProcessor;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.events.PlatformAuditTrail;
import com.mysms.platform.events.PlatformReferenceCleanup;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
 * event's tenant, once, and its own changes reach the trail through its outbox, relay and inbox.
 */
@SpringBootTest(classes = PlatformApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.name=" + PlatformApplication.CONFIG_NAME)
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

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "platform");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private <T> T as(UUID tenant, Supplier<T> work) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> database.tx().read(work::get));
    }

    private void tenant(UUID tenant) {
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null), () -> database.tx().write(
                () -> database.jdbc().sql("INSERT INTO platform.tenant (id, code, name) VALUES (:tenant, "
                        + "'t' || substr(md5(CAST(:tenant AS text)), 1, 12), 'Test tenant') ON CONFLICT (id) DO NOTHING")
                        .param("tenant", tenant).update()));
    }

    @Test
    void anAuditEventBecomesOneRowOfItsTenantsTrail() {
        tenant(TENANT);
        tenant(OTHER);
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
}
