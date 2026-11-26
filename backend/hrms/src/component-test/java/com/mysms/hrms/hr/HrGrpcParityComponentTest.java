package com.mysms.hrms.hr;

import com.mysms.hrms.HrmsApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Descriptors.FieldDescriptor;
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
import com.mysms.hrms.hr.config.HrDatabaseConfig;
import com.mysms.hr.v1.HrProto;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import java.net.URI;
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
import java.util.regex.Pattern;
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
 * Every RPC of hr's five gRPC services against the same REST operation, on real PostgreSQL: for each resource of
 * crud-spec.json the create, get, list, update and delete give the same resource over both transports, and both refuse
 * alike — 403 without the permission, 404 for an absent row and another tenant's row, 409 for a duplicate, 412 for a
 * stale etag, 428 without one, 422 for an invalid body; every state transition of crud-spec.json (a staff member's
 * status, a leave's decision) moves and refuses alike; the leave balance, a staff member's balance summary and the
 * portal account answer alike; and a request_id replays like an Idempotency-Key.
 */
@SpringBootTest(classes = HrmsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=HrGrpcParityComponentTest")
class HrGrpcParityComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final String ADMIN = token(TENANT, List.of("hr.*"));
    private static final String NOBODY = token(TENANT, List.of());
    private static final String STRANGER = token(OTHER_TENANT, List.of("hr.*"));
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;


    @Autowired
    private ProtoBridge bridge;

    @Autowired
    @Qualifier(HrDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "hr", "payroll");
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

    /**
     * One tenant of its own with its two callers: an administrator and a user holding no permission. Row ids are
     * unique across tenants, so the school has ids of its own: {@link #id} gives its id for a name, {@link #local}
     * rewrites every id of a crud-spec.json text (fixtures, bodies, paths) to the school's.
     */
    private record School(UUID tenant, String admin, String nobody) {

        private static final Pattern ID = Pattern.compile("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}");

        static School open() {
            UUID tenant = UUID.randomUUID();
            return new School(tenant, token(tenant, List.of("hr.*")), token(tenant, List.of()));
        }

        String id(String name) {
            return UUID.nameUUIDFromBytes((tenant + name).getBytes(StandardCharsets.UTF_8)).toString();
        }

        String local(String text) {
            return ID.matcher(text).replaceAll(match -> id(match.group()));
        }

        List<String> local(JsonNode statements) {
            return strings(statements).stream().map(this::local).toList();
        }
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
            channel = GrpcParity.channel("HrGrpcParityComponentTest");
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

    /**
     * The request of an RPC from the REST call it mirrors: the path ids by their proto names, then the REST body,
     * property by property through the service's own bridge (so an enum travels as its proto value).
     */
    @SuppressWarnings("unchecked")
    private DynamicMessage call(MethodDescriptor rpc, Map<String, String> ids, JsonNode body) {
        DynamicMessage.Builder request = request(rpc);
        ids.forEach((field, id) -> set(request, field, id));
        if (body != null) {
            for (FieldDescriptor field : rpc.getInputType().getFields()) {
                if (body.has(field.getJsonName())) {
                    bridge.reply(request, field.getName(), JSON.convertValue(body.get(field.getJsonName()), Object.class));
                }
            }
        }
        return request.build();
    }

    private DynamicMessage call(MethodDescriptor rpc, String idField, String id, JsonNode body) {
        return call(rpc, Map.of(idField, id), body);
    }

    /** The one resource a response carries. */
    private static JsonNode resource(Answer overGrpc) {
        assertThat(overGrpc.status()).as("gRPC answered %s", overGrpc.code()).isEqualTo(200);
        return overGrpc.body().properties().iterator().next().getValue();
    }

    private static ObjectNode json(String text) {
        return (ObjectNode) JSON.readTree(text);
    }

    private void fixtures(UUID tenant, List<String> statements) {
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

    // ---- the standard operations, resource by resource

    private GrpcParityContract contract() {
        return new GrpcParityContract(HrProto.getDescriptor(), this::rest, this::channel, bridge,
                new GrpcParityContract.Callers(ADMIN, NOBODY, STRANGER));
    }

    /** The resources serving all five standard operations; the others are verified by their own tests below. */
    @TestFactory
    List<DynamicNode> everyStandardRpcAnswersLikeRest() throws Exception {
        List<DynamicNode> tests = new ArrayList<>();
        for (GrpcParityContract.Resource resource : GrpcParityContract.resources("hr/crud-spec.json")) {
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

    // ---- the state transitions of crud-spec.json

    /**
     * Every transition crud-spec.json describes, on a row of a tenant of its own: both transports refuse alike for an
     * absent row, without the permission and with a stale etag; the move made over gRPC reads the same over REST and
     * sets what the specification says; the move repeated is answered alike; and REST makes the same move from the
     * same prepared state.
     */
    @TestFactory
    List<DynamicNode> everyTransitionAnswersLikeRest() throws Exception {
        JsonNode spec = JSON.readTree(getClass().getClassLoader().getResourceAsStream("hr/crud-spec.json"));
        List<DynamicNode> tests = new ArrayList<>();
        for (JsonNode resource : spec.path("resources")) {
            for (JsonNode transition : resource.path("transitions")) {
                String operation = transition.path("operation").asString();
                tests.add(DynamicTest.dynamicTest(operation, () -> {
                    School school = School.open();
                    fixtures(school.tenant(), school.local(resource.path("fixtures")));
                    String collection = school.local(resource.path("collection").asString());
                    String id;
                    if (resource.has("seed")) {
                        fixtures(school.tenant(), school.local(resource.path("seed")));
                        id = school.local(resource.path("seedId").asString());
                    } else {
                        Answer created = rest("POST", collection, school.admin(),
                                school.local(resource.path("create").toString()));
                        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
                        id = created.body().path("id").asString();
                    }
                    verifyTransition(school, collection, id, transition);
                }));
            }
        }
        return tests;
    }

    private void verifyTransition(School school, String collection, String id, JsonNode transition) throws Exception {
        MethodDescriptor rpc = rpc(transition.path("operation").asString().substring("hr.".length()));
        String idField = rpc.getInputType().getFields().get(0).getName();
        String path = transition.path("path").asString();
        ObjectNode body = (ObjectNode) transition.path("body");
        List<String> prepare = strings(transition.path("prepare")).stream().map(sql -> sql.replace("{id}", id)).toList();
        fixtures(school.tenant(), prepare);

        String absent = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(rest("POST", path.replace("{id}", absent), school.admin(), body.toString()),
                grpc(rpc, call(rpc, idField, absent, body), school.admin()));
        GrpcParityContract.assertSameAnswer(rest("POST", path.replace("{id}", id), school.nobody(), body.toString()),
                grpc(rpc, call(rpc, idField, id, body), school.nobody()));
        ObjectNode stale = body.deepCopy().put("etag", "\"99\"");
        GrpcParityContract.assertSameAnswer(rest("POST", path.replace("{id}", id), school.admin(), stale.toString()),
                grpc(rpc, call(rpc, idField, id, stale), school.admin()));

        JsonNode moved = resource(grpc(rpc, call(rpc, idField, id, body), school.admin()));
        JsonNode read = rest("GET", collection + "/" + id, school.admin(), null).body();
        sameResource(read, moved);
        transition.path("sets").properties().forEach(set -> assertThat(read.path(set.getKey()).asString())
                .as(set.getKey()).isEqualTo(set.getValue().asString()));
        GrpcParityContract.assertSameAnswer(rest("POST", path.replace("{id}", id), school.admin(), body.toString()),
                grpc(rpc, call(rpc, idField, id, body), school.admin()));

        fixtures(school.tenant(), prepare);
        Answer overRest = rest("POST", path.replace("{id}", id), school.admin(), body.toString());
        assertThat(overRest.status()).as(overRest.body().toString()).isEqualTo(200);
        transition.path("sets").properties().forEach(set -> assertThat(overRest.body().path(set.getKey()).asString())
                .as(set.getKey()).isEqualTo(set.getValue().asString()));
    }

    /** The same resource over both transports. */
    private static void sameResource(JsonNode overRest, JsonNode overGrpc) {
        GrpcParity.assertSameResource(overRest, overGrpc);
    }

    // ---- what the standard contract does not reach

    /** A staff member ("staff") and a leave type ("leaveType") of the school, by the school's ids. */
    private void staffAndLeaveType(School school) {
        fixtures(school.tenant(), List.of(
                "INSERT INTO hr.staff (tenant_id, person_id, institution_id, employee_code, row_version, id) VALUES (:tenant,"
                        + " '" + school.id("person") + "', '" + school.id("institution") + "', 'E-17', 1, '"
                        + school.id("staff") + "')",
                "INSERT INTO hr.leave_type (tenant_id, institution_id, name, max_days_per_year, row_version, code, id)"
                        + " VALUES (:tenant, '" + school.id("institution") + "', 'Casual', 12, 1, 'CL', '"
                        + school.id("leaveType") + "')"));
    }

    /** LeaveBalance has no Delete, so its four operations are compared here. */
    @Test
    void aLeaveBalanceIsCreatedReadListedAndUpdatedAlikeOverBoth() throws Exception {
        School school = School.open();
        staffAndLeaveType(school);
        String staff = school.id("staff");
        String leaveType = school.id("leaveType");
        String year = school.id("year");
        ObjectNode body = json("{\"staffId\":\"" + staff + "\",\"leaveTypeId\":\"" + leaveType
                + "\",\"entitledDays\":\"10\",\"academicYearId\":\"" + year + "\"}");
        MethodDescriptor create = rpc("CreateLeaveBalance");
        DynamicMessage creation = call(create, Map.of(), json("{}").set("leaveBalance", body));
        JsonNode created = resource(grpc(create, creation, school.admin()));
        String id = created.path("id").asString();
        Answer read = rest("GET", "/v1/hr/leave-balances/" + id, school.admin(), null);
        assertThat(read.status()).isEqualTo(200);
        sameResource(read.body(), created);
        // the same balance again is refused alike, and so are an empty body and a caller without the permission
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/hr/leave-balances", school.admin(), body.toString()),
                grpc(create, creation, school.admin()));
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/hr/leave-balances", school.admin(), "{}"),
                grpc(create, request(create).build(), school.admin()));
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/hr/leave-balances", school.nobody(), body.toString()),
                grpc(create, creation, school.nobody()));

        MethodDescriptor get = rpc("GetLeaveBalance");
        sameResource(read.body(), resource(grpc(get, call(get, "id", id, null), school.admin())));
        String absent = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/hr/leave-balances/" + absent, school.admin(), null),
                grpc(get, call(get, "id", absent, null), school.admin()));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/hr/leave-balances/" + id, STRANGER, null),
                grpc(get, call(get, "id", id, null), STRANGER));

        MethodDescriptor list = rpc("ListLeaveBalances");
        Answer listed = grpc(list, request(list).build(), school.admin());
        assertThat(listed.status()).isEqualTo(200);
        JsonNode overRest = rest("GET", "/v1/hr/leave-balances", school.admin(), null).body();
        assertThat(listed.body().path("items")).hasSize(overRest.path("data").size()).hasSize(1);
        sameResource(overRest.path("data").get(0), listed.body().path("items").get(0));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/hr/leave-balances?filter=nope%20%3D%201", school.admin(),
                null), grpc(list, set(request(list), "filter", "nope = 1").build(), school.admin()));

        MethodDescriptor update = rpc("UpdateLeaveBalance");
        ObjectNode changes = json("{\"entitledDays\":\"12\"}");
        com.google.protobuf.FieldMask mask = com.google.protobuf.FieldMask.newBuilder().addPaths("entitled_days").build();
        String path = "/v1/hr/leave-balances/" + id;
        GrpcParityContract.assertSameAnswer(rest("PATCH", path, school.admin(), changes.toString(), "If-Match", "\"99\""),
                grpc(update, set(call(update, Map.of(), json("{}").set("leaveBalance", changes.deepCopy().put("id", id)
                        .put("etag", "\"99\""))).toBuilder(), "update_mask", mask).build(), school.admin()));
        GrpcParityContract.assertSameAnswer(rest("PATCH", path, school.admin(), changes.toString()),
                grpc(update, set(call(update, Map.of(), json("{}").set("leaveBalance", changes.deepCopy().put("id", id)))
                        .toBuilder(), "update_mask", mask).build(), school.admin()));
        JsonNode updated = resource(grpc(update, set(call(update, Map.of(), json("{}").set("leaveBalance",
                changes.deepCopy().put("id", id).put("etag", read.body().path("etag").asString()))).toBuilder(),
                "update_mask", mask).build(), school.admin()));
        assertThat(new java.math.BigDecimal(updated.path("entitledDays").asString())).isEqualByComparingTo("12");
        sameResource(rest("GET", path, school.admin(), null).body(),
                resource(grpc(get, call(get, "id", id, null), school.admin())));
    }

    @Test
    void aStaffMembersLeaveBalanceReadsTheSameOverBoth() throws Exception {
        School school = School.open();
        staffAndLeaveType(school);
        String staff = school.id("staff");
        String leaveType = school.id("leaveType");
        String year = school.id("year");
        Answer balance = rest("POST", "/v1/hr/leave-balances", school.admin(), "{\"staffId\":\"" + staff
                + "\",\"leaveTypeId\":\"" + leaveType + "\",\"entitledDays\":\"10\",\"academicYearId\":\"" + year + "\"}");
        assertThat(balance.status()).as(balance.body().toString()).isEqualTo(201);
        MethodDescriptor summary = rpc("GetStaffLeaveBalance");
        String path = "/v1/hr/staff/" + staff + ":leaveBalance";
        Answer overRest = rest("GET", path + "?academicYearId=" + year, school.admin(), null);
        Answer overGrpc = grpc(summary, call(summary, Map.of("staff_id", staff, "academic_year_id", year), null),
                school.admin());
        assertThat(overRest.status()).as(overRest.body().toString()).isEqualTo(200);
        assertThat(overGrpc.status()).as(String.valueOf(overGrpc.code())).isEqualTo(200);
        assertThat(overGrpc.body().path("items")).hasSize(overRest.body().path("data").size()).isNotEmpty();
        sameResource(overRest.body().path("data").get(0), overGrpc.body().path("items").get(0));
        assertThat(overGrpc.body().path("items").get(0).path("entitledDays").asString()).isEqualTo("10");
        assertThat(overGrpc.body().path("pagination").path("page").asInt())
                .isEqualTo(overRest.body().path("paging").path("page").asInt());
        // the academic year is required on both, and the refusals match
        GrpcParityContract.assertSameAnswer(rest("GET", path, school.admin(), null),
                grpc(summary, call(summary, "staff_id", staff, null), school.admin()));
        String absent = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/hr/staff/" + absent + ":leaveBalance?academicYearId=" + year,
                school.admin(), null), grpc(summary, call(summary, Map.of("staff_id", absent, "academic_year_id", year),
                null), school.admin()));
        GrpcParityContract.assertSameAnswer(rest("GET", path + "?academicYearId=" + year, school.nobody(), null),
                grpc(summary, call(summary, Map.of("staff_id", staff, "academic_year_id", year), null), school.nobody()));
        GrpcParityContract.assertSameAnswer(rest("GET", path + "?academicYearId=" + year, STRANGER, null),
                grpc(summary, call(summary, Map.of("staff_id", staff, "academic_year_id", year), null), STRANGER));
    }

    /** hr runs alone here, without an iam address: what hr decides itself is refused alike, then 503 on both. */
    @Test
    void aPortalAccountIsRefusedAlikeOverBoth() throws Exception {
        School school = School.open();
        staffAndLeaveType(school);
        String staff = school.id("staff");
        MethodDescriptor account = rpc("CreateStaffPortalAccount");
        String path = "/v1/hr/staff/" + staff + ":createPortalAccount";
        for (String body : List.of("{\"sendInvite\":true}", "{\"sendInvite\":false}", "{}")) {
            Answer overRest = rest("POST", path, school.admin(), body);
            assertThat(overRest.status()).as(body).isGreaterThanOrEqualTo(400);
            GrpcParityContract.assertSameAnswer(overRest,
                    grpc(account, call(account, "staff_id", staff, json(body)), school.admin()));
        }
        GrpcParityContract.assertSameAnswer(rest("POST", path, school.nobody(), "{}"),
                grpc(account, call(account, "staff_id", staff, null), school.nobody()));
        String absent = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/hr/staff/" + absent + ":createPortalAccount",
                school.admin(), "{}"), grpc(account, call(account, "staff_id", absent, null), school.admin()));
        fixtures(school.tenant(), List.of("UPDATE hr.staff SET status = 'exited' WHERE id = '" + staff + "'"));
        Answer left = rest("POST", path, school.admin(), "{}");
        assertThat(left.status()).isEqualTo(409);
        GrpcParityContract.assertSameAnswer(left, grpc(account, call(account, "staff_id", staff, null), school.admin()));
    }

    @Test
    void theMovesOfAStaffMembersStatusAndALeaveDecisionAreRefusedAlike() throws Exception {
        School school = School.open();
        staffAndLeaveType(school);
        String staff = school.id("staff");
        String year = school.id("year");
        MethodDescriptor change = rpc("ChangeStaffStatus");
        String path = "/v1/hr/staff/" + staff + ":changeStatus";
        // the status is required; a member already active is not activated; one who left is not suspended
        for (String body : List.of("{}", "{\"status\":\"active\"}")) {
            Answer overRest = rest("POST", path, school.admin(), body);
            assertThat(overRest.status()).as(body).isGreaterThanOrEqualTo(400);
            GrpcParityContract.assertSameAnswer(overRest,
                    grpc(change, call(change, "staff_id", staff, json(body)), school.admin()));
        }
        JsonNode exited = resource(grpc(change, call(change, "staff_id", staff, json("{\"status\":\"exited\"}")),
                school.admin()));
        assertThat(exited.path("status").asString()).isEqualTo("STATUS_EXITED");
        GrpcParityContract.assertSameAnswer(rest("POST", path, school.admin(), "{\"status\":\"inactive\"}"),
                grpc(change, call(change, "staff_id", staff, json("{\"status\":\"inactive\"}")), school.admin()));

        // a leave already decided is not decided again, whichever transport decided it
        Answer leave = rest("POST", "/v1/hr/leave-applications", school.admin(), "{\"institutionId\":\""
                + school.id("institution") + "\",\"academicYearId\":\"" + year + "\",\"staffId\":\"" + staff
                + "\",\"fromAt\":\"2026-01-10T09:00:00Z\",\"toAt\":\"2026-01-10T10:00:00Z\"}");
        assertThat(leave.status()).as(leave.body().toString()).isEqualTo(201);
        String id = leave.body().path("id").asString();
        fixtures(school.tenant(), List.of("UPDATE hr.leave_application SET status = 'pending' WHERE id = '" + id + "'"));
        MethodDescriptor reject = rpc("RejectLeave");
        JsonNode rejected = resource(grpc(reject, call(reject, "leave_application_id", id,
                json("{\"reason\":\"Exams that week\"}")), school.admin()));
        assertThat(rejected.path("status").asString()).isEqualTo("STATUS_REJECTED");
        for (String operation : List.of("ApproveLeave", "RejectLeave", "CancelLeave")) {
            MethodDescriptor decide = rpc(operation);
            String verb = operation.substring(0, operation.length() - "Leave".length()).toLowerCase(java.util.Locale.ROOT);
            Answer overRest = rest("POST", "/v1/hr/leave-applications/" + id + ":" + verb, school.admin(), "{}");
            assertThat(overRest.status()).as(operation).isEqualTo(409);
            GrpcParityContract.assertSameAnswer(overRest,
                    grpc(decide, call(decide, "leave_application_id", id, null), school.admin()));
        }
    }

    /** request_id over gRPC follows the rules of REST's Idempotency-Key. */
    @Test
    void aRequestIdReplaysTheFirstAnswerLikeAnIdempotencyKey() throws Exception {
        School school = School.open();
        MethodDescriptor create = rpc("CreateDesignation");
        ObjectNode body = json("{\"code\":\"HOD\",\"name\":\"Head of department\",\"staffCategory\":\"teaching\"}");
        ObjectNode other = json("{\"code\":\"LAB\",\"name\":\"Lab assistant\",\"staffCategory\":\"non_teaching\"}");
        String key = UUID.randomUUID().toString();
        DynamicMessage first = call(create, "request_id", key, json("{}").set("designation", body));
        JsonNode created = resource(grpc(create, first, school.admin()));
        JsonNode replayed = resource(grpc(create, first, school.admin()));
        assertThat(replayed.path("id").asString()).as("the first answer, not a second row or a 409")
                .isEqualTo(created.path("id").asString());
        Answer reused = grpc(create, call(create, "request_id", key, json("{}").set("designation", other)), school.admin());

        String restKey = UUID.randomUUID().toString();
        Answer restFirst = rest("POST", "/v1/hr/designations", school.admin(), other.toString(), "Idempotency-Key", restKey);
        Answer restReplayed = rest("POST", "/v1/hr/designations", school.admin(), other.toString(), "Idempotency-Key",
                restKey);
        assertThat(restFirst.status()).as(restFirst.body().toString()).isEqualTo(201);
        assertThat(restReplayed.body().path("id").asString()).isEqualTo(restFirst.body().path("id").asString());
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/hr/designations", school.admin(), body.toString(),
                "Idempotency-Key", restKey), reused);
        assertThat(reused.code()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
        assertThat(rest("GET", "/v1/hr/designations", school.admin(), null).body().path("data")).hasSize(2);
    }
}
