package com.mysms.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.ByteString;
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
import com.mysms.finance.config.FinanceDatabaseConfig;
import com.mysms.finance.v1.FinanceProto;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
 * Every RPC of finance's thirteen gRPC services against the same operation over REST, on real PostgreSQL: the standard
 * operations, the seeded resources (dues, invoices, lines, payments, refunds), the archive transition and the fee
 * workflow give the same resource and the same refusals over both transports. What needs sis, academics, org, master or
 * the gateway stops at 503 over both; the gateway's webhook is REST's alone (UNIMPLEMENTED over gRPC).
 */
@SpringBootTest(classes = FinanceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=FinanceGrpcParityComponentTest")
class FinanceGrpcParityComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final String ADMIN = token(TENANT, List.of("finance.*"));
    private static final String NOBODY = token(TENANT, List.of());
    private static final String STRANGER = token(OTHER_TENANT, List.of("finance.*"));
    private static final String WEBHOOK_SECRET = "whsec-parity-5678";
    private static final String DUES = "/v1/finance/student-fee-dues";
    private static final String INVOICES = "/v1/finance/invoices";
    private static final String PAYMENTS = "/v1/finance/payments";
    private static ManagedChannel channel;


    @LocalServerPort
    private int port;


    @Autowired
    private ProtoBridge bridge;

    @Autowired
    @Qualifier(FinanceDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "finance");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.finance.razorpay-url", () -> "http://127.0.0.1:9/v1/");
        registry.add("mysms.finance.razorpay-timeout", () -> "PT2S");
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
            return new School(tenant, token(tenant, List.of("finance.*")), token(tenant, List.of()));
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

    // ------------------------------------------------------------------ the two transports

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

    /** A POST of a body under a fresh Idempotency-Key. */
    private Answer postWithKey(String path, String token, JsonNode body) throws Exception {
        return rest("POST", path, token, body.toString(), "Idempotency-Key", UUID.randomUUID().toString());
    }

    private Channel channel() {
        if (channel == null) {
            channel = GrpcParity.channel("FinanceGrpcParityComponentTest");
        }
        return channel;
    }

    private Answer grpc(MethodDescriptor rpc, DynamicMessage request, String token) {
        return GrpcParity.call(channel(), rpc, request, token);
    }

    private static MethodDescriptor rpc(String name) {
        for (var service : FinanceProto.getDescriptor().getServices()) {
            MethodDescriptor method = service.findMethodByName(name);
            if (method != null) {
                return method;
            }
        }
        throw new IllegalArgumentException("no RPC " + name);
    }

    private static DynamicMessage.Builder request(MethodDescriptor rpc) {
        return GrpcParityContract.request(rpc);
    }

    private static DynamicMessage.Builder set(DynamicMessage.Builder builder, String field, Object value) {
        return GrpcParityContract.set(builder, field, value);
    }

    /**
     * The request of an RPC from the REST call it mirrors: the path ids (and request_id) by their proto names, then
     * the REST body, property by property through the service's own bridge (so an enum travels as its proto value).
     */
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

    /** A request carrying only the request_id. */
    private DynamicMessage withKey(MethodDescriptor rpc, String key, JsonNode body) {
        return call(rpc, Map.of("request_id", key), body);
    }

    /** The one message a response carries. */
    private static JsonNode resource(Answer overGrpc) {
        assertThat(overGrpc.status()).as("gRPC answered %s", overGrpc.code()).isEqualTo(200);
        return overGrpc.body().properties().iterator().next().getValue();
    }

    private static ObjectNode json(String text) {
        return (ObjectNode) JSON.readTree(text);
    }

    private static ObjectNode json(Map<String, ?> value) {
        return JSON.valueToTree(value);
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

    private static List<String> ids(JsonNode items) {
        List<String> ids = new ArrayList<>();
        items.forEach(item -> ids.add(item.path("id").asString()));
        return ids;
    }

    private static String snake(String name) {
        return name.replaceAll("(?<!^)([A-Z])", "_$1").toLowerCase(Locale.ROOT);
    }

    private static JsonNode spec() {
        return JSON.readTree(FinanceGrpcParityComponentTest.class.getClassLoader().getResourceAsStream("finance/crud-spec.json"));
    }

    /** The same page over both transports: the same rows in the same order, each the same resource. */
    private static void samePage(JsonNode overRest, Answer overGrpc) {
        assertThat(overGrpc.status()).as("gRPC answered %s", overGrpc.code()).isEqualTo(200);
        JsonNode items = overGrpc.body().path("items");
        assertThat(ids(items)).isEqualTo(ids(overRest.path("data")));
        for (int i = 0; i < items.size(); i++) {
            GrpcParity.assertSameResource(overRest.path("data").get(i), items.get(i));
        }
    }

    // ------------------------------------------------------------------ the standard operations, resource by resource

    private GrpcParityContract contract() {
        return new GrpcParityContract(FinanceProto.getDescriptor(), this::rest, this::channel, bridge,
                new GrpcParityContract.Callers(ADMIN, NOBODY, STRANGER));
    }

    /** The resources serving all five standard operations; the others are verified by their own tests below. */
    @TestFactory
    List<DynamicNode> everyStandardRpcAnswersLikeRest() throws Exception {
        List<DynamicNode> tests = new ArrayList<>();
        for (GrpcParityContract.Resource resource : GrpcParityContract.resources("finance/crud-spec.json")) {
            if (!resource.standardOnly()) {
                continue; // seeded or partly served: theOtherResourcesAnswerLikeRest
            }
            tests.add(DynamicTest.dynamicTest(resource.name(), () -> {
                fixtures(TENANT, resource.fixtures());
                fixtures(OTHER_TENANT, resource.fixtures());
                contract().verify(resource);
            }));
        }
        assertThat(tests).hasSize(10);
        return tests;
    }

    /**
     * The resources crud-spec.json describes with fewer operations — read from a seeded row (invoice lines, dues,
     * payments, refunds) or created over gRPC (invoices) — in a tenant of their own: Get and List answer the same
     * rows and refuse alike (absent, malformed, no permission, another tenant, an invalid filter); Update, where
     * served, refuses a stale or missing etag and changes the row alike; Create, where served, refuses alike and
     * answers a dry run without storing.
     */
    @TestFactory
    List<DynamicNode> theOtherResourcesAnswerLikeRest() {
        List<DynamicNode> tests = new ArrayList<>();
        for (JsonNode resource : spec().path("resources")) {
            if (resource.path("ops").size() < 5) {
                tests.add(DynamicTest.dynamicTest(resource.path("name").asString(), () -> partlyServed(resource)));
            }
        }
        assertThat(tests).hasSize(5);
        return tests;
    }

    private void partlyServed(JsonNode resource) throws Exception {
        String name = resource.path("name").asString();
        List<String> ops = strings(resource.path("ops"));
        School school = School.open();
        fixtures(school.tenant(), school.local(resource.path("fixtures")));
        String collection = school.local(resource.path("collection").asString());
        ObjectNode body = resource.has("create") ? json(school.local(resource.path("create").toString()))
                : JSON.createObjectNode();
        String field = snake(name);
        String id;
        if (resource.has("seed")) {
            fixtures(school.tenant(), school.local(resource.path("seed")));
            id = school.local(resource.path("seedId").asString());
        } else {
            MethodDescriptor create = rpc("Create" + name);
            DynamicMessage creation = call(create, Map.of(),
                    json("{}").set(create.getInputType().findFieldByName(field).getJsonName(), body));
            JsonNode created = resource(grpc(create, creation, school.admin()));
            id = created.path("id").asString();
            GrpcParity.assertSameResource(rest("GET", collection + "/" + id, school.admin(), null).body(), created);
            GrpcParityContract.assertSameAnswer(rest("POST", collection, school.admin(), "{}"),
                    grpc(create, request(create).build(), school.admin()));
            GrpcParityContract.assertSameAnswer(rest("POST", collection, school.nobody(), body.toString()),
                    grpc(create, creation, school.nobody()));
            Answer dryRun = grpc(create, set(creation.toBuilder(), "validate_only", true).build(), school.admin());
            assertThat(dryRun.status()).as("a dry run: %s", dryRun.code()).isEqualTo(200);
            assertThat(rest("GET", collection, school.admin(), null).body().path("data")).as("nothing stored")
                    .hasSize(1);
        }
        Map<String, String> parents = parents(rpc("Get" + name), body);

        MethodDescriptor get = rpc("Get" + name);
        Answer read = rest("GET", collection + "/" + id, school.admin(), null);
        assertThat(read.status()).as(read.body().toString()).isEqualTo(200);
        GrpcParity.assertSameResource(read.body(), resource(grpc(get, addressed(get, parents, id), school.admin())));
        for (String other : List.of(UUID.randomUUID().toString(), "not-a-uuid")) {
            GrpcParityContract.assertSameAnswer(rest("GET", collection + "/" + other, school.admin(), null),
                    grpc(get, addressed(get, parents, other), school.admin()));
        }
        GrpcParityContract.assertSameAnswer(rest("GET", collection + "/" + id, school.nobody(), null),
                grpc(get, addressed(get, parents, id), school.nobody()));
        GrpcParityContract.assertSameAnswer(rest("GET", collection + "/" + id, STRANGER, null),
                grpc(get, addressed(get, parents, id), STRANGER));

        MethodDescriptor list = contract().listRpc(name);
        samePage(rest("GET", collection, school.admin(), null).body(),
                grpc(list, addressed(list, parents, null), school.admin()));
        GrpcParityContract.assertSameAnswer(rest("GET", collection + "?filter=nope%20%3D%201", school.admin(), null),
                grpc(list, set(addressed(list, parents, null).toBuilder(), "filter", "nope = 1").build(), school.admin()));
        GrpcParityContract.assertSameAnswer(rest("GET", collection, school.nobody(), null),
                grpc(list, addressed(list, parents, null), school.nobody()));

        if (ops.contains("Update")) {
            MethodDescriptor update = rpc("Update" + name);
            ObjectNode changes = (ObjectNode) resource.path("update");
            String etag = read.body().path("etag").asString();
            GrpcParityContract.assertSameAnswer(rest("PATCH", collection + "/" + id, school.admin(), changes.toString(),
                    "If-Match", "\"99\""), grpc(update, updating(update, parents, id, "99", changes), school.admin()));
            GrpcParityContract.assertSameAnswer(rest("PATCH", collection + "/" + id, school.admin(), changes.toString()),
                    grpc(update, updating(update, parents, id, "", changes), school.admin()));
            GrpcParityContract.assertSameAnswer(rest("PATCH", collection + "/" + id, school.nobody(), changes.toString(),
                    "If-Match", etag), grpc(update, updating(update, parents, id, etag, changes), school.nobody()));
            JsonNode updated = resource(grpc(update, updating(update, parents, id, etag, changes), school.admin()));
            GrpcParity.assertSameResource(rest("GET", collection + "/" + id, school.admin(), null).body(), updated);
            changes.properties().forEach(change -> assertThat(updated.path(change.getKey()).asString())
                    .as(change.getKey()).isEqualTo(change.getValue().asString()));
        }
    }

    /** The parent ids a nested resource's requests carry, from its create body (an invoice line's invoice_id). */
    private static Map<String, String> parents(MethodDescriptor rpc, JsonNode body) {
        Map<String, String> parents = new HashMap<>();
        for (FieldDescriptor field : rpc.getInputType().getFields()) {
            if (field.getName().endsWith("_id") && body.has(field.getJsonName())) {
                parents.put(field.getName(), body.path(field.getJsonName()).asString());
            }
        }
        return parents;
    }

    /** A request keyed by the parents and the row id (none for a list). */
    private static DynamicMessage addressed(MethodDescriptor rpc, Map<String, String> parents, String id) {
        DynamicMessage.Builder request = request(rpc);
        parents.forEach((field, value) -> set(request, field, value));
        if (id != null) {
            set(request, "id", id);
        }
        return request.build();
    }

    /** An update: the {@code <Resource>Update} message of a REST PATCH body, keyed by its parents, id and etag. */
    private DynamicMessage updating(MethodDescriptor update, Map<String, String> parents, String id, String etag,
                                    ObjectNode changes) {
        DynamicMessage.Builder request = contract().changes(update, changes);
        parents.forEach((field, value) -> set(request, field, value));
        set(request, "id", id);
        return set(request, "etag", etag).build();
    }

    // ------------------------------------------------------------------ the state transitions of crud-spec.json

    /**
     * Every transition crud-spec.json describes (the archive of a published fee structure), on a row of a tenant of
     * its own: REST's required If-Match is the request's etag over gRPC; both transports refuse alike for an absent
     * row, without the permission, with a stale etag and without one; the move made over gRPC reads the same over
     * REST and sets what the specification says; the move repeated is refused alike; and REST makes the same move from
     * the same prepared state.
     */
    @TestFactory
    List<DynamicNode> everyTransitionAnswersLikeRest() {
        List<DynamicNode> tests = new ArrayList<>();
        for (JsonNode resource : spec().path("resources")) {
            for (JsonNode transition : resource.path("transitions")) {
                assertThat(transition.path("ifMatch").asBoolean()).as("the If-Match travels as the etag").isTrue();
                tests.add(DynamicTest.dynamicTest(transition.path("operation").asString(), () -> {
                    School school = School.open();
                    fixtures(school.tenant(), school.local(resource.path("fixtures")));
                    String collection = school.local(resource.path("collection").asString());
                    Answer created = rest("POST", collection, school.admin(),
                            school.local(resource.path("create").toString()));
                    assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
                    verifyTransition(school, collection, created.body().path("id").asString(),
                            resource.path("name").asString(), transition);
                }));
            }
        }
        assertThat(tests).hasSize(1);
        return tests;
    }

    private void verifyTransition(School school, String collection, String id, String name, JsonNode transition)
            throws Exception {
        MethodDescriptor rpc = rpc(transition.path("operation").asString().substring("finance.".length()));
        String idField = rpc.getInputType().getFields().get(0).getName();
        String path = transition.path("path").asString();
        String body = transition.path("body").toString();
        List<String> prepare = strings(transition.path("prepare")).stream().map(sql -> sql.replace("{id}", id)).toList();
        fixtures(school.tenant(), prepare);
        String etag = rest("GET", collection + "/" + id, school.admin(), null).body().path("etag").asString();

        String absent = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(rest("POST", path.replace("{id}", absent), school.admin(), body,
                "If-Match", etag), grpc(rpc, moving(rpc, idField, absent, etag), school.admin()));
        GrpcParityContract.assertSameAnswer(rest("POST", path.replace("{id}", id), school.nobody(), body,
                "If-Match", etag), grpc(rpc, moving(rpc, idField, id, etag), school.nobody()));
        GrpcParityContract.assertSameAnswer(rest("POST", path.replace("{id}", id), school.admin(), body,
                "If-Match", "\"99\""), grpc(rpc, moving(rpc, idField, id, "\"99\""), school.admin()));
        Answer missing = rest("POST", path.replace("{id}", id), school.admin(), body);
        assertThat(missing.status()).isEqualTo(428);
        GrpcParityContract.assertSameAnswer(missing, grpc(rpc, moving(rpc, idField, id, ""), school.admin()));

        JsonNode moved = resource(grpc(rpc, moving(rpc, idField, id, etag), school.admin()));
        JsonNode read = rest("GET", collection + "/" + id, school.admin(), null).body();
        GrpcParity.assertSameResource(read, moved);
        transition.path("sets").properties().forEach(set -> assertThat(read.path(set.getKey()).asString())
                .as(set.getKey()).isEqualTo(set.getValue().asString()));
        String current = read.path("etag").asString();
        Answer again = rest("POST", path.replace("{id}", id), school.admin(), body, "If-Match", current);
        assertThat(again.status()).as("moved once").isEqualTo(409);
        GrpcParityContract.assertSameAnswer(again, grpc(rpc, moving(rpc, idField, id, current), school.admin()));

        fixtures(school.tenant(), prepare);
        Answer overRest = rest("POST", path.replace("{id}", id), school.admin(), body, "If-Match", current);
        assertThat(overRest.status()).as(overRest.body().toString()).isEqualTo(200);
        transition.path("sets").properties().forEach(set -> assertThat(overRest.body().path(set.getKey()).asString())
                .as(set.getKey()).isEqualTo(set.getValue().asString()));
    }

    private DynamicMessage moving(MethodDescriptor rpc, String idField, String id, String etag) {
        return set(request(rpc), idField, id).setField(rpc.getInputType().findFieldByName("etag"), etag).build();
    }

    // ------------------------------------------------------------------ the fee workflow

    /**
     * A school's fee office (FeeWorkflowComponentTest's plan): Tuition (30000.00) and Lab (6000.00); installment 1
     * (40 %, due at the start) shares Tuition 75 % and Lab 25 %, installment 2 (60 %, 90 days later) is all Tuition;
     * the plan takes 5 % off every due. A student's dues from 2026-04-01: Tuition#1 10260.00, Lab#1 3420.00, Tuition#2
     * 20520.00 (net).
     */
    private record Office(FinanceFixtures fx, UUID tenant, UUID institution, UUID year, UUID tuition, UUID lab,
                          UUID plan, String admin, String nobody) {

        /** The student's fee run over REST. */
        void charge(UUID student) throws Exception {
            fx.generate(admin, plan, year, "2026-04-01", student);
        }

        UUID due(UUID student, UUID head, int installment) {
            return fx.due(tenant, student, head, installment);
        }

        long count(String table) {
            return fx.value(tenant, "SELECT COUNT(*) FROM " + table + " WHERE tenant_id = :tenantId", Long.class);
        }
    }

    private Office office() {
        FinanceFixtures fx = new FinanceFixtures(TOKENS, database, port);
        UUID tenant = UUID.randomUUID();
        UUID institution = UUID.randomUUID();
        UUID tuition = fx.feeHead(tenant, institution, "Tuition", "30000.00");
        UUID lab = fx.feeHead(tenant, institution, "Lab", "6000.00");
        UUID plan = fx.plan(tenant, institution, "5.00");
        UUID first = fx.installment(tenant, plan, 1, 0, "40.000");
        UUID second = fx.installment(tenant, plan, 2, 90, "60.000");
        fx.component(tenant, first, tuition, 1, "75.000");
        fx.component(tenant, first, lab, 2, "25.000");
        fx.component(tenant, second, tuition, 1, "100.000");
        return new Office(fx, tenant, institution, UUID.randomUUID(), tuition, lab, plan, fx.admin(tenant),
                fx.token(UUID.randomUUID(), tenant, List.of("*"), List.of()));
    }

    private static ObjectNode run(Office office, UUID... students) {
        return json(Map.of("feePlanId", office.plan().toString(), "academicYearId", office.year().toString(),
                "studentIds", List.of(students).stream().map(UUID::toString).toList(), "startDate", "2026-04-01"));
    }

    @Test
    void aFeeRunReplaysItsRequestIdAndItsOperationsReadAndCancelAlike() throws Exception {
        Office office = office();
        UUID student = UUID.randomUUID();
        MethodDescriptor generate = rpc("GenerateFeeDues");
        String key = UUID.randomUUID().toString();
        JsonNode started = resource(grpc(generate, withKey(generate, key, run(office, student)), office.admin()));
        assertThat(started.path("module").asString()).isEqualTo("finance");
        assertThat(started.path("kind").asString()).isEqualTo("finance.GenerateFeeDues");
        assertThat(started.path("status").asString()).isEqualTo("succeeded");
        assertThat(started.path("result").path("dues").asInt()).isEqualTo(3);
        String operation = started.path("operationId").asString();
        // the same request_id answers the first run, another request under it is refused
        assertThat(resource(grpc(generate, withKey(generate, key, run(office, student)), office.admin()))
                .path("operationId").asString()).isEqualTo(operation);
        Answer reused = grpc(generate, withKey(generate, key, run(office, UUID.randomUUID())), office.admin());
        assertThat(reused.status()).isEqualTo(422);
        assertThat(reused.code()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
        assertThat(office.count("finance.student_fee_due")).as("charged once").isEqualTo(3L);

        // the same run over REST keeps the dues, and both operations read alike
        Answer overRest = postWithKey(DUES + ":generate", office.admin(), run(office, student));
        assertThat(overRest.status()).as(overRest.body().toString()).isEqualTo(202);
        assertThat(overRest.body().path("result").path("kept").asInt()).as("the student's dues are kept").isEqualTo(1);
        assertThat(overRest.body().path("result").path("dues").asInt()).isZero();
        MethodDescriptor get = rpc("GetOperation");
        for (String id : List.of(operation, overRest.body().path("operationId").asString())) {
            GrpcParity.assertSameResource(rest("GET", "/v1/finance/operations/" + id, office.admin(), null).body(),
                    resource(grpc(get, call(get, "operation_id", id, null), office.admin())));
        }
        MethodDescriptor list = rpc("ListOperations");
        Answer listed = grpc(list, set(request(list), "kind", "finance.GenerateFeeDues").build(), office.admin());
        JsonNode listedRest = rest("GET", "/v1/finance/operations?kind=finance.GenerateFeeDues", office.admin(), null)
                .body();
        assertThat(listed.status()).isEqualTo(200);
        assertThat(listed.body().path("items")).hasSize(2).hasSize(listedRest.path("data").size());
        assertThat(listed.body().path("items").get(0).path("operationId").asString())
                .isEqualTo(listedRest.path("data").get(0).path("operationId").asString());
        assertThat(grpc(list, set(request(list), "kind", "finance.Nothing").build(), office.admin()).body()
                .path("items")).isEmpty();

        // refused alike: no key, a run without students, no permission, a class roster sis would read
        GrpcParityContract.assertSameAnswer(rest("POST", DUES + ":generate", office.admin(),
                run(office, student).toString()), grpc(generate, call(generate, Map.of(), run(office, student)),
                office.admin()));
        ObjectNode nobodyCharged = run(office);
        nobodyCharged.remove("studentIds");
        Answer unnamed = postWithKey(DUES + ":generate", office.admin(), nobodyCharged);
        assertThat(unnamed.status()).isEqualTo(422);
        GrpcParityContract.assertSameAnswer(unnamed,
                grpc(generate, withKey(generate, UUID.randomUUID().toString(), nobodyCharged), office.admin()));
        GrpcParityContract.assertSameAnswer(postWithKey(DUES + ":generate", office.nobody(), run(office, student)),
                grpc(generate, withKey(generate, UUID.randomUUID().toString(), run(office, student)), office.nobody()));
        ObjectNode roster = nobodyCharged.deepCopy().put("classSectionId", UUID.randomUUID().toString());
        Answer unavailable = postWithKey(DUES + ":generate", office.admin(), roster);
        assertThat(unavailable.status()).as("sis is not running").isEqualTo(503);
        GrpcParityContract.assertSameAnswer(unavailable,
                grpc(generate, withKey(generate, UUID.randomUUID().toString(), roster), office.admin()));

        String absent = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/finance/operations/" + absent, office.admin(), null),
                grpc(get, call(get, "operation_id", absent, null), office.admin()));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/finance/operations/" + operation, STRANGER, null),
                grpc(get, call(get, "operation_id", operation, null), STRANGER));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/finance/operations", office.nobody(), null),
                grpc(list, request(list).build(), office.nobody()));

        // a finished run is not cancelled, a running one is, alike
        MethodDescriptor cancel = rpc("CancelOperation");
        Answer finished = rest("POST", "/v1/finance/operations/" + operation + ":cancel", office.admin(), null);
        assertThat(finished.status()).isEqualTo(409);
        GrpcParityContract.assertSameAnswer(finished,
                grpc(cancel, call(cancel, "operation_id", operation, null), office.admin()));
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/finance/operations/" + absent + ":cancel", office.admin(),
                null), grpc(cancel, call(cancel, "operation_id", absent, null), office.admin()));
        UUID running = UUID.randomUUID();
        office.fx().sql(office.tenant(), "INSERT INTO finance.operation (id, tenant_id, operation_name, status, "
                + "row_version) VALUES (:id, :tenantId, 'finance.GenerateFeeDues', 'running', 1)", "id", running);
        JsonNode cancelled = resource(grpc(cancel, call(cancel, "operation_id", running.toString(), null),
                office.admin()));
        assertThat(cancelled.path("status").asString()).isEqualTo("cancelled");
        GrpcParity.assertSameResource(rest("GET", "/v1/finance/operations/" + running, office.admin(), null).body(), cancelled);
    }

    @Test
    void invoicesAreRaisedFromDuesPostedAndCancelledAlike() throws Exception {
        Office office = office();
        UUID student = UUID.randomUUID();
        office.charge(student);
        UUID tuition1 = office.due(student, office.tuition(), 1);
        UUID lab1 = office.due(student, office.lab(), 1);
        UUID tuition2 = office.due(student, office.tuition(), 2);
        samePage(rest("GET", DUES, office.admin(), null).body(),
                grpc(rpc("ListStudentFeeDues"), request(rpc("ListStudentFeeDues")).build(), office.admin()));

        MethodDescriptor fromDues = rpc("CreateInvoiceFromDues");
        ObjectNode both = json(Map.of("studentId", student.toString(),
                "dueIds", List.of(tuition1.toString(), lab1.toString()), "billToName", "Asha Rao"));
        JsonNode draft = resource(grpc(fromDues, withKey(fromDues, UUID.randomUUID().toString(), both),
                office.admin()));
        assertThat(draft.path("status").asString()).isEqualTo("STATUS_DRAFT");
        assertThat(draft.path("total").asString()).as("14400.00 less 5 %").isEqualTo("13680.00");
        String id = draft.path("id").asString();
        GrpcParity.assertSameResource(rest("GET", INVOICES + "/" + id, office.admin(), null).body(), draft);
        // refused alike: the dues again, another student's, a name only sis knows, no permission, no key
        Map<ObjectNode, Integer> refusals = Map.of(both, 409,
                json(Map.of("studentId", UUID.randomUUID().toString(), "dueIds", List.of(tuition2.toString()),
                        "billToName", "Asha Rao")), 422,
                json(Map.of("studentId", student.toString(), "dueIds", List.of(tuition2.toString()))), 503);
        for (Map.Entry<ObjectNode, Integer> refusal : refusals.entrySet()) {
            Answer overRest = postWithKey(INVOICES + ":fromDues", office.admin(), refusal.getKey());
            assertThat(overRest.status()).as(refusal.getKey().toString()).isEqualTo(refusal.getValue());
            GrpcParityContract.assertSameAnswer(overRest, grpc(fromDues,
                    withKey(fromDues, UUID.randomUUID().toString(), refusal.getKey()), office.admin()));
        }
        GrpcParityContract.assertSameAnswer(postWithKey(INVOICES + ":fromDues", office.nobody(), both),
                grpc(fromDues, withKey(fromDues, UUID.randomUUID().toString(), both), office.nobody()));
        GrpcParityContract.assertSameAnswer(rest("POST", INVOICES + ":fromDues", office.admin(), both.toString()),
                grpc(fromDues, call(fromDues, Map.of(), both), office.admin()));

        // its lines read alike
        MethodDescriptor lines = rpc("ListInvoiceLines");
        JsonNode linesRest = rest("GET", INVOICES + "/" + id + "/lines", office.admin(), null).body();
        assertThat(linesRest.path("data")).hasSize(2);
        samePage(linesRest, grpc(lines, call(lines, "invoice_id", id, null), office.admin()));
        MethodDescriptor line = rpc("GetInvoiceLine");
        String lineId = linesRest.path("data").get(0).path("id").asString();
        GrpcParity.assertSameResource(rest("GET", INVOICES + "/" + id + "/lines/" + lineId, office.admin(), null).body(),
                resource(grpc(line, call(line, Map.of("invoice_id", id, "id", lineId), null), office.admin())));
        String absent = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(rest("GET", INVOICES + "/" + absent + "/lines/" + lineId, office.admin(),
                null), grpc(line, call(line, Map.of("invoice_id", absent, "id", lineId), null), office.admin()));

        // cancelled over gRPC: the dues are released, and it is posted no more
        MethodDescriptor cancel = rpc("CancelInvoice");
        JsonNode cancelled = resource(grpc(cancel, call(cancel, "invoice_id", id, json("{\"reason\":\"Wrong dues\"}")),
                office.admin()));
        assertThat(cancelled.path("status").asString()).isEqualTo("STATUS_CANCELLED");
        GrpcParity.assertSameResource(rest("GET", INVOICES + "/" + id, office.admin(), null).body(), cancelled);
        assertThat(rest("GET", DUES + "/" + tuition1, office.admin(), null).body().path("status").asString())
                .isEqualTo("draft");
        MethodDescriptor post = rpc("PostInvoice");
        Answer notDraft = rest("POST", INVOICES + "/" + id + ":post", office.admin(), "{}");
        assertThat(notDraft.status()).isEqualTo(409);
        GrpcParityContract.assertSameAnswer(notDraft, grpc(post, call(post, "invoice_id", id, null), office.admin()));

        // posted over gRPC: numbered, then neither posted, cancelled nor changed again
        String again = postWithKey(INVOICES + ":fromDues", office.admin(), both).body().path("id").asString();
        ObjectNode stale = json("{\"etag\":\"\\\"9\\\"\"}");
        GrpcParityContract.assertSameAnswer(rest("POST", INVOICES + "/" + again + ":post", office.admin(),
                stale.toString()), grpc(post, call(post, "invoice_id", again, stale), office.admin()));
        GrpcParityContract.assertSameAnswer(rest("POST", INVOICES + "/" + again + ":post", office.nobody(), "{}"),
                grpc(post, call(post, "invoice_id", again, null), office.nobody()));
        JsonNode posted = resource(grpc(post, call(post, "invoice_id", again, null), office.admin()));
        assertThat(posted.path("status").asString()).isEqualTo("STATUS_POSTED");
        assertThat(posted.path("invoiceNumber").asString()).isEqualTo("INV-000001");
        JsonNode read = rest("GET", INVOICES + "/" + again, office.admin(), null).body();
        GrpcParity.assertSameResource(read, posted);
        for (MethodDescriptor move : List.of(post, cancel)) {
            String verb = move == post ? ":post" : ":cancel";
            Answer overRest = rest("POST", INVOICES + "/" + again + verb, office.admin(), "{}");
            assertThat(overRest.status()).as(verb).isEqualTo(409);
            GrpcParityContract.assertSameAnswer(overRest, grpc(move, call(move, "invoice_id", again, null),
                    office.admin()));
        }
        GrpcParityContract.assertSameAnswer(rest("POST", INVOICES + "/" + absent + ":cancel", office.admin(), "{}"),
                grpc(cancel, call(cancel, "invoice_id", absent, null), office.admin()));
        MethodDescriptor update = rpc("UpdateInvoice");
        ObjectNode rename = json("{\"billToName\":\"Other\"}");
        String etag = read.path("etag").asString();
        Answer locked = rest("PATCH", INVOICES + "/" + again, office.admin(), rename.toString(), "If-Match", etag);
        assertThat(locked.status()).isEqualTo(409);
        GrpcParityContract.assertSameAnswer(locked, grpc(update, updating(update, Map.of(), again, etag, rename),
                office.admin()));
    }

    /** A posted invoice of the student's installment-1 dues (13680.00), raised over REST. */
    private String postedInvoice(Office office, UUID student) throws Exception {
        ObjectNode dues = json(Map.of("studentId", student.toString(), "dueIds", List.of(
                office.due(student, office.tuition(), 1).toString(), office.due(student, office.lab(), 1).toString()),
                "billToName", "Asha Rao"));
        String id = postWithKey(INVOICES + ":fromDues", office.admin(), dues).body().path("id").asString();
        assertThat(rest("POST", INVOICES + "/" + id + ":post", office.admin(), "{}").status()).isEqualTo(200);
        return id;
    }

    /** A cash payment at the counter of the amount, sliced between the invoice and the due. */
    private static ObjectNode counter(String amount, String invoice, String toInvoice, String due, String toDue) {
        return json(Map.of("payerName", "Parent", "billingName", "Asha Rao", "amount", amount,
                "currencyId", FinanceFixtures.INR.toString(), "paymentMethod", "cash", "allocations", List.of(
                        Map.of("invoiceId", invoice, "amount", toInvoice), Map.of("studentFeeDueId", due, "amount", toDue))));
    }

    @Test
    void paymentsRefundsTheStatementAndRemindersAnswerAlike() throws Exception {
        Office office = office();
        UUID student = UUID.randomUUID();
        office.charge(student);
        String invoice = postedInvoice(office, student);
        String tuition2 = office.due(student, office.tuition(), 2).toString();

        // a counter payment: refused alike, recorded once over gRPC under its request_id
        MethodDescriptor record = rpc("RecordPayment");
        ObjectNode unbalanced = counter("15000.00", invoice, "10000.00", tuition2, "4000.00");
        Answer refused = postWithKey(PAYMENTS + ":record", office.admin(), unbalanced);
        assertThat(refused.status()).isEqualTo(422);
        GrpcParityContract.assertSameAnswer(refused,
                grpc(record, withKey(record, UUID.randomUUID().toString(), unbalanced), office.admin()));
        ObjectNode payment = counter("15000.00", invoice, "10000.00", tuition2, "5000.00");
        GrpcParityContract.assertSameAnswer(rest("POST", PAYMENTS + ":record", office.admin(), payment.toString()),
                grpc(record, call(record, Map.of(), payment), office.admin()));
        GrpcParityContract.assertSameAnswer(postWithKey(PAYMENTS + ":record", office.nobody(), payment),
                grpc(record, withKey(record, UUID.randomUUID().toString(), payment), office.nobody()));
        String key = UUID.randomUUID().toString();
        JsonNode paid = resource(grpc(record, withKey(record, key, payment), office.admin()));
        assertThat(paid.path("status").asString()).isEqualTo("STATUS_RECEIVED");
        assertThat(paid.path("allocations").findValuesAsString("amount")).containsExactlyInAnyOrder("10000.00",
                "5000.00");
        String id = paid.path("id").asString();
        assertThat(resource(grpc(record, withKey(record, key, payment), office.admin())).path("id").asString())
                .as("replayed").isEqualTo(id);
        assertThat(office.count("finance.payment")).isEqualTo(1L);
        MethodDescriptor get = rpc("GetPayment");
        GrpcParity.assertSameResource(rest("GET", PAYMENTS + "/" + id, office.admin(), null).body(),
                resource(grpc(get, call(get, "id", id, null), office.admin())));
        GrpcParityContract.assertSameAnswer(rest("GET", PAYMENTS + "/" + id, STRANGER, null),
                grpc(get, call(get, "id", id, null), STRANGER));
        samePage(rest("GET", PAYMENTS, office.admin(), null).body(),
                grpc(rpc("ListPayments"), request(rpc("ListPayments")).build(), office.admin()));

        // the statement adds up alike
        MethodDescriptor statement = rpc("GetFeeStatement");
        JsonNode statementRest = rest("GET", "/v1/finance/students/" + student + "/fee-statement", office.admin(), null)
                .body();
        JsonNode statementGrpc = resource(grpc(statement, call(statement, "student_id", student.toString(), null),
                office.admin()));
        assertThat(statementRest.path("totalPaid").asString()).isEqualTo("15000.00");
        GrpcParity.assertSameResource(statementRest, statementGrpc);
        String year = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/finance/students/" + student + "/fee-statement"
                + "?academicYearId=" + year, office.admin(), null), grpc(statement, call(statement,
                Map.of("student_id", student.toString(), "academic_year_id", year), null), office.admin()));
        GrpcParity.assertSameResource(rest("GET", "/v1/finance/students/" + student + "/fee-statement?academicYearId="
                + office.year(), office.admin(), null).body(), resource(grpc(statement, call(statement,
                Map.of("student_id", student.toString(), "academic_year_id", office.year().toString()), null),
                office.admin())));

        // a cash refund over gRPC, replayed under its request_id; refused alike beyond what is left
        MethodDescriptor refund = rpc("RefundPayment");
        ObjectNode back = json(Map.of("amount", "6000.00", "refundMethod", "cash", "reason", "Left"));
        String refundKey = UUID.randomUUID().toString();
        JsonNode first = resource(grpc(refund, call(refund, Map.of("payment_id", id, "request_id", refundKey), back),
                office.admin()));
        assertThat(first.path("status").asString()).isEqualTo("processed");
        assertThat(resource(grpc(refund, call(refund, Map.of("payment_id", id, "request_id", refundKey), back),
                office.admin())).path("id").asString()).isEqualTo(first.path("id").asString());
        assertThat(office.count("finance.refund")).isEqualTo(1L);
        ObjectNode over = json(Map.of("amount", "9000.01", "refundMethod", "cash"));
        String refundPath = PAYMENTS + "/" + id + ":refund";
        Answer overRefund = postWithKey(refundPath, office.admin(), over);
        assertThat(overRefund.status()).isEqualTo(422);
        assertThat(overRefund.body().toString()).contains("OVER_REFUND");
        GrpcParityContract.assertSameAnswer(overRefund, grpc(refund, call(refund, Map.of("payment_id", id,
                "request_id", UUID.randomUUID().toString()), over), office.admin()));
        String absent = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(postWithKey(PAYMENTS + "/" + absent + ":refund", office.admin(), back),
                grpc(refund, call(refund, Map.of("payment_id", absent, "request_id", UUID.randomUUID().toString()),
                        back), office.admin()));
        GrpcParityContract.assertSameAnswer(postWithKey(refundPath, office.nobody(), back), grpc(refund, call(refund,
                Map.of("payment_id", id, "request_id", UUID.randomUUID().toString()), back), office.nobody()));
        GrpcParityContract.assertSameAnswer(rest("POST", refundPath, office.admin(), back.toString()),
                grpc(refund, call(refund, "payment_id", id, back), office.admin()));

        // the refunds read alike
        String filter = "paymentId = \"" + id + "\"";
        MethodDescriptor refunds = rpc("ListRefunds");
        samePage(rest("GET", "/v1/finance/refunds?filter=" + URLEncoder.encode(filter, StandardCharsets.UTF_8),
                office.admin(), null).body(), grpc(refunds, set(request(refunds), "filter", filter).build(),
                office.admin()));
        MethodDescriptor getRefund = rpc("GetRefund");
        String refundId = first.path("id").asString();
        GrpcParity.assertSameResource(rest("GET", "/v1/finance/refunds/" + refundId, office.admin(), null).body(),
                resource(grpc(getRefund, call(getRefund, "id", refundId, null), office.admin())));

        // reminders for the open invoice, queued alike; another school's office may not send them
        MethodDescriptor remind = rpc("SendFeeReminders");
        ObjectNode reminders = json(Map.of("institutionId", office.institution().toString(),
                "invoiceIds", List.of(invoice, absent), "channel", "sms"));
        JsonNode sent = resource(grpc(remind, withKey(remind, UUID.randomUUID().toString(), reminders),
                office.admin()));
        assertThat(sent.path("queued").asInt()).isEqualTo(1);
        assertThat(sent.path("skipped").asInt()).isEqualTo(1);
        JsonNode sentRest = postWithKey(INVOICES + ":sendReminders", office.admin(), reminders).body();
        assertThat(sentRest.path("queued").asInt()).isEqualTo(1);
        assertThat(sentRest.path("skipped").asInt()).isEqualTo(1);
        String elsewhere = office.fx().office(office.tenant(), UUID.randomUUID());
        GrpcParityContract.assertSameAnswer(postWithKey(INVOICES + ":sendReminders", elsewhere, reminders),
                grpc(remind, withKey(remind, UUID.randomUUID().toString(), reminders), elsewhere));
    }

    // ------------------------------------------------------------------ money through the gateway

    @Test
    void aCheckoutWithoutTheGatewayOrMasterIsRefusedAlikeAndTheWebhookIsRestsAlone() throws Exception {
        Office office = office();
        UUID student = UUID.randomUUID();
        office.charge(student);
        String invoice = postedInvoice(office, student);
        String account = gatewayAccount(office);

        MethodDescriptor quote = rpc("QuoteCheckout");
        ObjectNode quoted = json(Map.of("purpose", "invoice", "targetId", invoice));
        Answer quoteRest = rest("POST", "/v1/finance/checkout:quote", office.admin(), quoted.toString());
        assertThat(quoteRest.status()).as("master is not running").isEqualTo(503);
        GrpcParityContract.assertSameAnswer(quoteRest, grpc(quote, call(quote, Map.of(), quoted), office.admin()));
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/finance/checkout:quote", office.nobody(),
                quoted.toString()), grpc(quote, call(quote, Map.of(), quoted), office.nobody()));

        MethodDescriptor start = rpc("StartCheckout");
        ObjectNode checkout = json(Map.of("purpose", "invoice", "targetId", invoice, "billingName", "Asha Rao",
                "payerConsent", true));
        List<ObjectNode> refused = List.of(checkout, checkout.deepCopy().put("payerConsent", false),
                checkout.deepCopy().put("purpose", "lms_course"),
                checkout.deepCopy().put("targetId", UUID.randomUUID().toString()));
        for (ObjectNode body : refused) {
            Answer overRest = postWithKey("/v1/finance/checkout:start", office.admin(), body);
            assertThat(overRest.status()).as(body.toString()).isGreaterThanOrEqualTo(400);
            GrpcParityContract.assertSameAnswer(overRest,
                    grpc(start, withKey(start, UUID.randomUUID().toString(), body), office.admin()));
        }
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/finance/checkout:start", office.admin(),
                checkout.toString()), grpc(start, call(start, Map.of(), checkout), office.admin()));
        assertThat(office.count("finance.payment")).as("a checkout that stops writes nothing").isZero();

        // the gateway's notification is signed over its raw HTTP body: REST's alone
        MethodDescriptor webhook = rpc("ReceiveGatewayWebhook");
        DynamicMessage notification = set(request(webhook), "gateway", "razorpay")
                .setField(webhook.getInputType().findFieldByName("raw_body"),
                        ByteString.copyFromUtf8("{\"event\":\"payment.captured\"}")).build();
        for (String caller : Arrays.asList(null, office.admin())) {
            Answer overGrpc = grpc(webhook, notification, caller);
            assertThat(overGrpc.status()).isEqualTo(501);
            assertThat(overGrpc.code()).isEqualTo("NOT_IMPLEMENTED");
        }

        // a gateway refund the gateway cannot be reached for fails at once; retried under its request_id over gRPC
        // (the 503 is not kept by the idempotency store, so the retry runs) it answers that refund, never a new one
        String payment = capturedPayment(office, invoice, account);
        MethodDescriptor refund = rpc("RefundPayment");
        ObjectNode partial = json(Map.of("amount", "400.00", "reason", "Withdrawn"));
        String key = UUID.randomUUID().toString();
        Answer unanswered = grpc(refund, call(refund, Map.of("payment_id", payment, "request_id", key), partial),
                office.admin());
        GrpcParityContract.assertSameAnswer(postWithKey(PAYMENTS + "/" + payment + ":refund", office.admin(), partial),
                unanswered);
        assertThat(unanswered.status()).as("the gateway is not reachable").isEqualTo(503);
        String opened = office.fx().value(office.tenant(), "SELECT id::text FROM finance.refund WHERE tenant_id = "
                + ":tenantId AND request_key = :key", String.class, "key", UUID.fromString(key));
        JsonNode retried = resource(grpc(refund, call(refund, Map.of("payment_id", payment, "request_id", key),
                partial), office.admin()));
        assertThat(retried.path("id").asString()).isEqualTo(opened);
        assertThat(retried.path("status").asString()).isEqualTo("failed");
        assertThat(office.count("finance.refund")).as("one refund per key, REST's and gRPC's").isEqualTo(2L);
    }

    /** The school's Razorpay account, over REST. */
    private String gatewayAccount(Office office) throws Exception {
        Answer created = postWithKey("/v1/finance/payment-gateway-accounts", office.admin(), json(Map.of(
                "institutionId", office.institution().toString(), "gatewayName", "razorpay",
                "displayName", "Main account", "keyId", "rzp_test_key", "keySecret", "key-secret-1234",
                "webhookSecret", WEBHOOK_SECRET, "isDefault", true)));
        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
        return created.body().path("id").asString();
    }

    /**
     * A checkout's payment of the whole posted invoice (13680.00), captured: written as StartCheckout leaves it, then
     * received through the gateway's signed notification over REST.
     */
    private String capturedPayment(Office office, String invoice, String account) throws Exception {
        UUID id = UUID.randomUUID();
        office.fx().sql(office.tenant(), "INSERT INTO finance.payment (id, tenant_id, institution_id, payer_user_id, "
                + "purpose, invoice_id, gateway_name, gateway_order_id, original_amount, amount, amount_with_tax, "
                + "currency_id, billing_name, payer_consent, status, payment_method, gateway_account_id, "
                + "gateway_currency, gateway_amount_minor, row_version) VALUES (:id, :tenantId, :institution, :payer, "
                + "'invoice', :invoice, 'razorpay', 'order_P1', 13680.00, 13680.00, 13680.00, :currency, 'Asha Rao', "
                + "true, 'initiated', 'gateway', :account, 'INR', 1368000, 1)", "id", id, "institution",
                office.institution(), "payer", UUID.randomUUID(), "invoice", UUID.fromString(invoice), "currency",
                FinanceFixtures.INR, "account", UUID.fromString(account));
        String capture = FinanceFixtures.json(Map.of("event", "payment.captured", "payload", Map.of("payment",
                Map.of("entity", Map.of("id", "pay_P1", "order_id", "order_P1", "amount", 1368000L, "currency", "INR",
                        "status", "captured", "method", "upi", "notes", Map.of("mysms_tenant",
                                office.tenant().toString(), "mysms_payment", id.toString()))))));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal(capture.getBytes(StandardCharsets.UTF_8)));
        Answer received = rest("POST", "/v1/finance/payment-gateways/razorpay:webhook", null, capture,
                "X-Razorpay-Signature", signature, "X-Razorpay-Event-Id", "evt_P1");
        assertThat(received.status()).isEqualTo(204);
        return id.toString();
    }

    // ------------------------------------------------------------------ publishing a fee structure

    @Test
    void aFeeStructureIsPublishedNowhereWithoutItsRostersAndRefusedAlike() throws Exception {
        Office office = office();
        String collection = "/v1/finance/fee-structures";
        ObjectNode structure = json(Map.of("institutionId", office.institution().toString(),
                "academicYearId", office.year().toString(), "feePlanId", office.plan().toString(),
                "name", "Grade 1 fees"));
        String id = postWithKey(collection, office.admin(), structure).body().path("id").asString();
        MethodDescriptor publish = rpc("PublishFeeStructure");
        String path = collection + "/" + id + ":publish";
        Answer unavailable = rest("POST", path, office.admin(), null);
        assertThat(unavailable.status()).as("its rosters are sis' and academics', which are not running")
                .isEqualTo(503);
        GrpcParityContract.assertSameAnswer(unavailable,
                grpc(publish, call(publish, "fee_structure_id", id, null), office.admin()));
        GrpcParityContract.assertSameAnswer(rest("POST", path, office.nobody(), null),
                grpc(publish, call(publish, "fee_structure_id", id, null), office.nobody()));
        String absent = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(rest("POST", collection + "/" + absent + ":publish", office.admin(), null),
                grpc(publish, call(publish, "fee_structure_id", absent, null), office.admin()));
        office.fx().sql(office.tenant(), "UPDATE finance.fee_structure SET status = 'archived' WHERE id = :id",
                "id", UUID.fromString(id));
        Answer notDraft = rest("POST", path, office.admin(), null);
        assertThat(notDraft.status()).isEqualTo(409);
        GrpcParityContract.assertSameAnswer(notDraft,
                grpc(publish, call(publish, "fee_structure_id", id, null), office.admin()));
        assertThat(office.count("finance.student_fee_due")).as("nothing charged").isZero();
    }

    /** A charge raised over either transport makes the same one-line draft, once per source, and is refused alike. */
    @Test
    void aChargeInvoiceIsRaisedAndRefusedLikeRest() throws Exception {
        MethodDescriptor raise = rpc("RaiseChargeInvoice");
        String institution = "0196f000-0000-7000-8000-00000000a001";
        String currency = UUID.randomUUID().toString();
        String byRestSource = UUID.randomUUID().toString();
        String byGrpcSource = UUID.randomUUID().toString();
        String borrower = UUID.randomUUID().toString();
        fixtures(TENANT, List.of("INSERT INTO finance.staff_record (tenant_id, staff_id, user_id, institution_id,"
                + " source_version, synced_at) VALUES (:tenant, '" + UUID.randomUUID() + "', '" + borrower + "', '"
                + institution + "', 1, now())"));
        java.util.function.Function<String, String> charge = source -> "{\"institutionId\":\"" + institution
                + "\",\"billToUserId\":\"" + borrower + "\",\"billToName\":\"Parity borrower\","
                + "\"description\":\"Library fine\",\"amount\":\"12.50\",\"currencyId\":\"" + currency
                + "\",\"sourceType\":\"library.loan\",\"sourceId\":\"" + source + "\"}";
        Answer byRest = rest("POST", "/v1/finance/invoices:raiseCharge", ADMIN, charge.apply(byRestSource));
        DynamicMessage.Builder request = request(raise);
        com.google.protobuf.util.JsonFormat.parser().merge(charge.apply(byGrpcSource), request);
        Answer byGrpc = grpc(raise, request.build(), ADMIN);
        assertThat(byRest.status()).as(byRest.body().toString()).isEqualTo(200);
        assertThat(byGrpc.status()).as("RaiseChargeInvoice: " + byGrpc.code()).isEqualTo(200);
        assertThat(byGrpc.body().path("invoice").path("total").asString()).isEqualTo(byRest.body().path("total")
                .asString()).isEqualTo("12.50");
        GrpcParity.assertSameResource(rest("GET", "/v1/finance/invoices/" + byGrpc.body().path("invoice").path("id")
                .asString(), ADMIN, null).body(), byGrpc.body().path("invoice"));
        assertThat(grpc(raise, request.build(), ADMIN).body().path("invoice").path("id").asString())
                .as("one invoice per source").isEqualTo(byGrpc.body().path("invoice").path("id").asString());

        DynamicMessage.Builder free = request(raise);
        com.google.protobuf.util.JsonFormat.parser().merge(charge.apply(byGrpcSource).replace("12.50", "0"), free);
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/finance/invoices:raiseCharge", ADMIN,
                charge.apply(byRestSource).replace("12.50", "0")), grpc(raise, free.build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/finance/invoices:raiseCharge", NOBODY,
                charge.apply(byRestSource)), grpc(raise, request.build(), NOBODY));
        assertThat(grpc(raise, request.build(), null).status()).as("authentication first, as on REST").isEqualTo(401);
        DynamicMessage.Builder stranger = request(raise);
        com.google.protobuf.util.JsonFormat.parser().merge(charge.apply(UUID.randomUUID().toString())
                .replace(borrower, UUID.randomUUID().toString()), stranger);
        Answer refused = rest("POST", "/v1/finance/invoices:raiseCharge", ADMIN, charge.apply(byRestSource)
                .replace(borrower, UUID.randomUUID().toString()));
        assertThat(refused.status()).as("an account that is not the school's").isEqualTo(422);
        GrpcParityContract.assertSameAnswer(refused, grpc(raise, stranger.build(), ADMIN));
        DynamicMessage.Builder more = request(raise);
        com.google.protobuf.util.JsonFormat.parser().merge(charge.apply(byGrpcSource).replace("12.50", "13.00"), more);
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/finance/invoices:raiseCharge", ADMIN,
                charge.apply(byRestSource).replace("12.50", "13.00")), grpc(raise, more.build(), ADMIN));
    }
}
