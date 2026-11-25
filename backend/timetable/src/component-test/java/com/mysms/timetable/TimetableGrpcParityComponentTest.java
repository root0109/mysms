package com.mysms.timetable;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.DynamicMessage;
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
import com.mysms.timetable.config.TimetableDatabaseConfig;
import com.mysms.timetable.v1.TimetableProto;
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
 * Every RPC of timetable's three gRPC services against the same REST operation, on real PostgreSQL: for each resource
 * of crud-spec.json the create, get, list, update and delete give the same resource over both transports, and both
 * refuse alike — 403 without the permission, 404 for an absent row and another tenant's row, 409 for a duplicate, 412
 * for a stale etag, 428 without one, 422 for an invalid body; a session's four steps change it the same way and are
 * refused alike; the timetable view answers the same sessions page by page; the module's operations are read, listed
 * and cancelled alike; generating sessions from a weekly pattern and confirming them in a batch answer and refuse
 * alike; a step retried with its request_id answers the first result.
 */
@SpringBootTest(classes = TimetableApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=TimetableGrpcParityComponentTest")
class TimetableGrpcParityComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String INSTITUTION = "0196f000-0000-7000-8000-00000000a001";
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final String ADMIN = token(TENANT, List.of("timetable.*"));
    private static final String NOBODY = token(TENANT, List.of());
    private static final String STRANGER = token(OTHER_TENANT, List.of("timetable.*"));
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;


    @Autowired
    private ProtoBridge bridge;

    @Autowired
    @Qualifier(TimetableDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "timetable");
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
            channel = GrpcParity.channel("TimetableGrpcParityComponentTest");
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
        return JSON.readTree(TimetableGrpcParityComponentTest.class.getClassLoader().getResourceAsStream("crud-spec.json"));
    }

    private static List<String> strings(JsonNode array) {
        List<String> strings = new ArrayList<>();
        array.forEach(element -> strings.add(element.asString()));
        return strings;
    }

    // ---- the standard operations, resource by resource

    private GrpcParityContract contract() {
        return new GrpcParityContract(TimetableProto.getDescriptor(), this::rest, this::channel, bridge,
                new GrpcParityContract.Callers(ADMIN, NOBODY, STRANGER));
    }

    @TestFactory
    List<DynamicNode> everyStandardRpcAnswersLikeRest() throws Exception {
        List<DynamicNode> tests = new ArrayList<>();
        for (GrpcParityContract.Resource resource : GrpcParityContract.resources("crud-spec.json")) {
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


    private static List<String> ids(JsonNode rows, String field) {
        List<String> ids = new ArrayList<>();
        rows.forEach(row -> ids.add(row.path(field).asString()));
        return ids;
    }

    private static DynamicMessage pageable(MethodDescriptor rpc, int page, int size, String cursor) {
        var type = rpc.getInputType().findFieldByName("pageable").getMessageType();
        return DynamicMessage.newBuilder(type).setField(type.findFieldByName("page"), page)
                .setField(type.findFieldByName("size"), size).setField(type.findFieldByName("cursor"), cursor).build();
    }

    // ---- the state transitions of crud-spec.json

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

    // ---- the idempotency key

    @Test
    void aTransitionRetriedWithItsRequestIdAnswersTheFirstResultOnBoth() throws Exception {
        JsonNode resource = resource("Session");
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

    // ---- the timetable view

    private String session(UUID staff, String title, String startAt, String endAt) {
        String id = UUID.randomUUID().toString();
        fixtures(TENANT, List.of("INSERT INTO timetable.session (id, tenant_id, institution_id, start_at, end_at,"
                + " class_section_id, staff_id, room_id, title, status) VALUES ('" + id + "', :tenant, '" + INSTITUTION
                + "', '" + startAt + "', '" + endAt + "', '" + UUID.randomUUID() + "', '" + staff + "', '" + UUID.randomUUID()
                + "', '" + title + "', 'confirmed')"));
        return id;
    }

    private static DynamicMessage.Builder timetable(MethodDescriptor view, String from, String to) {
        return set(set(request(view), "from_date", from), "to_date", to);
    }

    @Test
    void aTeachersTimetableReadsAlikePageByPageAndIsRefusedAlike() throws Exception {
        UUID teacher = UUID.randomUUID();
        session(teacher, "Mon first", "2026-03-02T08:00:00Z", "2026-03-02T08:45:00Z");
        session(teacher, "Mon second", "2026-03-02T09:00:00Z", "2026-03-02T09:45:00Z");
        session(teacher, "Wed", "2026-03-04T08:00:00Z", "2026-03-04T08:45:00Z");
        String view = "/v1/timetable/sessions:timetable";
        String week = view + "?fromDate=2026-03-02&toDate=2026-03-08&staffId=" + teacher;
        MethodDescriptor rpc = rpc("GetTimetable");

        Answer all = rest("GET", week, ADMIN, null);
        Answer allGrpc = grpc(rpc, set(timetable(rpc, "2026-03-02", "2026-03-08"), "staff_id", teacher.toString()).build(),
                ADMIN);
        assertThat(all.status()).as(all.body().toString()).isEqualTo(200);
        assertThat(allGrpc.status()).as("GetTimetable: " + allGrpc.code()).isEqualTo(200);
        assertThat(all.body().path("data")).hasSize(3);
        assertThat(allGrpc.body().path("items")).hasSize(3);
        for (int i = 0; i < 3; i++) {
            GrpcParity.assertSameResource(all.body().path("data").get(i), allGrpc.body().path("items").get(i));
        }

        Answer first = rest("GET", week + "&size=2", ADMIN, null);
        Answer firstGrpc = grpc(rpc, set(set(timetable(rpc, "2026-03-02", "2026-03-08"), "staff_id", teacher.toString()),
                "pageable", pageable(rpc, 0, 2, "")).build(), ADMIN);
        assertThat(sessions(firstGrpc.body().path("items"))).hasSize(2).isEqualTo(sessions(first.body().path("data")));
        JsonNode pagination = firstGrpc.body().path("pagination");
        assertThat(pagination.path("next").asInt()).isEqualTo(2);
        String next = first.body().path("paging").path("next").asString();
        GrpcParity.assertSameCursor(next, pagination.path("nextCursor").asString());
        Answer second = rest("GET", next, ADMIN, null);
        Answer secondGrpc = grpc(rpc, set(set(timetable(rpc, "2026-03-02", "2026-03-08"), "staff_id", teacher.toString()),
                "pageable", pageable(rpc, 2, 2, pagination.path("nextCursor").asString())).build(), ADMIN);
        assertThat(sessions(secondGrpc.body().path("items"))).hasSize(1).isEqualTo(sessions(second.body().path("data")));

        // refused alike: no dates, a malformed date, nobody's timetable, too many days, a malformed id, no permission,
        // a cursor of another view
        GrpcParityContract.assertSameAnswer(rest("GET", view + "?staffId=" + teacher, ADMIN, null),
                grpc(rpc, set(request(rpc), "staff_id", teacher.toString()).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(
                rest("GET", view + "?fromDate=2026-13-45&toDate=2026-03-08&staffId=" + teacher, ADMIN, null),
                grpc(rpc, set(timetable(rpc, "2026-13-45", "2026-03-08"), "staff_id", teacher.toString()).build(), ADMIN));
        Answer nobodys = rest("GET", view + "?fromDate=2026-03-02&toDate=2026-03-08", ADMIN, null);
        assertThat(nobodys.status()).isEqualTo(422);
        GrpcParityContract.assertSameAnswer(nobodys, grpc(rpc, timetable(rpc, "2026-03-02", "2026-03-08").build(), ADMIN));
        GrpcParityContract.assertSameAnswer(
                rest("GET", view + "?fromDate=2026-03-08&toDate=2026-03-02&staffId=" + teacher, ADMIN, null),
                grpc(rpc, set(timetable(rpc, "2026-03-08", "2026-03-02"), "staff_id", teacher.toString()).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(
                rest("GET", view + "?fromDate=2026-01-01&toDate=2027-12-31&staffId=" + teacher, ADMIN, null),
                grpc(rpc, set(timetable(rpc, "2026-01-01", "2027-12-31"), "staff_id", teacher.toString()).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(
                rest("GET", view + "?fromDate=2026-03-02&toDate=2026-03-08&staffId=nobody", ADMIN, null),
                grpc(rpc, set(timetable(rpc, "2026-03-02", "2026-03-08"), "staff_id", "nobody").build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", week, NOBODY, null),
                grpc(rpc, set(timetable(rpc, "2026-03-02", "2026-03-08"), "staff_id", teacher.toString()).build(), NOBODY));
        String room = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(
                rest("GET", view + "?fromDate=2026-03-02&toDate=2026-03-08&roomId=" + room + "&page=2&size=2&cursor="
                        + pagination.path("nextCursor").asString(), ADMIN, null),
                grpc(rpc, set(set(timetable(rpc, "2026-03-02", "2026-03-08"), "room_id", room), "pageable",
                        pageable(rpc, 2, 2, pagination.path("nextCursor").asString())).build(), ADMIN));
    }

    private static List<String> sessions(JsonNode views) {
        List<String> ids = new ArrayList<>();
        views.forEach(view -> ids.add(view.path("session").path("id").asString()));
        return ids;
    }

    // ---- the long-running operations

    private String operation(String kind, String status) {
        String id = UUID.randomUUID().toString();
        fixtures(TENANT, List.of("INSERT INTO timetable.operation (id, tenant_id, operation_name, status, finished_at,"
                + " row_version) VALUES ('" + id + "', :tenant, '" + kind + "', '" + status + "', "
                + ("running".equals(status) ? "NULL" : "now()") + ", 1)"));
        return id;
    }

    @Test
    void operationsAreReadListedAndCancelledAlike() throws Exception {
        String operations = "/v1/timetable/operations";
        String running = operation("timetable.GenerateSessions", "running");
        String alsoRunning = operation("timetable.GenerateSessions", "running");
        String finished = operation("timetable.BatchConfirmSessions", "succeeded");

        MethodDescriptor get = rpc("GetOperation");
        for (String id : List.of(running, finished)) {
            Answer read = rest("GET", operations + "/" + id, ADMIN, null);
            Answer got = grpc(get, set(request(get), "operation_id", id).build(), ADMIN);
            assertThat(read.status()).as(read.body().toString()).isEqualTo(200);
            assertThat(got.status()).as("GetOperation: " + got.code()).isEqualTo(200);
            GrpcParity.assertSameResource(read.body(), got.body().path("result"));
        }
        String absent = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(rest("GET", operations + "/" + absent, ADMIN, null),
                grpc(get, set(request(get), "operation_id", absent).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", operations + "/not-a-uuid", ADMIN, null),
                grpc(get, set(request(get), "operation_id", "not-a-uuid").build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", operations + "/" + running, NOBODY, null),
                grpc(get, set(request(get), "operation_id", running).build(), NOBODY));
        Answer strangers = rest("GET", operations + "/" + running, STRANGER, null);
        assertThat(strangers.status()).isEqualTo(404);
        GrpcParityContract.assertSameAnswer(strangers,
                grpc(get, set(request(get), "operation_id", running).build(), STRANGER));

        MethodDescriptor list = rpc("ListOperations");
        Answer listed = rest("GET", operations, ADMIN, null);
        Answer listedGrpc = grpc(list, request(list).build(), ADMIN);
        assertThat(listedGrpc.status()).as("ListOperations: " + listedGrpc.code()).isEqualTo(200);
        assertThat(ids(listedGrpc.body().path("items"), "operationId")).hasSize(3)
                .isEqualTo(ids(listed.body().path("data"), "operationId"));
        Answer ofKind = rest("GET", operations + "?kind=timetable.GenerateSessions", ADMIN, null);
        Answer ofKindGrpc = grpc(list, set(request(list), "kind", "timetable.GenerateSessions").build(), ADMIN);
        assertThat(ids(ofKindGrpc.body().path("items"), "operationId")).containsExactlyInAnyOrder(running, alsoRunning)
                .isEqualTo(ids(ofKind.body().path("data"), "operationId"));
        Answer first = rest("GET", operations + "?size=2", ADMIN, null);
        Answer firstGrpc = grpc(list, set(request(list), "pageable", pageable(list, 0, 2, "")).build(), ADMIN);
        assertThat(ids(firstGrpc.body().path("items"), "operationId")).hasSize(2)
                .isEqualTo(ids(first.body().path("data"), "operationId"));
        String cursor = firstGrpc.body().path("pagination").path("nextCursor").asString();
        String next = first.body().path("paging").path("next").asString();
        GrpcParity.assertSameCursor(next, cursor);
        assertThat(ids(grpc(list, set(request(list), "pageable", pageable(list, 2, 2, cursor)).build(), ADMIN).body()
                .path("items"), "operationId")).hasSize(1)
                .isEqualTo(ids(rest("GET", next, ADMIN, null).body().path("data"), "operationId"));
        GrpcParityContract.assertSameAnswer(rest("GET", operations, NOBODY, null), grpc(list, request(list).build(), NOBODY));

        // REST answers 202 with the operation, gRPC answers it as the result; a finished operation is 409 on both
        MethodDescriptor cancel = rpc("CancelOperation");
        GrpcParityContract.assertSameAnswer(rest("POST", operations + "/" + running + ":cancel", NOBODY, null),
                grpc(cancel, set(request(cancel), "operation_id", alsoRunning).build(), NOBODY));
        Answer cancelled = rest("POST", operations + "/" + running + ":cancel", ADMIN, null);
        Answer cancelledGrpc = grpc(cancel, set(request(cancel), "operation_id", alsoRunning).build(), ADMIN);
        assertThat(cancelled.status()).as(cancelled.body().toString()).isEqualTo(202);
        assertThat(cancelledGrpc.status()).as("CancelOperation: " + cancelledGrpc.code()).isEqualTo(200);
        assertThat(cancelled.body().path("status").asString()).isEqualTo("cancelled");
        assertThat(cancelledGrpc.body().path("result").path("status").asString()).isEqualTo("cancelled");
        GrpcParity.assertSameResource(rest("GET", operations + "/" + alsoRunning, ADMIN, null).body(),
                cancelledGrpc.body().path("result"));
        Answer twice = rest("POST", operations + "/" + running + ":cancel", ADMIN, null);
        assertThat(twice.status()).isEqualTo(409);
        GrpcParityContract.assertSameAnswer(twice,
                grpc(cancel, set(request(cancel), "operation_id", alsoRunning).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("POST", operations + "/" + finished + ":cancel", ADMIN, null),
                grpc(cancel, set(request(cancel), "operation_id", finished).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("POST", operations + "/" + absent + ":cancel", ADMIN, null),
                grpc(cancel, set(request(cancel), "operation_id", absent).build(), ADMIN));
    }

    // ---- generating sessions from a weekly pattern, and confirming them in a batch

    private String pattern(String classSection, String period, String staff) {
        return "{\"institutionId\":\"" + INSTITUTION + "\",\"classSectionId\":\"" + classSection
                + "\",\"startDate\":\"2026-03-02\",\"endDate\":\"2026-03-15\",\"timeZone\":\"Asia/Kolkata\","
                + "\"slots\":[{\"weekday\":1,\"periodId\":\"" + period + "\",\"subjectId\":\"" + UUID.randomUUID()
                + "\",\"staffId\":\"" + staff + "\"},{\"weekday\":3,\"periodId\":\"" + period + "\",\"subjectId\":\""
                + UUID.randomUUID() + "\",\"staffId\":\"" + staff + "\"}]}";
    }

    private static DynamicMessage json(MethodDescriptor rpc, String body) throws Exception {
        DynamicMessage.Builder builder = request(rpc);
        com.google.protobuf.util.JsonFormat.parser().merge(body, builder);
        return builder.build();
    }

    /** In a tenant of its own, so the runs it records do not reach the operation listing above. */
    @Test
    void generatingAndBatchConfirmingSessionsAnswerLikeRest() throws Exception {
        UUID tenant = UUID.randomUUID();
        String admin = token(tenant, List.of("timetable.*"));
        String nobody = token(tenant, List.of());
        String period = UUID.randomUUID().toString();
        fixtures(tenant, List.of("INSERT INTO timetable.period (id, tenant_id, institution_id, name, start_time,"
                + " duration_minutes, row_version) VALUES ('" + period + "', :tenant, '" + INSTITUTION
                + "', 'Parity period', '09:00', 45, 1)"));
        MethodDescriptor generate = rpc("GenerateSessions");
        String overRest = pattern(UUID.randomUUID().toString(), period, UUID.randomUUID().toString());
        String overGrpc = pattern(UUID.randomUUID().toString(), period, UUID.randomUUID().toString());
        Answer byRest = rest("POST", "/v1/timetable/sessions:generate", admin, overRest);
        Answer byGrpc = grpc(generate, json(generate, overGrpc), admin);
        assertThat(byRest.status()).as(byRest.body().toString()).isEqualTo(202);
        assertThat(byGrpc.status()).as("GenerateSessions: " + byGrpc.code()).isEqualTo(200);
        JsonNode restRun = byRest.body();
        JsonNode grpcRun = byGrpc.body().path("result");
        assertThat(grpcRun.path("status").asString()).isEqualTo(restRun.path("status").asString())
                .isEqualTo("succeeded");
        for (String count : List.of("created", "kept", "clashCount")) {
            assertThat(grpcRun.path("result").path(count).asInt()).as(count)
                    .isEqualTo(restRun.path("result").path(count).asInt());
        }
        assertThat(restRun.path("result").path("created").asInt()).isEqualTo(4);
        Answer again = grpc(generate, json(generate, overGrpc), admin);
        assertThat(again.body().path("result").path("result").path("kept").asInt()).as("a re-run keeps them")
                .isEqualTo(4);
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/timetable/sessions:generate", admin,
                        overRest.replace("Asia/Kolkata", "Mars/Olympus")),
                grpc(generate, json(generate, overGrpc.replace("Asia/Kolkata", "Mars/Olympus")), admin));
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/timetable/sessions:generate", nobody, overRest),
                grpc(generate, json(generate, overGrpc), nobody));
        assertThat(grpc(generate, json(generate, overGrpc), null).status()).as("authentication first, as on REST")
                .isEqualTo(401);

        MethodDescriptor confirm = rpc("BatchConfirmSessions");
        List<String> drafts = ids(rest("GET", "/v1/timetable/sessions?filter=periodId%20%3D%20%22" + period
                + "%22&size=10", admin, null).body().path("data"), "id");
        assertThat(drafts).hasSize(8);
        Answer confirmedByRest = rest("POST", "/v1/timetable/sessions:batchConfirm", admin, "{\"sessionIds\":[\""
                + drafts.get(0) + "\",\"" + drafts.get(1) + "\"]}");
        Answer confirmedByGrpc = grpc(confirm, json(confirm, "{\"sessionIds\":[\"" + drafts.get(2) + "\",\""
                + drafts.get(3) + "\"]}"), admin);
        assertThat(confirmedByRest.status()).as(confirmedByRest.body().toString()).isEqualTo(200);
        assertThat(confirmedByGrpc.status()).as("BatchConfirmSessions: " + confirmedByGrpc.code()).isEqualTo(200);
        assertThat(confirmedByRest.body().path("items").get(0).path("status").asString()).isEqualTo("confirmed");
        GrpcParity.assertSameResource(rest("GET", "/v1/timetable/sessions/" + drafts.get(2), admin, null).body(),
                confirmedByGrpc.body().path("items").get(0));
        String unknown = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/timetable/sessions:batchConfirm", admin,
                        "{\"sessionIds\":[\"" + unknown + "\"]}"),
                grpc(confirm, json(confirm, "{\"sessionIds\":[\"" + unknown + "\"]}"), admin));
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/timetable/sessions:batchConfirm", nobody,
                        "{\"sessionIds\":[\"" + drafts.get(4) + "\"]}"),
                grpc(confirm, json(confirm, "{\"sessionIds\":[\"" + drafts.get(4) + "\"]}"), nobody));
    }
}
