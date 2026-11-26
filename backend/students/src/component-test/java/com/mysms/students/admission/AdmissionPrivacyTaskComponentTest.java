package com.mysms.students.admission;

import com.mysms.students.StudentsApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.students.admission.config.AdmissionDatabaseConfig;
import com.mysms.students.admission.events.AdmissionPrivacyTaskHandler;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
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
 * admission's privacy tasks against real PostgreSQL as the application role, with privacy's legal holds from
 * {@link TestLegalHolds} (design PRIVACY-AND-CONSENT-CENTRE §9.1): an erasure clears and soft-deletes the subject's
 * application with its guardian and emergency contact once per task id, keeping the application number; a held
 * subject's rows are kept and counted; every answer is on the outbox.
 */
@SpringBootTest(classes = StudentsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdmissionPrivacyTaskComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    @Autowired
    @Qualifier(AdmissionDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private AdmissionPrivacyTaskHandler handler;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "admission", "sis", "careers");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    /** An application of the person with a guardian and an emergency contact; the person's id. */
    private UUID applicant(UUID tenant) {
        UUID person = UUID.randomUUID();
        UUID application = UUID.randomUUID();
        write(tenant, () -> {
            database.jdbc().sql("INSERT INTO admission.application (id, tenant_id, institution_id, application_number,"
                    + " applicant_person_id, full_name, date_of_birth, gender, mobile, row_version) VALUES (:id,"
                    + " :tenantId, :institution, :number, :person, 'Asha Rao', :born, 'female', '9800000000', 1)")
                    .param("id", application).param("institution", UUID.randomUUID())
                    .param("number", "APP-" + application.toString().substring(0, 8)).param("person", person)
                    .param("born", LocalDate.of(2015, 6, 1)).update();
            database.jdbc().sql("INSERT INTO admission.application_guardian (id, tenant_id, application_id,"
                    + " relationship, name, row_version) VALUES (:id, :tenantId, :application, 'mother', 'Meera Rao',"
                    + " 1)").param("id", UUID.randomUUID()).param("application", application).update();
            database.jdbc().sql("INSERT INTO admission.application_emergency_contact (id, tenant_id, application_id,"
                    + " priority, contact_number, relationship, row_version) VALUES (:id, :tenantId, :application, 1,"
                    + " '9811111111', 'Uncle', 1)").param("id", UUID.randomUUID()).param("application", application)
                    .update();
            return null;
        });
        return person;
    }

    private void erase(UUID tenant, UUID taskId, UUID subject) {
        EventEnvelope event = new EventEnvelope(UUID.randomUUID(), "privacy", "privacy.rights_request.task_assigned",
                Optional.of(tenant), "privacy.rights_request_task", taskId, Instant.now(),
                "{\"requestId\":\"" + UUID.randomUUID() + "\",\"taskId\":\"" + taskId + "\",\"type\":\"erasure\","
                + "\"action\":\"erase\",\"subjectPersonId\":\"" + subject + "\",\"module\":\"admission\",\"wave\":2,"
                + "\"deadline\":\"2027-01-31T00:00:00Z\"}", Optional.of(TRACE));
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        assertThat(inbox.deliver(event, handler::receive)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null), () -> {
            handler.run();
            return null;
        });
    }

    private List<JsonNode> answers(UUID tenant, UUID taskId) {
        return read(tenant, () -> database.jdbc().sql("SELECT payload FROM admission.outbox_event WHERE tenant_id ="
                + " :tenantId AND aggregate_id = :id AND event_type = 'admission.privacy_task.completed' ORDER BY id")
                .param("id", taskId).query(String.class).list()).stream().map(JSON::readTree).toList();
    }

    private String mobile(UUID tenant, UUID person) {
        return read(tenant, () -> database.jdbc().sql("SELECT mobile FROM admission.application WHERE tenant_id ="
                + " :tenantId AND applicant_person_id = :person").param("person", person)
                .query(String.class).list().get(0)); // one row; list() because the value may be null
    }

    private int audited(UUID tenant) {
        return read(tenant, () -> database.jdbc().sql("SELECT count(*) FROM admission.outbox_event WHERE tenant_id ="
                + " :tenantId AND event_type = 'audit.recorded'").query(Integer.class).single());
    }

    @Test
    void anErasureClearsTheApplicationOncePerTask() {
        UUID tenant = UUID.randomUUID();
        UUID person = applicant(tenant);
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, person);

        assertThat(mobile(tenant, person)).isNull();
        assertThat(read(tenant, () -> database.jdbc().sql("SELECT full_name = 'Erased applicant'"
                + " AND application_number IS NOT NULL AND deleted_at IS NOT NULL FROM admission.application"
                + " WHERE tenant_id = :tenantId AND applicant_person_id = :person").param("person", person)
                .query(Boolean.class).single())).as("a placeholder name, the number kept, soft-deleted").isTrue();
        assertThat(read(tenant, () -> database.jdbc().sql("SELECT count(*) FROM admission.application_guardian"
                + " WHERE tenant_id = :tenantId AND name = 'Erased' AND deleted_at IS NOT NULL")
                .query(Integer.class).single())).isEqualTo(1);
        JsonNode answer = answers(tenant, taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("done");
        assertThat(answer.path("counts").path("admission.application").asInt()).isEqualTo(1);
        assertThat(answer.path("counts").path("admission.application_guardian").asInt()).isEqualTo(1);
        assertThat(answer.path("counts").path("admission.application_emergency_contact").asInt()).isEqualTo(1);
        int audits = audited(tenant);

        erase(tenant, taskId, person);

        assertThat(answers(tenant, taskId)).hasSize(2).allSatisfy(again -> assertThat(again).isEqualTo(answer));
        assertThat(audited(tenant)).as("nothing is erased twice").isEqualTo(audits);
    }

    @Test
    void aHeldSubjectsRowsAreKeptAndCounted() {
        UUID tenant = UUID.randomUUID();
        UUID person = applicant(tenant);
        TestLegalHolds.holdSubject(person);
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, person);

        assertThat(mobile(tenant, person)).isEqualTo("9800000000");
        JsonNode answer = answers(tenant, taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("partial");
        assertThat(answer.path("heldItems").asInt()).as("the application, its guardian and contact").isEqualTo(3);
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
