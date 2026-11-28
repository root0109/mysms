package com.mysms.hrms.timeattendance;

import com.mysms.hrms.HrmsApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.hrms.timeattendance.config.TimeAttendanceDatabaseConfig;
import com.mysms.hrms.timeattendance.events.TimeAttendancePrivacyTaskHandler;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.InboxProcessor;
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
 * time_attendance's privacy tasks against real PostgreSQL as the application role (design
 * PRIVACY-AND-CONSENT-CENTRE §9.1): the person of a staff record is read from hr.staff, the sibling area's table; an
 * erasure changes no attendance day, answers the subject's days as kept under time_attendance.after_leaving — once
 * per task id, answered again from the stored receipt — and every answer is on the outbox.
 */
@SpringBootTest(classes = HrmsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TimeAttendancePrivacyTaskComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    @Autowired
    @Qualifier(TimeAttendanceDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private TimeAttendancePrivacyTaskHandler handler;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "time_attendance", "hr", "payroll");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    /** A staff member (an hr.staff record, the sibling area) with one attendance day; the person's id. */
    private UUID staff(UUID tenant) {
        UUID person = UUID.randomUUID();
        UUID staff = UUID.randomUUID();
        write(tenant, () -> {
            database.jdbc().sql("INSERT INTO hr.staff (id, tenant_id, person_id, institution_id) VALUES (:id,"
                    + " :tenantId, :person, :institution)").param("id", staff).param("person", person)
                    .param("institution", UUID.randomUUID()).update();
            database.jdbc().sql("INSERT INTO time_attendance.staff_attendance (id, tenant_id, staff_id,"
                    + " attendance_date, status, row_version) VALUES (:id, :tenantId, :staff, :day, 'present', 1)")
                    .param("id", UUID.randomUUID()).param("staff", staff)
                    .param("day", LocalDate.of(2026, 7, 15)).update();
            return null;
        });
        return person;
    }

    private void erase(UUID tenant, UUID taskId, UUID subject) {
        deliver(tenant, "privacy", "privacy.rights_request.task_assigned", "privacy.rights_request_task", taskId,
                "{\"requestId\":\"" + UUID.randomUUID() + "\",\"taskId\":\"" + taskId + "\",\"type\":\"erasure\","
                + "\"action\":\"erase\",\"subjectPersonId\":\"" + subject + "\",\"module\":\"time_attendance\","
                + "\"wave\":2,\"deadline\":\"2027-01-31T00:00:00Z\"}", handler::receive);
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null), () -> {
            handler.run();
            return null;
        });
    }

    private void deliver(UUID tenant, String source, String type, String aggregate, UUID id, String data,
            EventHandler receiver) {
        EventEnvelope event = new EventEnvelope(UUID.randomUUID(), source, type, Optional.of(tenant), aggregate, id,
                Instant.now(), data, Optional.of(TRACE));
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        assertThat(inbox.deliver(event, receiver)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
    }

    private List<JsonNode> answers(UUID tenant, UUID taskId) {
        return read(tenant, () -> database.jdbc().sql("SELECT payload FROM time_attendance.outbox_event WHERE"
                + " tenant_id = :tenantId AND aggregate_id = :id AND event_type ="
                + " 'time_attendance.privacy_task.completed' ORDER BY id")
                .param("id", taskId).query(String.class).list()).stream().map(JSON::readTree).toList();
    }

    @Test
    void anErasureKeepsTheAttendanceDaysAndAnswersOncePerTask() {
        UUID tenant = UUID.randomUUID();
        UUID person = staff(tenant);
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, person);

        assertThat(read(tenant, () -> database.jdbc().sql("SELECT count(*) FROM time_attendance.staff_attendance"
                + " WHERE tenant_id = :tenantId AND deleted_at IS NULL").query(Integer.class).single()))
                .as("the register is untouched").isEqualTo(1);
        JsonNode answer = answers(tenant, taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("partial");
        assertThat(answer.path("heldItems").asInt()).as("the attendance day").isEqualTo(1);
        assertThat(answer.path("policyCodes").get(0).asString()).isEqualTo("time_attendance.after_leaving");

        erase(tenant, taskId, person);

        assertThat(answers(tenant, taskId)).hasSize(2).allSatisfy(again -> assertThat(again).isEqualTo(answer));
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
