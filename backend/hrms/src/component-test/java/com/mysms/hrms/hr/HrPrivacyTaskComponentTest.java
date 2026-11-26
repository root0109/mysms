package com.mysms.hrms.hr;

import com.mysms.hrms.HrmsApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.hrms.hr.config.HrDatabaseConfig;
import com.mysms.hrms.hr.events.HrPrivacyTaskHandler;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.InboxProcessor;
import com.mysms.privacyclient.testing.TestLegalHolds;
import java.time.Clock;
import java.time.Instant;
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
 * hr's privacy tasks against real PostgreSQL as the application role, with privacy's legal holds from
 * {@link TestLegalHolds} (design PRIVACY-AND-CONSENT-CENTRE §9.1): an erasure anonymises a staff record and clears its
 * leave applications' free text once per task id, keeping the employment record and the leave ledger; a held
 * subject's rows are kept and counted; every answer is on the outbox.
 */
@SpringBootTest(classes = HrmsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HrPrivacyTaskComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    @Autowired
    @Qualifier(HrDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private HrPrivacyTaskHandler handler;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "hr", "payroll");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    /** A staff member with interests and a leave application with remarks; the person's id. */
    private UUID staff(UUID tenant) {
        UUID person = UUID.randomUUID();
        UUID staff = UUID.randomUUID();
        UUID institution = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(3);
        write(tenant, () -> {
            database.jdbc().sql("INSERT INTO hr.staff (id, tenant_id, person_id, institution_id, employee_code,"
                    + " interests, row_version) VALUES (:id, :tenantId, :person, :institution, :code, 'Chess', 1)")
                    .param("id", staff).param("person", person).param("institution", institution)
                    .param("code", "E-" + staff.toString().substring(0, 8)).update();
            database.jdbc().sql("INSERT INTO hr.leave_application (id, tenant_id, institution_id, academic_year_id,"
                    + " staff_id, from_at, to_at, remarks, row_version) VALUES (:id, :tenantId, :institution, :year,"
                    + " :staff, :from, :to, 'Family wedding', 1)").param("id", UUID.randomUUID())
                    .param("institution", institution).param("year", UUID.randomUUID()).param("staff", staff)
                    .param("from", from).param("to", from.plusDays(1)).update();
            return null;
        });
        return person;
    }

    private void erase(UUID tenant, UUID taskId, UUID subject) {
        EventEnvelope event = new EventEnvelope(UUID.randomUUID(), "privacy", "privacy.rights_request.task_assigned",
                Optional.of(tenant), "privacy.rights_request_task", taskId, Instant.now(),
                "{\"requestId\":\"" + UUID.randomUUID() + "\",\"taskId\":\"" + taskId + "\",\"type\":\"erasure\","
                + "\"action\":\"erase\",\"subjectPersonId\":\"" + subject + "\",\"module\":\"hr\",\"wave\":2,"
                + "\"deadline\":\"2027-01-31T00:00:00Z\"}", Optional.of(TRACE));
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        assertThat(inbox.deliver(event, handler::receive)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null), () -> {
            handler.run();
            return null;
        });
    }

    private List<JsonNode> answers(UUID tenant, UUID taskId) {
        return read(tenant, () -> database.jdbc().sql("SELECT payload FROM hr.outbox_event WHERE tenant_id ="
                + " :tenantId AND aggregate_id = :id AND event_type = 'hr.privacy_task.completed' ORDER BY id")
                .param("id", taskId).query(String.class).list()).stream().map(JSON::readTree).toList();
    }

    private String remarks(UUID tenant, UUID person) {
        return read(tenant, () -> database.jdbc().sql("SELECT l.remarks FROM hr.leave_application l"
                + " JOIN hr.staff s ON s.id = l.staff_id AND s.tenant_id = :tenantId"
                + " WHERE l.tenant_id = :tenantId AND s.person_id = :person").param("person", person)
                .query(String.class).list().get(0)); // one row; list() because the value may be null
    }

    private int audited(UUID tenant) {
        return read(tenant, () -> database.jdbc().sql("SELECT count(*) FROM hr.outbox_event WHERE tenant_id ="
                + " :tenantId AND event_type = 'audit.recorded'").query(Integer.class).single());
    }

    @Test
    void anErasureAnonymisesTheStaffRecordOncePerTaskAndKeepsTheEmploymentRecord() {
        UUID tenant = UUID.randomUUID();
        UUID person = staff(tenant);
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, person);

        assertThat(remarks(tenant, person)).isNull();
        assertThat(read(tenant, () -> database.jdbc().sql("SELECT employee_code IS NOT NULL AND interests IS NULL"
                + " AND deleted_at IS NULL FROM hr.staff WHERE tenant_id = :tenantId AND person_id = :person")
                .param("person", person).query(Boolean.class).single()))
                .as("the employment record stays, the interests go").isTrue();
        JsonNode answer = answers(tenant, taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("done");
        assertThat(answer.path("counts").path("hr.staff").asInt()).isEqualTo(1);
        assertThat(answer.path("counts").path("hr.leave_application").asInt()).isEqualTo(1);
        int audits = audited(tenant);

        erase(tenant, taskId, person);

        assertThat(answers(tenant, taskId)).hasSize(2).allSatisfy(again -> assertThat(again).isEqualTo(answer));
        assertThat(audited(tenant)).as("nothing is erased twice").isEqualTo(audits);
    }

    @Test
    void aHeldSubjectsRowsAreKeptAndCounted() {
        UUID tenant = UUID.randomUUID();
        UUID person = staff(tenant);
        TestLegalHolds.holdSubject(person);
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, person);

        assertThat(remarks(tenant, person)).isEqualTo("Family wedding");
        JsonNode answer = answers(tenant, taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("partial");
        assertThat(answer.path("heldItems").asInt()).as("the leave application and the staff record").isEqualTo(2);
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
