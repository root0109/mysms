package com.mysms.iam;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.events.IamPrivacyTaskHandler;
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
 * iam's privacy tasks against real PostgreSQL as the application role, with privacy's legal holds from
 * {@link TestLegalHolds} (design PRIVACY-AND-CONSENT-CENTRE §9.1): an erasure anonymises the subject's account and
 * deactivates it as iam.DeactivateUser does, once per task id; a held subject's account is kept and counted; a run of
 * iam.after_inactivity answers failed (its notice is not built); every answer is on the outbox.
 */
@SpringBootTest(classes = IamApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IamPrivacyTaskComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    @Autowired
    @Qualifier(IamDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private IamPrivacyTaskHandler handler;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "iam");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        // iam issues tokens: it needs a signing key even when the test signs its own tokens
        registry.add("mysms.iam.token.signing-key", TOKENS::privateKeyPem);
    }

    /** An active account of a person; the person's id. */
    private UUID account(UUID tenant) {
        UUID person = UUID.randomUUID();
        write(tenant, () -> database.jdbc().sql("INSERT INTO iam.user_account (id, tenant_id, person_id, username,"
                + " email, password_hash, status, row_version) VALUES (:id, :tenantId, :person, :username,"
                + " :email, 'hash', 'active', 1)").param("id", UUID.randomUUID()).param("person", person)
                .param("username", "u" + person.toString().substring(0, 8))
                .param("email", person + "@example.org").update());
        return person;
    }

    private void deliver(UUID tenant, String type, String aggregate, UUID id, String data) {
        EventEnvelope event = new EventEnvelope(UUID.randomUUID(), "privacy", type, Optional.of(tenant), aggregate, id,
                Instant.now(), data, Optional.of(TRACE));
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        assertThat(inbox.deliver(event, handler::receive)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null), () -> {
            handler.run();
            return null;
        });
    }

    private void erase(UUID tenant, UUID taskId, UUID subject) {
        deliver(tenant, "privacy.rights_request.task_assigned", "privacy.rights_request_task", taskId,
                "{\"requestId\":\"" + UUID.randomUUID() + "\",\"taskId\":\"" + taskId + "\",\"type\":\"erasure\","
                + "\"action\":\"erase\",\"subjectPersonId\":\"" + subject + "\",\"module\":\"iam\",\"wave\":1,"
                + "\"deadline\":\"2027-01-31T00:00:00Z\"}");
    }

    private List<JsonNode> answers(UUID tenant, String type, UUID id) {
        return read(tenant, () -> database.jdbc().sql("SELECT payload FROM iam.outbox_event WHERE tenant_id ="
                + " :tenantId AND aggregate_id = :id AND event_type = :type ORDER BY id").param("id", id)
                .param("type", type).query(String.class).list()).stream().map(JSON::readTree).toList();
    }

    private String accountColumn(UUID tenant, UUID person, String column) {
        return read(tenant, () -> database.jdbc().sql("SELECT " + column + " FROM iam.user_account"
                + " WHERE tenant_id = :tenantId AND person_id = :person").param("person", person)
                .query(String.class).list().get(0)); // one row; list() because the value may be null
    }

    private int audited(UUID tenant) {
        return read(tenant, () -> database.jdbc().sql("SELECT count(*) FROM iam.outbox_event WHERE tenant_id ="
                + " :tenantId AND event_type = 'audit.recorded'").query(Integer.class).single());
    }

    @Test
    void anErasureAnonymisesAndDeactivatesTheAccountOncePerTask() {
        UUID tenant = UUID.randomUUID();
        UUID person = account(tenant);
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, person);

        String id = accountColumn(tenant, person, "id::text");
        assertThat(accountColumn(tenant, person, "username")).isEqualTo("erased-" + id);
        assertThat(accountColumn(tenant, person, "email")).isNull();
        assertThat(accountColumn(tenant, person, "password_hash")).isNull();
        assertThat(accountColumn(tenant, person, "status")).as("signed off as DeactivateUser does")
                .isEqualTo("inactive");
        assertThat(answers(tenant, "iam.user_account.deactivateUser", UUID.fromString(id))).hasSize(1);
        JsonNode answer = answers(tenant, "iam.privacy_task.completed", taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("done");
        assertThat(answer.path("counts").path("iam.user_account").asInt()).isEqualTo(1);
        int audits = audited(tenant);

        erase(tenant, taskId, person);

        assertThat(answers(tenant, "iam.privacy_task.completed", taskId)).hasSize(2)
                .allSatisfy(again -> assertThat(again).isEqualTo(answer));
        assertThat(audited(tenant)).as("nothing is erased twice").isEqualTo(audits);
    }

    @Test
    void aHeldSubjectsAccountIsKeptAndCounted() {
        UUID tenant = UUID.randomUUID();
        UUID person = account(tenant);
        TestLegalHolds.holdSubject(person);
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, person);

        assertThat(accountColumn(tenant, person, "status")).isEqualTo("active");
        assertThat(accountColumn(tenant, person, "email")).isNotNull();
        JsonNode answer = answers(tenant, "iam.privacy_task.completed", taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("partial");
        assertThat(answer.path("heldItems").asInt()).isEqualTo(1);
    }

    @Test
    void aRetentionRunOfAPolicyIamDoesNotSweepAnswersFailed() {
        UUID tenant = UUID.randomUUID();
        UUID person = account(tenant);
        UUID runId = UUID.randomUUID();

        deliver(tenant, "privacy.retention.due", "privacy.retention_run", runId, "{\"runId\":\"" + runId
                + "\",\"policyCode\":\"iam.after_inactivity\",\"categoryCode\":\"identity\",\"module\":\"iam\","
                + "\"cutoff\":\"2023-01-01T00:00:00Z\",\"action\":\"anonymise\"}");

        assertThat(accountColumn(tenant, person, "status")).isEqualTo("active");
        JsonNode answer = answers(tenant, "iam.privacy_retention.completed", runId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("failed");
        assertThat(answer.path("errorCode").asString()).isEqualTo("RETENTION_POLICY_UNSUPPORTED");
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
