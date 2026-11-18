package com.mysms.facility;

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
import com.mysms.facility.config.FacilityDatabaseConfig;
import com.mysms.facility.v1.FacilityProto;
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
 * Every RPC of facility's three gRPC services against the same REST operation, on real PostgreSQL: for each resource of
 * crud-spec.json the create, get, list, update and delete give the same resource over both transports, and both refuse
 * alike — 403 without the permission, 404 for an absent row and another tenant's row, 409 for a duplicate, 412 for a
 * stale etag, 428 without one, 422 for an invalid body; a create retried with its request_id answers the first row on
 * both.
 */
@SpringBootTest(classes = FacilityApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=FacilityGrpcParityComponentTest")
class FacilityGrpcParityComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final String ADMIN = token(TENANT, List.of("facility.*"));
    private static final String NOBODY = token(TENANT, List.of());
    private static final String STRANGER = token(OTHER_TENANT, List.of("facility.*"));
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;


    @Autowired
    private ProtoBridge bridge;

    @Autowired
    @Qualifier(FacilityDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "facility");
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
            channel = GrpcParity.channel("FacilityGrpcParityComponentTest");
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
        return JSON.readTree(FacilityGrpcParityComponentTest.class.getClassLoader().getResourceAsStream("crud-spec.json"));
    }

    // ---- the standard operations, resource by resource

    private GrpcParityContract contract() {
        return new GrpcParityContract(FacilityProto.getDescriptor(), this::rest, this::channel, bridge,
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

    // ---- the idempotency key

    @Test
    void aCreateRetriedWithItsRequestIdAnswersTheFirstRowOnBoth() throws Exception {
        JsonNode resource = resource("Facility");
        String collection = resource.path("collection").asString();
        MethodDescriptor create = rpc("CreateFacility");
        String field = create.getOutputType().getFields().get(0).getName();
        ObjectNode forRest = ((ObjectNode) resource.path("create")).deepCopy();
        forRest.put("name", "Replayed over REST").put("code", "RPLREST");
        ObjectNode forGrpc = forRest.deepCopy();
        forGrpc.put("name", "Replayed over gRPC").put("code", "RPLGRPC");

        String restKey = UUID.randomUUID().toString();
        Answer first = rest("POST", collection, ADMIN, forRest.toString(), "Idempotency-Key", restKey);
        Answer retried = rest("POST", collection, ADMIN, forRest.toString(), "Idempotency-Key", restKey);
        assertThat(first.status()).as(first.body().toString()).isEqualTo(201);
        assertThat(retried.body().path("id").asString()).as("the retry is the first row, not a second one")
                .isEqualTo(first.body().path("id").asString());

        String grpcKey = UUID.randomUUID().toString();
        DynamicMessage keyed = set(bridge.reply(request(create), field, JSON.convertValue(forGrpc, Map.class)), "request_id",
                grpcKey).build();
        Answer firstGrpc = grpc(create, keyed, ADMIN);
        Answer retriedGrpc = grpc(create, keyed, ADMIN);
        assertThat(firstGrpc.status()).as("CreateFacility: " + firstGrpc.code()).isEqualTo(200);
        assertThat(retriedGrpc.body()).as("the retry is the first row, not a second one").isEqualTo(firstGrpc.body());

        // the key belongs to its request: with another body it is refused alike
        GrpcParityContract.assertSameAnswer(
                rest("POST", collection, ADMIN, forGrpc.toString(), "Idempotency-Key", restKey),
                grpc(create, set(bridge.reply(request(create), field, JSON.convertValue(forRest, Map.class)), "request_id",
                        grpcKey).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(
                rest("POST", collection, ADMIN, forRest.toString(), "Idempotency-Key", "not-a-uuid"),
                grpc(create, set(bridge.reply(request(create), field, JSON.convertValue(forRest, Map.class)), "request_id",
                        "not-a-uuid").build(), ADMIN));
    }
}
