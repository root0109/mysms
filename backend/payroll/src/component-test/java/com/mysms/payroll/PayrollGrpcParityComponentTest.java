package com.mysms.payroll;

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
import com.mysms.payroll.config.PayrollDatabaseConfig;
import com.mysms.payroll.v1.PayrollProto;
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
 * Every RPC of payroll's ten gRPC services against the same REST operation, on real PostgreSQL: for each resource of
 * crud-spec.json serving the five standard operations the create, get, list, update and delete give the same resource
 * over both transports, and both refuse alike — 403 without the permission, 404 for an absent row and another tenant's
 * row, 409 for a duplicate, 412 for a stale etag, 428 without one, 422 for an invalid body; the payslip run (a 202
 * operation keyed by request_id), the operations it leaves, the payslips and lines it writes, the payment of a payslip
 * and its cancellation answer alike.
 */
@SpringBootTest(classes = PayrollApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=PayrollGrpcParityComponentTest")
class PayrollGrpcParityComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final String ADMIN = token(TENANT, List.of("payroll.*"));
    private static final String NOBODY = token(TENANT, List.of());
    private static final String STRANGER = token(OTHER_TENANT, List.of("payroll.*"));
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;


    @Autowired
    private ProtoBridge bridge;

    @Autowired
    @Qualifier(PayrollDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "payroll");
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
            return new School(tenant, token(tenant, List.of("payroll.*")), token(tenant, List.of()));
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
            channel = GrpcParity.channel("PayrollGrpcParityComponentTest");
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
        return new GrpcParityContract(PayrollProto.getDescriptor(), this::rest, this::channel, bridge,
                new GrpcParityContract.Callers(ADMIN, NOBODY, STRANGER));
    }

    /** The resources serving all five standard operations; the others are verified by their own tests below. */
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

    // ---- the state transitions of crud-spec.json

    /**
     * Every transition crud-spec.json describes, on a row of a tenant of its own: both transports refuse alike for an
     * absent row, without the permission and with a stale etag; the move made over gRPC reads the same over REST and
     * sets what the specification says; the move repeated is answered alike; and REST makes the same move from the
     * same prepared state.
     */
    @TestFactory
    List<DynamicNode> everyTransitionAnswersLikeRest() throws Exception {
        JsonNode spec = JSON.readTree(getClass().getClassLoader().getResourceAsStream("crud-spec.json"));
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
        MethodDescriptor rpc = rpc(transition.path("operation").asString().substring("payroll.".length()));
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
        GrpcParity.assertSameResource(read, moved);
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

    // ---- what the standard contract does not reach

    /**
     * A school whose one teacher is paid a basic pay of 30000.00 with 3600.00 provident fund deducted, from a salary
     * account.
     */
    private School payingSchool() {
        School school = School.open();
        String institution = school.id("institution");
        String template = school.id("template");
        String category = school.id("category");
        String salary = school.id("salary");
        fixtures(school.tenant(), List.of(
                "INSERT INTO payroll.pay_category (id, tenant_id, name, code) VALUES ('" + category
                        + "', :tenant, 'Fixed amount', 'FIXED')",
                "INSERT INTO payroll.pay_component (id, tenant_id, code, name, component_type, sort_order) VALUES ('"
                        + school.id("basic") + "', :tenant, 'BASIC', 'Basic pay', 'earning', 1)",
                "INSERT INTO payroll.pay_component (id, tenant_id, code, name, component_type, sort_order) VALUES ('"
                        + school.id("pf") + "', :tenant, 'PF', 'Provident fund', 'deduction', 2)",
                "INSERT INTO payroll.salary_template (id, tenant_id, institution_id, name) VALUES ('" + template
                        + "', :tenant, '" + institution + "', 'Teaching staff')",
                "INSERT INTO payroll.disbursement_account (id, tenant_id, institution_id, managed_by_user_id, name,"
                        + " account_key, account_number_enc, bank_routing_code) VALUES ('" + school.id("account")
                        + "', :tenant, '" + institution + "', '" + UUID.randomUUID()
                        + "', 'Salary account', 'SAL', 'sealed', 'TEST0000001')",
                "INSERT INTO payroll.salary_template_component (id, tenant_id, salary_template_id, pay_component_id,"
                        + " pay_category_id) VALUES ('" + school.id("templateBasic") + "', :tenant, '" + template + "', '"
                        + school.id("basic") + "', '" + category + "')",
                "INSERT INTO payroll.salary_template_component (id, tenant_id, salary_template_id, pay_component_id,"
                        + " pay_category_id) VALUES ('" + school.id("templatePf") + "', :tenant, '" + template + "', '"
                        + school.id("pf") + "', '" + category + "')",
                "INSERT INTO payroll.staff_salary (id, tenant_id, staff_id, salary_template_id, gross_salary,"
                        + " effective_date) VALUES ('" + salary + "', :tenant, '" + school.id("teacher") + "', '"
                        + template + "', 1, '2025-04-01')",
                "INSERT INTO payroll.staff_salary_component (tenant_id, staff_salary_id, salary_template_component_id,"
                        + " amount) VALUES (:tenant, '" + salary + "', '" + school.id("templateBasic") + "', 30000.00)",
                "INSERT INTO payroll.staff_salary_component (tenant_id, staff_salary_id, salary_template_component_id,"
                        + " amount) VALUES (:tenant, '" + salary + "', '" + school.id("templatePf") + "', 3600.00)"));
        return school;
    }

    private static ObjectNode run(School school, String periodStart, String periodEnd) {
        return json("{\"institutionId\":\"" + school.id("institution") + "\",\"periodStart\":\"" + periodStart
                + "\",\"periodEnd\":\"" + periodEnd + "\"}");
    }

    @Test
    void aPayslipRunStartsReplaysAndIsReadAlikeOverBoth() throws Exception {
        School school = payingSchool();
        MethodDescriptor generate = rpc("GeneratePayslips");
        String key = UUID.randomUUID().toString();
        DynamicMessage january = call(generate, "request_id", key, run(school, "2026-01-01", "2026-01-31"));
        JsonNode started = resource(grpc(generate, january, school.admin()));
        assertThat(started.path("module").asString()).isEqualTo("payroll");
        assertThat(started.path("kind").asString()).isEqualTo("payroll.GeneratePayslips");
        assertThat(started.path("status").asString()).isEqualTo("succeeded");
        assertThat(started.path("result").path("generated").asInt()).isEqualTo(1);
        String operation = started.path("operationId").asString();
        // the same request_id answers the first run, another request under it is refused
        assertThat(resource(grpc(generate, january, school.admin())).path("operationId").asString()).isEqualTo(operation);
        Answer reused = grpc(generate, call(generate, "request_id", key, run(school, "2026-02-01", "2026-02-28")),
                school.admin());
        assertThat(reused.status()).isEqualTo(422);
        assertThat(reused.code()).isEqualTo("IDEMPOTENCY_KEY_REUSED");

        // the same run over REST keeps the payslip (nothing is generated twice), and both operations read alike
        Answer overRest = rest("POST", "/v1/payroll/payslips:generate", school.admin(),
                run(school, "2026-01-01", "2026-01-31").toString(), "Idempotency-Key", UUID.randomUUID().toString());
        assertThat(overRest.status()).as(overRest.body().toString()).isEqualTo(202);
        assertThat(overRest.body().path("result").path("kept").asInt()).isEqualTo(1);
        MethodDescriptor get = rpc("GetOperation");
        for (String id : List.of(operation, overRest.body().path("operationId").asString())) {
            GrpcParity.assertSameResource(rest("GET", "/v1/payroll/operations/" + id, school.admin(), null).body(),
                    resource(grpc(get, call(get, "operation_id", id, null), school.admin())));
        }
        MethodDescriptor list = rpc("ListOperations");
        Answer listed = grpc(list, set(request(list), "kind", "payroll.GeneratePayslips").build(), school.admin());
        JsonNode listedRest = rest("GET", "/v1/payroll/operations?kind=payroll.GeneratePayslips", school.admin(), null)
                .body();
        assertThat(listed.status()).isEqualTo(200);
        assertThat(listed.body().path("items")).hasSize(2).hasSize(listedRest.path("data").size());
        assertThat(listed.body().path("items").get(0).path("operationId").asString())
                .isEqualTo(listedRest.path("data").get(0).path("operationId").asString());
        assertThat(grpc(list, set(request(list), "kind", "payroll.Nothing").build(), school.admin()).body().path("items"))
                .isEmpty();

        // refused alike: no key, an incomplete run, no permission, an absent operation, a finished one cancelled
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/payroll/payslips:generate", school.admin(),
                run(school, "2026-03-01", "2026-03-31").toString()),
                grpc(generate, call(generate, Map.of(), run(school, "2026-03-01", "2026-03-31")), school.admin()));
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/payroll/payslips:generate", school.admin(),
                "{\"institutionId\":\"" + school.id("institution") + "\"}", "Idempotency-Key", UUID.randomUUID().toString()),
                grpc(generate, call(generate, "request_id", UUID.randomUUID().toString(),
                        json("{\"institutionId\":\"" + school.id("institution") + "\"}")), school.admin()));
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/payroll/payslips:generate", school.nobody(),
                run(school, "2026-03-01", "2026-03-31").toString(), "Idempotency-Key", UUID.randomUUID().toString()),
                grpc(generate, call(generate, "request_id", UUID.randomUUID().toString(),
                        run(school, "2026-03-01", "2026-03-31")), school.nobody()));
        String absent = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/payroll/operations/" + absent, school.admin(), null),
                grpc(get, call(get, "operation_id", absent, null), school.admin()));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/payroll/operations/" + operation, STRANGER, null),
                grpc(get, call(get, "operation_id", operation, null), STRANGER));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/payroll/operations", school.nobody(), null),
                grpc(list, request(list).build(), school.nobody()));
        MethodDescriptor cancel = rpc("CancelOperation");
        Answer cancelled = rest("POST", "/v1/payroll/operations/" + operation + ":cancel", school.admin(), null);
        assertThat(cancelled.status()).as("a finished run is not cancelled").isGreaterThanOrEqualTo(400);
        GrpcParityContract.assertSameAnswer(cancelled,
                grpc(cancel, call(cancel, "operation_id", operation, null), school.admin()));
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/payroll/operations/" + absent + ":cancel", school.admin(),
                null), grpc(cancel, call(cancel, "operation_id", absent, null), school.admin()));
    }

    @Test
    void aPayslipItsLinesAndItsPaymentReadAndMoveAlikeOverBoth() throws Exception {
        School school = payingSchool();
        Answer run = rest("POST", "/v1/payroll/payslips:generate", school.admin(),
                run(school, "2026-01-01", "2026-01-31").toString(), "Idempotency-Key", UUID.randomUUID().toString());
        assertThat(run.status()).as(run.body().toString()).isEqualTo(202);

        MethodDescriptor list = rpc("ListPayslips");
        JsonNode payslips = rest("GET", "/v1/payroll/payslips", school.admin(), null).body().path("data");
        Answer listed = grpc(list, request(list).build(), school.admin());
        assertThat(listed.status()).isEqualTo(200);
        assertThat(listed.body().path("items")).hasSize(1).hasSize(payslips.size());
        GrpcParity.assertSameResource(payslips.get(0), listed.body().path("items").get(0));
        assertThat(payslips.get(0).path("netAmount").asString()).as("30000.00 - 3600.00").isEqualTo("26400.00");
        String id = payslips.get(0).path("id").asString();
        MethodDescriptor get = rpc("GetPayslip");
        GrpcParity.assertSameResource(rest("GET", "/v1/payroll/payslips/" + id, school.admin(), null).body(),
                resource(grpc(get, call(get, "id", id, null), school.admin())));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/payroll/payslips/" + id, school.nobody(), null),
                grpc(get, call(get, "id", id, null), school.nobody()));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/payroll/payslips/" + id, STRANGER, null),
                grpc(get, call(get, "id", id, null), STRANGER));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/payroll/payslips?filter=nope%20%3D%201", school.admin(),
                null), grpc(list, set(request(list), "filter", "nope = 1").build(), school.admin()));

        MethodDescriptor lines = rpc("ListPayslipLines");
        JsonNode linesRest = rest("GET", "/v1/payroll/payslips/" + id + "/lines", school.admin(), null).body().path("data");
        Answer linesGrpc = grpc(lines, call(lines, "payslip_id", id, null), school.admin());
        assertThat(linesGrpc.status()).isEqualTo(200);
        assertThat(linesGrpc.body().path("items")).hasSize(2).hasSize(linesRest.size());
        for (int i = 0; i < linesRest.size(); i++) {
            GrpcParity.assertSameResource(linesRest.get(i), linesGrpc.body().path("items").get(i));
        }
        String line = linesRest.get(0).path("id").asString();
        MethodDescriptor getLine = rpc("GetPayslipLine");
        GrpcParity.assertSameResource(rest("GET", "/v1/payroll/payslips/" + id + "/lines/" + line, school.admin(), null).body(),
                resource(grpc(getLine, call(getLine, Map.of("payslip_id", id, "id", line), null), school.admin())));
        String absent = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/payroll/payslips/" + absent + "/lines/" + line,
                school.admin(), null),
                grpc(getLine, call(getLine, Map.of("payslip_id", absent, "id", line), null), school.admin()));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/payroll/payslips/" + absent + "/lines", school.admin(), null),
                grpc(lines, call(lines, "payslip_id", absent, null), school.admin()));

        // payroll.MarkPayslipPaid: refused alike, paid once over gRPC, and its payment reads the same
        MethodDescriptor pay = rpc("MarkPayslipPaid");
        String path = "/v1/payroll/payslips/" + id + ":markPaid";
        ObjectNode payment = json("{\"disbursementAccountId\":\"" + school.id("account")
                + "\",\"paymentMethod\":\"bank_transfer\",\"transactionDate\":\"2026-02-02\",\"referenceNumber\":\"UTR-1\"}");
        for (ObjectNode refused : List.of(json("{}"), payment.deepCopy().put("etag", "\"99\""),
                payment.deepCopy().put("disbursementAccountId", absent))) {
            Answer overRest = rest("POST", path, school.admin(), refused.toString());
            assertThat(overRest.status()).as(refused.toString()).isGreaterThanOrEqualTo(400);
            GrpcParityContract.assertSameAnswer(overRest, grpc(pay, call(pay, "payslip_id", id, refused), school.admin()));
        }
        GrpcParityContract.assertSameAnswer(rest("POST", path, school.nobody(), payment.toString()),
                grpc(pay, call(pay, "payslip_id", id, payment), school.nobody()));
        JsonNode paid = resource(grpc(pay, call(pay, "payslip_id", id, payment), school.admin()));
        assertThat(paid.path("status").asString()).isEqualTo("STATUS_PAID");
        GrpcParity.assertSameResource(rest("GET", "/v1/payroll/payslips/" + id, school.admin(), null).body(), paid);
        Answer again = rest("POST", path, school.admin(), payment.toString());
        assertThat(again.status()).as("paid once").isEqualTo(409);
        GrpcParityContract.assertSameAnswer(again, grpc(pay, call(pay, "payslip_id", id, payment), school.admin()));
        MethodDescriptor cancel = rpc("CancelPayslip");
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/payroll/payslips/" + id + ":cancel", school.admin(), "{}"),
                grpc(cancel, call(cancel, "payslip_id", id, null), school.admin()));

        MethodDescriptor payments = rpc("ListPayrollPayments");
        JsonNode paymentsRest = rest("GET", "/v1/payroll/payroll-payments", school.admin(), null).body().path("data");
        Answer paymentsGrpc = grpc(payments, request(payments).build(), school.admin());
        assertThat(paymentsGrpc.body().path("items")).hasSize(1).hasSize(paymentsRest.size());
        GrpcParity.assertSameResource(paymentsRest.get(0), paymentsGrpc.body().path("items").get(0));
        assertThat(paymentsRest.get(0).path("amount").asString()).isEqualTo("26400.00");
    }

    /** PayrollPayment has no Update and no Delete, so its three operations are compared here. */
    @Test
    void aPayrollPaymentIsCreatedReadAndListedAlikeOverBoth() throws Exception {
        School school = School.open();
        ObjectNode body = json("{\"institutionId\":\"" + school.id("institution")
                + "\",\"transactionNumber\":\"TXN-1\",\"staffId\":\"" + school.id("teacher")
                + "\",\"transactionDate\":\"2026-01-10\",\"amount\":\"1500.50\",\"paymentMethod\":\"cheque\"}");
        MethodDescriptor create = rpc("CreatePayrollPayment");
        DynamicMessage creation = call(create, Map.of(), json("{}").set("payrollPayment", body));
        JsonNode created = resource(grpc(create, creation, school.admin()));
        assertThat(created.path("paymentMethod").asString()).isEqualTo("PAYMENT_METHOD_CHEQUE");
        String path = "/v1/payroll/payroll-payments";
        String id = created.path("id").asString();
        Answer read = rest("GET", path + "/" + id, school.admin(), null);
        assertThat(read.status()).isEqualTo(200);
        GrpcParity.assertSameResource(read.body(), created);
        MethodDescriptor get = rpc("GetPayrollPayment");
        GrpcParity.assertSameResource(read.body(), resource(grpc(get, call(get, "id", id, null), school.admin())));
        // the same transaction number again, an empty body, no permission, an absent row, another tenant, a bad filter
        GrpcParityContract.assertSameAnswer(rest("POST", path, school.admin(), body.toString()),
                grpc(create, creation, school.admin()));
        GrpcParityContract.assertSameAnswer(rest("POST", path, school.admin(), "{}"),
                grpc(create, request(create).build(), school.admin()));
        GrpcParityContract.assertSameAnswer(rest("POST", path, school.nobody(), body.toString()),
                grpc(create, creation, school.nobody()));
        String absent = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(rest("GET", path + "/" + absent, school.admin(), null),
                grpc(get, call(get, "id", absent, null), school.admin()));
        GrpcParityContract.assertSameAnswer(rest("GET", path + "/" + id, STRANGER, null),
                grpc(get, call(get, "id", id, null), STRANGER));
        MethodDescriptor list = rpc("ListPayrollPayments");
        Answer listed = grpc(list, request(list).build(), school.admin());
        assertThat(listed.body().path("items")).hasSize(1);
        GrpcParity.assertSameResource(rest("GET", path, school.admin(), null).body().path("data").get(0),
                listed.body().path("items").get(0));
        GrpcParityContract.assertSameAnswer(rest("GET", path + "?filter=nope%20%3D%201", school.admin(), null),
                grpc(list, set(request(list), "filter", "nope = 1").build(), school.admin()));
        // a dry run answers without storing
        assertThat(grpc(create, set(call(create, Map.of(), json("{}").set("payrollPayment",
                body.deepCopy().put("transactionNumber", "TXN-2"))).toBuilder(), "validate_only", true).build(),
                school.admin()).status()).isEqualTo(200);
        assertThat(rest("GET", path, school.admin(), null).body().path("data")).hasSize(1);
    }
}
