package com.mysms.people;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.InboxProcessor;
import com.mysms.people.config.PeopleDatabaseConfig;
import com.mysms.people.events.PeoplePrivacyTaskHandler;
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
 * people's privacy tasks against real PostgreSQL as the application role, with privacy's legal holds from
 * {@link TestLegalHolds} (design PRIVACY-AND-CONSENT-CENTRE §9.1): an erasure clears and soft-deletes the subject's
 * rows and anonymises the person, once per task id; a held subject's rows are kept and counted; without privacy's
 * answer on the holds nothing is erased; each answer is people.privacy_task.completed on the outbox.
 */
@SpringBootTest(classes = PeopleApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PeoplePrivacyTaskComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    @Autowired
    @Qualifier(PeopleDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private PeoplePrivacyTaskHandler handler;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "people");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    /** A person with an address, a social link and a greeting, written directly. */
    private UUID person(UUID tenant) {
        UUID person = UUID.randomUUID();
        write(tenant, () -> {
            database.jdbc().sql("INSERT INTO people.person (id, tenant_id, first_name, display_name, primary_email,"
                    + " national_id_enc, row_version) VALUES (:id, :tenantId, 'Asha', 'Asha Rao', 'asha@example.org',"
                    + " 'v1:secret', 1)").param("id", person).update();
            database.jdbc().sql("INSERT INTO people.person_address (id, tenant_id, person_id, address_type, line1,"
                    + " row_version) VALUES (:id, :tenantId, :person, 'permanent', '12 Lake Road', 1)")
                    .param("id", UUID.randomUUID()).param("person", person).update();
            database.jdbc().sql("INSERT INTO people.person_social_link (id, tenant_id, person_id, network, handle,"
                    + " row_version) VALUES (:id, :tenantId, :person, 'github', 'asha-rao', 1)")
                    .param("id", UUID.randomUUID()).param("person", person).update();
            database.jdbc().sql("INSERT INTO people.greeting (id, tenant_id, person_id, greeted_on, channel,"
                    + " row_version) VALUES (:id, :tenantId, :person, '2026-11-20', 'sms', 1)")
                    .param("id", UUID.randomUUID()).param("person", person).update();
            return null;
        });
        return person;
    }

    private EventEnvelope task(UUID tenant, UUID taskId, UUID subject, String module) {
        String data = "{\"requestId\":\"" + UUID.randomUUID() + "\",\"taskId\":\"" + taskId + "\",\"type\":\"erasure\","
                + "\"action\":\"erase\",\"subjectPersonId\":\"" + subject + "\",\"module\":\"" + module + "\","
                + "\"wave\":2,\"deadline\":\"2027-01-31T00:00:00Z\"}";
        return new EventEnvelope(UUID.randomUUID(), "privacy", "privacy.rights_request.task_assigned",
                Optional.of(tenant), "privacy.rights_request_task", taskId, Instant.now(), data, Optional.of(TRACE));
    }

    /** privacy dispatches the task (through the inbox), then the module's job runs. */
    private void dispatch(EventEnvelope task) {
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        assertThat(inbox.deliver(task, handler::receive)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(task.tenantId().orElseThrow(), null),
                () -> {
                    handler.run();
                    return null;
                });
    }

    private List<JsonNode> answers(UUID tenant, UUID taskId) {
        return read(tenant, () -> database.jdbc().sql("SELECT payload FROM people.outbox_event WHERE tenant_id ="
                + " :tenantId AND aggregate_id = :id AND event_type = 'people.privacy_task.completed' ORDER BY id")
                .param("id", taskId).query(String.class).list()).stream().map(JSON::readTree).toList();
    }

    private String personColumn(UUID tenant, UUID person, String column) {
        return read(tenant, () -> database.jdbc().sql("SELECT " + column + " FROM people.person"
                + " WHERE tenant_id = :tenantId AND id = :id").param("id", person).query(String.class)
                .list().get(0)); // one row; list() because the value may be null
    }

    private int liveRows(UUID tenant, UUID person, String table) {
        return read(tenant, () -> database.jdbc().sql("SELECT count(*) FROM " + table
                + " WHERE tenant_id = :tenantId AND person_id = :person AND deleted_at IS NULL")
                .param("person", person).query(Integer.class).single());
    }

    @Test
    void anErasureAnonymisesThePersonOncePerTask() {
        UUID tenant = UUID.randomUUID();
        UUID person = person(tenant);
        UUID taskId = UUID.randomUUID();

        dispatch(task(tenant, taskId, person, "people"));

        assertThat(personColumn(tenant, person, "display_name")).isEqualTo("Erased person");
        assertThat(personColumn(tenant, person, "first_name")).isNull();
        assertThat(personColumn(tenant, person, "primary_email")).isNull();
        assertThat(personColumn(tenant, person, "national_id_enc")).as("the envelope is cleared").isNull();
        assertThat(liveRows(tenant, person, "people.person_address")).isZero();
        assertThat(liveRows(tenant, person, "people.person_social_link")).isZero();
        assertThat(liveRows(tenant, person, "people.greeting")).isZero();
        assertThat(read(tenant, () -> database.jdbc().sql("SELECT count(*) FROM people.person_address WHERE"
                + " tenant_id = :tenantId AND person_id = :person AND line1 IS NOT NULL").param("person", person)
                .query(Integer.class).single())).as("soft delete is not erasure: the values are gone").isZero();
        List<JsonNode> answers = answers(tenant, taskId);
        assertThat(answers).hasSize(1);
        JsonNode answer = answers.get(0);
        assertThat(answer.path("status").asString()).isEqualTo("done");
        assertThat(answer.path("counts").path("people.person").asInt()).isEqualTo(1);
        assertThat(answer.path("counts").path("people.person_address").asInt()).isEqualTo(1);
        assertThat(answer.path("heldItems").asInt()).isZero();
        int audited = read(tenant, () -> database.jdbc().sql("SELECT count(*) FROM people.outbox_event WHERE"
                + " tenant_id = :tenantId AND event_type = 'audit.recorded'").query(Integer.class).single());

        dispatch(task(tenant, taskId, person, "people"));

        assertThat(answers(tenant, taskId)).as("the same task is answered again, from its receipt")
                .hasSize(2).allSatisfy(again -> assertThat(again).isEqualTo(answer));
        assertThat(read(tenant, () -> database.jdbc().sql("SELECT count(*) FROM people.outbox_event WHERE"
                + " tenant_id = :tenantId AND event_type = 'audit.recorded'").query(Integer.class).single()))
                .as("and erases nothing again").isEqualTo(audited);
    }

    @Test
    void aHeldSubjectsRowsAreKeptAndCounted() {
        UUID tenant = UUID.randomUUID();
        UUID person = person(tenant);
        TestLegalHolds.holdSubject(person);
        UUID taskId = UUID.randomUUID();

        dispatch(task(tenant, taskId, person, "people"));

        assertThat(personColumn(tenant, person, "display_name")).isEqualTo("Asha Rao");
        assertThat(liveRows(tenant, person, "people.person_address")).isEqualTo(1);
        JsonNode answer = answers(tenant, taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("partial");
        assertThat(answer.path("heldItems").asInt()).as("person, address, social link and greeting").isEqualTo(4);
        assertThat(answer.path("counts").isEmpty()).isTrue();
    }

    @Test
    void withoutPrivacysAnswerOnTheHoldsNothingIsErased() {
        UUID tenant = UUID.randomUUID();
        UUID person = person(tenant);
        TestLegalHolds.unavailableFor(person);
        UUID taskId = UUID.randomUUID();

        dispatch(task(tenant, taskId, person, "people"));

        assertThat(personColumn(tenant, person, "display_name")).isEqualTo("Asha Rao");
        JsonNode answer = answers(tenant, taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("failed");
        assertThat(answer.path("errorCode").asString()).isEqualTo("LEGAL_HOLDS_UNAVAILABLE");
    }

    @Test
    void anotherModulesTaskIsIgnored() {
        UUID tenant = UUID.randomUUID();
        UUID person = person(tenant);
        UUID taskId = UUID.randomUUID();

        dispatch(task(tenant, taskId, person, "sis"));

        assertThat(personColumn(tenant, person, "display_name")).isEqualTo("Asha Rao");
        assertThat(answers(tenant, taskId)).isEmpty();
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
