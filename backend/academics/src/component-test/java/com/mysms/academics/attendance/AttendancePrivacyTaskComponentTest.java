package com.mysms.academics.attendance;

import com.mysms.academics.AcademicsApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.academics.attendance.config.AttendanceDatabaseConfig;
import com.mysms.academics.attendance.events.AttendancePrivacyTaskHandler;
import com.mysms.academics.attendance.events.MemberRecordSync;
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
 * attendance's privacy tasks and retention sweep against real PostgreSQL as the application role, with privacy's
 * legal holds from {@link TestLegalHolds} (design PRIVACY-AND-CONSENT-CENTRE §9.1, §10): the person of a student
 * record arrives with sis's event; an erasure keeps the register and answers it as kept under attendance.after_leaving,
 * once per task id; attendance.after_leaving anonymises only the attendance of students not marked since the cutoff
 * and keeps a held subject's; every answer is on the outbox.
 */
@SpringBootTest(classes = AcademicsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AttendancePrivacyTaskComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final LocalDate JANUARY = LocalDate.of(2026, 1, 15);
    private static final LocalDate OCTOBER = LocalDate.of(2026, 10, 1);

    @Autowired
    @Qualifier(AttendanceDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private AttendancePrivacyTaskHandler handler;

    @Autowired
    private MemberRecordSync records;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "attendance", "academics", "timetable", "assessment");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    /** A student (made known by sis's event) marked on each date, with a remark; the person's id. */
    private UUID student(UUID tenant, LocalDate... dates) {
        UUID person = UUID.randomUUID();
        UUID student = UUID.randomUUID();
        deliver(tenant, "sis", "sis.student.created", "sis.student", student, "{\"id\":\"" + student + "\","
                + "\"userId\":null,\"personId\":\"" + person + "\",\"institutionId\":\"" + UUID.randomUUID() + "\","
                + "\"rowVersion\":1}", records::apply);
        UUID register = UUID.randomUUID();
        write(tenant, () -> {
            database.jdbc().sql("INSERT INTO attendance.attendance_register (id, tenant_id, institution_id, code, name,"
                    + " register_scope, row_version) VALUES (:id, :tenantId, :institution, :code, 'Daily',"
                    + " 'institution', 1)").param("id", register).param("institution", UUID.randomUUID())
                    .param("code", register.toString().substring(0, 8)).update();
            for (LocalDate date : dates) {
                UUID sheet = UUID.randomUUID();
                database.jdbc().sql("INSERT INTO attendance.attendance_sheet (id, tenant_id, register_id,"
                        + " attendance_date, row_version) VALUES (:id, :tenantId, :register, :date, 1)")
                        .param("id", sheet).param("register", register).param("date", date).update();
                database.jdbc().sql("INSERT INTO attendance.attendance_record (id, tenant_id, sheet_id, student_id,"
                        + " status, remark, attendance_date, row_version) VALUES (:id, :tenantId, :sheet, :student,"
                        + " 'absent', 'Unwell', :date, 1)").param("id", UUID.randomUUID()).param("sheet", sheet)
                        .param("student", student).param("date", date).update();
            }
            return null;
        });
        return person;
    }

    private void deliver(UUID tenant, String source, String type, String aggregate, UUID id, String data,
            EventHandler receiver) {
        EventEnvelope event = new EventEnvelope(UUID.randomUUID(), source, type, Optional.of(tenant), aggregate, id,
                Instant.now(), data, Optional.of(TRACE));
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        assertThat(inbox.deliver(event, receiver)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
    }

    private void privacy(UUID tenant, String type, String aggregate, UUID id, String data) {
        deliver(tenant, "privacy", type, aggregate, id, data, handler::receive);
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null), () -> {
            handler.run();
            return null;
        });
    }

    private void erase(UUID tenant, UUID taskId, UUID subject) {
        privacy(tenant, "privacy.rights_request.task_assigned", "privacy.rights_request_task", taskId,
                "{\"requestId\":\"" + UUID.randomUUID() + "\",\"taskId\":\"" + taskId + "\",\"type\":\"erasure\","
                + "\"action\":\"erase\",\"subjectPersonId\":\"" + subject + "\",\"module\":\"attendance\",\"wave\":2,"
                + "\"deadline\":\"2027-01-31T00:00:00Z\"}");
    }

    private List<JsonNode> answers(UUID tenant, String type, UUID id) {
        return read(tenant, () -> database.jdbc().sql("SELECT payload FROM attendance.outbox_event WHERE tenant_id ="
                + " :tenantId AND aggregate_id = :id AND event_type = :type ORDER BY id").param("id", id)
                .param("type", type).query(String.class).list()).stream().map(JSON::readTree).toList();
    }

    /** The remarks still marked against the person's student record. */
    private List<String> remarks(UUID tenant, UUID person) {
        return read(tenant, () -> database.jdbc().sql("SELECT r.remark FROM attendance.attendance_record r"
                + " JOIN attendance.member_record m ON m.record_id = r.student_id AND m.tenant_id = :tenantId"
                + " WHERE r.tenant_id = :tenantId AND m.person_id = :person ORDER BY r.attendance_date")
                .param("person", person).query(String.class).list());
    }

    @Test
    void anErasureKeepsTheRegisterAndCountsItUnderThePolicyOncePerTask() {
        UUID tenant = UUID.randomUUID();
        UUID person = student(tenant, JANUARY, OCTOBER);
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, person);

        assertThat(remarks(tenant, person)).containsExactly("Unwell", "Unwell");
        JsonNode answer = answers(tenant, "attendance.privacy_task.completed", taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("partial");
        assertThat(answer.path("heldItems").asInt()).isEqualTo(2);
        assertThat(answer.path("policyCodes").get(0).asString()).isEqualTo("attendance.after_leaving");

        erase(tenant, taskId, person);

        assertThat(answers(tenant, "attendance.privacy_task.completed", taskId)).hasSize(2)
                .allSatisfy(again -> assertThat(again).isEqualTo(answer));
    }

    @Test
    void anErasureOfAPersonWithNoAttendanceIsNotApplicable() {
        UUID tenant = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, UUID.randomUUID());

        assertThat(answers(tenant, "attendance.privacy_task.completed", taskId).get(0).path("status").asString())
                .isEqualTo("not_applicable");
    }

    @Test
    void afterLeavingAnonymisesOnlyTheStudentsGoneBeforeTheCutoffAndKeepsHeldOnes() {
        UUID tenant = UUID.randomUUID();
        UUID longGone = student(tenant, JANUARY);
        UUID held = student(tenant, JANUARY);
        TestLegalHolds.holdSubject(held);
        UUID present = student(tenant, JANUARY, OCTOBER);
        UUID runId = UUID.randomUUID();

        privacy(tenant, "privacy.retention.due", "privacy.retention_run", runId, "{\"runId\":\"" + runId
                + "\",\"policyCode\":\"attendance.after_leaving\",\"categoryCode\":\"attendance\","
                + "\"module\":\"attendance\",\"cutoff\":\"2026-06-01T00:00:00Z\",\"action\":\"anonymise\"}");

        assertThat(remarks(tenant, longGone)).as("no mark is the person's any more").isEmpty();
        assertThat(read(tenant, () -> database.jdbc().sql("SELECT count(*) FROM attendance.attendance_record"
                + " WHERE tenant_id = :tenantId AND remark IS NULL AND attendance_date = :date")
                .param("date", JANUARY).query(Integer.class).single())).as("the mark stays, anonymised").isEqualTo(1);
        assertThat(remarks(tenant, held)).containsExactly("Unwell");
        assertThat(remarks(tenant, present)).containsExactly("Unwell", "Unwell");
        JsonNode answer = answers(tenant, "attendance.privacy_retention.completed", runId).get(0);
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
