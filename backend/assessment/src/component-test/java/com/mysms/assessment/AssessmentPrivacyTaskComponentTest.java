package com.mysms.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.events.AssessmentPrivacyTaskHandler;
import com.mysms.assessment.events.StudentRecordSync;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.InboxProcessor;
import com.mysms.privacyclient.testing.TestLegalHolds;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;

/**
 * assessment's privacy tasks against real PostgreSQL as the application role, with privacy's legal holds from
 * {@link TestLegalHolds} (design PRIVACY-AND-CONSENT-CENTRE §9.1): the person of a student record arrives with sis's
 * event; an erasure clears and soft-deletes the subject's homework submission and clears the answer of the assignment
 * submission once per task id, keeping its mark, and keeps the exam result under assessment.after_leaving, counted
 * with the policy code; a held subject's rows are kept and counted; every answer is on the outbox.
 */
@SpringBootTest(classes = AssessmentApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AssessmentPrivacyTaskComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    @Autowired
    @Qualifier(AssessmentDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private AssessmentPrivacyTaskHandler handler;

    @Autowired
    private StudentRecordSync students;

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "assessment");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private AssessmentFixtures fx() {
        return new AssessmentFixtures(TOKENS, database, port);
    }

    /**
     * A student (made known by sis's event) with a homework submission, an assignment submission and an exam result;
     * the person's id.
     */
    private UUID student(UUID tenant) {
        UUID person = UUID.randomUUID();
        UUID student = UUID.randomUUID();
        UUID institution = UUID.randomUUID();
        deliver(tenant, "sis", "sis.student.created", "sis.student", student, "{\"id\":\"" + student + "\","
                + "\"userId\":null,\"personId\":\"" + person + "\",\"institutionId\":\"" + institution + "\","
                + "\"rowVersion\":1}", students::apply);
        UUID homework = UUID.randomUUID();
        UUID assignment = UUID.randomUUID();
        fx().inTenant(tenant, () -> {
            database.jdbc().sql("INSERT INTO assessment.homework (id, tenant_id, institution_id, academic_year_id,"
                    + " class_section_id) VALUES (:id, :tenantId, :institution, :year, :section)").param("id", homework)
                    .param("institution", institution).param("year", UUID.randomUUID())
                    .param("section", UUID.randomUUID()).update();
            database.jdbc().sql("INSERT INTO assessment.homework_submission (id, tenant_id, homework_id, student_id,"
                    + " student_reply, teacher_comments) VALUES (:id, :tenantId, :homework, :student, 'My essay',"
                    + " 'Well argued')").param("id", UUID.randomUUID()).param("homework", homework)
                    .param("student", student).update();
            database.jdbc().sql("INSERT INTO assessment.assignment (id, tenant_id, institution_id, title,"
                    + " class_section_id) VALUES (:id, :tenantId, :institution, 'Project', :section)")
                    .param("id", assignment).param("institution", institution).param("section", UUID.randomUUID())
                    .update();
            database.jdbc().sql("INSERT INTO assessment.assignment_submission (id, tenant_id, assignment_id,"
                    + " student_id, content, submitted_at, marks_obtained, feedback) VALUES (:id, :tenantId,"
                    + " :assignment, :student, 'My answer', :at, 8, 'Neat')").param("id", UUID.randomUUID())
                    .param("assignment", assignment).param("student", student)
                    .param("at", OffsetDateTime.parse("2026-03-05T10:00:00Z")).update();
            return null;
        });
        UUID exam = fx().exam(tenant, fx().session(tenant, institution), "held", "100", "40");
        fx().result(tenant, exam, student, "present", "72", "100", "pass", null);
        return person;
    }

    private void deliver(UUID tenant, String source, String type, String aggregate, UUID id, String data,
            EventHandler receiver) {
        EventEnvelope event = new EventEnvelope(UUID.randomUUID(), source, type, Optional.of(tenant), aggregate, id,
                Instant.now(), data, Optional.of(TRACE));
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        assertThat(inbox.deliver(event, receiver)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
    }

    private void erase(UUID tenant, UUID taskId, UUID subject) {
        deliver(tenant, "privacy", "privacy.rights_request.task_assigned", "privacy.rights_request_task", taskId,
                "{\"requestId\":\"" + UUID.randomUUID() + "\",\"taskId\":\"" + taskId + "\",\"type\":\"erasure\","
                + "\"action\":\"erase\",\"subjectPersonId\":\"" + subject + "\",\"module\":\"assessment\",\"wave\":2,"
                + "\"deadline\":\"2027-01-31T00:00:00Z\"}", handler::receive);
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null), () -> {
            handler.run();
            return null;
        });
    }

    private List<JsonNode> answers(UUID tenant, UUID taskId) {
        return fx().inTenant(tenant, () -> database.jdbc().sql("SELECT payload FROM assessment.outbox_event"
                + " WHERE tenant_id = :tenantId AND aggregate_id = :id"
                + " AND event_type = 'assessment.privacy_task.completed' ORDER BY id").param("id", taskId)
                .query(String.class).list()).stream().map(AssessmentFixtures.JSON::readTree).toList();
    }

    private String reply(UUID tenant) {
        return fx().inTenant(tenant, () -> database.jdbc().sql("SELECT student_reply FROM"
                + " assessment.homework_submission WHERE tenant_id = :tenantId")
                .query(String.class).list().get(0)); // one row; list() because the value may be null
    }

    private int audited(UUID tenant) {
        return fx().inTenant(tenant, () -> database.jdbc().sql("SELECT count(*) FROM assessment.outbox_event"
                + " WHERE tenant_id = :tenantId AND event_type = 'audit.recorded'").query(Integer.class).single());
    }

    @Test
    void anErasureClearsTheSubmissionsOncePerTaskAndKeepsTheMarksOfRecord() {
        UUID tenant = UUID.randomUUID();
        UUID person = student(tenant);
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, person);

        assertThat(reply(tenant)).isNull();
        assertThat(fx().inTenant(tenant, () -> database.jdbc().sql("SELECT content IS NULL AND feedback IS NULL"
                + " AND marks_obtained = 8 AND deleted_at IS NULL FROM assessment.assignment_submission"
                + " WHERE tenant_id = :tenantId").query(Boolean.class).single())).as("the mark stays").isTrue();
        JsonNode answer = answers(tenant, taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("partial");
        assertThat(answer.path("counts").path("assessment.homework_submission").asInt()).isEqualTo(1);
        assertThat(answer.path("counts").path("assessment.assignment_submission").asInt()).isEqualTo(1);
        assertThat(answer.path("heldItems").asInt()).as("the exam result").isEqualTo(1);
        assertThat(answer.path("policyCodes").get(0).asString()).isEqualTo("assessment.after_leaving");
        int audits = audited(tenant);

        erase(tenant, taskId, person);

        assertThat(answers(tenant, taskId)).hasSize(2).allSatisfy(again -> assertThat(again).isEqualTo(answer));
        assertThat(audited(tenant)).as("nothing is erased twice").isEqualTo(audits);
    }

    @Test
    void aHeldSubjectsRowsAreKeptAndCounted() {
        UUID tenant = UUID.randomUUID();
        UUID person = student(tenant);
        TestLegalHolds.holdSubject(person);
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, person);

        assertThat(reply(tenant)).isEqualTo("My essay");
        JsonNode answer = answers(tenant, taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("partial");
        assertThat(answer.path("heldItems").asInt()).as("both submissions and the exam result").isEqualTo(3);
    }
}
