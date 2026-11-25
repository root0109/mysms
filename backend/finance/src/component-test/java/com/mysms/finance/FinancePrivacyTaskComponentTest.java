package com.mysms.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.finance.config.FinanceDatabaseConfig;
import com.mysms.finance.events.FinancePrivacyTaskHandler;
import com.mysms.finance.events.StudentRecordSync;
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
 * finance's privacy tasks against real PostgreSQL as the application role, with privacy's legal holds from
 * {@link TestLegalHolds} (design PRIVACY-AND-CONSENT-CENTRE §9.1): the person of a student record arrives with sis's
 * event; an erasure clears the raw body of the gateway's notification about the subject's payment once per task id
 * and keeps the invoice and the payment under finance.after_leaving, counted with the policy code; a held subject's
 * rows are kept and counted; every answer is on the outbox.
 */
@SpringBootTest(classes = FinanceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FinancePrivacyTaskComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String BODY = "{\"payer\":{\"email\":\"parent@example.com\"}}";

    @Autowired
    @Qualifier(FinanceDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private FinancePrivacyTaskHandler handler;

    @Autowired
    private StudentRecordSync students;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "finance");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    /**
     * A student (made known by sis's event) with an invoice, a payment of it and the gateway's notification of the
     * payment; the person's id.
     */
    private UUID student(UUID tenant) {
        UUID person = UUID.randomUUID();
        UUID student = UUID.randomUUID();
        UUID institution = UUID.randomUUID();
        deliver(tenant, "sis", "sis.student.created", "sis.student", student, "{\"id\":\"" + student + "\","
                + "\"userId\":null,\"personId\":\"" + person + "\",\"institutionId\":\"" + institution + "\","
                + "\"rowVersion\":1}", students::apply);
        UUID invoice = UUID.randomUUID();
        UUID payment = UUID.randomUUID();
        UUID currency = UUID.randomUUID();
        write(tenant, () -> {
            database.jdbc().sql("INSERT INTO finance.invoice (id, tenant_id, institution_id, invoice_number,"
                    + " student_id, bill_to_name, invoice_date, currency_id, row_version) VALUES (:id, :tenantId,"
                    + " :institution, :number, :student, 'Parent', :date, :currency, 1)").param("id", invoice)
                    .param("institution", institution).param("number", "INV-" + invoice.toString().substring(0, 8))
                    .param("student", student).param("date", LocalDate.parse("2026-04-01"))
                    .param("currency", currency).update();
            database.jdbc().sql("INSERT INTO finance.payment (id, tenant_id, institution_id, purpose, invoice_id,"
                    + " amount, currency_id, billing_name, payer_name, row_version) VALUES (:id, :tenantId,"
                    + " :institution, 'invoice', :invoice, 100, :currency, 'Parent', 'Parent', 1)")
                    .param("id", payment).param("institution", institution).param("invoice", invoice)
                    .param("currency", currency).update();
            database.jdbc().sql("INSERT INTO finance.gateway_event (id, tenant_id, gateway_name, event_type,"
                    + " payment_id, raw_payload, row_version) VALUES (:id, :tenantId, 'razorpay', 'payment.captured',"
                    + " :payment, :body, 1)").param("id", UUID.randomUUID()).param("payment", payment)
                    .param("body", BODY).update();
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
                + "\"action\":\"erase\",\"subjectPersonId\":\"" + subject + "\",\"module\":\"finance\",\"wave\":2,"
                + "\"deadline\":\"2027-01-31T00:00:00Z\"}", handler::receive);
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null), () -> {
            handler.run();
            return null;
        });
    }

    private List<JsonNode> answers(UUID tenant, UUID taskId) {
        return read(tenant, () -> database.jdbc().sql("SELECT payload FROM finance.outbox_event WHERE tenant_id ="
                + " :tenantId AND aggregate_id = :id AND event_type = 'finance.privacy_task.completed' ORDER BY id")
                .param("id", taskId).query(String.class).list()).stream().map(JSON::readTree).toList();
    }

    private String body(UUID tenant) {
        return read(tenant, () -> database.jdbc().sql("SELECT raw_payload FROM finance.gateway_event"
                + " WHERE tenant_id = :tenantId")
                .query(String.class).list().get(0)); // one row; list() because the value may be null
    }

    private int audited(UUID tenant) {
        return read(tenant, () -> database.jdbc().sql("SELECT count(*) FROM finance.outbox_event WHERE tenant_id ="
                + " :tenantId AND event_type = 'audit.recorded'").query(Integer.class).single());
    }

    @Test
    void anErasureClearsTheGatewayBodyOncePerTaskAndKeepsTheBooksOfAccount() {
        UUID tenant = UUID.randomUUID();
        UUID person = student(tenant);
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, person);

        assertThat(body(tenant)).isNull();
        assertThat(read(tenant, () -> database.jdbc().sql("SELECT count(*) FROM finance.payment WHERE tenant_id ="
                + " :tenantId AND billing_name = 'Parent' AND deleted_at IS NULL").query(Integer.class).single()))
                .as("the payment stays as it was").isEqualTo(1);
        JsonNode answer = answers(tenant, taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("partial");
        assertThat(answer.path("counts").path("finance.gateway_event").asInt()).isEqualTo(1);
        assertThat(answer.path("heldItems").asInt()).as("the invoice and the payment").isEqualTo(2);
        assertThat(answer.path("policyCodes").get(0).asString()).isEqualTo("finance.after_leaving");
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

        assertThat(body(tenant)).isEqualTo(BODY);
        JsonNode answer = answers(tenant, taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("partial");
        assertThat(answer.path("heldItems").asInt()).as("the notification, the invoice and the payment").isEqualTo(3);
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
