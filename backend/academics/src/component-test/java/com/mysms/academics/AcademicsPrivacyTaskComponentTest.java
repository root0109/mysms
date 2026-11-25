package com.mysms.academics;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.academics.config.AcademicsDatabaseConfig;
import com.mysms.academics.events.AcademicsPrivacyTaskHandler;
import com.mysms.academics.events.StaffRecordSync;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.InboxProcessor;
import com.mysms.privacyclient.testing.TestLegalHolds;
import java.time.Clock;
import java.time.Instant;
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
 * academics' privacy tasks against real PostgreSQL as the application role, with privacy's legal holds from
 * {@link TestLegalHolds} (design PRIVACY-AND-CONSENT-CENTRE §9.1): the person of a staff record is learnt from hr's
 * events (an older version changes nothing); an erasure clears the notes of the person's lesson plans once per task id,
 * keeping the plans and the teaching assignment; a held subject's plans are kept and counted; every answer is on the
 * outbox.
 */
@SpringBootTest(classes = AcademicsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AcademicsPrivacyTaskComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String NOTES = "Bring my own laptop";

    @Autowired
    @Qualifier(AcademicsDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private AcademicsPrivacyTaskHandler handler;

    @Autowired
    private StaffRecordSync staff;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "academics");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    /** A teacher (made known by hr's event) teaching one class subject with a lesson plan with notes; the person. */
    private UUID teacher(UUID tenant) {
        UUID person = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        staffEvent(tenant, staffId, person, 1);
        UUID institution = UUID.randomUUID();
        UUID grade = UUID.randomUUID();
        UUID section = UUID.randomUUID();
        UUID classSection = UUID.randomUUID();
        UUID subject = UUID.randomUUID();
        UUID classSubject = UUID.randomUUID();
        UUID teaching = UUID.randomUUID();
        String start = "(:tenantId, '" + institution + "', ";
        write(tenant, () -> {
            for (String statement : List.of(
                    "INSERT INTO academics.grade_level (tenant_id, institution_id, name, slug, id) VALUES " + start
                            + "'Grade 7', 'grade-7', '" + grade + "')",
                    "INSERT INTO academics.section (tenant_id, institution_id, name, code, id) VALUES " + start
                            + "'A', 'A', '" + section + "')",
                    "INSERT INTO academics.class_section (tenant_id, institution_id, academic_year_id, grade_level_id,"
                            + " section_id, id) VALUES " + start + "'" + UUID.randomUUID() + "', '" + grade + "', '"
                            + section + "', '" + classSection + "')",
                    "INSERT INTO academics.subject (tenant_id, institution_id, name, id) VALUES " + start
                            + "'Science', '" + subject + "')",
                    "INSERT INTO academics.class_subject (tenant_id, class_section_id, subject_id, id) VALUES "
                            + "(:tenantId, '" + classSection + "', '" + subject + "', '" + classSubject + "')",
                    "INSERT INTO academics.teaching_assignment (tenant_id, class_subject_id, staff_id, id) VALUES "
                            + "(:tenantId, '" + classSubject + "', '" + staffId + "', '" + teaching + "')",
                    "INSERT INTO academics.lesson_plan (tenant_id, teaching_assignment_id, unit_no, unit_name, title,"
                            + " planned_duration_minutes, description, notes, id) VALUES (:tenantId, '" + teaching
                            + "', '1', 'Plants', 'Photosynthesis', 40, 'Light and leaves', '" + NOTES + "', '"
                            + UUID.randomUUID() + "')")) {
                database.jdbc().sql(statement).update();
            }
            return null;
        });
        return person;
    }

    private boolean staffEvent(UUID tenant, UUID staffId, UUID person, int version) {
        return deliver(tenant, "hr", "hr.staff.updated", "hr.staff", staffId, "{\"id\":\"" + staffId + "\","
                + "\"personId\":\"" + person + "\",\"rowVersion\":" + version + "}", staff::apply);
    }

    private boolean deliver(UUID tenant, String source, String type, String aggregate, UUID id, String data,
            EventHandler receiver) {
        EventEnvelope event = new EventEnvelope(UUID.randomUUID(), source, type, Optional.of(tenant), aggregate, id,
                Instant.now(), data, Optional.of(TRACE));
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        return inbox.deliver(event, receiver) == InboxProcessor.Outcome.PROCESSED;
    }

    private void erase(UUID tenant, UUID taskId, UUID subject) {
        assertThat(deliver(tenant, "privacy", "privacy.rights_request.task_assigned", "privacy.rights_request_task",
                taskId, "{\"requestId\":\"" + UUID.randomUUID() + "\",\"taskId\":\"" + taskId + "\","
                + "\"type\":\"erasure\",\"action\":\"erase\",\"subjectPersonId\":\"" + subject + "\","
                + "\"module\":\"academics\",\"wave\":2,\"deadline\":\"2027-01-31T00:00:00Z\"}", handler::receive))
                .isTrue();
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null), () -> {
            handler.run();
            return null;
        });
    }

    private List<JsonNode> answers(UUID tenant, UUID taskId) {
        return read(tenant, () -> database.jdbc().sql("SELECT payload FROM academics.outbox_event WHERE tenant_id ="
                + " :tenantId AND aggregate_id = :id AND event_type = 'academics.privacy_task.completed' ORDER BY id")
                .param("id", taskId).query(String.class).list()).stream().map(JSON::readTree).toList();
    }

    private String notes(UUID tenant) {
        return read(tenant, () -> database.jdbc().sql("SELECT notes FROM academics.lesson_plan WHERE tenant_id ="
                + " :tenantId").query(String.class).list().get(0)); // one row; list() because the value may be null
    }

    private int count(UUID tenant, String sql) {
        return read(tenant, () -> database.jdbc().sql(sql).query(Integer.class).single());
    }

    @Test
    void anErasureClearsTheLessonPlansNotesOncePerTaskAndKeepsThePlans() {
        UUID tenant = UUID.randomUUID();
        UUID person = teacher(tenant);
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, person);

        assertThat(notes(tenant)).isNull();
        assertThat(count(tenant, "SELECT count(*) FROM academics.lesson_plan WHERE tenant_id = :tenantId"
                + " AND title = 'Photosynthesis' AND deleted_at IS NULL")).as("the plan stays").isEqualTo(1);
        assertThat(count(tenant, "SELECT count(*) FROM academics.teaching_assignment WHERE tenant_id = :tenantId"
                + " AND deleted_at IS NULL")).as("the assignment stays").isEqualTo(1);
        JsonNode answer = answers(tenant, taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("done");
        assertThat(answer.path("counts").path("academics.lesson_plan").asInt()).isEqualTo(1);
        String audits = "SELECT count(*) FROM academics.outbox_event WHERE tenant_id = :tenantId"
                + " AND event_type = 'audit.recorded'";
        int audited = count(tenant, audits);

        erase(tenant, taskId, person);

        assertThat(answers(tenant, taskId)).hasSize(2).allSatisfy(again -> assertThat(again).isEqualTo(answer));
        assertThat(count(tenant, audits)).as("nothing is erased twice").isEqualTo(audited);
    }

    @Test
    void aHeldSubjectsPlansAreKeptAndCounted() {
        UUID tenant = UUID.randomUUID();
        UUID person = teacher(tenant);
        TestLegalHolds.holdSubject(person);
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, person);

        assertThat(notes(tenant)).isEqualTo(NOTES);
        JsonNode answer = answers(tenant, taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("partial");
        assertThat(answer.path("heldItems").asInt()).isEqualTo(1);
    }

    @Test
    void theStaffRecordsPersonChangesOnlyToANewerVersion() {
        UUID tenant = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID person = UUID.randomUUID();

        assertThat(staffEvent(tenant, staffId, person, 2)).isTrue();
        assertThat(staffEvent(tenant, staffId, UUID.randomUUID(), 1)).isTrue();

        assertThat(read(tenant, () -> database.jdbc().sql("SELECT person_id FROM academics.staff_record"
                + " WHERE tenant_id = :tenantId AND staff_id = :id").param("id", staffId).query(UUID.class)
                .single())).as("a late event changes nothing").isEqualTo(person);
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
