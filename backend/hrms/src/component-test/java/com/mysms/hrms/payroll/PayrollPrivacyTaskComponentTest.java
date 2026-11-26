package com.mysms.hrms.payroll;

import com.mysms.hrms.HrmsApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.InboxProcessor;
import com.mysms.hrms.payroll.config.PayrollDatabaseConfig;
import com.mysms.hrms.payroll.events.PayrollPrivacyTaskHandler;
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
 * payroll's privacy tasks against real PostgreSQL as the application role, with privacy's legal holds from
 * {@link TestLegalHolds} (design PRIVACY-AND-CONSENT-CENTRE §9.1): the person of a staff record arrives with hr's
 * event; an erasure clears and soft-deletes the subject's bank account once per task id and keeps the salary structure
 * under payroll.after_leaving, counted with the policy code; a held subject's rows are kept and counted; every answer
 * is on the outbox.
 */
@SpringBootTest(classes = HrmsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PayrollPrivacyTaskComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    @Autowired
    @Qualifier(PayrollDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private PayrollPrivacyTaskHandler handler;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "payroll", "hr");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    /**
     * A staff member (an hr.staff record, payroll's sibling area) with a bank account and a salary structure; the
     * person's id.
     */
    private UUID staff(UUID tenant) {
        UUID person = UUID.randomUUID();
        UUID staff = UUID.randomUUID();
        UUID institution = UUID.randomUUID();
        UUID template = UUID.randomUUID();
        write(tenant, () -> {
            database.jdbc().sql("INSERT INTO hr.staff (id, tenant_id, person_id, institution_id) VALUES (:id, :tenantId,"
                    + " :person, :institution)").param("id", staff).param("person", person)
                    .param("institution", institution).update();
            database.jdbc().sql("INSERT INTO payroll.salary_template (id, tenant_id, institution_id, name, row_version)"
                    + " VALUES (:id, :tenantId, :institution, :name, 1)").param("id", template)
                    .param("institution", institution).param("name", "Grade " + template).update();
            database.jdbc().sql("INSERT INTO payroll.staff_salary (id, tenant_id, staff_id, salary_template_id,"
                    + " gross_salary, effective_date, row_version) VALUES (:id, :tenantId, :staff, :template, 50000,"
                    + " :from, 1)").param("id", UUID.randomUUID()).param("staff", staff).param("template", template)
                    .param("from", LocalDate.of(2026, 4, 1)).update();
            database.jdbc().sql("INSERT INTO payroll.staff_bank_account (id, tenant_id, staff_id, account_holder_name,"
                    + " account_number_enc, bank_name, row_version) VALUES (:id, :tenantId, :staff, 'Asha Rao',"
                    + " 'envelope', 'State Bank', 1)").param("id", UUID.randomUUID()).param("staff", staff).update();
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

    private void erase(UUID tenant, UUID taskId, UUID subject) {
        deliver(tenant, "privacy", "privacy.rights_request.task_assigned", "privacy.rights_request_task", taskId,
                "{\"requestId\":\"" + UUID.randomUUID() + "\",\"taskId\":\"" + taskId + "\",\"type\":\"erasure\","
                + "\"action\":\"erase\",\"subjectPersonId\":\"" + subject + "\",\"module\":\"payroll\",\"wave\":2,"
                + "\"deadline\":\"2027-01-31T00:00:00Z\"}", handler::receive);
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null), () -> {
            handler.run();
            return null;
        });
    }

    private List<JsonNode> answers(UUID tenant, UUID taskId) {
        return read(tenant, () -> database.jdbc().sql("SELECT payload FROM payroll.outbox_event WHERE tenant_id ="
                + " :tenantId AND aggregate_id = :id AND event_type = 'payroll.privacy_task.completed' ORDER BY id")
                .param("id", taskId).query(String.class).list()).stream().map(JSON::readTree).toList();
    }

    private String bankName(UUID tenant, UUID person) {
        return read(tenant, () -> database.jdbc().sql("SELECT b.bank_name FROM payroll.staff_bank_account b"
                + " JOIN hr.staff m ON m.id = b.staff_id AND m.tenant_id = :tenantId"
                + " WHERE b.tenant_id = :tenantId AND m.person_id = :person").param("person", person)
                .query(String.class).list().get(0)); // one row; list() because the value may be null
    }

    private int audited(UUID tenant) {
        return read(tenant, () -> database.jdbc().sql("SELECT count(*) FROM payroll.outbox_event WHERE tenant_id ="
                + " :tenantId AND event_type = 'audit.recorded'").query(Integer.class).single());
    }

    @Test
    void anErasureClearsTheBankAccountOncePerTaskAndKeepsTheWageRecords() {
        UUID tenant = UUID.randomUUID();
        UUID person = staff(tenant);
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, person);

        assertThat(bankName(tenant, person)).isNull();
        assertThat(read(tenant, () -> database.jdbc().sql("SELECT deleted_at IS NOT NULL AND account_number_enc = ''"
                + " FROM payroll.staff_bank_account WHERE tenant_id = :tenantId AND bank_name IS NULL")
                .query(Boolean.class).single())).as("the account is cleared and soft-deleted").isTrue();
        JsonNode answer = answers(tenant, taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("partial");
        assertThat(answer.path("counts").path("payroll.staff_bank_account").asInt()).isEqualTo(1);
        assertThat(answer.path("heldItems").asInt()).as("the salary structure").isEqualTo(1);
        assertThat(answer.path("policyCodes").get(0).asString()).isEqualTo("payroll.after_leaving");
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

        assertThat(bankName(tenant, person)).isEqualTo("State Bank");
        JsonNode answer = answers(tenant, taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("partial");
        assertThat(answer.path("heldItems").asInt()).as("the bank account and the salary structure").isEqualTo(2);
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
