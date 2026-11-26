package com.mysms.campus.frontoffice;

import com.mysms.campus.CampusApplication;
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
import com.mysms.campus.frontoffice.config.FrontofficeDatabaseConfig;
import com.mysms.frontoffice.v1.FrontofficeProto;
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

/**
 * Every RPC of frontoffice's four gRPC services against the same REST operation, on real PostgreSQL: for each resource
 * of crud-spec.json the create, get, list, update and delete give the same resource over both transports, and both
 * refuse alike — 403 without the permission, 404 for an absent row and another tenant's row, 412 for a stale etag, 428
 * without one, 422 for an invalid body; a call's duration and an enum travel alike; and a visitor's check-out answers
 * and refuses alike.
 */
@SpringBootTest(classes = CampusApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=FrontofficeGrpcParityComponentTest")
class FrontofficeGrpcParityComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final String ADMIN = token(TENANT, List.of("frontoffice.*"));
    private static final String NOBODY = token(TENANT, List.of());
    private static final String STRANGER = token(OTHER_TENANT, List.of("frontoffice.*"));
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;


    @Autowired
    private ProtoBridge bridge;

    @Autowired
    @Qualifier(FrontofficeDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "frontoffice", "library", "transport", "facility");
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
            channel = GrpcParity.channel("FrontofficeGrpcParityComponentTest");
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

    // ---- the standard operations, resource by resource

    private GrpcParityContract contract() {
        return new GrpcParityContract(FrontofficeProto.getDescriptor(), this::rest, this::channel, bridge,
                new GrpcParityContract.Callers(ADMIN, NOBODY, STRANGER));
    }

    @TestFactory
    List<DynamicNode> everyStandardRpcAnswersLikeRest() throws Exception {
        JsonNode spec = JSON.readTree(getClass().getClassLoader().getResourceAsStream("frontoffice/crud-spec.json"));
        List<DynamicNode> tests = new ArrayList<>();
        List<GrpcParityContract.Resource> resources = GrpcParityContract.resources("frontoffice/crud-spec.json");
        for (int i = 0; i < resources.size(); i++) {
            GrpcParityContract.Resource resource = resources.get(i);
            List<String> fixtures = new ArrayList<>();
            spec.path("resources").get(i).path("fixtures").forEach(statement -> fixtures.add(statement.asString()));
            tests.add(DynamicTest.dynamicTest(resource.name(), () -> {
                fixtures(TENANT, fixtures);
                fixtures(OTHER_TENANT, fixtures);
                contract().verify(resource);
            }));
        }
        return tests;
    }

    // ---- what the standard contract does not reach

    @Test
    void aCallLogWithItsDurationAndDirectionReadsTheSameOverBoth() throws Exception {
        MethodDescriptor create = rpc("CreateCallLog");
        Map<String, Object> call = new HashMap<>();
        call.put("institutionId", "0196f000-0000-7000-8000-00000000a001");
        call.put("direction", "outgoing");
        call.put("callerName", "Parity caller");
        call.put("callDate", "2026-02-03");
        call.put("startTime", "09:15");
        call.put("durationSeconds", "95s");
        Answer created = grpc(create, bridge.reply(request(create), "call_log", call).build(), ADMIN);
        assertThat(created.status()).as(String.valueOf(created.code())).isEqualTo(200);
        JsonNode row = created.body().path("callLog");
        assertThat(row.path("direction").asString()).isEqualTo("DIRECTION_OUTGOING");
        assertThat(row.path("durationSeconds").asString()).isEqualTo("95s");
        Answer read = rest("GET", "/v1/frontoffice/call-logs/" + row.path("id").asString(), ADMIN, null);
        assertThat(read.status()).isEqualTo(200);
        GrpcParity.assertSameResource(read.body(), row);
        // a dry run stores nothing on either transport
        Answer dryRun = grpc(create, set(bridge.reply(request(create), "call_log", call), "validate_only", true).build(),
                ADMIN);
        assertThat(dryRun.status()).isEqualTo(200);
        assertThat(dryRun.body().path("callLog").has("id")).isEqualTo(
                rest("POST", "/v1/frontoffice/call-logs?validateOnly=true", ADMIN, JSON.writeValueAsString(call)).body()
                        .has("id"));
        // a malformed time is refused alike
        call.put("startTime", "nine");
        GrpcParityContract.assertSameAnswer(
                rest("POST", "/v1/frontoffice/call-logs", ADMIN, JSON.writeValueAsString(call)),
                grpc(create, bridge.reply(request(create), "call_log", call).build(), ADMIN));
    }

    @Test
    void aVisitorCheckOutAnswersAndRefusesTheSameOverBoth() throws Exception {
        MethodDescriptor checkOut = rpc("CheckOutVisitor");
        String overRest = visitor();
        String overGrpc = visitor();
        Answer byRest = rest("POST", "/v1/frontoffice/visitor-logs/" + overRest + ":checkOut", ADMIN,
                "{\"exitTime\":\"17:30\"}");
        Answer byGrpc = grpc(checkOut, set(set(request(checkOut), "visitor_log_id", overGrpc), "exit_time", "17:30")
                .build(), ADMIN);
        assertThat(byRest.status()).isEqualTo(200);
        assertThat(byGrpc.status()).as(String.valueOf(byGrpc.code())).isEqualTo(200);
        assertThat(byGrpc.body().path("visitorLog").path("exitTime").asString())
                .isEqualTo(byRest.body().path("exitTime").asString());
        assertThat(byGrpc.body().path("visitorLog").path("etag").asString())
                .isEqualTo(byRest.body().path("etag").asString());
        GrpcParity.assertSameResource(rest("GET", "/v1/frontoffice/visitor-logs/" + overGrpc, ADMIN, null).body(),
                byGrpc.body().path("visitorLog"));

        String again = "/v1/frontoffice/visitor-logs/" + overRest + ":checkOut";
        GrpcParityContract.assertSameAnswer(rest("POST", again, ADMIN, "{\"exitTime\":\"18:00\"}"),
                grpc(checkOut, set(set(request(checkOut), "visitor_log_id", overGrpc), "exit_time", "18:00").build(),
                        ADMIN));
        String fresh = visitor();
        DynamicMessage request = set(set(request(checkOut), "visitor_log_id", fresh), "exit_time", "17:30").build();
        String path = "/v1/frontoffice/visitor-logs/" + fresh + ":checkOut";
        GrpcParityContract.assertSameAnswer(rest("POST", path, NOBODY, "{\"exitTime\":\"17:30\"}"),
                grpc(checkOut, request, NOBODY));
        GrpcParityContract.assertSameAnswer(rest("POST", path, STRANGER, "{\"exitTime\":\"17:30\"}"),
                grpc(checkOut, request, STRANGER));
        GrpcParityContract.assertSameAnswer(rest("POST", path, ADMIN, "{\"exitTime\":\"17:30\",\"etag\":\"9\"}"),
                grpc(checkOut, set(set(set(request(checkOut), "visitor_log_id", fresh), "exit_time", "17:30"),
                        "etag", "9").build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("POST", path, ADMIN, "{\"exitTime\":\"07:00\"}"),
                grpc(checkOut, set(set(request(checkOut), "visitor_log_id", fresh), "exit_time", "07:00").build(),
                        ADMIN));
        assertThat(grpc(checkOut, request, null).status()).as("authentication first, as on REST").isEqualTo(401);
    }

    /** A visitor of the tenant who came in at nine and is still inside; answers its id. */
    private String visitor() throws Exception {
        Answer created = rest("POST", "/v1/frontoffice/visitor-logs", ADMIN, "{\"institutionId\":\""
                + "0196f000-0000-7000-8000-00000000a001\",\"visitorName\":\"Parity visitor\",\"visitorType\":"
                + "\"parent\",\"purpose\":\"meeting\",\"visitDate\":\"2026-02-03\",\"entryTime\":\"09:00\"}");
        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
        return created.body().path("id").asString();
    }
}
