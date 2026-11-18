package com.mysms.academics;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.FieldMask;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.testkit.GrpcParity;
import com.mysms.core.testkit.GrpcParity.Answer;
import com.mysms.core.testkit.GrpcParityContract;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.academics.config.AcademicsDatabaseConfig;
import com.mysms.academics.v1.AcademicsProto;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Every RPC of academics' eleven gRPC services against the same operation over REST, on real PostgreSQL: the same
 * resource and the same refusals (403, 404, 409, 412, 428, 422) over both transports; the five review steps of a lesson
 * plan change it alike; a plan's reviews read alike and are written by the review only; a step retried with its
 * request_id answers the first result.
 */
@SpringBootTest(classes = AcademicsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=AcademicsGrpcParityComponentTest")
class AcademicsGrpcParityComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final String ADMIN = token(TENANT, List.of("academics.*"));
    private static final String NOBODY = token(TENANT, List.of());
    private static final String STRANGER = token(OTHER_TENANT, List.of("academics.*"));
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;


    @Autowired
    private ProtoBridge bridge;

    @Autowired
    @Qualifier(AcademicsDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "academics");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @AfterAll
    static void closeChannel() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    /** A tenant-wide administrator of the tenant holding the permissions (every institution, tenant-level rows). */
    private static String token(UUID tenant, List<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", UUID.randomUUID().toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", permissions);
        claims.put("tperms", permissions);
        claims.put("ins", List.of("*"));
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    // ------------------------------------------------------------------ the two transports

    private Answer rest(String method, String path, String token, String body, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode json = response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body());
        return new Answer(response.statusCode(), response.statusCode() >= 400 ? json.path("code").asString() : null,
                json);
    }

    private Channel channel() {
        if (channel == null) {
            channel = GrpcParity.channel("AcademicsGrpcParityComponentTest");
        }
        return channel;
    }

    private Answer grpc(MethodDescriptor rpc, DynamicMessage request, String token) {
        return GrpcParity.call(channel(), rpc, request, token);
    }

    private MethodDescriptor rpc(String name) {
        return contract().rpc(name);
    }

    private static DynamicMessage.Builder request(MethodDescriptor rpc) {
        return GrpcParityContract.request(rpc);
    }

    private static DynamicMessage.Builder set(DynamicMessage.Builder builder, String field, Object value) {
        return GrpcParityContract.set(builder, field, value);
    }

    private void fixtures(UUID tenant, List<String> statements) {
        CallContext context = CallContext.anonymous(TRACE).withIdentity(tenant, null);
        CallContextHolder.callAs(context, () -> TestTransactions.of(database).write(() -> {
            statements.forEach(statement -> database.jdbc().sql(statement).param("tenant", tenant).update());
            return null;
        }));
    }

    private static JsonNode spec() throws Exception {
        return JSON.readTree(AcademicsGrpcParityComponentTest.class.getClassLoader().getResourceAsStream("crud-spec.json"));
    }

    private static List<String> strings(JsonNode array) {
        List<String> strings = new ArrayList<>();
        array.forEach(element -> strings.add(element.asString()));
        return strings;
    }

    // ------------------------------------------------------------------ the standard operations, resource by resource

    private GrpcParityContract contract() {
        return new GrpcParityContract(AcademicsProto.getDescriptor(), this::rest, this::channel, bridge,
                new GrpcParityContract.Callers(ADMIN, NOBODY, STRANGER));
    }

    /**
     * The resources serving all five standard operations; the reviews of a lesson plan (written by the review only)
     * and Batch have their own tests below — the contract names a resource's list RPC by adding "s", and Batch's is
     * ListBatches.
     */
    @TestFactory
    List<DynamicNode> everyStandardRpcAnswersLikeRest() throws Exception {
        List<DynamicNode> tests = new ArrayList<>();
        for (GrpcParityContract.Resource resource : GrpcParityContract.resources("crud-spec.json")) {
            if (!resource.standardOnly()) {
                continue;
            }
            tests.add(DynamicTest.dynamicTest(resource.name(), () -> {
                fixtures(TENANT, resource.fixtures());
                fixtures(OTHER_TENANT, resource.fixtures());
                contract().verify(resource);
            }));
        }
        return tests;
    }

    private static JsonNode resource(String name) throws Exception {
        for (JsonNode resource : spec().path("resources")) {
            if (name.equals(resource.path("name").asString())) {
                return resource;
            }
        }
        throw new IllegalArgumentException("no resource " + name + " in crud-spec.json");
    }

    private static String q(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    private static List<String> ids(JsonNode rows, String field) {
        List<String> ids = new ArrayList<>();
        rows.forEach(row -> ids.add(row.path(field).asString()));
        return ids;
    }


    /** An update request: the row's id and etag in the resource with the changes, and a mask naming them. */
    private DynamicMessage updateRequest(MethodDescriptor update, String field, String id, String etag,
            Map<String, Object> changes, String... masked) {
        Map<String, Object> resource = new HashMap<>(changes);
        resource.put("id", id);
        resource.put("etag", etag);
        return set(bridge.reply(request(update), field, resource), "update_mask",
                FieldMask.newBuilder().addAllPaths(List.of(masked)).build()).build();
    }

    // ------------------------------------------------------------------ the state transitions of crud-spec.json

    @TestFactory
    List<DynamicNode> everyTransitionAnswersLikeRest() throws Exception {
        List<DynamicNode> tests = new ArrayList<>();
        for (JsonNode resource : spec().path("resources")) {
            for (JsonNode transition : resource.path("transitions")) {
                tests.add(DynamicTest.dynamicTest(transition.path("operation").asString(),
                        () -> verifyTransition(resource, transition)));
            }
        }
        return tests;
    }

    /** A new row of the resource in the state the transition starts from. */
    private String prepared(JsonNode resource, JsonNode transition) throws Exception {
        fixtures(TENANT, strings(resource.path("fixtures")));
        Answer row = rest("POST", resource.path("collection").asString(), ADMIN, resource.path("create").toString());
        assertThat(row.status()).as(row.body().toString()).isEqualTo(201);
        String id = row.body().path("id").asString();
        List<String> statements = new ArrayList<>();
        transition.path("prepare").forEach(statement -> statements.add(statement.asString().replace("{id}", id)));
        fixtures(TENANT, statements);
        return id;
    }

    /** The transition's request: the path id (its first field) and the REST body's fields (etag, reason). */
    private static DynamicMessage.Builder transitionRequest(MethodDescriptor rpc, String id, JsonNode body) {
        DynamicMessage.Builder request = set(request(rpc), rpc.getInputType().getFields().get(0).getName(), id);
        body.propertyNames().forEach(name -> set(request, name, body.path(name).asString()));
        return request;
    }

    /**
     * One transition over both transports: refused alike without the permission, for an absent row, a malformed id
     * and a stale etag; taken over REST on one row and over gRPC on another with the same result; and refused alike
     * (409) once the row has left the state the step starts from.
     */
    private void verifyTransition(JsonNode resource, JsonNode transition) throws Exception {
        String operation = transition.path("operation").asString();
        MethodDescriptor rpc = rpc(operation.substring(operation.indexOf('.') + 1));
        String collection = resource.path("collection").asString();
        String path = transition.path("path").asString();
        String action = path.substring(path.lastIndexOf(':'));
        ObjectNode body = (ObjectNode) transition.path("body");
        String overRest = prepared(resource, transition);
        String overGrpc = prepared(resource, transition);

        GrpcParityContract.assertSameAnswer(rest("POST", collection + "/" + overRest + action, NOBODY, body.toString()),
                grpc(rpc, transitionRequest(rpc, overGrpc, body).build(), NOBODY));
        String absent = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(rest("POST", collection + "/" + absent + action, ADMIN, body.toString()),
                grpc(rpc, transitionRequest(rpc, absent, body).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("POST", collection + "/not-a-uuid" + action, ADMIN, body.toString()),
                grpc(rpc, transitionRequest(rpc, "not-a-uuid", body).build(), ADMIN));
        ObjectNode stale = body.deepCopy().put("etag", "\"99\"");
        Answer staleRest = rest("POST", collection + "/" + overRest + action, ADMIN, stale.toString());
        assertThat(staleRest.status()).as(operation + " with a stale etag").isEqualTo(412);
        GrpcParityContract.assertSameAnswer(staleRest, grpc(rpc, transitionRequest(rpc, overGrpc, stale).build(), ADMIN));

        Answer changedRest = rest("POST", collection + "/" + overRest + action, ADMIN, body.toString());
        assertThat(changedRest.status()).as(changedRest.body().toString()).isEqualTo(200);
        Answer changedGrpc = grpc(rpc, transitionRequest(rpc, overGrpc, body).build(), ADMIN);
        assertThat(changedGrpc.status()).as("%s: %s", operation, changedGrpc.code()).isEqualTo(200);
        JsonNode row = rest("GET", collection + "/" + overGrpc, ADMIN, null).body();
        GrpcParity.assertSameResource(row,
                changedGrpc.body().path(rpc.getOutputType().getFields().get(0).getJsonName()));
        for (Map.Entry<String, JsonNode> set : transition.path("sets").properties()) {
            assertThat(changedRest.body().path(set.getKey())).as(set.getKey() + " over REST").isEqualTo(set.getValue());
            assertThat(row.path(set.getKey())).as(set.getKey() + " over gRPC").isEqualTo(set.getValue());
        }

        Answer again = rest("POST", collection + "/" + overRest + action, ADMIN, body.toString());
        assertThat(again.status()).as(operation + " taken twice").isEqualTo(409);
        GrpcParityContract.assertSameAnswer(again, grpc(rpc, transitionRequest(rpc, overGrpc, body).build(), ADMIN));
    }

    // ------------------------------------------------------------------ the idempotency key

    @Test
    void aTransitionRetriedWithItsRequestIdAnswersTheFirstResultOnBoth() throws Exception {
        JsonNode resource = resource("LessonPlan");
        JsonNode transition = resource.path("transitions").get(0);
        String operation = transition.path("operation").asString();
        MethodDescriptor rpc = rpc(operation.substring(operation.indexOf('.') + 1));
        String path = transition.path("path").asString();
        String action = path.substring(path.lastIndexOf(':'));
        String overRest = resource.path("collection").asString() + "/" + prepared(resource, transition) + action;
        String overGrpc = prepared(resource, transition);

        String restKey = UUID.randomUUID().toString();
        Answer first = rest("POST", overRest, ADMIN, "{}", "Idempotency-Key", restKey);
        Answer retried = rest("POST", overRest, ADMIN, "{}", "Idempotency-Key", restKey);
        assertThat(first.status()).as(first.body().toString()).isEqualTo(200);
        assertThat(retried.status()).as("the retry is the first answer, not a second step").isEqualTo(200);
        assertThat(retried.body()).isEqualTo(first.body());

        DynamicMessage keyed = set(transitionRequest(rpc, overGrpc, transition.path("body")), "request_id",
                UUID.randomUUID().toString()).build();
        Answer firstGrpc = grpc(rpc, keyed, ADMIN);
        Answer retriedGrpc = grpc(rpc, keyed, ADMIN);
        assertThat(firstGrpc.status()).as(operation + ": " + firstGrpc.code()).isEqualTo(200);
        assertThat(retriedGrpc.status()).as("the retry is the first answer, not a second step").isEqualTo(200);
        assertThat(retriedGrpc.body()).isEqualTo(firstGrpc.body());

        // without the key the step is a second one: refused alike
        GrpcParityContract.assertSameAnswer(rest("POST", overRest, ADMIN, "{}"),
                grpc(rpc, transitionRequest(rpc, overGrpc, transition.path("body")).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("POST", overRest, ADMIN, "{}", "Idempotency-Key", "not-a-uuid"),
                grpc(rpc, set(transitionRequest(rpc, overGrpc, transition.path("body")), "request_id", "not-a-uuid").build(),
                        ADMIN));
    }

    // ------------------------------------------------------------------ the reviews of a lesson plan

    @Test
    void aLessonPlansReviewsReadAlikeAndAreWrittenByTheReviewOnlyOnBoth() throws Exception {
        JsonNode resource = resource("LessonPlanApproval");
        fixtures(TENANT, strings(resource.path("fixtures")));
        fixtures(TENANT, strings(resource.path("seed")));
        String collection = resource.path("collection").asString();
        String plan = collection.split("/")[4];
        String id = resource.path("seedId").asString();

        MethodDescriptor get = rpc("GetLessonPlanApproval");
        Answer read = rest("GET", collection + "/" + id, ADMIN, null);
        assertThat(read.status()).as(read.body().toString()).isEqualTo(200);
        Answer got = grpc(get, set(set(request(get), "lesson_plan_id", plan), "id", id).build(), ADMIN);
        assertThat(got.status()).as("GetLessonPlanApproval: " + got.code()).isEqualTo(200);
        GrpcParity.assertSameResource(read.body(), got.body().path("lessonPlanApproval"));
        String absent = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(rest("GET", collection + "/" + absent, ADMIN, null),
                grpc(get, set(set(request(get), "lesson_plan_id", plan), "id", absent).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", collection + "/" + id, NOBODY, null),
                grpc(get, set(set(request(get), "lesson_plan_id", plan), "id", id).build(), NOBODY));
        GrpcParityContract.assertSameAnswer(rest("GET", collection + "/" + id, STRANGER, null),
                grpc(get, set(set(request(get), "lesson_plan_id", plan), "id", id).build(), STRANGER));

        MethodDescriptor list = rpc("ListLessonPlanApprovals");
        Answer listed = rest("GET", collection, ADMIN, null);
        Answer listedGrpc = grpc(list, set(request(list), "lesson_plan_id", plan).build(), ADMIN);
        assertThat(listedGrpc.status()).as("ListLessonPlanApprovals: " + listedGrpc.code()).isEqualTo(200);
        assertThat(ids(listedGrpc.body().path("items"), "id")).containsExactly(id)
                .isEqualTo(ids(listed.body().path("data"), "id"));
        GrpcParity.assertSameResource(listed.body().path("data").get(0), listedGrpc.body().path("items").get(0));
        GrpcParityContract.assertSameAnswer(rest("GET", collection.replace(plan, absent), ADMIN, null),
                grpc(list, set(request(list), "lesson_plan_id", absent).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", collection + "?filter=" + q("nope = 1"), ADMIN, null),
                grpc(list, set(set(request(list), "lesson_plan_id", plan), "filter", "nope = 1").build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", collection, NOBODY, null),
                grpc(list, set(request(list), "lesson_plan_id", plan).build(), NOBODY));

        // a review is written by approving or rejecting the plan: create, update and delete are refused alike
        MethodDescriptor create = rpc("CreateLessonPlanApproval");
        Answer created = rest("POST", collection, ADMIN, "{\"comments\":\"By hand\"}");
        assertThat(created.status()).as(created.body().toString()).isEqualTo(409);
        GrpcParityContract.assertSameAnswer(created, grpc(create, set(bridge.reply(request(create),
                "lesson_plan_approval", Map.of("comments", "By hand")), "lesson_plan_id", plan).build(), ADMIN));
        MethodDescriptor update = rpc("UpdateLessonPlanApproval");
        String etag = read.body().path("etag").asString();
        Answer updated = rest("PATCH", collection + "/" + id, ADMIN, "{\"comments\":\"Changed\"}", "If-Match", etag);
        assertThat(updated.status()).as(updated.body().toString()).isEqualTo(409);
        GrpcParityContract.assertSameAnswer(updated, grpc(update, set(updateRequest(update, "lesson_plan_approval", id,
                etag, Map.of("comments", "Changed"), "comments").toBuilder(), "lesson_plan_id", plan).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("PATCH", collection + "/" + id, ADMIN, "{\"comments\":\"Changed\"}"),
                grpc(update, set(updateRequest(update, "lesson_plan_approval", id, "", Map.of("comments", "Changed"),
                        "comments").toBuilder(), "lesson_plan_id", plan).build(), ADMIN));
        MethodDescriptor delete = rpc("DeleteLessonPlanApproval");
        Answer deleted = rest("DELETE", collection + "/" + id, ADMIN, null, "If-Match", etag);
        assertThat(deleted.status()).as(deleted.body().toString()).isEqualTo(409);
        GrpcParityContract.assertSameAnswer(deleted, grpc(delete, set(set(set(request(delete), "lesson_plan_id", plan),
                "id", id), "etag", etag).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("DELETE", collection + "/" + id, ADMIN, null),
                grpc(delete, set(set(request(delete), "lesson_plan_id", plan), "id", id).build(), ADMIN));
    }

    @Test
    void aReviewWrittenByApprovingOverGrpcReadsLikeOneWrittenOverRest() throws Exception {
        JsonNode resource = resource("LessonPlan");
        JsonNode approve = resource.path("transitions").get(1);
        String collection = resource.path("collection").asString();
        String overRest = prepared(resource, approve);
        String overGrpc = prepared(resource, approve);
        MethodDescriptor rpc = rpc("ApproveLessonPlan");
        assertThat(rest("POST", collection + "/" + overRest + ":approve", ADMIN, "{\"reason\":\"Well planned\"}").status())
                .isEqualTo(200);
        assertThat(grpc(rpc, set(set(request(rpc), "lesson_plan_id", overGrpc), "reason", "Well planned").build(), ADMIN)
                .status()).isEqualTo(200);

        MethodDescriptor list = rpc("ListLessonPlanApprovals");
        JsonNode reviewRest = rest("GET", collection + "/" + overRest + "/approvals", ADMIN, null).body().path("data");
        JsonNode reviewGrpc = grpc(list, set(request(list), "lesson_plan_id", overGrpc).build(), ADMIN).body().path("items");
        assertThat(reviewRest).hasSize(1);
        assertThat(reviewGrpc).hasSize(1);
        assertThat(reviewGrpc.get(0).path("decision").asString()).isEqualTo("DECISION_APPROVED");
        assertThat(reviewGrpc.get(0).path("comments").asString()).isEqualTo(reviewRest.get(0).path("comments").asString())
                .isEqualTo("Well planned");
    }

}
