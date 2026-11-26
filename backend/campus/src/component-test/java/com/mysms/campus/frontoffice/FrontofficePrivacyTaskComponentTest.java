package com.mysms.campus.frontoffice;

import com.mysms.campus.CampusApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.campus.frontoffice.config.FrontofficeDatabaseConfig;
import com.mysms.campus.frontoffice.events.FrontofficePrivacyTaskHandler;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.InboxProcessor;
import com.mysms.privacyclient.testing.TestLegalHolds;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * frontoffice's privacy tasks and its frontoffice.visitor_log sweep against real PostgreSQL as the application role,
 * with privacy's legal holds from {@link TestLegalHolds} (design PRIVACY-AND-CONSENT-CENTRE §9.1, §10): an erasure is
 * not applicable (no register names a person), answered again from its receipt; the sweep erases only the visitor
 * entries before the cutoff, once per run id, and keeps and counts a held entry; every answer is on the outbox.
 */
@SpringBootTest(classes = CampusApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FrontofficePrivacyTaskComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final LocalDate JANUARY = LocalDate.of(2026, 1, 15);
    private static final LocalDate OCTOBER = LocalDate.of(2026, 10, 1);

    @Autowired
    @Qualifier(FrontofficeDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private FrontofficePrivacyTaskHandler handler;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "frontoffice", "library", "transport", "facility");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    /** A visitor entry of the date, with a phone number; the entry's id. */
    private UUID visitor(UUID tenant, LocalDate date) {
        UUID id = UUID.randomUUID();
        write(tenant, () -> database.jdbc().sql("INSERT INTO frontoffice.visitor_log (id, tenant_id, institution_id,"
                + " visitor_name, visitor_type, phone, purpose, visit_date, row_version) VALUES (:id, :tenantId,"
                + " :institution, 'Asha Rao', 'parent', '9800000000', 'Fee enquiry', :date, 1)").param("id", id)
                .param("institution", UUID.randomUUID()).param("date", date).update());
        return id;
    }

    private void privacy(UUID tenant, String type, String aggregate, UUID id, String data) {
        EventEnvelope event = new EventEnvelope(UUID.randomUUID(), "privacy", type, Optional.of(tenant), aggregate,
                id, Instant.now(), data, Optional.of(TRACE));
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        assertThat(inbox.deliver(event, handler::receive)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null), () -> {
            handler.run();
            return null;
        });
    }

    private void sweep(UUID tenant, UUID runId) {
        privacy(tenant, "privacy.retention.due", "privacy.retention_run", runId, "{\"runId\":\"" + runId
                + "\",\"policyCode\":\"frontoffice.visitor_log\",\"categoryCode\":\"contact\","
                + "\"module\":\"frontoffice\",\"cutoff\":\"2026-06-01T00:00:00Z\",\"action\":\"delete\"}");
    }

    private List<JsonNode> answers(UUID tenant, String type, UUID id) {
        return read(tenant, () -> database.jdbc().sql("SELECT payload FROM frontoffice.outbox_event WHERE tenant_id ="
                + " :tenantId AND aggregate_id = :id AND event_type = :type ORDER BY id").param("id", id)
                .param("type", type).query(String.class).list()).stream().map(JSON::readTree).toList();
    }

    private String phone(UUID tenant, UUID entry) {
        return read(tenant, () -> database.jdbc().sql("SELECT phone FROM frontoffice.visitor_log WHERE tenant_id ="
                + " :tenantId AND id = :id").param("id", entry)
                .query(String.class).list().get(0)); // one row; list() because the value may be null
    }

    private int audited(UUID tenant) {
        return read(tenant, () -> database.jdbc().sql("SELECT count(*) FROM frontoffice.outbox_event WHERE tenant_id ="
                + " :tenantId AND event_type = 'audit.recorded'").query(Integer.class).single());
    }

    @Test
    void anErasureIsNotApplicableAndAnsweredAgainFromItsReceipt() {
        UUID tenant = UUID.randomUUID();
        UUID entry = visitor(tenant, JANUARY);
        UUID taskId = UUID.randomUUID();
        String data = "{\"requestId\":\"" + UUID.randomUUID() + "\",\"taskId\":\"" + taskId + "\",\"type\":\"erasure\","
                + "\"action\":\"erase\",\"subjectPersonId\":\"" + UUID.randomUUID() + "\",\"module\":\"frontoffice\","
                + "\"wave\":2,\"deadline\":\"2027-01-31T00:00:00Z\"}";

        privacy(tenant, "privacy.rights_request.task_assigned", "privacy.rights_request_task", taskId, data);
        privacy(tenant, "privacy.rights_request.task_assigned", "privacy.rights_request_task", taskId, data);

        List<JsonNode> answers = answers(tenant, "frontoffice.privacy_task.completed", taskId);
        assertThat(answers).hasSize(2).allSatisfy(answer -> assertThat(answer).isEqualTo(answers.get(0)));
        assertThat(answers.get(0).path("status").asString()).isEqualTo("not_applicable");
        assertThat(phone(tenant, entry)).isEqualTo("9800000000");
    }

    @Test
    void theVisitorLogSweepErasesOnlyEntriesBeforeTheCutoffOncePerRunAndKeepsHeldOnes() {
        UUID tenant = UUID.randomUUID();
        UUID old = visitor(tenant, JANUARY);
        UUID held = visitor(tenant, JANUARY);
        TestLegalHolds.holdSubject(held);
        UUID recent = visitor(tenant, OCTOBER);
        UUID runId = UUID.randomUUID();

        sweep(tenant, runId);

        assertThat(phone(tenant, old)).isNull();
        assertThat(read(tenant, () -> database.jdbc().sql("SELECT visitor_name = 'Erased' AND purpose = 'Erased'"
                + " AND deleted_at IS NOT NULL AND visit_date = :date FROM frontoffice.visitor_log"
                + " WHERE tenant_id = :tenantId AND id = :id").param("id", old).param("date", JANUARY)
                .query(Boolean.class).single())).as("the visitor goes, the dated entry stays soft-deleted").isTrue();
        assertThat(phone(tenant, held)).isEqualTo("9800000000");
        assertThat(phone(tenant, recent)).isEqualTo("9800000000");
        JsonNode answer = answers(tenant, "frontoffice.privacy_retention.completed", runId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("partial");
        assertThat(answer.path("affected").asInt()).isEqualTo(1);
        assertThat(answer.path("held").asInt()).isEqualTo(1);
        int audits = audited(tenant);

        sweep(tenant, runId);

        assertThat(answers(tenant, "frontoffice.privacy_retention.completed", runId)).hasSize(2)
                .allSatisfy(again -> assertThat(again).isEqualTo(answer));
        assertThat(audited(tenant)).as("nothing is erased twice").isEqualTo(audits);
    }

    private void write(UUID tenant, Supplier<Object> work) {
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).write(work::get));
    }

    private <T> T read(UUID tenant, Supplier<T> work) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).read(work::get));
    }
}
