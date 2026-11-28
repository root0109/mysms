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
 * under its idempotency key on both; and every workflow operation gives the same result and the same refusals over
 * both ({@link #theInboxAnswersLikeRest} and the tests after it).
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
        return JSON.readTree(CommsGrpcParityComponentTest.class.getClassLoader().getResourceAsStream("comms/crud-spec.json"));
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
        for (GrpcParityContract.Resource resource : GrpcParityContract.resources("comms/crud-spec.json")) {
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
        assertThat(tests).hasSize(15);
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

    // ---- the workflow operations

    private static final String SCHOOL = "0196f000-0000-7000-8000-00000000a001";

    /** The operation over both transports with the same request: the same answer, and for a success the same body. */
    private Answer same(String rpcName, ObjectNode overGrpc, String method, String path, String body, String token)
            throws Exception {
        MethodDescriptor rpc = rpc(rpcName);
        if (rpc.getInputType().findFieldByName("request_id") != null && !overGrpc.has("requestId")) {
            overGrpc.put("requestId", UUID.randomUUID().toString());
        }
        Answer grpcAnswer = grpc(rpc, request(rpc, overGrpc), token);
        Answer restAnswer = rest(method, path, token, body);
        GrpcParityContract.assertSameAnswer(restAnswer, grpcAnswer);
        if (restAnswer.status() == 200 && !rpc.getOutputType().getFields().isEmpty()) {
            JsonNode result = grpcAnswer.body().path(resultField(rpc));
            boolean list = restAnswer.body().has("data") || restAnswer.body().has("items");
            if (result.isArray() || list && result.isMissingNode()) {
                JsonNode items = restAnswer.body().has("data") ? restAnswer.body().path("data")
                        : restAnswer.body().path("items");
                assertThat(ids(result, "id")).as(rpcName).isEqualTo(ids(items, "id"));
                for (int i = 0; i < items.size(); i++) {
                    GrpcParity.assertSameResource(items.get(i), result.get(i));
                }
            } else {
                GrpcParity.assertSameResource(restAnswer.body(), result);
            }
        }
        return restAnswer;
    }

    /** The step over gRPC: it succeeds, and its result is what REST reads back afterwards. */
    private void readsBack(String rpcName, ObjectNode overGrpc, String readPath, String token) throws Exception {
        MethodDescriptor rpc = rpc(rpcName);
        overGrpc.put("requestId", UUID.randomUUID().toString());
        Answer done = grpc(rpc, request(rpc, overGrpc), token);
        assertThat(done.status()).as("%s: %s", rpcName, done.code()).isEqualTo(200);
        Answer read = rest("GET", readPath, token, null);
        assertThat(read.status()).as(read.body().toString()).isEqualTo(200);
        GrpcParity.assertSameResource(read.body(), done.body().path(resultField(rpc)));
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }

    @Test
    void theInboxAnswersLikeRest() throws Exception {
        UUID me = UUID.randomUUID();
        String token = token(me, TENANT, List.of("comms.*"), false);
        List<String> mine = List.of(newId(), newId(), newId());
        String theirs = newId();
        List<String> rows = new ArrayList<>();
        for (String id : mine) {
            rows.add("INSERT INTO comms.in_app_notification (id, tenant_id, recipient_user_id, notification_type_id,"
                    + " title, created_at) VALUES ('" + id + "', :tenant, '" + me + "', '"
                    + "0196f0c0-6000-7000-8000-000000000001', 'Hello', now() - interval '1 hour')");
        }
        rows.add("INSERT INTO comms.in_app_notification (id, tenant_id, recipient_user_id, notification_type_id, title)"
                + " VALUES ('" + theirs + "', :tenant, '" + UUID.randomUUID() + "', "
                + "'0196f0c0-6000-7000-8000-000000000001', 'Hello')");
        sql(TENANT, rows);
        Instant now = Instant.now();
        String range = "createTime >= \"" + now.minusSeconds(86_400) + "\" AND createTime <= \"" + now.plusSeconds(60)
                + "\"";
        String query = "?filter=" + java.net.URLEncoder.encode(range, java.nio.charset.StandardCharsets.UTF_8);

        Answer listed = same("ListInAppNotifications", object().put("filter", range), "GET",
                "/v1/comms/notifications" + query, null, token);
        assertThat(listed.body().path("data")).hasSize(3);
        same("ListInAppNotifications", object(), "GET", "/v1/comms/notifications", null, token);
        same("GetInAppNotification", object().put("id", mine.get(0)), "GET",
                "/v1/comms/notifications/" + mine.get(0), null, token);
        same("GetInAppNotification", object().put("id", theirs), "GET",
                "/v1/comms/notifications/" + theirs, null, token);
        readsBack("MarkNotificationRead", object().put("inAppNotificationId", mine.get(0)),
                "/v1/comms/notifications/" + mine.get(0), token);
        same("MarkNotificationRead", object().put("inAppNotificationId", mine.get(0)), "POST",
                "/v1/comms/notifications/" + mine.get(0) + ":markRead", "{}", token);
        same("MarkNotificationRead", object().put("inAppNotificationId", theirs), "POST",
                "/v1/comms/notifications/" + theirs + ":markRead", "{}", token);
        same("MarkAllNotificationsRead", object(), "POST", "/v1/comms/notifications:markAllRead", "{}", token);
        ObjectNode archive = object();
        archive.putArray("notificationIds").add(mine.get(1));
        same("ArchiveNotifications", archive, "POST", "/v1/comms/notifications:archive",
                "{\"notificationIds\":[\"" + mine.get(2) + "\"]}", token);
        ObjectNode malformed = object();
        malformed.putArray("notificationIds").add("not-a-uuid");
        same("ArchiveNotifications", malformed, "POST", "/v1/comms/notifications:archive",
                "{\"notificationIds\":[\"not-a-uuid\"]}", token);
        assertThat(ids(rest("GET", "/v1/comms/notifications" + query, token, null).body().path("data"), "id"))
                .as("both archived theirs").containsExactly(mine.get(0));
    }

    /** A class page of the school with these members. */
    private String page(UUID... members) {
        String category = newId();
        String page = newId();
        List<String> rows = new ArrayList<>(List.of("INSERT INTO comms.page_category (id, tenant_id, institution_id,"
                + " name, code, row_version) VALUES ('" + category + "', :tenant, '" + SCHOOL + "', 'Classes', 'P"
                + category.substring(0, 8) + "', 1)", "INSERT INTO comms.class_page (id, tenant_id, institution_id,"
                + " page_category_id, name, description, row_version) VALUES ('" + page + "', :tenant, '" + SCHOOL
                + "', '" + category + "', 'Class 6B', 'The class page', 1)"));
        for (UUID member : members) {
            rows.add("INSERT INTO comms.class_page_member (id, tenant_id, class_page_id, user_id, row_version) VALUES ('"
                    + newId() + "', :tenant, '" + page + "', '" + member + "', 1)");
        }
        sql(TENANT, rows);
        return page;
    }

    private String post(UUID author, String page, String status) {
        String id = newId();
        sql(TENANT, List.of("INSERT INTO comms.post (id, tenant_id, institution_id, academic_year_id, class_page_id,"
                + " author_user_id, body, status, posted_at, row_version) VALUES ('" + id + "', :tenant, '" + SCHOOL
                + "', '" + newId() + "', " + (page == null ? "NULL" : "'" + page + "'") + ", '" + author + "', 'News', '"
                + status + "', now(), 1)"));
        return id;
    }

    @Test
    void theFeedAndTheReactionsAnswerLikeRest() throws Exception {
        UUID me = UUID.randomUUID();
        String token = token(me, TENANT, List.of("comms.*"), true);
        String page = page(me);
        String onPage = post(UUID.randomUUID(), page, "posted");
        String schoolWide = post(UUID.randomUUID(), null, "posted");
        String hidden = post(UUID.randomUUID(), page(UUID.randomUUID()), "posted");

        Answer feed = same("GetFeed", object(), "GET", "/v1/comms/posts:feed", null, token);
        assertThat(ids(feed.body().path("data"), "id")).contains(onPage, schoolWide).doesNotContain(hidden);
        same("GetFeed", object().put("classPageId", page), "GET", "/v1/comms/posts:feed?classPageId=" + page, null,
                token);
        same("GetPost", object().put("id", hidden), "GET", "/v1/comms/posts/" + hidden, null, token);

        String reactions = "/v1/comms/posts/" + onPage + "/reactions";
        ObjectNode create = object().put("postId", onPage).put("requestId", newId());
        create.putObject("postReaction").put("reaction", "REACTION_LIKE");
        MethodDescriptor createRpc = rpc("CreatePostReaction");
        Answer created = grpc(createRpc, request(createRpc, create), token);
        assertThat(created.status()).as(created.code()).isEqualTo(200);
        String reaction = created.body().path("postReaction").path("id").asString();
        GrpcParity.assertSameResource(rest("GET", reactions + "/" + reaction, token, null).body(),
                created.body().path("postReaction"));
        ObjectNode again = object().put("postId", onPage).put("requestId", newId());
        again.putObject("postReaction").put("reaction", "REACTION_LIKE");
        same("CreatePostReaction", again, "POST", reactions, "{\"reaction\":\"like\"}", token);
        ObjectNode unreadable = object().put("postId", hidden).put("requestId", newId());
        unreadable.putObject("postReaction").put("reaction", "REACTION_LIKE");
        same("CreatePostReaction", unreadable, "POST", "/v1/comms/posts/" + hidden + "/reactions",
                "{\"reaction\":\"like\"}", token);
        same("ListPostReactions", object().put("postId", onPage), "GET", reactions, null, token);
        same("GetPostReaction", object().put("postId", onPage).put("id", reaction), "GET",
                reactions + "/" + reaction, null, token);
        String stranger = token(UUID.randomUUID(), TENANT, List.of("comms.*"), true);
        same("GetPostReaction", object().put("postId", onPage).put("id", reaction), "GET",
                reactions + "/" + reaction, null, stranger);
        ObjectNode stale = object().put("postId", onPage).put("id", reaction).put("reaction", "REACTION_UNLIKE")
                .put("etag", STALE);
        MethodDescriptor update = rpc("UpdatePostReaction");
        GrpcParityContract.assertSameAnswer(rest("PATCH", reactions + "/" + reaction, token,
                "{\"reaction\":\"unlike\"}", "If-Match", STALE), grpc(update, request(update, stale), token));
        ObjectNode change = object().put("postId", onPage).put("id", reaction).put("reaction", "REACTION_UNLIKE")
                .put("etag", "\"1\"");
        Answer changed = grpc(update, request(update, change), token);
        assertThat(changed.status()).as(changed.code()).isEqualTo(200);
        GrpcParity.assertSameResource(rest("GET", reactions + "/" + reaction, token, null).body(),
                changed.body().path("postReaction"));
        Answer unchanged = grpc(update, request(update, object().put("postId", onPage).put("id", reaction)
                .put("etag", "\"2\"")), token);
        assertThat(unchanged.body().path("postReaction").path("etag").asString()).as("an empty update changes nothing")
                .isEqualTo("\"2\"");
        MethodDescriptor delete = rpc("DeletePostReaction");
        ObjectNode remove = object().put("postId", onPage).put("id", reaction).put("etag", "\"2\"");
        GrpcParityContract.assertSameAnswer(rest("DELETE", reactions + "/" + reaction, stranger, null, "If-Match",
                "\"2\""), grpc(delete, request(delete, remove), stranger));
        assertThat(grpc(delete, request(delete, remove), token).status()).isEqualTo(200);
        GrpcParityContract.assertSameAnswer(rest("GET", reactions + "/" + reaction, token, null),
                grpc(rpc("GetPostReaction"), request(rpc("GetPostReaction"), object().put("postId", onPage)
                        .put("id", reaction)), token));

        String comment = newId();
        sql(TENANT, List.of("INSERT INTO comms.post_comment (id, tenant_id, post_id, author_user_id, body, row_version)"
                + " VALUES ('" + comment + "', :tenant, '" + onPage + "', '" + UUID.randomUUID() + "', 'Nice', 1)"));
        ObjectNode onComment = object().put("requestId", newId());
        onComment.putObject("commentReaction").put("commentId", comment).put("reaction", "REACTION_LIKE");
        MethodDescriptor createComment = rpc("CreateCommentReaction");
        Answer liked = grpc(createComment, request(createComment, onComment), token);
        assertThat(liked.status()).as(liked.code()).isEqualTo(200);
        String commentReaction = liked.body().path("commentReaction").path("id").asString();
        GrpcParity.assertSameResource(rest("GET", "/v1/comms/comment-reactions/" + commentReaction, token, null).body(),
                liked.body().path("commentReaction"));
        ObjectNode duplicate = object().put("requestId", newId());
        duplicate.putObject("commentReaction").put("commentId", comment).put("reaction", "REACTION_LIKE");
        same("CreateCommentReaction", duplicate, "POST", "/v1/comms/comment-reactions",
                "{\"commentId\":\"" + comment + "\",\"reaction\":\"like\"}", token);
        same("ListCommentReactions", object(), "GET", "/v1/comms/comment-reactions", null, token);
        same("GetCommentReaction", object().put("id", commentReaction), "GET",
                "/v1/comms/comment-reactions/" + commentReaction, null, stranger);
        ObjectNode deactivate = object().put("id", commentReaction).put("etag", "\"1\"");
        deactivate.putObject("isActive").put("value", false);
        MethodDescriptor updateComment = rpc("UpdateCommentReaction");
        Answer deactivated = grpc(updateComment, request(updateComment, deactivate), token);
        assertThat(deactivated.status()).as(deactivated.code()).isEqualTo(200);
        GrpcParity.assertSameResource(rest("GET", "/v1/comms/comment-reactions/" + commentReaction, token, null)
                .body(), deactivated.body().path("commentReaction"));
        Answer untouched = grpc(updateComment, request(updateComment, object().put("id", commentReaction)
                .put("etag", "\"2\"")), token);
        assertThat(untouched.body().path("commentReaction").path("etag").asString())
                .as("an empty update changes nothing").isEqualTo("\"2\"");
        MethodDescriptor deleteComment = rpc("DeleteCommentReaction");
        ObjectNode staleDelete = object().put("id", commentReaction).put("etag", STALE);
        GrpcParityContract.assertSameAnswer(rest("DELETE", "/v1/comms/comment-reactions/" + commentReaction, token,
                null, "If-Match", STALE), grpc(deleteComment, request(deleteComment, staleDelete), token));
        assertThat(grpc(deleteComment, request(deleteComment, object().put("id", commentReaction)
                .put("etag", "\"2\"")), token).status()).isEqualTo(200);

        String draft = post(me, page, "draft");
        String twin = post(me, page, "draft");
        readsBack("SubmitPost", object().put("postId", draft), "/v1/comms/posts/" + draft, token);
        assertThat(rest("POST", "/v1/comms/posts/" + twin + ":submit", token, "{}").status()).isEqualTo(200);
        same("SubmitPost", object().put("postId", draft), "POST", "/v1/comms/posts/" + twin + ":submit", "{}", token);
    }

    @Test
    void thePageGroupAndTaskStepsAnswerLikeRest() throws Exception {
        UUID me = UUID.randomUUID();
        String token = token(me, TENANT, List.of("comms.*"), true);
        String page = page(me);
        same("FollowClassPage", object().put("classPageId", page).put("follow", true), "POST",
                "/v1/comms/class-pages/" + page + ":follow", "{\"follow\":true}", token);
        String foreign = page(UUID.randomUUID());
        same("FollowClassPage", object().put("classPageId", foreign).put("follow", true), "POST",
                "/v1/comms/class-pages/" + foreign + ":follow", "{\"follow\":true}", token);

        String group = newId();
        String member = newId();
        sql(TENANT, List.of("INSERT INTO comms.audience_group (id, tenant_id, institution_id, name, row_version) VALUES"
                + " ('" + group + "', :tenant, '" + SCHOOL + "', 'Choir', 1)"));
        ObjectNode add = object().put("audienceGroupId", group).put("memberType", "MEMBER_TYPE_STUDENT");
        add.putArray("userIds").add(member);
        Answer added = same("AddGroupMembers", add, "POST", "/v1/comms/audience-groups/" + group + ":addMembers",
                "{\"userIds\":[\"" + member + "\"],\"memberType\":\"student\"}", token);
        assertThat(added.body().path("items")).hasSize(1);
        ObjectNode none = object().put("audienceGroupId", group).put("memberType", "MEMBER_TYPE_STUDENT");
        none.putArray("userIds");
        same("AddGroupMembers", none, "POST", "/v1/comms/audience-groups/" + group + ":addMembers",
                "{\"userIds\":[],\"memberType\":\"student\"}", token);

        String task = newId();
        sql(TENANT, List.of("INSERT INTO comms.task (id, tenant_id, institution_id, owner_user_id, title, task_category,"
                + " due_at, reminder_option, checklist, assignment_mode, row_version) VALUES ('" + task + "', :tenant, '"
                + SCHOOL + "', '" + UUID.randomUUID() + "', 'Collect forms', 'teacher', now() + interval '1 day',"
                + " 'one_day_before', '[]', 'open', 1)", "INSERT INTO comms.task_assignee (id, tenant_id, task_id,"
                + " assignee_type, user_id, row_version) VALUES ('" + newId() + "', :tenant, '" + task + "', 'user', '"
                + me + "', 1)"));
        same("ClaimTask", object().put("taskId", task), "POST", "/v1/comms/tasks/" + task + ":claim", "{}", token);
        same("ClaimTask", object().put("taskId", task), "POST", "/v1/comms/tasks/" + task + ":claim", "{}",
                token(UUID.randomUUID(), TENANT, List.of("comms.*"), true));
    }

    @Test
    void theCalendarAnswersLikeRest() throws Exception {
        UUID me = UUID.randomUUID();
        String token = token(me, TENANT, List.of("comms.*"), true);
        String event = newId();
        String first = newId();
        String second = newId();
        sql(TENANT, List.of("INSERT INTO comms.event (id, tenant_id, institution_id, audience, owner_user_id, title,"
                + " start_at, status, row_version) VALUES ('" + event + "', :tenant, '" + SCHOOL + "', 'personal', '"
                + me + "', 'Revise', '2026-05-01T08:00:00Z', 'active', 1)", "INSERT INTO comms.event_occurrence (id,"
                + " tenant_id, event_id, start_at, row_version) VALUES ('" + first + "', :tenant, '" + event
                + "', '2026-05-02T08:00:00Z', 1)", "INSERT INTO comms.event_occurrence (id, tenant_id, event_id,"
                + " start_at, row_version) VALUES ('" + second + "', :tenant, '" + event
                + "', '2026-05-03T08:00:00Z', 1)"));
        ObjectNode window = object().put("from", "2026-05-01T00:00:00Z").put("to", "2026-06-01T00:00:00Z");
        Answer calendar = same("GetMyCalendar", window, "GET", "/v1/comms/me/calendar?from=2026-05-01T00:00:00Z"
                + "&to=2026-06-01T00:00:00Z", null, token);
        assertThat(ids(calendar.body().path("data"), "id")).containsExactly(first, second);
        same("GetMyCalendar", object().put("from", "2026-05-01T00:00:00Z").put("to", "2026-04-01T00:00:00Z"), "GET",
                "/v1/comms/me/calendar?from=2026-05-01T00:00:00Z&to=2026-04-01T00:00:00Z", null, token);

        String occurrences = "/v1/comms/events/" + event + "/occurrences/";
        readsBack("CompletePersonalEvent", object().put("eventId", event).put("eventOccurrenceId", first),
                occurrences + first, token);
        assertThat(rest("POST", occurrences + second + ":complete", token, "{}").status()).isEqualTo(200);
        same("CompletePersonalEvent", object().put("eventId", event).put("eventOccurrenceId", first), "POST",
                occurrences + second + ":complete", "{}", token);
        same("CompletePersonalEvent", object().put("eventId", event).put("eventOccurrenceId", first), "POST",
                occurrences + first + ":complete", "{}", token(UUID.randomUUID(), TENANT, List.of("comms.*"), true));

        ObjectNode export = window.deepCopy().put("provider", "PROVIDER_GOOGLE");
        Answer noPlatform = same("ExportCalendar", export, "POST", "/v1/comms/me/calendar:export",
                "{\"provider\":\"google\",\"from\":\"2026-05-01T00:00:00Z\",\"to\":\"2026-06-01T00:00:00Z\"}", token);
        assertThat(noPlatform.status()).as("the file store does not run in a module test").isEqualTo(503);
    }

    @Test
    void theConversationAndFeedbackStepsAnswerLikeRest() throws Exception {
        UUID me = UUID.randomUUID();
        UUID teacher = UUID.randomUUID();
        String token = token(me, TENANT, List.of("comms.*"), true);
        String stranger = token(UUID.randomUUID(), TENANT, List.of("comms.*"), true);
        String conversation = newId();
        String message = newId();
        sql(TENANT, List.of("INSERT INTO comms.conversation (id, tenant_id, institution_id, conversation_type,"
                + " created_by, row_version) VALUES ('" + conversation + "', :tenant, '" + SCHOOL + "', 'direct', '"
                + teacher + "', 1)", "INSERT INTO comms.conversation_participant (id, tenant_id, conversation_id,"
                + " user_id, row_version) VALUES ('" + newId() + "', :tenant, '" + conversation + "', '" + me + "', 1)",
                "INSERT INTO comms.conversation_message (id, tenant_id, conversation_id, sender_user_id, body,"
                + " row_version) VALUES ('" + message + "', :tenant, '" + conversation + "', '" + teacher
                + "', 'Hello', 1)"));
        same("MarkConversationRead", object().put("conversationId", conversation), "POST",
                "/v1/comms/conversations/" + conversation + ":markRead", "{}", token);
        same("MarkConversationRead", object().put("conversationId", conversation), "POST",
                "/v1/comms/conversations/" + conversation + ":markRead", "{}", stranger);
        same("GetConversation", object().put("id", conversation), "GET",
                "/v1/comms/conversations/" + conversation, null, stranger);
        same("HideMessageForMe", object().put("messageId", message), "POST", "/v1/comms/messages/" + message
                + ":hideForMe", "{}", token);
        same("HideMessageForMe", object().put("messageId", message), "POST", "/v1/comms/messages/" + message
                + ":hideForMe", "{}", stranger);

        String thread = newId();
        String[] messages = {newId(), newId()};
        List<String> rows = new ArrayList<>(List.of("INSERT INTO comms.feedback_thread (id, tenant_id, institution_id,"
                + " guardian_id, student_id, assigned_to_user_id, row_version) VALUES ('" + thread + "', :tenant, '"
                + SCHOOL + "', '" + UUID.randomUUID() + "', '" + UUID.randomUUID() + "', '" + me + "', 1)"));
        for (String id : messages) {
            rows.add("INSERT INTO comms.feedback_message (id, tenant_id, thread_id, sender_user_id, category, message,"
                    + " row_version) VALUES ('" + id + "', :tenant, '" + thread + "', '" + teacher
                    + "', 'suggestion', 'More sport', 1)");
        }
        sql(TENANT, rows);
        readsBack("MarkFeedbackMessage", object().put("messageId", messages[0]).put("reviewStatus",
                "REVIEW_STATUS_SEEN"), "/v1/comms/feedback-threads/" + thread + "/messages/" + messages[0], token);
        assertThat(rest("POST", "/v1/comms/feedback-messages/" + messages[1] + ":markReviewStatus", token,
                "{\"reviewStatus\":\"seen\"}").status()).isEqualTo(200);
        same("MarkFeedbackMessage", object().put("messageId", messages[0]).put("reviewStatus", "REVIEW_STATUS_SEEN"),
                "POST", "/v1/comms/feedback-messages/" + messages[1] + ":markReviewStatus",
                "{\"reviewStatus\":\"seen\"}", token);
    }

    /**
     * The author of a post or comment and the sender of a message are the caller over gRPC as over REST, whatever the
     * request says, and both refuse another user's edit alike.
     */
    @Test
    void theAuthorAndTheSenderComeFromTheTokenLikeRest() throws Exception {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        String token = token(me, TENANT, List.of("comms.*"), true);
        String theirs = token(other, TENANT, List.of("comms.*"), true);

        ObjectNode post = object();
        post.putObject("post").put("institutionId", SCHOOL).put("academicYearId", newId())
                .put("authorUserId", other.toString()).put("body", "News");
        String postId = created("CreatePost", post, "post", me, "/v1/comms/posts/", token);
        String item = "/v1/comms/posts/" + postId;
        ObjectNode editPost = object().put("id", postId).put("body", "Mine").put("etag", "\"1\"");
        GrpcParityContract.assertSameAnswer(rest("PATCH", item, theirs, "{\"body\":\"Mine\"}", "If-Match", "\"1\""),
                grpc(rpc("UpdatePost"), request(rpc("UpdatePost"), editPost), theirs));

        ObjectNode comment = object().put("postId", postId);
        comment.putObject("postComment").put("authorUserId", other.toString()).put("body", "Nice");
        String commentId = created("CreatePostComment", comment, "postComment", me, item + "/comments/", token);
        ObjectNode editComment = object().put("postId", postId).put("id", commentId).put("body", "Mine")
                .put("etag", "\"1\"");
        GrpcParityContract.assertSameAnswer(rest("PATCH", item + "/comments/" + commentId, theirs,
                "{\"body\":\"Mine\"}", "If-Match", "\"1\""),
                grpc(rpc("UpdatePostComment"), request(rpc("UpdatePostComment"), editComment), theirs));

        String conversation = newId();
        List<String> rows = new ArrayList<>(List.of("INSERT INTO comms.conversation (id, tenant_id, institution_id,"
                + " conversation_type, created_by, row_version) VALUES ('" + conversation + "', :tenant, '" + SCHOOL
                + "', 'direct', '" + other + "', 1)"));
        for (UUID participant : List.of(me, other)) {
            rows.add("INSERT INTO comms.conversation_participant (id, tenant_id, conversation_id, user_id, row_version)"
                    + " VALUES ('" + newId() + "', :tenant, '" + conversation + "', '" + participant + "', 1)");
        }
        String thread = newId();
        rows.add("INSERT INTO comms.feedback_thread (id, tenant_id, institution_id, guardian_id, student_id,"
                + " assigned_to_user_id, row_version) VALUES ('" + thread + "', :tenant, '" + SCHOOL + "', '"
                + newId() + "', '" + newId() + "', '" + other + "', 1)");
        sql(TENANT, rows);

        String messages = "/v1/comms/conversations/" + conversation + "/messages/";
        ObjectNode message = object().put("conversationId", conversation);
        message.putObject("conversationMessage").put("senderUserId", other.toString()).put("body", "Hi");
        String messageId = created("CreateConversationMessage", message, "conversationMessage", me, messages, token);
        ObjectNode editMessage = object().put("conversationId", conversation).put("id", messageId).put("body", "Mine")
                .put("etag", "\"1\"");
        GrpcParityContract.assertSameAnswer(rest("PATCH", messages + messageId, theirs, "{\"body\":\"Mine\"}",
                "If-Match", "\"1\""),
                grpc(rpc("UpdateConversationMessage"), request(rpc("UpdateConversationMessage"), editMessage), theirs));

        String feedback = "/v1/comms/feedback-threads/" + thread + "/messages/";
        ObjectNode sent = object().put("feedbackThreadId", thread);
        sent.putObject("feedbackMessage").put("senderUserId", other.toString()).put("category", "CATEGORY_COMPLAINT")
                .put("message", "Late");
        String feedbackId = created("CreateFeedbackMessage", sent, "feedbackMessage", me, feedback, token);
        ObjectNode editFeedback = object().put("feedbackThreadId", thread).put("id", feedbackId).put("message", "Mine")
                .put("etag", "\"1\"");
        GrpcParityContract.assertSameAnswer(rest("PATCH", feedback + feedbackId, theirs, "{\"message\":\"Mine\"}",
                "If-Match", "\"1\""),
                grpc(rpc("UpdateFeedbackMessage"), request(rpc("UpdateFeedbackMessage"), editFeedback), theirs));
    }

    /**
     * Creates the row over gRPC: it is stored as the caller's ({@code author}, whatever the request named) and REST
     * reads back the same resource at {@code itemPath} + its id.
     */
    private String created(String rpcName, ObjectNode overGrpc, String resultField, UUID author, String itemPath,
            String token) throws Exception {
        MethodDescriptor rpc = rpc(rpcName);
        overGrpc.put("requestId", newId());
        Answer done = grpc(rpc, request(rpc, overGrpc), token);
        assertThat(done.status()).as("%s: %s", rpcName, done.code()).isEqualTo(200);
        JsonNode resource = done.body().path(resultField);
        String id = resource.path("id").asString();
        String identity = resource.has("authorUserId") ? "authorUserId" : "senderUserId";
        assertThat(resource.path(identity).asString()).as("%s: %s from the token", rpcName, identity)
                .isEqualTo(author.toString());
        Answer read = rest("GET", itemPath + id, token, null);
        assertThat(read.status()).as(read.body().toString()).isEqualTo(200);
        GrpcParity.assertSameResource(read.body(), resource);
        return id;
    }

    @Test
    void theMailAndDeliveryStepsAnswerLikeRest() throws Exception {
        UUID me = UUID.randomUUID();
        String token = token(me, TENANT, List.of("comms.*"), true);
        String[] mail = {newId(), newId()};
        List<String> rows = new ArrayList<>();
        for (String id : mail) {
            rows.add("INSERT INTO comms.mailbox_message (id, tenant_id, institution_id, owner_user_id, folder, subject,"
                    + " row_version) VALUES ('" + id + "', :tenant, '" + SCHOOL + "', '" + me + "', 'inbox', 'Hi', 1)");
        }
        String[] queued = {newId(), newId()};
        for (String id : queued) {
            rows.add("INSERT INTO comms.outbound_message (id, tenant_id, institution_id, message_kind, channel, sender,"
                    + " recipient, body, status, row_version) VALUES ('" + id + "', :tenant, '" + SCHOOL
                    + "', 'notification', 'email', 'school', 'x@example.org', 'Hello', 'queued', 1)");
        }
        String failedSms = newId();
        rows.add("INSERT INTO comms.outbound_message (id, tenant_id, institution_id, message_kind, channel, sender,"
                + " recipient, body, status, row_version) VALUES ('" + failedSms + "', :tenant, '" + SCHOOL
                + "', 'notification', 'sms', 'school', '9800000000', 'Hello', 'failed', 1)");
        String channelId = newId();
        rows.add("INSERT INTO comms.notification_channel (id, tenant_id, code, row_version) VALUES ('" + channelId
                + "', :tenant, 'channel.sms', 1)");
        sql(TENANT, rows);

        readsBack("MarkMailRead", object().put("mailboxMessageId", mail[0]), "/v1/comms/mailbox-messages/" + mail[0],
                token);
        same("MarkMailRead", object().put("mailboxMessageId", mail[1]), "POST", "/v1/comms/mailbox-messages/"
                + mail[1] + ":markRead", "{}", token(UUID.randomUUID(), TENANT, List.of("comms.*"), true));
        ObjectNode compose = object().put("subject", "S").put("body", "B");
        compose.putArray("attachmentFileIds").add(newId());
        compose.putArray("toUserIds").add(newId());
        same("ComposeMail", compose, "POST", "/v1/comms/mailbox-messages:compose", compose.toString(), token);

        readsBack("CancelOutboundMessage", object().put("outboundMessageId", queued[0]),
                "/v1/comms/outbound-messages/" + queued[0], token);
        assertThat(rest("POST", "/v1/comms/outbound-messages/" + queued[1] + ":cancel", token, "{}").status())
                .isEqualTo(200);
        same("CancelOutboundMessage", object().put("outboundMessageId", queued[0]), "POST",
                "/v1/comms/outbound-messages/" + queued[1] + ":cancel", "{}", token);
        same("RetryOutboundMessage", object().put("outboundMessageId", failedSms), "POST",
                "/v1/comms/outbound-messages/" + failedSms + ":retry", "{}", token);
        same("RetryOutboundMessage", object().put("outboundMessageId", queued[0]), "POST",
                "/v1/comms/outbound-messages/" + queued[1] + ":retry", "{}", token);

        ObjectNode send = object().put("channel", "CHANNEL_SMS").put("messageKind", "MESSAGE_KIND_NOTIFICATION")
                .put("body", "Hi");
        send.putObject("recipients").putArray("userIds").add(newId());
        MethodDescriptor sendRpc = rpc("SendMessage");
        send.put("requestId", newId());
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/comms/outbound-messages:send", token,
                "{\"channel\":\"sms\",\"recipients\":{\"userIds\":[\"" + newId() + "\"]},\"messageKind\":"
                        + "\"notification\",\"body\":\"Hi\"}", "Idempotency-Key", newId()),
                grpc(sendRpc, request(sendRpc, send), token));
        same("SendTestMessage", object().put("notificationChannelId", channelId).put("to", "9800000000"), "POST",
                "/v1/comms/notification-channels/" + channelId + ":sendTest", "{\"to\":\"9800000000\"}", token);
    }

    /** Feedback threads and participants are refused alike over gRPC: another's thread is 404, a non-opener 403. */
    @Test
    void ownerRulesOfFeedbackAndParticipantsAnswerLikeRest() throws Exception {
        String thread = newId();
        String conversation = newId();
        UUID opener = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        sql(TENANT, List.of("INSERT INTO comms.feedback_thread (id, tenant_id, institution_id, guardian_id, student_id,"
                + " assigned_to_user_id, row_version) VALUES ('" + thread + "', :tenant, '" + SCHOOL + "', '" + newId()
                + "', '" + newId() + "', '" + newId() + "', 1)",
                "INSERT INTO comms.conversation (id, tenant_id, institution_id, conversation_type, created_by,"
                        + " row_version) VALUES ('" + conversation + "', :tenant, '" + SCHOOL + "', 'group', '" + opener
                        + "', 1)",
                "INSERT INTO comms.conversation_participant (id, tenant_id, conversation_id, user_id, row_version)"
                        + " VALUES ('" + newId() + "', :tenant, '" + conversation + "', '" + member + "', 1)"));
        String reader = token(UUID.randomUUID(), TENANT, List.of("comms.feedback_thread.read"), true);
        Answer hidden = same("GetFeedbackThread", object().put("id", thread), "GET",
                "/v1/comms/feedback-threads/" + thread, null, reader);
        assertThat(hidden.status()).as("the read permission alone").isEqualTo(404);
        ObjectNode add = object().put("conversationId", conversation);
        add.putObject("conversationParticipant").put("userId", newId());
        Answer notOpener = same("CreateConversationParticipant", add, "POST", "/v1/comms/conversations/"
                + conversation + "/participants", "{\"userId\":\"" + newId() + "\"}", token(member, TENANT,
                List.of("comms.*"), true));
        assertThat(notOpener.status()).as("a participant who did not open it").isEqualTo(403);
    }
}
