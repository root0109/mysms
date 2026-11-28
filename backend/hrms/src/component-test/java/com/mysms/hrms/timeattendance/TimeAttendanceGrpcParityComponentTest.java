package com.mysms.hrms.timeattendance;

import com.mysms.hrms.HrmsApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.DynamicMessage;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.testkit.GrpcParity;
import com.mysms.core.testkit.GrpcParity.Answer;
import com.mysms.core.testkit.GrpcParityContract;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.time_attendance.v1.TimeAttendanceProto;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Every RPC of time_attendance's gRPC service against the same REST operation, on real PostgreSQL: staff attendance
 * days, whose list is read by date range, are created, read, listed and updated alike over both transports, and both
 * refuse alike — 403 without the permission, 404 for an absent row and another tenant's row, 412 for a stale etag,
 * 428 without one, 422 for an invalid body, 400 for an unbounded list.
 */
@SpringBootTest(classes = HrmsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=TimeAttendanceGrpcParityComponentTest")
class TimeAttendanceGrpcParityComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final String ADMIN = token(TENANT, List.of("time_attendance.*"));
    private static final String NOBODY = token(TENANT, List.of());
    private static final String STRANGER = token(OTHER_TENANT, List.of("time_attendance.*"));
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;

    @Autowired
    private ProtoBridge bridge;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "time_attendance", "hr", "payroll");
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
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
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
            channel = GrpcParity.channel("TimeAttendanceGrpcParityComponentTest");
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

    private static JsonNode spec() throws Exception {
        return JSON.readTree(TimeAttendanceGrpcParityComponentTest.class.getClassLoader()
                .getResourceAsStream("time_attendance/crud-spec.json"));
    }

    private GrpcParityContract contract() {
        return new GrpcParityContract(TimeAttendanceProto.getDescriptor(), this::rest, this::channel, bridge,
                new GrpcParityContract.Callers(ADMIN, NOBODY, STRANGER));
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

    /** An update request: the {@code StaffAttendanceUpdate} message of a REST PATCH body, with the row's id and etag. */
    private DynamicMessage updateRequest(MethodDescriptor update, String id, String etag, Map<String, Object> changes) {
        DynamicMessage.Builder request = contract().changes(update, JSON.valueToTree(changes));
        set(request, "id", id);
        return set(request, "etag", etag).build();
    }

    // ---- staff attendance: no delete, a list by date range

    @Test
    void staffAttendanceDaysAreWrittenAndReadAlike() throws Exception {
        JsonNode resource = resource("StaffAttendance");
        String collection = resource.path("collection").asString();
        ObjectNode body = (ObjectNode) resource.path("create");
        MethodDescriptor create = rpc("CreateStaffAttendance");
        DynamicMessage creation = bridge.reply(request(create), "staff_attendance", JSON.convertValue(body, Map.class))
                .build();
        Answer created = grpc(create, creation, ADMIN);
        assertThat(created.status()).as("CreateStaffAttendance: " + created.code()).isEqualTo(200);
        String id = created.body().path("staffAttendance").path("id").asString();
        Answer read = rest("GET", collection + "/" + id, ADMIN, null);
        assertThat(read.status()).as(read.body().toString()).isEqualTo(200);
        GrpcParity.assertSameResource(read.body(), created.body().path("staffAttendance"));
        GrpcParityContract.assertSameAnswer(rest("POST", collection, ADMIN, body.toString()), grpc(create, creation, ADMIN));
        Answer invalid = rest("POST", collection, ADMIN, "{}");
        assertThat(invalid.status()).isEqualTo(422);
        GrpcParityContract.assertSameAnswer(invalid, grpc(create, request(create).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("POST", collection, NOBODY, body.toString()),
                grpc(create, creation, NOBODY));

        MethodDescriptor get = rpc("GetStaffAttendance");
        GrpcParity.assertSameResource(read.body(),
                grpc(get, set(request(get), "id", id).build(), ADMIN).body().path("staffAttendance"));
        String absent = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(rest("GET", collection + "/" + absent, ADMIN, null),
                grpc(get, set(request(get), "id", absent).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", collection + "/" + id, NOBODY, null),
                grpc(get, set(request(get), "id", id).build(), NOBODY));
        GrpcParityContract.assertSameAnswer(rest("GET", collection + "/" + id, STRANGER, null),
                grpc(get, set(request(get), "id", id).build(), STRANGER));

        // the list is read by date range: with one both answer the day, without one both refuse
        MethodDescriptor list = rpc("ListStaffAttendances");
        String january = "attendanceDate >= \"2026-01-01\" AND attendanceDate < \"2026-02-01\"";
        Answer listed = rest("GET", collection + "?filter=" + q(january), ADMIN, null);
        Answer listedGrpc = grpc(list, set(request(list), "filter", january).build(), ADMIN);
        assertThat(listedGrpc.status()).as("ListStaffAttendances: " + listedGrpc.code()).isEqualTo(200);
        assertThat(ids(listedGrpc.body().path("items"), "id")).containsExactly(id)
                .isEqualTo(ids(listed.body().path("data"), "id"));
        GrpcParity.assertSameResource(listed.body().path("data").get(0), listedGrpc.body().path("items").get(0));
        Answer unbounded = rest("GET", collection, ADMIN, null);
        assertThat(unbounded.status()).isEqualTo(400);
        GrpcParityContract.assertSameAnswer(unbounded, grpc(list, request(list).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", collection + "?filter=" + q(january), NOBODY, null),
                grpc(list, set(request(list), "filter", january).build(), NOBODY));

        MethodDescriptor update = rpc("UpdateStaffAttendance");
        String etag = read.body().path("etag").asString();
        Map<String, Object> changes = Map.of("remark", "Left early");
        String patch = "{\"remark\":\"Left early\"}";
        GrpcParityContract.assertSameAnswer(rest("PATCH", collection + "/" + id, ADMIN, patch, "If-Match", "\"99\""),
                grpc(update, updateRequest(update, id, "99", changes), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("PATCH", collection + "/" + id, ADMIN, patch),
                grpc(update, updateRequest(update, id, "", changes), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("PATCH", collection + "/" + id, NOBODY, patch, "If-Match", etag),
                grpc(update, updateRequest(update, id, etag, changes), NOBODY));
        Answer updated = grpc(update, updateRequest(update, id, etag, changes), ADMIN);
        assertThat(updated.status()).as("UpdateStaffAttendance: " + updated.code()).isEqualTo(200);
        Answer reread = rest("GET", collection + "/" + id, ADMIN, null);
        assertThat(reread.body().path("remark").asString()).isEqualTo("Left early");
        GrpcParity.assertSameResource(reread.body(), updated.body().path("staffAttendance"));
        contract().patchStates(GrpcParityContract.Resource.of(resource), id,
                reread.body().path("etag").asString());
    }
}
