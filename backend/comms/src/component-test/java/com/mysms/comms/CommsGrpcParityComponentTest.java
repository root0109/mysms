package com.mysms.comms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.v1.CommsProto;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.testkit.GrpcParity;
import com.mysms.core.testkit.GrpcParity.Answer;
import com.mysms.core.testkit.GrpcParityContract;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * Every RPC of comms's 22 gRPC services against the same REST operation, on real PostgreSQL: for each resource of
 * crud-spec.json the standard operations give the same resource over both transports and both refuse alike (403, 404,
 * 409, 412, 428, 422) — the resources this release only reads or deletes from their seeded row; every state transition
 * of crud-spec.json answers and refuses alike; the long-running operations read and cancel alike; a create replays
 * under its idempotency key on both; and every operation this release does not serve is 501 on both.
 */
@SpringBootTest(classes = CommsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=CommsGrpcParityComponentTest")
class CommsGrpcParityComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String STALE = "\"99\"";
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final String ADMIN = token(UUID.randomUUID(), TENANT, List.of("comms.*"), true);
    private static final String NOBODY = token(UUID.randomUUID(), TENANT, List.of(), true);
    private static final String STRANGER = token(UUID.randomUUID(), OTHER_TENANT, List.of("comms.*"), true);
    /**
     * The REST bodies the unserved operations need to reach their controller (a body is validated before it); every
     * other one takes an empty object.
     */
    private static final Map<String, String> UNSERVED_BODIES = Map.of(
            "AddGroupMembers", "{\"userIds\":[\"0196f000-0000-7000-8000-00000000b001\"],\"memberType\":\"student\"}",
            "ExportCalendar", "{\"provider\":\"google\",\"from\":\"2026-01-01T00:00:00Z\",\"to\":\"2026-02-01T00:00:00Z\"}",
            "FollowClassPage", "{\"follow\":true}",
            "MarkFeedbackMessage", "{\"reviewStatus\":\"seen\"}",
            "ComposeMail", "{\"subject\":\"Subject\",\"body\":\"Body\",\"toUserIds\":[\"0196f000-0000-7000-8000-00000000b001\"]}",
            "SendTestMessage", "{\"to\":\"someone@example.org\"}",
            "SendMessage", "{\"channel\":\"sms\",\"recipients\":{},\"messageKind\":\"notification\",\"body\":\"Body\"}",
            "CreatePostReaction", "{\"userId\":\"0196f000-0000-7000-8000-00000000b001\",\"reaction\":\"like\"}");
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;


    @Autowired
    private ProtoBridge bridge;

    @Autowired
    @Qualifier(CommsDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "comms");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @AfterAll
    static void closeChannel() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    /**
     * A user of the tenant holding the permissions in every institution — tenant-wide too (an administrator, who also
     * reaches other users' rows) or not (who reaches only their own owner-only rows and operations).
     */
    private static String token(UUID user, UUID tenant, List<String> permissions, boolean tenantWide) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", permissions);
        claims.put("tperms", tenantWide ? permissions : List.of());
        claims.put("ins", List.of("*"));
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    // ---- the two transports

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
            channel = GrpcParity.channel("CommsGrpcParityComponentTest");
        }
        return channel;
    }

    private Answer grpc(MethodDescriptor rpc, DynamicMessage request, String token) {
        return GrpcParity.call(channel(), rpc, request, token);
    }

    private MethodDescriptor rpc(String name) {
        return contract(ADMIN).rpc(name);
    }

    /** A request from its protobuf JSON (lowerCamelCase names, RFC 3339 timestamps, enum value names). */
    private static DynamicMessage request(MethodDescriptor rpc, ObjectNode json) throws Exception {
        DynamicMessage.Builder builder = GrpcParityContract.request(rpc);
        JsonFormat.parser().merge(json.toString(), builder);
        return builder.build();
    }

    private static ObjectNode object() {
        return JSON.createObjectNode();
    }

    private void sql(UUID tenant, List<String> statements) {
        CallContext context = CallContext.anonymous(TRACE).withIdentity(tenant, null);
        CallContextHolder.callAs(context, () -> TestTransactions.of(database).write(() -> {
            statements.forEach(statement -> database.jdbc().sql(statement).param("tenant", tenant).update());
            return null;
        }));
    }

    private static List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.asString()));
        return values;
    }

    private static JsonNode spec() throws Exception {
        return JSON.readTree(CommsGrpcParityComponentTest.class.getClassLoader().getResourceAsStream("crud-spec.json"));
    }

    private static List<String> ids(JsonNode items, String field) {
        List<String> ids = new ArrayList<>();
        items.forEach(item -> ids.add(item.path(field).asString()));
        return ids;
    }

    /** The request's path ids: a parent's from the resource's create body, the row's own otherwise. */
    private static ObjectNode pathIds(MethodDescriptor rpc, JsonNode createBody, String id) {
        ObjectNode json = object();
        for (FieldDescriptor field : rpc.getInputType().getFields()) {
            boolean pathId = "id".equals(field.getName())
                    || field.getName().endsWith("_id") && !"request_id".equals(field.getName());
            if (pathId && field.getJavaType() == FieldDescriptor.JavaType.STRING && !field.isRepeated()) {
                json.put(field.getJsonName(), createBody.has(field.getJsonName())
                        ? createBody.path(field.getJsonName()).asString() : id);
            }
        }
        return json;
    }

    /** The JSON name of the one field a response carries. */
    private static String resultField(MethodDescriptor rpc) {
        return rpc.getOutputType().getFields().get(0).getJsonName();
    }

    // ---- the standard operations, resource by resource

    private GrpcParityContract contract(String admin) {
        return new GrpcParityContract(CommsProto.getDescriptor(), this::rest, this::channel, bridge,
                new GrpcParityContract.Callers(admin, NOBODY, STRANGER));
    }

    /**
     * The administrator of a resource: platform staff for global data, the owner of the rows where only the owner
     * reaches them, the tenant's administrator otherwise.
     */
    private static String admin(JsonNode resource) {
        if ("global".equals(resource.path("scope").asString())) {
            return TOKENS.platformOperator(UUID.randomUUID(), "comms.*");
        }
        return resource.has("ownerOnly")
                ? token(UUID.fromString(resource.path("ownerOnly").asString()), TENANT, List.of("comms.*"), true) : ADMIN;
    }

    /**
     * Every resource of crud-spec.json: the standard operations it serves answer alike over both transports — from a
     * row created over gRPC, or from its seeded row where the API creates none.
     */
    @TestFactory
    List<DynamicNode> everyStandardRpcAnswersLikeRest() throws Exception {
        List<DynamicNode> tests = new ArrayList<>();
        for (GrpcParityContract.Resource resource : GrpcParityContract.resources("crud-spec.json")) {
            tests.add(DynamicTest.dynamicTest(resource.name(), () -> {
                sql(TENANT, resource.fixtures());
                sql(TENANT, resource.seed());
                contract(admin(resource.described())).verify(resource);
            }));
        }
        assertThat(tests).hasSize(39);
        return tests;
    }

    // ---- the state transitions

    /**
     * Every transition of crud-spec.json: both transports refuse alike without the permission, for a stale etag and
     * for an absent row, both perform it from its guarded state with the same resulting resource, and both answer
     * alike when it is repeated.
     */
    @TestFactory
    List<DynamicNode> everyTransitionAnswersLikeRest() throws Exception {
        List<DynamicNode> tests = new ArrayList<>();
        for (JsonNode resource : spec().path("resources")) {
            for (JsonNode transition : resource.path("transitions")) {
                tests.add(DynamicTest.dynamicTest(transition.path("operation").asString(),
                        () -> transition(resource, transition)));
            }
        }
        assertThat(tests).hasSize(14);
        return tests;
    }

    private void transition(JsonNode resource, JsonNode transition) throws Exception {
        sql(TENANT, strings(resource.path("fixtures")));
        JsonNode create = resource.path("create");
        Answer created = rest("POST", resource.path("collection").asString(), ADMIN, create.toString());
        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
        String id = created.body().path("id").asString();
        String item = resource.path("item").asString().replace("{id}", id);
        String path = transition.path("path").asString();
        String absent = UUID.randomUUID().toString();
        List<String> prepare = strings(transition.path("prepare")).stream().map(s -> s.replace("{id}", id)).toList();
        ObjectNode body = (ObjectNode) transition.path("body").deepCopy();
        ObjectNode stale = body.deepCopy().put("etag", STALE);
        MethodDescriptor rpc = rpc(transition.path("operation").asString().substring("comms.".length()));

        sql(TENANT, prepare);
        GrpcParityContract.assertSameAnswer(rest("POST", path.replace("{id}", id), NOBODY, body.toString()),
                grpc(rpc, request(rpc, pathIds(rpc, create, id).setAll(body)), NOBODY));
        Answer refused = rest("POST", path.replace("{id}", id), ADMIN, stale.toString());
        assertThat(refused.status()).as("a stale etag").isEqualTo(412);
        GrpcParityContract.assertSameAnswer(refused, grpc(rpc, request(rpc, pathIds(rpc, create, id).setAll(stale)), ADMIN));
        Answer missing = rest("POST", path.replace("{id}", absent), ADMIN, body.toString());
        assertThat(missing.status()).as("an absent row").isEqualTo(404);
        GrpcParityContract.assertSameAnswer(missing, grpc(rpc, request(rpc, pathIds(rpc, create, absent).setAll(body)), ADMIN));
        if (!body.isEmpty()) {
            // without the fields the body carries, both answer alike (a required one is 422 on both)
            Answer bare = rest("POST", path.replace("{id}", id), ADMIN, "{}");
            sql(TENANT, prepare);
            GrpcParityContract.assertSameAnswer(bare, grpc(rpc, request(rpc, pathIds(rpc, create, id)), ADMIN));
            sql(TENANT, prepare);
        }

        Answer overRest = rest("POST", path.replace("{id}", id), ADMIN, body.toString());
        assertThat(overRest.status()).as(overRest.body().toString()).isEqualTo(200);
        sql(TENANT, prepare);
        Answer overGrpc = grpc(rpc, request(rpc, pathIds(rpc, create, id).setAll(body)), ADMIN);
        assertThat(overGrpc.status()).as("%s: %s", rpc.getName(), overGrpc.code()).isEqualTo(200);
        GrpcParity.assertSameResource(rest("GET", item, ADMIN, null).body(), overGrpc.body().path(resultField(rpc)));
        ((ObjectNode) transition.path("sets")).properties().forEach(set -> assertThat(overRest.body().path(set.getKey()))
                .as(set.getKey()).isEqualTo(set.getValue()));

        // repeated outside its guard (or inside it, where the transition does not leave it): the same answer
        Answer repeated = rest("POST", path.replace("{id}", id), ADMIN, body.toString());
        GrpcParityContract.assertSameAnswer(repeated,
                grpc(rpc, request(rpc, pathIds(rpc, create, id).setAll(body)), ADMIN));
    }

    // ---- long-running operations

    @Test
    void theLongRunningOperationsAnswerLikeRest() throws Exception {
        UUID user = UUID.randomUUID();
        String own = token(user, TENANT, List.of("comms.*"), false);
        String[] mine = {UUID.randomUUID().toString(), UUID.randomUUID().toString()};
        String theirs = UUID.randomUUID().toString();
        String insert = "INSERT INTO comms.operation (id, tenant_id, operation_name, status, progress_percent,"
                + " started_by_user_id, started_at, created_at, updated_at, row_version) VALUES ('%s', :tenant, '%s',"
                + " 'running', 40, '%s', now() - interval '%d minutes', now(), now(), 1)";
        sql(TENANT, List.of(insert.formatted(mine[0], "report.run", user, 3),
                insert.formatted(mine[1], "report.run", user, 2),
                insert.formatted(theirs, "import.rows", UUID.randomUUID(), 1)));

        MethodDescriptor get = rpc("GetOperation");
        Answer read = rest("GET", "/v1/comms/operations/" + mine[0], own, null);
        assertThat(read.status()).as(read.body().toString()).isEqualTo(200);
        Answer got = grpc(get, request(get, object().put("operationId", mine[0])), own);
        assertThat(got.status()).as(got.code()).isEqualTo(200);
        GrpcParity.assertSameResource(read.body(), got.body().path("result"));
        for (String token : List.of(own, NOBODY, STRANGER)) {
            // someone else's operation, no permission, another tenant
            GrpcParityContract.assertSameAnswer(rest("GET", "/v1/comms/operations/" + theirs, token, null),
                    grpc(get, request(get, object().put("operationId", theirs)), token));
        }
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/comms/operations/not-a-uuid", own, null),
                grpc(get, request(get, object().put("operationId", "not-a-uuid")), own));

        MethodDescriptor list = rpc("ListOperations");
        Answer listed = rest("GET", "/v1/comms/operations", own, null);
        Answer listedGrpc = grpc(list, request(list, object()), own);
        assertThat(ids(listedGrpc.body().path("items"), "operationId")).containsExactly(mine[1], mine[0])
                .isEqualTo(ids(listed.body().path("data"), "operationId"));
        GrpcParity.assertSameResource(listed.body().path("data").get(0), listedGrpc.body().path("items").get(0));
        Answer paged = rest("GET", "/v1/comms/operations?size=1", own, null);
        ObjectNode firstPage = object();
        firstPage.putObject("pageable").put("size", 1);
        Answer pagedGrpc = grpc(list, request(list, firstPage), own);
        assertThat(ids(pagedGrpc.body().path("items"), "operationId")).containsExactly(mine[1])
                .isEqualTo(ids(paged.body().path("data"), "operationId"));
        GrpcParity.assertSameCursor(paged.body().path("paging").path("next").asString(),
                pagedGrpc.body().path("pagination").path("nextCursor").asString());
        Answer kind = rest("GET", "/v1/comms/operations?kind=import.rows", ADMIN, null);
        Answer kindGrpc = grpc(list, request(list, object().put("kind", "import.rows")), ADMIN);
        assertThat(ids(kindGrpc.body().path("items"), "operationId")).containsExactly(theirs)
                .isEqualTo(ids(kind.body().path("data"), "operationId"));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/comms/operations", NOBODY, null),
                grpc(list, request(list, object()), NOBODY));

        MethodDescriptor cancel = rpc("CancelOperation");
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/comms/operations/" + theirs + ":cancel", own, null),
                grpc(cancel, request(cancel, object().put("operationId", theirs)), own));
        Answer cancelled = rest("POST", "/v1/comms/operations/" + mine[0] + ":cancel", own, null);
        assertThat(cancelled.status()).as(cancelled.body().toString()).isEqualTo(202);
        Answer cancelledGrpc = grpc(cancel, request(cancel, object().put("operationId", mine[1])), own);
        assertThat(cancelledGrpc.status()).as(cancelledGrpc.code()).isEqualTo(200);
        GrpcParity.assertSameResource(rest("GET", "/v1/comms/operations/" + mine[1], own, null).body(),
                cancelledGrpc.body().path("result"));
        assertThat(cancelledGrpc.body().path("result").path("status").asString()).isEqualTo("cancelled");
        Answer again = rest("POST", "/v1/comms/operations/" + mine[0] + ":cancel", own, null);
        assertThat(again.status()).isEqualTo(409);
        GrpcParityContract.assertSameAnswer(again, grpc(cancel, request(cancel, object().put("operationId", mine[1])), own));
    }

    // ---- idempotency

    @Test
    void aCreateReplaysUnderItsIdempotencyKeyOnBoth() throws Exception {
        String body = "{\"institutionId\":\"0196f000-0000-7000-8000-00000000a001\",\"audience\":\"school\","
                + "\"title\":\"Replayed\",\"body\":\"Once\"}";
        String key = UUID.randomUUID().toString();
        Answer first = rest("POST", "/v1/comms/notices", ADMIN, body, "Idempotency-Key", key);
        assertThat(first.status()).as(first.body().toString()).isEqualTo(201);
        Answer replayed = rest("POST", "/v1/comms/notices", ADMIN, body, "Idempotency-Key", key);
        assertThat(replayed.body().path("id").asString()).isEqualTo(first.body().path("id").asString());

        MethodDescriptor create = rpc("CreateNotice");
        ObjectNode request = object().put("requestId", UUID.randomUUID().toString());
        request.putObject("notice").put("institutionId", "0196f000-0000-7000-8000-00000000a001")
                .put("audience", "AUDIENCE_SCHOOL").put("title", "Replayed").put("body", "Once");
        Answer firstGrpc = grpc(create, request(create, request), ADMIN);
        assertThat(firstGrpc.status()).as(firstGrpc.code()).isEqualTo(200);
        Answer replayedGrpc = grpc(create, request(create, request), ADMIN);
        assertThat(replayedGrpc.body()).isEqualTo(firstGrpc.body());
        assertThat(firstGrpc.body().path("notice").path("id").asString()).isNotEqualTo(first.body().path("id").asString());
        Answer fresh = grpc(create, request(create, request.deepCopy().put("requestId", UUID.randomUUID().toString())),
                ADMIN);
        assertThat(fresh.body().path("notice").path("id").asString())
                .isNotEqualTo(firstGrpc.body().path("notice").path("id").asString());
        // the key reused for another request is refused alike
        String other = body.replace("Once", "Twice");
        ((ObjectNode) request.path("notice")).put("body", "Twice");
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/comms/notices", ADMIN, other, "Idempotency-Key", key),
                grpc(create, request(create, request), ADMIN));
    }

    // ---- the operations this release does not serve

    /**
     * The operations whose controller answers 501, and the ten of the two services REST has no controller for (in-app
     * notifications, comment reactions), which REST answers 501 as well.
     */
    private static final Set<String> UNSERVED = Set.of("SendMessage", "CancelOutboundMessage", "RetryOutboundMessage",
            "ComposeMail", "MarkMailRead", "CloseNoticeReplies", "GetMyCalendar", "CompletePersonalEvent",
            "MarkConversationRead", "HideMessageForMe", "MarkFeedbackMessage", "AddGroupMembers", "ClaimTask",
            "FollowClassPage", "ListPostReactions", "GetPostReaction", "CreatePostReaction", "UpdatePostReaction",
            "DeletePostReaction", "SubmitPost", "GetFeed", "ExportCalendar", "SendTestMessage", "ListInAppNotifications",
            "GetInAppNotification", "MarkNotificationRead", "MarkAllNotificationsRead", "ArchiveNotifications",
            "ListCommentReactions", "GetCommentReaction", "CreateCommentReaction", "UpdateCommentReaction",
            "DeleteCommentReaction");

    @Test
    void theOperationsThisReleaseDoesNotServeAre501OnBoth() throws Exception {
        JsonNode operations = JSON.readTree(getClass().getClassLoader()
                .getResourceAsStream("META-INF/mysms/operations/comms.json"));
        int checked = 0;
        for (JsonNode operation : operations) {
            String name = operation.path("operationId").asString().substring("comms.".length());
            if (!UNSERVED.contains(name)) {
                continue;
            }
            MethodDescriptor rpc = rpc(name);
            ObjectNode request = object();
            if (rpc.getInputType().findFieldByName("request_id") != null) {
                request.put("requestId", UUID.randomUUID().toString());
            }
            Answer overGrpc = grpc(rpc, request(rpc, request), ADMIN);
            assertThat(overGrpc.status()).as(name).isEqualTo(501);
            assertThat(overGrpc.code()).as(name).isEqualTo("NOT_IMPLEMENTED");
            assertThat(grpc(rpc, request(rpc, request), null).status()).as("%s: authentication first", name).isEqualTo(401);
            assertThat(grpc(rpc, request(rpc, request), NOBODY).status()).as("%s: then the permission", name).isEqualTo(403);
            String method = operation.path("method").asString();
            String path = operation.path("path").asString().replaceAll("\\{[^}]+}", UUID.randomUUID().toString());
            if ("GetMyCalendar".equals(name)) {
                path += "?from=2026-01-01T00:00:00Z&to=2026-02-01T00:00:00Z"; // required: without them REST answers 400
            }
            String body = "GET".equals(method) || "DELETE".equals(method) ? null : UNSERVED_BODIES.getOrDefault(name, "{}");
            Answer overRest = rest(method, path, ADMIN, body, "If-Match", "\"1\"", "Idempotency-Key",
                    UUID.randomUUID().toString());
            assertThat(overRest.status()).as("%s over REST: %s", name, overRest.body()).isEqualTo(501);
            GrpcParityContract.assertSameAnswer(overRest, overGrpc);
            checked++;
        }
        assertThat(checked).isEqualTo(UNSERVED.size()).isEqualTo(33);
    }
}
