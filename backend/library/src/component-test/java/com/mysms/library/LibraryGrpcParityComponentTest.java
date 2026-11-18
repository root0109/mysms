package com.mysms.library;

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
import com.mysms.library.config.LibraryDatabaseConfig;
import com.mysms.library.v1.LibraryProto;
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
import java.util.Locale;
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
 * Every RPC of library's ten gRPC services against the same REST operation, on real PostgreSQL: for each resource of
 * crud-spec.json the create, get, list, update and delete give the same resource over both transports, and both refuse
 * alike — 403 without the permission, 404 for an absent row and another tenant's row, 409 for a duplicate, 412 for a
 * stale etag, 428 without one, 422 for an invalid body; every state transition of crud-spec.json (a reservation's
 * accept and reject, a purchase request's submit, accept and reject) with its refusals, the five operations this
 * release does not serve (501), and a keyed create replayed by its {@code request_id}, the same way.
 */
@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=LibraryGrpcParityComponentTest")
class LibraryGrpcParityComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final String ADMIN = token(TENANT, List.of("library.*"));
    private static final String NOBODY = token(TENANT, List.of());
    private static final String STRANGER = token(OTHER_TENANT, List.of("library.*"));
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;


    @Autowired
    private ProtoBridge bridge;

    @Autowired
    @Qualifier(LibraryDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "library");
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
            channel = GrpcParity.channel("LibraryGrpcParityComponentTest");
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
        return JSON.readTree(LibraryGrpcParityComponentTest.class.getClassLoader().getResourceAsStream("crud-spec.json"));
    }

    private static List<String> fixturesOf(JsonNode resource) {
        List<String> fixtures = new ArrayList<>();
        resource.path("fixtures").forEach(statement -> fixtures.add(statement.asString()));
        return fixtures;
    }

    // ---- the standard operations, resource by resource

    private GrpcParityContract contract() {
        return new GrpcParityContract(LibraryProto.getDescriptor(), this::rest, this::channel, bridge,
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

    // ---- the state transitions

    @TestFactory
    List<DynamicNode> everyStateTransitionAnswersLikeRest() throws Exception {
        List<DynamicNode> tests = new ArrayList<>();
        for (JsonNode resource : spec().path("resources")) {
            for (JsonNode transition : resource.path("transitions")) {
                tests.add(DynamicTest.dynamicTest(transition.path("operation").asString(),
                        () -> transitionAnswersLikeRest(resource, transition)));
            }
        }
        assertThat(tests).as("accept and reject a reservation; submit, accept and reject a purchase request").hasSize(5);
        return tests;
    }

    /**
     * One transition on two rows of the resource, one per transport: refused alike outside its guard state (409), for
     * a stale etag (412), without the permission (403), for another tenant's row and an absent row (404) and a
     * malformed id (400); then done on both, giving the row REST reads; and refused alike once done (409).
     */
    private void transitionAnswersLikeRest(JsonNode resource, JsonNode transition) throws Exception {
        fixtures(TENANT, fixturesOf(resource));
        String collection = resource.path("collection").asString();
        String table = resource.path("table").asString();
        String operation = transition.path("operation").asString();
        MethodDescriptor rpc = rpc(operation.substring(operation.indexOf('.') + 1));
        String target = transition.path("sets").path("status").asString();
        ObjectNode body = (ObjectNode) transition.path("body");
        String[] ids = new String[2];
        for (int i = 0; i < 2; i++) {
            Answer created = rest("POST", collection, ADMIN, resource.path("create").toString());
            assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
            ids[i] = created.body().path("id").asString();
        }
        fixtures(TENANT, List.of("UPDATE " + table + " SET status = '" + target + "' WHERE tenant_id = :tenant AND id = '"
                + ids[0] + "'"));
        sameAnswer(transition, rpc, ids[0], body, null, ADMIN, 409);
        for (String id : ids) {
            List<String> prepare = new ArrayList<>();
            transition.path("prepare").forEach(statement -> prepare.add(statement.asString().replace("{id}", id)
                    + " AND tenant_id = :tenant"));
            fixtures(TENANT, prepare);
        }
        sameAnswer(transition, rpc, ids[0], body, "\"99\"", ADMIN, 412);
        sameAnswer(transition, rpc, ids[0], body, null, NOBODY, 403);
        sameAnswer(transition, rpc, ids[0], body, null, STRANGER, 404);
        sameAnswer(transition, rpc, UUID.randomUUID().toString(), body, null, ADMIN, 404);
        sameAnswer(transition, rpc, "not-a-uuid", body, null, ADMIN, 400);

        Answer overRest = rest("POST", path(transition, ids[0]), ADMIN, body.toString());
        Answer overGrpc = grpc(rpc, transitionRequest(rpc, ids[1], body, null), ADMIN);
        assertThat(overRest.status()).as(overRest.body().toString()).isEqualTo(200);
        assertThat(overGrpc.status()).as("%s: %s", operation, overGrpc.code()).isEqualTo(200);
        assertThat(overRest.body().path("status").asString()).isEqualTo(target);
        JsonNode row = overGrpc.body().path(rpc.getOutputType().getFields().get(0).getJsonName());
        assertThat(row.path("status").asString()).isEqualTo("STATUS_" + target.toUpperCase(Locale.ROOT));
        GrpcParity.assertSameResource(rest("GET", collection + "/" + ids[1], ADMIN, null).body(), row);
        if (!transition.path("guard").path("status").toString().contains("\"" + target + "\"")) {
            sameAnswer(transition, rpc, ids[0], body, null, ADMIN, 409);
            sameAnswer(transition, rpc, ids[1], body, null, ADMIN, 409);
        }
    }

    private static String path(JsonNode transition, String id) {
        return transition.path("path").asString().replace("{id}", id);
    }

    /** The transition's request: the path id (the request's first field), the REST body's fields, the etag if any. */
    private static DynamicMessage transitionRequest(MethodDescriptor rpc, String id, ObjectNode body, String etag) {
        DynamicMessage.Builder request = request(rpc);
        request.setField(rpc.getInputType().getFields().get(0), id);
        body.properties().forEach(field -> set(request, field.getKey(), field.getValue().asString()));
        if (etag != null) {
            set(request, "etag", etag);
        }
        return request.build();
    }

    private void sameAnswer(JsonNode transition, MethodDescriptor rpc, String id, ObjectNode body, String etag,
                            String token, int expected) throws Exception {
        ObjectNode restBody = body.deepCopy();
        if (etag != null) {
            restBody.put("etag", etag);
        }
        Answer overRest = rest("POST", path(transition, id), token, restBody.toString());
        assertThat(overRest.status()).as("%s over REST: %s", rpc.getName(), overRest.body()).isEqualTo(expected);
        GrpcParityContract.assertSameAnswer(overRest, grpc(rpc, transitionRequest(rpc, id, body, etag), token));
    }

    // ---- the operations not served, and a replay

    @Test
    void theOperationsThisReleaseDoesNotServeAre501OnBoth() throws Exception {
        String loan = UUID.randomUUID().toString();
        Map<String, String> loanOperations = Map.of("RequestLoanReturn", ":requestReturn", "ReturnLoan", ":return",
                "MarkLoanLost", ":markLost", "InvoiceLoanFine", ":invoiceFine");
        for (Map.Entry<String, String> operation : loanOperations.entrySet()) {
            MethodDescriptor rpc = rpc(operation.getKey());
            String body = "ReturnLoan".equals(operation.getKey()) ? "{\"returnedOn\":\"2026-01-10\"}" : "{}";
            Answer overRest = rest("POST", "/v1/library/loans/" + loan + operation.getValue(), ADMIN, body);
            assertThat(overRest.status()).as(operation.getKey()).isEqualTo(501);
            GrpcParityContract.assertSameAnswer(overRest, grpc(rpc, set(request(rpc), "loan_id", loan).build(), ADMIN));
        }
        MethodDescriptor again = rpc("RequestReservationAgain");
        Answer overRest = rest("POST", "/v1/library/reservations/" + loan + ":requestAgain", ADMIN, "{}");
        assertThat(overRest.status()).isEqualTo(501);
        GrpcParityContract.assertSameAnswer(overRest, grpc(again, set(request(again), "reservation_id", loan).build(), ADMIN));
        assertThat(grpc(again, set(request(again), "reservation_id", loan).build(), null).status())
                .as("authentication first, as on REST").isEqualTo(401);
        assertThat(grpc(again, set(request(again), "reservation_id", loan).build(), NOBODY).status())
                .as("then the permission, as on REST").isEqualTo(403);
    }

    @Test
    void aKeyedCreateIsReplayedByItsRequestIdLikeRestsIdempotencyKey() throws Exception {
        MethodDescriptor create = rpc("CreateAuthor");
        DynamicMessage.Builder author = DynamicMessage.newBuilder(create.getInputType().findFieldByName("author")
                .getMessageType());
        String key = UUID.randomUUID().toString();
        DynamicMessage first = set(set(request(create), "author", set(author, "name", "Replayed author").build()),
                "request_id", key).build();
        Answer created = grpc(create, first, ADMIN);
        Answer replayed = grpc(create, first, ADMIN);
        assertThat(created.status()).as(String.valueOf(created.code())).isEqualTo(200);
        assertThat(replayed.body()).as("the first answer, not a second author").isEqualTo(created.body());
        Answer listed = rest("GET", "/v1/library/authors?filter=name%20%3D%20%22Replayed%20author%22", ADMIN, null);
        assertThat(listed.body().path("data")).hasSize(1);

        String restKey = UUID.randomUUID().toString();
        Answer overRest = rest("POST", "/v1/library/authors", ADMIN, "{\"name\":\"Replayed over REST\"}",
                "Idempotency-Key", restKey);
        assertThat(overRest.status()).isEqualTo(201);
        assertThat(rest("POST", "/v1/library/authors", ADMIN, "{\"name\":\"Replayed over REST\"}", "Idempotency-Key",
                restKey).body().path("id")).isEqualTo(overRest.body().path("id"));
        // the key reused for another request is refused alike
        DynamicMessage other = set(set(request(create), "author", set(author, "name", "Another author").build()),
                "request_id", key).build();
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/library/authors", ADMIN, "{\"name\":\"Another author\"}",
                "Idempotency-Key", restKey), grpc(create, other, ADMIN));
        assertThat(grpc(create, other, ADMIN).code()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
    }
}
