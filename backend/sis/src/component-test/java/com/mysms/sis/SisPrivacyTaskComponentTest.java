package com.mysms.sis;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.InboxProcessor;
import com.mysms.privacyclient.testing.TestLegalHolds;
import com.mysms.sis.config.SisDatabaseConfig;
import com.mysms.sis.events.SisPrivacyTaskHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
 * sis's privacy tasks and retention sweep against real PostgreSQL as the application role, with privacy's legal holds
 * from {@link TestLegalHolds} (design PRIVACY-AND-CONSENT-CENTRE §9.1, §10): an erasure clears a student's health
 * record once per task id and keeps the admission register; a held subject's rows are kept and counted;
 * sis.health_after_leaving clears the health records of students who left before the cutoff only; every answer is on
 * the outbox.
 */
@SpringBootTest(classes = SisApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SisPrivacyTaskComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    @Autowired
    @Qualifier(SisDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private SisPrivacyTaskHandler handler;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "sis");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    /** A student who left on {@code exitDate} (null: still at school), with a health record; the person's id. */
    private UUID student(UUID tenant, LocalDate exitDate) {
        UUID person = UUID.randomUUID();
        UUID student = UUID.randomUUID();
        write(tenant, () -> {
            database.jdbc().sql("INSERT INTO sis.student (id, tenant_id, person_id, institution_id, admission_number,"
                    + " exit_date, exit_reason, row_version) VALUES (:id, :tenantId, :person, :institution, :number,"
                    + " :exitDate, 'Family moved', 1)").param("id", student).param("person", person)
                    .param("institution", UUID.randomUUID()).param("number", "A-" + student.toString().substring(0, 8))
                    .param("exitDate", exitDate).update();
            database.jdbc().sql("INSERT INTO sis.student_health_record (id, tenant_id, student_id, medical_conditions,"
                    + " row_version) VALUES (:id, :tenantId, :student, 'Asthma', 1)").param("id", UUID.randomUUID())
                    .param("student", student).update();
            return null;
        });
        return person;
    }

    private void deliver(UUID tenant, String type, String aggregate, UUID id, String data) {
        EventEnvelope event = new EventEnvelope(UUID.randomUUID(), "privacy", type, Optional.of(tenant), aggregate, id,
                Instant.now(), data, Optional.of(TRACE));
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        assertThat(inbox.deliver(event, handler::receive)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null), () -> {
            handler.run();
            return null;
        });
    }

    private void erase(UUID tenant, UUID taskId, UUID subject) {
        deliver(tenant, "privacy.rights_request.task_assigned", "privacy.rights_request_task", taskId,
                "{\"requestId\":\"" + UUID.randomUUID() + "\",\"taskId\":\"" + taskId + "\",\"type\":\"erasure\","
                + "\"action\":\"erase\",\"subjectPersonId\":\"" + subject + "\",\"module\":\"sis\",\"wave\":2,"
                + "\"deadline\":\"2027-01-31T00:00:00Z\"}");
    }

    private List<JsonNode> answers(UUID tenant, String type, UUID id) {
        return read(tenant, () -> database.jdbc().sql("SELECT payload FROM sis.outbox_event WHERE tenant_id ="
                + " :tenantId AND aggregate_id = :id AND event_type = :type ORDER BY id").param("id", id)
                .param("type", type).query(String.class).list()).stream().map(JSON::readTree).toList();
    }

    private String health(UUID tenant, UUID person) {
        return read(tenant, () -> database.jdbc().sql("SELECT h.medical_conditions FROM sis.student_health_record h"
                + " JOIN sis.student s ON s.id = h.student_id AND s.tenant_id = :tenantId"
                + " WHERE h.tenant_id = :tenantId AND s.person_id = :person").param("person", person)
                .query(String.class).list().get(0)); // one row; list() because the value may be null
    }

    private int audited(UUID tenant) {
        return read(tenant, () -> database.jdbc().sql("SELECT count(*) FROM sis.outbox_event WHERE tenant_id ="
                + " :tenantId AND event_type = 'audit.recorded'").query(Integer.class).single());
    }

    @Test
    void anErasureClearsTheStudentsHealthOncePerTaskAndKeepsTheRegister() {
        UUID tenant = UUID.randomUUID();
        UUID person = student(tenant, null);
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, person);

        assertThat(health(tenant, person)).isNull();
        assertThat(read(tenant, () -> database.jdbc().sql("SELECT admission_number IS NOT NULL AND exit_reason IS NULL"
                + " FROM sis.student WHERE tenant_id = :tenantId AND person_id = :person").param("person", person)
                .query(Boolean.class).single())).as("the register stays, why the student left goes").isTrue();
        JsonNode answer = answers(tenant, "sis.privacy_task.completed", taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("done");
        assertThat(answer.path("counts").path("sis.student_health_record").asInt()).isEqualTo(1);
        assertThat(answer.path("counts").path("sis.student").asInt()).isEqualTo(1);
        int audits = audited(tenant);

        erase(tenant, taskId, person);

        assertThat(answers(tenant, "sis.privacy_task.completed", taskId)).hasSize(2)
                .allSatisfy(again -> assertThat(again).isEqualTo(answer));
        assertThat(audited(tenant)).as("nothing is erased twice").isEqualTo(audits);
    }

    @Test
    void aHeldSubjectsRowsAreKeptAndCounted() {
        UUID tenant = UUID.randomUUID();
        UUID person = student(tenant, null);
        TestLegalHolds.holdSubject(person);
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, person);

        assertThat(health(tenant, person)).isEqualTo("Asthma");
        JsonNode answer = answers(tenant, "sis.privacy_task.completed", taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("partial");
        assertThat(answer.path("heldItems").asInt()).as("the health record and the student").isEqualTo(2);
    }

    @Test
    void healthAfterLeavingClearsOnlyTheRecordsPastTheCutoffAndKeepsHeldOnes() {
        UUID tenant = UUID.randomUUID();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        UUID longGone = student(tenant, today.minusYears(2));
        UUID held = student(tenant, today.minusYears(2));
        TestLegalHolds.holdSubject(held);
        UUID recent = student(tenant, today.minusDays(10));
        UUID present = student(tenant, null);
        UUID runId = UUID.randomUUID();
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(365);

        deliver(tenant, "privacy.retention.due", "privacy.retention_run", runId, "{\"runId\":\"" + runId
                + "\",\"policyCode\":\"sis.health_after_leaving\",\"categoryCode\":\"health\",\"module\":\"sis\","
                + "\"cutoff\":\"" + cutoff + "\",\"action\":\"delete\"}");

        assertThat(health(tenant, longGone)).isNull();
        assertThat(health(tenant, held)).isEqualTo("Asthma");
        assertThat(health(tenant, recent)).isEqualTo("Asthma");
        assertThat(health(tenant, present)).isEqualTo("Asthma");
        JsonNode answer = answers(tenant, "sis.privacy_retention.completed", runId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("partial");
        assertThat(answer.path("affected").asInt()).isEqualTo(1);
        assertThat(answer.path("held").asInt()).isEqualTo(1);
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
