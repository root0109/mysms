package com.mysms.privacy;

import static com.mysms.privacy.ConsentFixtures.MARKETING;
import static com.mysms.privacy.ConsentFixtures.MY_CONSENTS;
import static com.mysms.privacy.ConsentFixtures.MY_REQUESTS;
import static com.mysms.privacy.ConsentFixtures.REQUESTER;
import static com.mysms.privacy.ConsentFixtures.RIGHTS_REQUESTS;
import static com.mysms.privacy.ConsentFixtures.SELF_SERVICE;
import static com.mysms.privacy.ConsentFixtures.decision;
import static com.mysms.privacy.ConsentFixtures.rightsRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.privacy.business.TaskSweepJob;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.events.GuardianLinkSync;
import com.mysms.privacy.events.SubjectSync;
import com.mysms.privacy.events.TaskAnswerHandler;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;

/**
 * The fulfilment of erasure, export and withdraw-all requests against real PostgreSQL, the modules' answers delivered
 * to privacy's real handler as the inbox delivers them: an erasure sends its tasks to comms and iam first and to the
 * rest once they answered, and ends partially fulfilled when a module kept rows under a hold — the subject's consents
 * withdrawn, their birth date forgotten, the tombstone published; a failed task waits a back-off growing per attempt
 * before the sweep dispatches it again; a task failing for the fifth time leaves the request waiting for action until
 * a retry; an unanswered task is dispatched again by the sweep; a withdraw-all
 * request withdraws the requester's consents in one step. No event carries a hold, a name or what was erased.
 */
@SpringBootTest(classes = PrivacyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "mysms.jobs.enabled=false")
class RightsRequestTaskComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String[] STAFF = {"privacy.rights_request.read", "privacy.rights_request.assign",
        "privacy.rights_request.close", "privacy.rights_request.fulfil", "privacy.rights_request.retry_task"};
    private static final String DONE = ",\"counts\":{\"student\":1},\"heldItems\":0,\"policyCodes\":[]";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(PrivacyDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private SubjectSync subjects;

    @Autowired
    private GuardianLinkSync links;

    @Autowired
    private TaskAnswerHandler answers;

    @Autowired
    private TaskSweepJob sweep;

    private PrivacyCalls calls;
    private ConsentFixtures fixtures;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "privacy");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.privacy.erasure.modules", () -> "comms,iam,people,sis");
        registry.add("mysms.privacy.erasure.first-wave", () -> "comms,iam");
        registry.add("mysms.privacy.erasure.retry-delay", () -> "PT10M");
        registry.add("mysms.privacy.erasure.redispatch-after", () -> "PT30M");
    }

    @BeforeEach
    void connect() {
        calls = new PrivacyCalls(port, TOKENS);
        fixtures = new ConsentFixtures(calls, database, subjects, links);
    }

    /** An adult who granted marketing and submitted a request of the type, assigned to a staff member: its id. */
    private String assigned(UUID tenant, UUID self, String type) throws Exception {
        fixtures.adult(tenant, self);
        String version = fixtures.enable(tenant, MARKETING);
        assertThat(calls.post(MY_CONSENTS, calls.person(tenant, self, SELF_SERVICE),
                decision(self, MARKETING, "tenant", "granted", version, "en")).status()).isEqualTo(201);
        PrivacyCalls.Reply submitted = calls.post(MY_REQUESTS, calls.person(tenant, self, REQUESTER),
                rightsRequest(self, type, "Please act on everything you hold about me."));
        assertThat(submitted.status()).as(submitted.body().toString()).isEqualTo(201);
        String id = submitted.text("id");
        PrivacyCalls.Reply assigned = calls.post(RIGHTS_REQUESTS + "/" + id + ":assign",
                calls.administrator(tenant, STAFF), Map.of("assigneeUserId", UUID.randomUUID().toString()));
        assertThat(assigned.status()).as(assigned.body().toString()).isEqualTo(200);
        return id;
    }

    private JsonNode step(UUID tenant, String id, String step, Map<String, Object> body) throws Exception {
        PrivacyCalls.Reply stepped = calls.post(RIGHTS_REQUESTS + "/" + id + step, calls.administrator(tenant, STAFF),
                body);
        assertThat(stepped.status()).as(stepped.body().toString()).isEqualTo(200);
        return stepped.body();
    }

    private JsonNode request(UUID tenant, String id) throws Exception {
        return calls.get(RIGHTS_REQUESTS + "/" + id, calls.administrator(tenant, STAFF)).body();
    }

    /** Each task as module:wave:status:attempts, in the request's order (wave, module). */
    private static List<String> tasks(JsonNode request) {
        List<String> tasks = new ArrayList<>();
        for (JsonNode task : request.path("tasks")) {
            tasks.add(task.path("module").asString() + ":" + task.path("wave").asInt() + ":"
                    + task.path("status").asString() + ":" + task.path("attempts").asInt());
        }
        return tasks;
    }

    /** The id of the request's task of the module. */
    private static String task(JsonNode request, String module) {
        Map<String, String> ids = new LinkedHashMap<>();
        request.path("tasks").forEach(t -> ids.put(t.path("module").asString(), t.path("id").asString()));
        return ids.get(module);
    }

    /** Delivers the module's answer to its task as the inbox does; whether a task changed. */
    private boolean answer(UUID tenant, String module, String taskId, String requestId, String status, String rest) {
        String data = "{\"taskId\":\"" + taskId + "\",\"requestId\":\"" + requestId + "\",\"status\":\"" + status
                + "\"" + rest + "}";
        return CallContextHolder.callAs(context(tenant), () -> TestTransactions.of(database).write(() -> answers.apply(
                ConsentFixtures.event(tenant, module + ".privacy_task.completed", UUID.fromString(taskId), data))));
    }

    private List<String> outbox(UUID tenant) {
        return fixtures.read(tenant, () -> database.jdbc().sql(
                "SELECT o.event_type || ' ' || o.payload FROM privacy.outbox_event o WHERE o.tenant_id = :tenantId"
                        + " ORDER BY o.id").query(String.class).list());
    }

    private String single(UUID tenant, String sql, UUID person) {
        return fixtures.read(tenant, () -> database.jdbc().sql(sql).param("person", person).query(String.class)
                .single());
    }

    /** The sweep job's run for the tenant. */
    private void sweep(UUID tenant) {
        CallContextHolder.callAs(context(tenant), () -> {
            sweep.run();
            return null;
        });
    }

    /** How many minutes the failed task waits for its next attempt after its answer. */
    private int backOffMinutes(UUID tenant, String taskId) {
        return fixtures.read(tenant, () -> database.jdbc().sql(
                "SELECT CAST(EXTRACT(EPOCH FROM t.next_attempt_at - t.completed_at) / 60 AS integer)"
                        + " FROM privacy.rights_request_task t WHERE t.tenant_id = :tenantId AND t.id = :id")
                .param("id", UUID.fromString(taskId)).query(Integer.class).single());
    }

    /** Moves the failed task's next attempt an hour back, as if its back-off had run out. */
    private void comeDue(UUID tenant, String taskId) {
        CallContextHolder.callAs(context(tenant), () -> TestTransactions.of(database).write(() -> database.jdbc()
                .sql("UPDATE privacy.rights_request_task SET next_attempt_at = next_attempt_at - INTERVAL '1 hour'"
                        + " WHERE tenant_id = :tenantId AND id = :id").param("id", UUID.fromString(taskId)).update()));
    }

    private static CallContext context(UUID tenant) {
        return CallContext.anonymous(TRACE).withIdentity(tenant, null);
    }

    @Test
    void anErasureRunsWaveByWaveAndEndsWithTheConsentsWithdrawnAndATombstone() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        String id = assigned(tenant, self, "erasure");
        String etag = request(tenant, id).path("etag").asString();

        PrivacyCalls.Reply asFulfilled = calls.post(RIGHTS_REQUESTS + "/" + id + ":close",
                calls.administrator(tenant, STAFF), Map.of("outcomeCode", "fulfilled"));
        assertThat(asFulfilled.status()).as("an erasure is fulfilled by its modules").isEqualTo(409);
        assertThat(calls.post(RIGHTS_REQUESTS + "/" + id + ":startFulfilment", calls.administrator(tenant, STAFF),
                Map.of()).status()).as("the etag is required").isEqualTo(422);

        JsonNode started = step(tenant, id, ":startFulfilment", Map.of("etag", etag));
        assertThat(started.path("status").asString()).isEqualTo("in_progress");
        assertThat(tasks(started)).containsExactly("comms:1:dispatched:1", "iam:1:dispatched:1",
                "people:2:pending:0", "sis:2:pending:0");
        assertThat(calls.post(RIGHTS_REQUESTS + "/" + id + ":startFulfilment", calls.administrator(tenant, STAFF),
                Map.of("etag", started.path("etag").asString())).status()).as("started once").isEqualTo(409);
        assertThat(calls.post(RIGHTS_REQUESTS + "/" + id + ":close", calls.administrator(tenant, STAFF),
                Map.of("outcomeCode", "declined", "response", "No.")).status()).as("never declined once started")
                .isEqualTo(409);

        assertThat(answer(tenant, "comms", task(started, "comms"), id, "done", DONE)).isTrue();
        assertThat(tasks(request(tenant, id))).as("wave 2 waits for the whole first wave")
                .containsExactly("comms:1:done:1", "iam:1:dispatched:1", "people:2:pending:0", "sis:2:pending:0");
        assertThat(answer(tenant, "iam", task(started, "iam"), id, "not_applicable", DONE)).isTrue();
        assertThat(tasks(request(tenant, id))).containsExactly("comms:1:done:1", "iam:1:not_applicable:1",
                "people:2:dispatched:1", "sis:2:dispatched:1");

        assertThat(answer(tenant, "people", task(started, "people"), id, "partial",
                ",\"counts\":{\"person\":1,\"address\":2},\"heldItems\":3,\"policyCodes\":[\"finance.after_leaving\"]"))
                .isTrue();
        assertThat(answer(tenant, "sis", task(started, "sis"), id, "done", DONE)).isTrue();
        JsonNode closed = request(tenant, id);
        assertThat(closed.path("status").asString()).isEqualTo("closed");
        assertThat(closed.path("outcomeCode").asString()).isEqualTo("partially_fulfilled");
        JsonNode people = closed.path("tasks").get(2);
        assertThat(people.path("affectedRows").asInt()).isEqualTo(3);
        assertThat(people.path("heldItems").asInt()).isEqualTo(3);
        assertThat(people.path("policyCodes").get(0).asString()).isEqualTo("finance.after_leaving");

        assertThat(answer(tenant, "sis", task(started, "sis"), id, "done", DONE)).as("a repeated answer").isFalse();
        assertThatThrownBy(() -> answer(tenant, "lms", task(started, "sis"), id, "done", DONE))
                .as("an answer about another module's task").isInstanceOf(RejectedEventException.class);

        assertThat(single(tenant, "SELECT count(*)::text FROM privacy.consent_record r WHERE r.tenant_id = :tenantId"
                + " AND r.subject_person_id = :person AND r.withdrawn_at IS NULL", self)).isEqualTo("0");
        assertThat(single(tenant, "SELECT s.status || ':' || (s.birth_date IS NULL) FROM privacy.subject s"
                + " WHERE s.tenant_id = :tenantId AND s.person_id = :person", self))
                .as("a partial erasure keeps the subject's age: what a hold kept still needs it").isEqualTo("adult:false");
        List<String> events = outbox(tenant);
        assertThat(events.stream().filter(e -> e.startsWith("privacy.rights_request.task_assigned ")))
                .hasSize(4).allSatisfy(e -> assertThat(e).contains("\"deadline\"").doesNotContain("finance"));
        assertThat(events).anyMatch(e -> e.startsWith("privacy.erasure.completed ") && e.contains(id)
                        && e.contains(self.toString()))
                .anyMatch(e -> e.startsWith("privacy.consent_record.withdrawn "))
                .noneMatch(e -> e.contains("Please act on everything"));
    }

    @Test
    void aFullErasureAlsoForgetsTheSubjectsAge() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        String id = assigned(tenant, self, "erasure");
        JsonNode started = step(tenant, id, ":startFulfilment",
                Map.of("etag", request(tenant, id).path("etag").asString()));
        assertThat(answer(tenant, "comms", task(started, "comms"), id, "done", DONE)).isTrue();
        assertThat(answer(tenant, "iam", task(started, "iam"), id, "done", DONE)).isTrue();
        assertThat(answer(tenant, "people", task(started, "people"), id, "done", DONE)).isTrue();
        assertThat(answer(tenant, "sis", task(started, "sis"), id, "done", DONE)).isTrue();

        assertThat(request(tenant, id).path("outcomeCode").asString()).isEqualTo("fulfilled");
        assertThat(single(tenant, "SELECT s.status || ':' || (s.birth_date IS NULL) FROM privacy.subject s"
                + " WHERE s.tenant_id = :tenantId AND s.person_id = :person", self)).isEqualTo("unknown:true");
    }

    @Test
    void aTaskFailingForTheFifthTimeWaitsForTheStaffUntilARetry() throws Exception {
        UUID tenant = UUID.randomUUID();
        String id = assigned(tenant, UUID.randomUUID(), "export");
        JsonNode started = step(tenant, id, ":startFulfilment",
                Map.of("etag", request(tenant, id).path("etag").asString()));
        assertThat(tasks(started)).as("an export's tasks all start at once").containsExactly("comms:1:dispatched:1",
                "iam:1:dispatched:1", "people:1:dispatched:1", "sis:1:dispatched:1");
        String comms = task(started, "comms");
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThat(answer(tenant, "comms", comms, id, "failed", ",\"errorCode\":\"STORAGE_DOWN\"")).isTrue();
            if (attempt < 5) {
                comeDue(tenant, comms);
                sweep(tenant);
            }
        }
        for (String module : List.of("iam", "people", "sis")) {
            assertThat(answer(tenant, module, task(started, module), id, "done", DONE)).isTrue();
        }
        JsonNode waiting = request(tenant, id);
        assertThat(waiting.path("status").asString()).isEqualTo("action_required");
        assertThat(tasks(waiting).get(0)).isEqualTo("comms:1:failed:5");
        assertThat(waiting.path("tasks").get(0).path("lastErrorCode").asString()).isEqualTo("STORAGE_DOWN");

        JsonNode retried = step(tenant, id, "/tasks/" + comms + ":retry", Map.of("etag", waiting.path("etag")
                .asString()));
        assertThat(retried.path("status").asString()).isEqualTo("in_progress");
        assertThat(tasks(retried).get(0)).isEqualTo("comms:1:dispatched:6");
        assertThat(calls.post(RIGHTS_REQUESTS + "/" + id + "/tasks/" + task(started, "sis") + ":retry",
                calls.administrator(tenant, STAFF), Map.of()).status()).as("an answered task stays answered")
                .isEqualTo(409);
        assertThat(answer(tenant, "comms", comms, id, "done", DONE)).isTrue();
        JsonNode fulfilled = request(tenant, id);
        assertThat(fulfilled.path("status").asString()).isEqualTo("closed");
        assertThat(fulfilled.path("outcomeCode").asString()).isEqualTo("fulfilled");
    }

    @Test
    void aFailedTaskWaitsItsBackOffBeforeTheSweepDispatchesItAgain() throws Exception {
        UUID tenant = UUID.randomUUID();
        String id = assigned(tenant, UUID.randomUUID(), "export");
        JsonNode started = step(tenant, id, ":startFulfilment",
                Map.of("etag", request(tenant, id).path("etag").asString()));
        String comms = task(started, "comms");
        for (String module : List.of("iam", "people", "sis")) {
            assertThat(answer(tenant, module, task(started, module), id, "done", DONE)).isTrue();
        }
        List<Integer> backOffs = new ArrayList<>();
        for (int attempt = 1; attempt <= 3; attempt++) {
            assertThat(answer(tenant, "comms", comms, id, "failed", ",\"errorCode\":\"STORAGE_DOWN\"")).isTrue();
            assertThat(answer(tenant, "comms", comms, id, "failed", ",\"errorCode\":\"STORAGE_DOWN\""))
                    .as("a repeated answer to a waiting task changes nothing").isFalse();
            backOffs.add(backOffMinutes(tenant, comms));
            sweep(tenant);
            JsonNode waiting = request(tenant, id);
            assertThat(waiting.path("status").asString()).as("a task waiting for its next attempt holds the request")
                    .isEqualTo("in_progress");
            assertThat(tasks(waiting).get(0)).as("the sweep leaves a task whose back-off has not run out")
                    .isEqualTo("comms:1:failed:" + attempt);
            JsonNode failed = waiting.path("tasks").get(0);
            assertThat(OffsetDateTime.parse(failed.path("nextAttemptAt").asString()))
                    .as("the task shows when it is dispatched again")
                    .isAfter(OffsetDateTime.parse(failed.path("completedAt").asString()));
            comeDue(tenant, comms);
            sweep(tenant);
            JsonNode redispatched = request(tenant, id);
            assertThat(tasks(redispatched).get(0)).isEqualTo("comms:1:dispatched:" + (attempt + 1));
            assertThat(redispatched.path("tasks").get(0).has("nextAttemptAt")).as("absent once dispatched again")
                    .isFalse();
        }
        assertThat(backOffs).as("retry-delay doubled per attempt, at most redispatch-after").containsExactly(10, 20,
                30);
        assertThat(outbox(tenant).stream().filter(e -> e.startsWith("privacy.rights_request.task_assigned ")
                && e.contains(comms))).hasSize(4);
        assertThat(answer(tenant, "comms", comms, id, "done", DONE)).isTrue();
        assertThat(request(tenant, id).path("outcomeCode").asString()).isEqualTo("fulfilled");
    }

    @Test
    void theSweepDispatchesAnUnansweredTaskAgainUnderTheSameId() throws Exception {
        UUID tenant = UUID.randomUUID();
        String id = assigned(tenant, UUID.randomUUID(), "export");
        JsonNode started = step(tenant, id, ":startFulfilment",
                Map.of("etag", request(tenant, id).path("etag").asString()));
        String comms = task(started, "comms");
        CallContextHolder.callAs(context(tenant), () -> TestTransactions.of(database).write(() -> database.jdbc()
                .sql("UPDATE privacy.rights_request_task SET dispatched_at = dispatched_at - INTERVAL '2 days'"
                        + " WHERE tenant_id = :tenantId AND id = :id").param("id", UUID.fromString(comms)).update()));

        sweep(tenant);

        assertThat(tasks(request(tenant, id))).containsExactly("comms:1:dispatched:2", "iam:1:dispatched:1",
                "people:1:dispatched:1", "sis:1:dispatched:1");
        assertThat(outbox(tenant).stream().filter(e -> e.startsWith("privacy.rights_request.task_assigned ")
                && e.contains(comms))).hasSize(2);
    }

    @Test
    void aWithdrawAllRequestWithdrawsTheRequestersConsentsInOneStep() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        String id = assigned(tenant, self, "withdraw_all");

        JsonNode fulfilled = step(tenant, id, ":fulfilWithdrawAll",
                Map.of("etag", request(tenant, id).path("etag").asString()));

        assertThat(fulfilled.path("status").asString()).isEqualTo("closed");
        assertThat(fulfilled.path("outcomeCode").asString()).isEqualTo("fulfilled");
        assertThat(fulfilled.path("tasks").isEmpty()).isTrue();
        assertThat(single(tenant, "SELECT count(*)::text FROM privacy.consent_record r WHERE r.tenant_id = :tenantId"
                + " AND r.subject_person_id = :person AND r.withdrawn_at IS NULL", self)).isEqualTo("0");
        assertThat(single(tenant, "SELECT s.reason FROM privacy.consent_state s WHERE s.tenant_id = :tenantId"
                + " AND s.subject_person_id = :person AND s.purpose_code = 'marketing_comms'", self))
                .isEqualTo("NOT_ANSWERED");
        assertThat(single(tenant, "SELECT string_agg(e.event || ':' || e.channel, ',' ORDER BY e.occurred_at)"
                + " FROM privacy.consent_evidence e WHERE e.tenant_id = :tenantId", self))
                .isEqualTo("recorded:web,withdrawn:system");
        assertThat(calls.post(RIGHTS_REQUESTS + "/" + id + ":startFulfilment", calls.administrator(tenant, STAFF),
                Map.of("etag", fulfilled.path("etag").asString())).status()).isEqualTo(409);
    }
}
