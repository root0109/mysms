package com.mysms.students.careers;

import com.mysms.students.StudentsApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.students.careers.config.CareersDatabaseConfig;
import com.mysms.students.careers.events.CareersPrivacyTaskHandler;
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
 * careers' privacy tasks against real PostgreSQL as the application role, with privacy's legal holds from
 * {@link TestLegalHolds} (design PRIVACY-AND-CONSENT-CENTRE §9.1): an erasure clears and soft-deletes the subject's
 * job-search preferences once per task id; a held subject's rows are kept and counted; a person careers knows nothing
 * of is not applicable; every answer is on the outbox.
 */
@SpringBootTest(classes = StudentsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CareersPrivacyTaskComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    @Autowired
    @Qualifier(CareersDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private CareersPrivacyTaskHandler handler;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "careers", "sis", "admission");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    /** A candidate profile with a preferred location and one preferred function; the person's id. */
    private UUID candidate(UUID tenant) {
        UUID person = UUID.randomUUID();
        UUID preference = UUID.randomUUID();
        UUID function = UUID.randomUUID();
        write(tenant, () -> {
            database.jdbc().sql("INSERT INTO careers.function_area (id, tenant_id, name, code, row_version)"
                    + " VALUES (:id, :tenantId, 'Teaching', :code, 1)").param("id", function)
                    .param("code", function.toString().substring(0, 8)).update();
            database.jdbc().sql("INSERT INTO careers.candidate_preference (id, tenant_id, person_id, open_to,"
                    + " preferred_location, row_version) VALUES (:id, :tenantId, :person, 'work', 'Pune', 1)")
                    .param("id", preference).param("person", person).update();
            database.jdbc().sql("INSERT INTO careers.candidate_preferred_function (id, tenant_id,"
                    + " candidate_preference_id, function_area_id, row_version) VALUES (:id, :tenantId, :preference,"
                    + " :function, 1)").param("id", UUID.randomUUID()).param("preference", preference)
                    .param("function", function).update();
            return null;
        });
        return person;
    }

    private void erase(UUID tenant, UUID taskId, UUID subject) {
        EventEnvelope event = new EventEnvelope(UUID.randomUUID(), "privacy", "privacy.rights_request.task_assigned",
                Optional.of(tenant), "privacy.rights_request_task", taskId, Instant.now(),
                "{\"requestId\":\"" + UUID.randomUUID() + "\",\"taskId\":\"" + taskId + "\",\"type\":\"erasure\","
                + "\"action\":\"erase\",\"subjectPersonId\":\"" + subject + "\",\"module\":\"careers\",\"wave\":2,"
                + "\"deadline\":\"2027-01-31T00:00:00Z\"}", Optional.of(TRACE));
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        assertThat(inbox.deliver(event, handler::receive)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null), () -> {
            handler.run();
            return null;
        });
    }

    private List<JsonNode> answers(UUID tenant, UUID taskId) {
        return read(tenant, () -> database.jdbc().sql("SELECT payload FROM careers.outbox_event WHERE tenant_id ="
                + " :tenantId AND aggregate_id = :id AND event_type = 'careers.privacy_task.completed' ORDER BY id")
                .param("id", taskId).query(String.class).list()).stream().map(JSON::readTree).toList();
    }

    private String location(UUID tenant, UUID person) {
        return read(tenant, () -> database.jdbc().sql("SELECT preferred_location FROM careers.candidate_preference"
                + " WHERE tenant_id = :tenantId AND person_id = :person").param("person", person)
                .query(String.class).list().get(0)); // one row; list() because the value may be null
    }

    private int live(UUID tenant, UUID person) {
        return read(tenant, () -> database.jdbc().sql("SELECT count(*) FROM careers.candidate_preferred_function f"
                + " JOIN careers.candidate_preference p ON p.id = f.candidate_preference_id"
                + " WHERE f.tenant_id = :tenantId AND p.person_id = :person AND f.deleted_at IS NULL"
                + " AND p.deleted_at IS NULL").param("person", person).query(Integer.class).single());
    }

    private int audited(UUID tenant) {
        return read(tenant, () -> database.jdbc().sql("SELECT count(*) FROM careers.outbox_event WHERE tenant_id ="
                + " :tenantId AND event_type = 'audit.recorded'").query(Integer.class).single());
    }

    @Test
    void anErasureClearsThePreferencesOncePerTask() {
        UUID tenant = UUID.randomUUID();
        UUID person = candidate(tenant);
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, person);

        assertThat(location(tenant, person)).isNull();
        assertThat(live(tenant, person)).as("the profile and its function are soft-deleted").isZero();
        JsonNode answer = answers(tenant, taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("done");
        assertThat(answer.path("counts").path("careers.candidate_preference").asInt()).isEqualTo(1);
        assertThat(answer.path("counts").path("careers.candidate_preferred_function").asInt()).isEqualTo(1);
        int audits = audited(tenant);

        erase(tenant, taskId, person);

        assertThat(answers(tenant, taskId)).hasSize(2).allSatisfy(again -> assertThat(again).isEqualTo(answer));
        assertThat(audited(tenant)).as("nothing is erased twice").isEqualTo(audits);
    }

    @Test
    void aHeldSubjectsRowsAreKeptAndCounted() {
        UUID tenant = UUID.randomUUID();
        UUID person = candidate(tenant);
        TestLegalHolds.holdSubject(person);
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, person);

        assertThat(location(tenant, person)).isEqualTo("Pune");
        assertThat(live(tenant, person)).isEqualTo(1);
        JsonNode answer = answers(tenant, taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("partial");
        assertThat(answer.path("heldItems").asInt()).as("the profile and its function").isEqualTo(2);
    }

    @Test
    void anErasureOfAPersonCareersDoesNotKnowIsNotApplicable() {
        UUID tenant = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, UUID.randomUUID());

        assertThat(answers(tenant, taskId).get(0).path("status").asString()).isEqualTo("not_applicable");
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
