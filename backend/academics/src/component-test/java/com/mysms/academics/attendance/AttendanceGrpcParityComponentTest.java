package com.mysms.academics.attendance;

import com.mysms.academics.AcademicsApplication;
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
import com.mysms.academics.attendance.config.AttendanceDatabaseConfig;
import com.mysms.attendance.v1.AttendanceProto;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
 * Every RPC of attendance's five gRPC services against the same REST operation, on real PostgreSQL: for each resource
 * of crud-spec.json the create, get, list, update and delete give the same resource over both transports, and both
 * refuse alike — 403 without the permission, 404 for an absent row and another tenant's row, 409 for a duplicate, 412
 * for a stale etag, 428 without one, 422 for an invalid body; a sheet's steps change it the same way and are refused
 * alike; a sheet's records, whose list is read by date range, answer alike; marks are recorded, summarized and the
 * sheet completed alike; a step retried with its request_id answers the first result.
 */
@SpringBootTest(classes = AcademicsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=AttendanceGrpcParityComponentTest")
class AttendanceGrpcParityComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String SHEETS = "/v1/attendance/attendance-sheets";
    private static final AtomicInteger DAYS = new AtomicInteger();
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final String ADMIN = token(TENANT, List.of("attendance.*"));
    private static final String NOBODY = token(TENANT, List.of());
    private static final String STRANGER = token(OTHER_TENANT, List.of("attendance.*"));
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;


    @Autowired
    private ProtoBridge bridge;

    @Autowired
    @Qualifier(AttendanceDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "attendance", "academics", "timetable", "assessment");
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
            channel = GrpcParity.channel("AttendanceGrpcParityComponentTest");
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
        return JSON.readTree(AttendanceGrpcParityComponentTest.class.getClassLoader()
                .getResourceAsStream("attendance/crud-spec.json"));
    }

    private static List<String> strings(JsonNode array) {
        List<String> strings = new ArrayList<>();
        array.forEach(element -> strings.add(element.asString()));
        return strings;
    }

    // ---- the standard operations, resource by resource

    private GrpcParityContract contract() {
        return new GrpcParityContract(AttendanceProto.getDescriptor(), this::rest, this::channel, bridge,
                new GrpcParityContract.Callers(ADMIN, NOBODY, STRANGER));
    }

    /**
     * The resources serving all five standard operations with an unpartitioned list; the others (records written by a
     * workflow only, lists read by date range) have their own tests below.
     */
    @TestFactory
    List<DynamicNode> everyStandardRpcAnswersLikeRest() throws Exception {
        List<DynamicNode> tests = new ArrayList<>();
        for (GrpcParityContract.Resource resource : GrpcParityContract.resources("attendance/crud-spec.json")) {
            if (!resource.standardOnly() || resource.partition() != null) {
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
        Answer row = rest("POST", resource.path("collection").asString(), ADMIN, onItsOwnDay(resource));
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
        JsonNode resource = resource("AttendanceSheet");
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

    /** The sheet's create body on a day of its own: a register has one sheet per day and part of the day. */
    private static String onItsOwnDay(JsonNode resource) {
        ObjectNode body = ((ObjectNode) resource.path("create")).deepCopy();
        return body.put("attendanceDate", LocalDate.of(2027, 1, 1).plusDays(DAYS.getAndIncrement()).toString()).toString();
    }

    // ---- the records of a sheet: read only

    @Test
    void theRecordsOfASheetReadAlike() throws Exception {
        JsonNode resource = resource("AttendanceRecord");
        fixtures(TENANT, strings(resource.path("fixtures")));
        fixtures(TENANT, strings(resource.path("seed")));
        String collection = resource.path("collection").asString();
        String sheet = collection.split("/")[4];
        String id = resource.path("seedId").asString();

        MethodDescriptor get = rpc("GetAttendanceRecord");
        Answer read = rest("GET", collection + "/" + id, ADMIN, null);
        assertThat(read.status()).as(read.body().toString()).isEqualTo(200);
        Answer got = grpc(get, set(set(request(get), "attendance_sheet_id", sheet), "id", id).build(), ADMIN);
        assertThat(got.status()).as("GetAttendanceRecord: " + got.code()).isEqualTo(200);
        GrpcParity.assertSameResource(read.body(), got.body().path("attendanceRecord"));
        String absent = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(rest("GET", collection + "/" + absent, ADMIN, null),
                grpc(get, set(set(request(get), "attendance_sheet_id", sheet), "id", absent).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", collection.replace(sheet, absent) + "/" + id, ADMIN, null),
                grpc(get, set(set(request(get), "attendance_sheet_id", absent), "id", id).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", collection + "/" + id, NOBODY, null),
                grpc(get, set(set(request(get), "attendance_sheet_id", sheet), "id", id).build(), NOBODY));
        GrpcParityContract.assertSameAnswer(rest("GET", collection + "/" + id, STRANGER, null),
                grpc(get, set(set(request(get), "attendance_sheet_id", sheet), "id", id).build(), STRANGER));

        MethodDescriptor list = rpc("ListAttendanceRecords");
        String day = "attendanceDate >= \"2026-01-12\" AND attendanceDate < \"2026-01-13\"";
        Answer listed = rest("GET", collection + "?filter=" + q(day), ADMIN, null);
        Answer listedGrpc = grpc(list, set(set(request(list), "attendance_sheet_id", sheet), "filter", day).build(), ADMIN);
        assertThat(listedGrpc.status()).as("ListAttendanceRecords: " + listedGrpc.code()).isEqualTo(200);
        assertThat(ids(listedGrpc.body().path("items"), "id")).containsExactly(id)
                .isEqualTo(ids(listed.body().path("data"), "id"));
        GrpcParity.assertSameResource(listed.body().path("data").get(0), listedGrpc.body().path("items").get(0));
        Answer unbounded = rest("GET", collection, ADMIN, null);
        assertThat(unbounded.status()).isEqualTo(400);
        GrpcParityContract.assertSameAnswer(unbounded,
                grpc(list, set(request(list), "attendance_sheet_id", sheet).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", collection + "?filter=" + q(day), NOBODY, null),
                grpc(list, set(set(request(list), "attendance_sheet_id", sheet), "filter", day).build(), NOBODY));
    }

    // ---- taking attendance

    /** A sheet of the register on the date, in progress, expecting the students (each pre-filled as present). */
    private String sheetInProgress(String register, String date, UUID... students) throws Exception {
        Answer sheet = rest("POST", SHEETS, ADMIN, "{\"registerId\":\"" + register + "\",\"attendanceDate\":\"" + date
                + "\"}");
        assertThat(sheet.status()).as(sheet.body().toString()).isEqualTo(201);
        String id = sheet.body().path("id").asString();
        assertThat(rest("POST", SHEETS + "/" + id + ":start", ADMIN, "{}").status()).isEqualTo(200);
        List<String> records = new ArrayList<>();
        for (UUID student : students) {
            records.add("INSERT INTO attendance.attendance_record (id, tenant_id, sheet_id, student_id, status,"
                    + " attendance_date, row_version) VALUES ('" + UUID.randomUUID() + "', :tenant, '" + id + "', '" + student
                    + "', 'present', '" + date + "', 1)");
        }
        fixtures(TENANT, records);
        return id;
    }

    private DynamicMessage.Builder recording(MethodDescriptor record, String sheet, Object marks) {
        return set(bridge.reply(request(record), "marks", marks), "attendance_sheet_id", sheet);
    }

    @Test
    void attendanceIsRecordedSummarizedAndTheSheetCompletedAlike() throws Exception {
        String institution = UUID.randomUUID().toString();
        Answer register = rest("POST", "/v1/attendance/attendance-registers", ADMIN, "{\"institutionId\":\"" + institution
                + "\",\"code\":\"TAKE\",\"name\":\"Taking\",\"registerScope\":\"institution\"}");
        assertThat(register.status()).as(register.body().toString()).isEqualTo(201);
        UUID absentee = UUID.randomUUID();
        UUID latecomer = UUID.randomUUID();
        String overRest = sheetInProgress(register.body().path("id").asString(), "2026-03-02", absentee, latecomer);
        String overGrpc = sheetInProgress(register.body().path("id").asString(), "2026-03-03", absentee, latecomer);
        List<Map<String, Object>> marks = List.of(
                Map.of("studentId", absentee.toString(), "status", "absent", "remark", "Fever"),
                Map.of("studentId", latecomer.toString(), "status", "late"));
        String marked = JSON.writeValueAsString(Map.of("marks", marks));
        MethodDescriptor record = rpc("RecordAttendance");

        // refused alike: no permission, an absent sheet, no marks, an unknown status, a stale etag of the sheet
        GrpcParityContract.assertSameAnswer(rest("POST", SHEETS + "/" + overRest + ":recordAttendance", NOBODY, marked),
                grpc(record, recording(record, overGrpc, marks).build(), NOBODY));
        String absent = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(rest("POST", SHEETS + "/" + absent + ":recordAttendance", ADMIN, marked),
                grpc(record, recording(record, absent, marks).build(), ADMIN));
        Answer unmarked = rest("POST", SHEETS + "/" + overRest + ":recordAttendance", ADMIN, "{}");
        assertThat(unmarked.status()).isEqualTo(422);
        GrpcParityContract.assertSameAnswer(unmarked,
                grpc(record, set(request(record), "attendance_sheet_id", overGrpc).build(), ADMIN));
        List<Map<String, Object>> asleep = List.of(Map.of("studentId", absentee.toString(), "status", "asleep"));
        Answer unknown = rest("POST", SHEETS + "/" + overRest + ":recordAttendance", ADMIN,
                JSON.writeValueAsString(Map.of("marks", asleep)));
        assertThat(unknown.status()).isEqualTo(400);
        GrpcParityContract.assertSameAnswer(unknown, grpc(record, recording(record, overGrpc, asleep).build(), ADMIN));
        Answer stale = rest("POST", SHEETS + "/" + overRest + ":recordAttendance", ADMIN,
                JSON.writeValueAsString(Map.of("marks", marks, "etag", "\"99\"")));
        assertThat(stale.status()).isEqualTo(412);
        GrpcParityContract.assertSameAnswer(stale,
                grpc(record, set(recording(record, overGrpc, marks), "etag", "\"99\"").build(), ADMIN));

        // recorded over REST on one sheet and over gRPC on the other: the records answered are the sheet's
        Answer recorded = rest("POST", SHEETS + "/" + overRest + ":recordAttendance", ADMIN, marked);
        Answer recordedGrpc = grpc(record, recording(record, overGrpc, marks).build(), ADMIN);
        assertThat(recorded.status()).as(recorded.body().toString()).isEqualTo(200);
        assertThat(recordedGrpc.status()).as("RecordAttendance: " + recordedGrpc.code()).isEqualTo(200);
        assertThat(ids(recorded.body().path("items"), "status")).containsExactly("absent", "late");
        assertThat(ids(recordedGrpc.body().path("items"), "studentId"))
                .containsExactly(absentee.toString(), latecomer.toString());
        JsonNode stored = rest("GET", SHEETS + "/" + overGrpc + "/records?filter="
                + q("attendanceDate >= \"2026-03-03\" AND attendanceDate < \"2026-03-04\""), ADMIN, null).body().path("data");
        assertThat(stored).hasSize(2);
        for (JsonNode answered : recordedGrpc.body().path("items")) {
            for (JsonNode row : stored) {
                if (row.path("id").asString().equals(answered.path("id").asString())) {
                    GrpcParity.assertSameResource(row, answered);
                }
            }
        }
        assertThat(ids(stored, "status")).containsExactlyInAnyOrder("absent", "late");

        // completed over REST on one sheet and over gRPC on the other; a completed sheet takes no step twice
        MethodDescriptor complete = rpc("CompleteAttendanceSheet");
        GrpcParityContract.assertSameAnswer(rest("POST", SHEETS + "/" + overRest + ":complete", NOBODY, "{}"),
                grpc(complete, set(request(complete), "attendance_sheet_id", overGrpc).build(), NOBODY));
        Answer staleSheet = rest("POST", SHEETS + "/" + overRest + ":complete", ADMIN, "{\"etag\":\"\\\"99\\\"\"}");
        assertThat(staleSheet.status()).isEqualTo(412);
        GrpcParityContract.assertSameAnswer(staleSheet, grpc(complete, set(set(request(complete), "attendance_sheet_id",
                overGrpc), "etag", "\"99\"").build(), ADMIN));
        Answer completed = rest("POST", SHEETS + "/" + overRest + ":complete", ADMIN, "{}");
        Answer completedGrpc = grpc(complete, set(request(complete), "attendance_sheet_id", overGrpc).build(), ADMIN);
        assertThat(completed.status()).as(completed.body().toString()).isEqualTo(200);
        assertThat(completedGrpc.status()).as("CompleteAttendanceSheet: " + completedGrpc.code()).isEqualTo(200);
        assertThat(completed.body().path("status").asString()).isEqualTo("done");
        GrpcParity.assertSameResource(rest("GET", SHEETS + "/" + overGrpc, ADMIN, null).body(),
                completedGrpc.body().path("attendanceSheet"));
        Answer twice = rest("POST", SHEETS + "/" + overRest + ":complete", ADMIN, "{}");
        assertThat(twice.status()).isEqualTo(409);
        GrpcParityContract.assertSameAnswer(twice,
                grpc(complete, set(request(complete), "attendance_sheet_id", overGrpc).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("POST", SHEETS + "/" + overRest + ":recordAttendance", ADMIN, marked),
                grpc(record, recording(record, overGrpc, marks).build(), ADMIN));

        // completed sheets are what the summaries count
        summariesAnswerAlike(institution, absentee.toString());
    }

    /** The summaries of the institution's March, by each grouping and page by page, and their refusals. */
    private void summariesAnswerAlike(String institution, String student) throws Exception {
        String summaries = "/v1/attendance/summaries";
        String march = summaries + "?institutionId=" + institution + "&fromDate=2026-03-01&toDate=2026-03-31";
        MethodDescriptor rpc = rpc("SummarizeAttendance");
        var groupings = rpc.getInputType().findFieldByName("group_by").getEnumType();
        for (String grouping : List.of("student", "class_section", "date")) {
            Answer summary = rest("GET", march + "&groupBy=" + grouping, ADMIN, null);
            Answer summaryGrpc = grpc(rpc, set(summary(rpc, institution, "2026-03-01", "2026-03-31"), "group_by",
                    groupings.findValueByName("GROUP_BY_" + grouping.toUpperCase(Locale.ROOT))).build(), ADMIN);
            assertThat(summary.status()).as(summary.body().toString()).isEqualTo(200);
            assertThat(summaryGrpc.status()).as("SummarizeAttendance by " + grouping + ": " + summaryGrpc.code())
                    .isEqualTo(200);
            JsonNode groups = summary.body().path("data");
            assertThat(summaryGrpc.body().path("items")).as("by " + grouping).hasSameSizeAs(groups);
            for (int i = 0; i < groups.size(); i++) {
                GrpcParity.assertSameResource(groups.get(i), summaryGrpc.body().path("items").get(i));
            }
        }
        var byStudent = groupings.findValueByName("GROUP_BY_STUDENT");
        Answer one = rest("GET", march + "&groupBy=student&studentId=" + student, ADMIN, null);
        Answer oneGrpc = grpc(rpc, set(set(summary(rpc, institution, "2026-03-01", "2026-03-31"), "group_by", byStudent),
                "student_id", student).build(), ADMIN);
        assertThat(one.body().path("data")).hasSize(1);
        assertThat(one.body().path("data").get(0).path("absent").asInt()).isEqualTo(2);
        GrpcParity.assertSameResource(one.body().path("data").get(0), oneGrpc.body().path("items").get(0));

        Answer first = rest("GET", march + "&groupBy=student&size=1", ADMIN, null);
        Answer firstGrpc = grpc(rpc, set(set(summary(rpc, institution, "2026-03-01", "2026-03-31"), "group_by", byStudent),
                "pageable", pageable(rpc, 0, 1, "")).build(), ADMIN);
        assertThat(ids(firstGrpc.body().path("items"), "studentId")).hasSize(1)
                .isEqualTo(ids(first.body().path("data"), "studentId"));
        String cursor = firstGrpc.body().path("pagination").path("nextCursor").asString();
        String next = first.body().path("paging").path("next").asString();
        GrpcParity.assertSameCursor(next, cursor);
        assertThat(ids(grpc(rpc, set(set(summary(rpc, institution, "2026-03-01", "2026-03-31"), "group_by", byStudent),
                "pageable", pageable(rpc, 2, 1, cursor)).build(), ADMIN).body().path("items"), "studentId")).hasSize(1)
                .isEqualTo(ids(rest("GET", next, ADMIN, null).body().path("data"), "studentId"));

        // refused alike: no grouping, no institution, a malformed date, the dates the wrong way round, too many
        // days, a malformed student id, no permission
        Answer ungrouped = rest("GET", march, ADMIN, null);
        assertThat(ungrouped.status()).isEqualTo(400);
        GrpcParityContract.assertSameAnswer(ungrouped,
                grpc(rpc, summary(rpc, institution, "2026-03-01", "2026-03-31").build(), ADMIN));
        GrpcParityContract.assertSameAnswer(
                rest("GET", summaries + "?fromDate=2026-03-01&toDate=2026-03-31&groupBy=student", ADMIN, null),
                grpc(rpc, set(summary(rpc, "", "2026-03-01", "2026-03-31"), "group_by", byStudent).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", summaries + "?institutionId=" + institution
                        + "&fromDate=March&toDate=2026-03-31&groupBy=student", ADMIN, null),
                grpc(rpc, set(summary(rpc, institution, "March", "2026-03-31"), "group_by", byStudent).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", summaries + "?institutionId=" + institution
                        + "&fromDate=2026-03-31&toDate=2026-03-01&groupBy=student", ADMIN, null),
                grpc(rpc, set(summary(rpc, institution, "2026-03-31", "2026-03-01"), "group_by", byStudent).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", summaries + "?institutionId=" + institution
                        + "&fromDate=2026-01-01&toDate=2027-12-31&groupBy=student", ADMIN, null),
                grpc(rpc, set(summary(rpc, institution, "2026-01-01", "2027-12-31"), "group_by", byStudent).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", march + "&groupBy=student&studentId=nobody", ADMIN, null),
                grpc(rpc, set(set(summary(rpc, institution, "2026-03-01", "2026-03-31"), "group_by", byStudent),
                        "student_id", "nobody").build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", march + "&groupBy=student", NOBODY, null),
                grpc(rpc, set(summary(rpc, institution, "2026-03-01", "2026-03-31"), "group_by", byStudent).build(), NOBODY));
    }

    private static DynamicMessage.Builder summary(MethodDescriptor rpc, String institution, String from, String to) {
        return set(set(set(request(rpc), "institution_id", institution), "from_date", from), "to_date", to);
    }

    /**
     * Opening a sheet for a timetable session asks timetable, sis and org, which a module-only service does not reach:
     * both transports answer that alike, and refuse alike before it.
     */
    @Test
    void takingAttendanceForASessionAnswersAlike() throws Exception {
        MethodDescriptor take = rpc("TakeAttendanceForSession");
        String session = UUID.randomUUID().toString();
        Answer unreachable = rest("POST", SHEETS + ":forSession", ADMIN, "{\"timetableSessionId\":\"" + session + "\"}");
        assertThat(unreachable.status()).as(unreachable.body().toString()).isGreaterThanOrEqualTo(400);
        GrpcParityContract.assertSameAnswer(unreachable,
                grpc(take, set(request(take), "timetable_session_id", session).build(), ADMIN));
        Answer unnamed = rest("POST", SHEETS + ":forSession", ADMIN, "{}");
        assertThat(unnamed.status()).isEqualTo(422);
        GrpcParityContract.assertSameAnswer(unnamed, grpc(take, request(take).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("POST", SHEETS + ":forSession", ADMIN, "{\"timetableSessionId\":\"x\"}"),
                grpc(take, set(request(take), "timetable_session_id", "x").build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("POST", SHEETS + ":forSession", NOBODY, "{\"timetableSessionId\":\""
                + session + "\"}"), grpc(take, set(request(take), "timetable_session_id", session).build(), NOBODY));
    }
}
