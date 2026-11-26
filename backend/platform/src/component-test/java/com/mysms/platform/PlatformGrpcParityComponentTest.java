package com.mysms.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.testkit.GrpcParity;
import com.mysms.core.testkit.GrpcParity.Answer;
import com.mysms.core.testkit.GrpcParityContract;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.platform.PlatformCalls.Reply;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.v1.PlatformProto;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
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
 * Every RPC of platform's seventeen gRPC services against the same operation over REST, on real PostgreSQL: the
 * standard operations, the resources without a full set, the transitions and the workflow operations give the same
 * resource and the same refusals over both transports; a create replays under its request id. Import runs are compared
 * in PlatformImportsComponentTest, which has a file store. The gateway and the stock-photo service point at a closed
 * port (503 UNAVAILABLE).
 */
@SpringBootTest(classes = PlatformApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=PlatformGrpcParityComponentTest")
class PlatformGrpcParityComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String KEY_SECRET = "platform-key-secret";
    private static final String CLOSED = "http://127.0.0.1:9";
    private static final String STALE = "\"99\"";
    private static final UUID TENANT = UUID.fromString("0190b0a0-0000-7000-8000-0000000f3001");
    private static final UUID OTHER_TENANT = UUID.fromString("0190b0a0-0000-7000-8000-0000000f3002");
    private static final UUID SCHOOL = UUID.fromString("0190b0a0-0000-7000-8000-0000000f3003");
    private static final String ADMIN = token(UUID.randomUUID(), TENANT, List.of("*"), List.of("*"));
    private static final String NOBODY = token(UUID.randomUUID(), TENANT, List.of(), List.of("*"));
    private static final String STRANGER = token(UUID.randomUUID(), OTHER_TENANT, List.of("*"), List.of("*"));
    /** A school-level user holding every permission in one school only (no tenant-wide grant). */
    private static final String SCHOOL_USER = token(UUID.randomUUID(), TENANT, List.of("*"), List.of(SCHOOL.toString()));
    private static final String STAFF = TOKENS.platformOperator(UUID.randomUUID(), "*");
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;


    @Autowired
    private ProtoBridge bridge;

    @Autowired
    @Qualifier(PlatformDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    private PlatformCalls http;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "platform", "master", "org");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.platform.razorpay.url", () -> CLOSED);
        registry.add("mysms.platform.razorpay.key-id", () -> "rzp_test_platform");
        registry.add("mysms.platform.razorpay.key-secret", () -> KEY_SECRET);
        registry.add("mysms.platform.razorpay.webhook-secret", () -> "platform-webhook-secret");
        registry.add("mysms.platform.stock-images.url", () -> CLOSED);
        registry.add("mysms.platform.stock-images.api-key", () -> "pexels-test-key");
    }

    @AfterAll
    static void closeChannel() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    @BeforeEach
    void tenants() {
        http = new PlatformCalls(TOKENS, () -> port, database);
        http.tenant(TENANT, "grpc-a");
        http.tenant(OTHER_TENANT, "grpc-b");
    }

    /** A user of the tenant holding the permissions in the listed institutions ({@code *}: tenant-wide). */
    private static String token(UUID user, UUID tenant, List<String> permissions, List<String> institutions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", permissions);
        claims.put("tperms", institutions.contains("*") ? permissions : List.of());
        claims.put("ins", institutions);
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    // ------------------------------------------------------------------ the two transports

    private Answer rest(String method, String path, String token, String body, String... headers) {
        Reply reply = http.call(method, path, token, body, headers);
        return new Answer(reply.status(), reply.status() >= 400 ? reply.body().path("code").asString() : null,
                reply.body());
    }

    private Channel channel() {
        if (channel == null) {
            channel = GrpcParity.channel("PlatformGrpcParityComponentTest");
        }
        return channel;
    }

    private Answer grpc(MethodDescriptor rpc, ObjectNode request, String token) throws Exception {
        return GrpcParity.call(channel(), rpc, request(rpc, request), token);
    }

    private MethodDescriptor rpc(String name) {
        return contract(ADMIN, STRANGER).rpc(name);
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
        statements.forEach(statement -> http.sql(tenant, statement, Map.of("tenant", tenant)));
    }

    private static List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.asString()));
        return values;
    }

    private static JsonNode spec() throws Exception {
        return JSON.readTree(PlatformGrpcParityComponentTest.class.getClassLoader()
                .getResourceAsStream("platform/crud-spec.json"));
    }

    private static List<String> ids(JsonNode items, String field) {
        List<String> ids = new ArrayList<>();
        items.forEach(item -> ids.add(item.path(field).asString()));
        return ids;
    }

    /** The JSON name of the one field a response carries. */
    private static String resultField(MethodDescriptor rpc) {
        return rpc.getOutputType().getFields().get(0).getJsonName();
    }

    /** A request carrying only its path id ({@code id} of the standard operations, {@code <resource>_id} else). */
    private static ObjectNode keyed(MethodDescriptor rpc, String id) {
        for (FieldDescriptor field : rpc.getInputType().getFields()) {
            if ("id".equals(field.getName()) || field.getName().endsWith("_id") && !"request_id".equals(field.getName())) {
                return object().put(field.getJsonName(), id);
            }
        }
        throw new IllegalArgumentException(rpc.getName() + " has no path id");
    }

    // ------------------------------------------------------------------ the standard operations, resource by resource

    private GrpcParityContract contract(String admin, String stranger) {
        return new GrpcParityContract(PlatformProto.getDescriptor(), this::rest, this::channel, bridge,
                new GrpcParityContract.Callers(admin, NOBODY, stranger));
    }

    /**
     * The contract of a resource: global data is written by platform staff and read alike by every tenant (no
     * stranger); a tenant's rows by its administrator, and another tenant's administrator finds none.
     */
    private GrpcParityContract contract(GrpcParityContract.Resource resource) {
        return "global".equals(resource.scope()) ? contract(STAFF, null) : contract(ADMIN, STRANGER);
    }

    @TestFactory
    List<DynamicNode> everyStandardRpcAnswersLikeRest() throws Exception {
        List<DynamicNode> tests = new ArrayList<>();
        for (GrpcParityContract.Resource resource : GrpcParityContract.resources("platform/crud-spec.json")) {
            if (!resource.standardOnly()) {
                continue; // theResourcesWithoutAFullSetAnswerLikeRest
            }
            tests.add(DynamicTest.dynamicTest(resource.name(), () -> {
                sql(TENANT, resource.fixtures());
                contract(resource).verify(resource);
            }));
        }
        assertThat(tests).hasSize(10);
        return tests;
    }

    /** The resources of crud-spec.json without the full five: read from a seeded row, or created and deleted. */
    @TestFactory
    List<DynamicNode> theResourcesWithoutAFullSetAnswerLikeRest() throws Exception {
        List<DynamicNode> tests = new ArrayList<>();
        for (JsonNode resource : spec().path("resources")) {
            List<String> ops = strings(resource.path("ops"));
            if (ops.size() == 5) {
                continue;
            }
            String name = resource.path("name").asString();
            tests.add(DynamicTest.dynamicTest(name, () -> {
                sql(TENANT, strings(resource.path("fixtures")));
                if (ops.contains("Create")) {
                    createdAndDeleted(resource);
                } else {
                    sql(TENANT, strings(resource.path("seed")));
                    seeded(name, resource.path("collection").asString(), resource.path("seedId").asString(), "");
                }
            }));
        }
        assertThat(tests).hasSize(4);
        return tests;
    }

    /** Get and List of a seeded row, with their refusals; {@code filter} narrows a list that needs one. */
    private void seeded(String name, String collection, String id, String filter) throws Exception {
        MethodDescriptor get = rpc("Get" + name);
        Answer read = rest("GET", collection + "/" + id, ADMIN, null);
        assertThat(read.status()).as(read.body().toString()).isEqualTo(200);
        Answer got = grpc(get, keyed(get, id), ADMIN);
        assertThat(got.status()).as("%s get: %s", name, got.code()).isEqualTo(200);
        GrpcParity.assertSameResource(read.body(), got.body().path(resultField(get)));
        for (String absent : List.of(UUID.randomUUID().toString(), "not-a-uuid")) {
            GrpcParityContract.assertSameAnswer(rest("GET", collection + "/" + absent, ADMIN, null),
                    grpc(get, keyed(get, absent), ADMIN));
        }
        for (String refused : List.of(NOBODY, STRANGER)) {
            GrpcParityContract.assertSameAnswer(rest("GET", collection + "/" + id, refused, null),
                    grpc(get, keyed(get, id), refused));
        }

        MethodDescriptor list = contract(ADMIN, STRANGER).listRpc(name);
        Answer listed = rest("GET", collection + "?filter=" + PlatformCalls.encode(filter), ADMIN, null);
        assertThat(listed.status()).as(listed.body().toString()).isEqualTo(200);
        Answer listedGrpc = grpc(list, object().put("filter", filter), ADMIN);
        assertThat(listedGrpc.status()).as("%s list: %s", name, listedGrpc.code()).isEqualTo(200);
        assertThat(ids(listedGrpc.body().path("items"), "id")).contains(id)
                .isEqualTo(ids(listed.body().path("data"), "id"));
        GrpcParity.assertSameResource(listed.body().path("data").get(0), listedGrpc.body().path("items").get(0));
        GrpcParityContract.assertSameAnswer(rest("GET", collection + "?filter=nope%20%3D%201", ADMIN, null),
                grpc(list, object().put("filter", "nope = 1"), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", collection, NOBODY, null), grpc(list, object(), NOBODY));
    }

    /** A record created and deleted but never read or changed alone: Create, List and Delete with their refusals. */
    private void createdAndDeleted(JsonNode resource) throws Exception {
        String name = resource.path("name").asString();
        String collection = resource.path("collection").asString();
        ObjectNode body = (ObjectNode) resource.path("create");
        MethodDescriptor create = rpc("Create" + name);
        MethodDescriptor list = contract(ADMIN, STRANGER).listRpc(name);
        MethodDescriptor delete = rpc("Delete" + name);
        String field = resultField(create);
        ObjectNode creation = (ObjectNode) GrpcParity.json(bridge.reply(GrpcParityContract.request(create),
                create.getInputType().getFields().get(0).getName(), JSON.convertValue(body, Map.class)).build());

        Answer created = grpc(create, creation, ADMIN);
        assertThat(created.status()).as("%s: %s", name, created.code()).isEqualTo(200);
        String id = created.body().path(field).path("id").asString();
        Answer duplicate = rest("POST", collection, ADMIN, body.toString());
        assertThat(duplicate.status()).isEqualTo(resource.path("duplicateConflicts").asBoolean() ? 409 : 201);
        GrpcParityContract.assertSameAnswer(duplicate, grpc(create, creation, ADMIN));
        GrpcParityContract.assertSameAnswer(rest("POST", collection, ADMIN, "{}"), grpc(create, object(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("POST", collection, NOBODY, body.toString()),
                grpc(create, creation, NOBODY));

        Answer listed = rest("GET", collection, ADMIN, null);
        Answer listedGrpc = grpc(list, object(), ADMIN);
        assertThat(ids(listedGrpc.body().path("items"), "id")).contains(id)
                .isEqualTo(ids(listed.body().path("data"), "id"));
        JsonNode row = listed.body().path("data").get(ids(listed.body().path("data"), "id").indexOf(id));
        GrpcParity.assertSameResource(row, created.body().path(field));
        GrpcParityContract.assertSameAnswer(rest("GET", collection + "?filter=nope%20%3D%201", ADMIN, null),
                grpc(list, object().put("filter", "nope = 1"), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", collection, NOBODY, null), grpc(list, object(), NOBODY));

        String etag = row.path("etag").asString();
        GrpcParityContract.assertSameAnswer(rest("DELETE", collection + "/" + id, ADMIN, null, "If-Match", STALE),
                grpc(delete, keyed(delete, id).put("etag", "99"), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("DELETE", collection + "/" + id, ADMIN, null),
                grpc(delete, keyed(delete, id), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("DELETE", collection + "/" + id, NOBODY, null, "If-Match", etag),
                grpc(delete, keyed(delete, id).put("etag", etag), NOBODY));
        GrpcParityContract.assertSameAnswer(rest("DELETE", collection + "/" + id, STRANGER, null, "If-Match", etag),
                grpc(delete, keyed(delete, id).put("etag", etag), STRANGER));
        assertThat(grpc(delete, keyed(delete, id).put("etag", etag), ADMIN).status()).as("%s delete", name)
                .isEqualTo(200);
        GrpcParityContract.assertSameAnswer(rest("DELETE", collection + "/" + id, ADMIN, null, "If-Match", etag),
                grpc(delete, keyed(delete, id).put("etag", etag), ADMIN));
    }

    // ------------------------------------------------------------------ the records outside crud-spec.json

    @Test
    void theAuditTrailAnswersLikeRest() throws Exception {
        UUID entry = UUID.randomUUID();
        http.sql(TENANT, "INSERT INTO platform.audit_log (id, tenant_id, log_name, description, event, subject_type, "
                + "subject_id, causer_type, causer_id, properties, occurred_at) VALUES (:id, :tenant, 'platform', "
                + "'update platform.tag', 'update', 'platform.tag', :subject, 'user', :causer, "
                + "CAST('{\"changedFields\":[\"name\"]}' AS jsonb), TIMESTAMPTZ '2026-09-10 10:00:00+00')",
                Map.of("id", entry, "tenant", TENANT, "subject", UUID.randomUUID(), "causer", UUID.randomUUID()));
        seeded("AuditLog", "/v1/platform/audit-logs", entry.toString(),
                "occurredAt >= \"2026-09-01T00:00:00Z\" AND occurredAt < \"2026-10-01T00:00:00Z\"");
        MethodDescriptor list = rpc("ListAuditLogs");
        Answer unbounded = rest("GET", "/v1/platform/audit-logs", ADMIN, null);
        assertThat(unbounded.code()).isEqualTo("INVALID_FILTER");
        GrpcParityContract.assertSameAnswer(unbounded, grpc(list, object(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/platform/audit-logs/" + entry, SCHOOL_USER, null),
                grpc(rpc("GetAuditLog"), object().put("id", entry.toString()), SCHOOL_USER));
    }

    @Test
    void storedFilesAnswerLikeRestAndNeedTheStore() throws Exception {
        String[] files = {file(), file()};
        seeded("FileObject", "/v1/platform/files", files[0], "");

        MethodDescriptor delete = rpc("DeleteFileObject");
        GrpcParityContract.assertSameAnswer(rest("DELETE", "/v1/platform/files/" + files[0], ADMIN, null,
                "If-Match", STALE), grpc(delete, object().put("id", files[0]).put("etag", "99"), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("DELETE", "/v1/platform/files/" + files[0], ADMIN, null),
                grpc(delete, object().put("id", files[0]), ADMIN));

        MethodDescriptor upload = rpc("CreateUploadUrl");
        String declared = "{\"fileName\":\"a.pdf\",\"mimeType\":\"application/pdf\",\"sizeBytes\":\"3\"}";
        Answer noStore = rest("POST", "/v1/platform/files:createUploadUrl", ADMIN, declared);
        assertThat(noStore.status()).isEqualTo(503);
        assertThat(noStore.code()).isEqualTo("NOT_CONFIGURED");
        GrpcParityContract.assertSameAnswer(noStore, grpc(upload, (ObjectNode) JSON.readTree(declared), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/platform/files:createUploadUrl", ADMIN, "{}"),
                grpc(upload, object(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/platform/files:createUploadUrl", NOBODY, declared),
                grpc(upload, (ObjectNode) JSON.readTree(declared), NOBODY));

        String absent = UUID.randomUUID().toString();
        for (String id : List.of(files[1], absent)) {
            GrpcParityContract.assertSameAnswer(rest("POST", "/v1/platform/files/" + id + ":complete", ADMIN, "{}"),
                    grpc(rpc("CompleteUpload"), object().put("fileObjectId", id), ADMIN));
            GrpcParityContract.assertSameAnswer(rest("GET", "/v1/platform/files/" + id + ":downloadUrl", ADMIN, null),
                    grpc(rpc("GetDownloadUrl"), object().put("fileObjectId", id), ADMIN));
            GrpcParityContract.assertSameAnswer(rest("GET", "/v1/platform/files/" + id + ":downloadUrl", null, null),
                    grpc(rpc("GetDownloadUrl"), object().put("fileObjectId", id), null));
            GrpcParityContract.assertSameAnswer(rest("POST", "/v1/platform/files/" + id + ":copy", ADMIN, "{}"),
                    grpc(rpc("CopyFile"), object().put("fileObjectId", id), ADMIN));
        }
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/platform/files/" + files[1] + ":copy", NOBODY, "{}"),
                grpc(rpc("CopyFile"), object().put("fileObjectId", files[1]), NOBODY));

        Answer deleted = rest("DELETE", "/v1/platform/files/" + files[0], ADMIN, null, "If-Match", "\"1\"");
        GrpcParityContract.assertSameAnswer(deleted,
                grpc(delete, object().put("id", files[1]).put("etag", "1"), ADMIN));
    }

    /** A stored file of the test tenant, as CompleteUpload leaves it. */
    private String file() {
        UUID id = UUID.randomUUID();
        http.sql(TENANT, "INSERT INTO platform.file_object (id, tenant_id, storage_disk, storage_key, file_name, "
                + "mime_type, size_bytes, status, row_version) VALUES (:id, :tenant, 'gcs', :key, 'notes.pdf', "
                + "'application/pdf', 3, 'available', 1)", Map.of("id", id, "tenant", TENANT, "key", "t/" + id));
        return id.toString();
    }

    // ------------------------------------------------------------------ subscriptions

    @TestFactory
    List<DynamicNode> everyTransitionAnswersLikeRest() throws Exception {
        List<DynamicNode> tests = new ArrayList<>();
        for (JsonNode resource : spec().path("resources")) {
            for (JsonNode transition : resource.path("transitions")) {
                tests.add(DynamicTest.dynamicTest(transition.path("operation").asString(),
                        () -> transition(resource, transition)));
            }
        }
        assertThat(tests).hasSize(1);
        return tests;
    }

    /**
     * One transition of crud-spec.json on two seeded rows, one per transport: both refuse alike without the
     * permission, for a stale etag and for an absent row, both perform it from its guarded state with the same
     * resulting resource, and both answer alike when it is repeated.
     */
    private void transition(JsonNode resource, JsonNode transition) throws Exception {
        String overRestId = subscription("pending", null);
        String overGrpcId = subscription("pending", null);
        String item = resource.path("item").asString();
        String path = transition.path("path").asString();
        ObjectNode body = (ObjectNode) transition.path("body");
        ObjectNode stale = body.deepCopy().put("etag", STALE);
        String operation = transition.path("operation").asString();
        MethodDescriptor rpc = rpc(operation.substring("platform.".length()));
        for (String id : List.of(overRestId, overGrpcId)) {
            sql(TENANT, strings(transition.path("prepare")).stream().map(s -> s.replace("{id}", id)).toList());
        }
        String absent = UUID.randomUUID().toString();

        GrpcParityContract.assertSameAnswer(rest("POST", path.replace("{id}", overGrpcId), NOBODY, body.toString()),
                grpc(rpc, keyed(rpc, overGrpcId).setAll(body), NOBODY));
        Answer refused = rest("POST", path.replace("{id}", overGrpcId), ADMIN, stale.toString());
        assertThat(refused.status()).as("a stale etag").isEqualTo(412);
        GrpcParityContract.assertSameAnswer(refused, grpc(rpc, keyed(rpc, overGrpcId).setAll(stale), ADMIN));
        Answer missing = rest("POST", path.replace("{id}", absent), ADMIN, body.toString());
        assertThat(missing.status()).as("an absent row").isEqualTo(404);
        GrpcParityContract.assertSameAnswer(missing, grpc(rpc, keyed(rpc, absent).setAll(body), ADMIN));

        Answer overRest = rest("POST", path.replace("{id}", overRestId), ADMIN, body.toString());
        assertThat(overRest.status()).as(overRest.body().toString()).isEqualTo(200);
        Answer overGrpc = grpc(rpc, keyed(rpc, overGrpcId).setAll(body), ADMIN);
        assertThat(overGrpc.status()).as("%s: %s", operation, overGrpc.code()).isEqualTo(200);
        GrpcParity.assertSameResource(rest("GET", item.replace("{id}", overGrpcId), ADMIN, null).body(),
                overGrpc.body().path(resultField(rpc)));
        ((ObjectNode) transition.path("sets")).properties().forEach(set -> assertThat(overRest.body()
                .path(set.getKey())).as(set.getKey()).isEqualTo(set.getValue()));
        GrpcParityContract.assertSameAnswer(rest("POST", path.replace("{id}", overRestId), ADMIN, body.toString()),
                grpc(rpc, keyed(rpc, overGrpcId).setAll(body), ADMIN));
    }

    @Test
    void confirmingACheckoutAnswersLikeRest() throws Exception {
        String overRestId = subscription("pending", "order_GrpcR");
        String overGrpcId = subscription("pending", "order_GrpcG");
        MethodDescriptor confirm = rpc("ConfirmSubscriptionCheckout");
        String forged = confirmation("order_GrpcG", "pay_1", "00ff");
        Answer refused = rest("POST", confirmPath(overGrpcId), ADMIN, forged);
        assertThat(refused.status()).isEqualTo(422);
        GrpcParityContract.assertSameAnswer(refused, grpc(confirm, confirmRequest(overGrpcId, forged), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("POST", confirmPath(overGrpcId), ADMIN, "{}"),
                grpc(confirm, keyed(confirm, overGrpcId), ADMIN));
        String proof = confirmation("order_GrpcG", "pay_1", hmac("order_GrpcG|pay_1"));
        for (String caller : List.of(SCHOOL_USER, NOBODY, STRANGER)) {
            GrpcParityContract.assertSameAnswer(rest("POST", confirmPath(overGrpcId), caller, proof),
                    grpc(confirm, confirmRequest(overGrpcId, proof), caller));
        }

        Answer overRest = rest("POST", confirmPath(overRestId), ADMIN,
                confirmation("order_GrpcR", "pay_2", hmac("order_GrpcR|pay_2")));
        assertThat(overRest.status()).as(overRest.body().toString()).isEqualTo(200);
        Answer overGrpc = grpc(confirm, confirmRequest(overGrpcId, proof), ADMIN);
        assertThat(overGrpc.status()).as(overGrpc.code()).isEqualTo(200);
        JsonNode confirmed = overGrpc.body().path("tenantSubscription");
        assertThat(confirmed.path("status").asString())
                .as("the gateway (not reachable here) has not reported the payment captured")
                .isEqualTo("STATUS_PENDING");
        GrpcParity.assertSameResource(rest("GET", "/v1/platform/tenant-subscriptions/" + overGrpcId, ADMIN, null)
                .body(), confirmed);
        Answer again = grpc(confirm, confirmRequest(overGrpcId, proof), ADMIN);
        assertThat(again.body()).as("waiting for the webhook changes nothing").isEqualTo(overGrpc.body());

        String activeGrpcId = subscription("active", "order_GrpcAG");
        String activeProof = confirmation("order_GrpcAG", "pay_3", hmac("order_GrpcAG|pay_3"));
        Answer activeRest = rest("POST", confirmPath(subscription("active", "order_GrpcAR")), ADMIN,
                confirmation("order_GrpcAR", "pay_4", hmac("order_GrpcAR|pay_4")));
        assertThat(activeRest.status()).as(activeRest.body().toString()).isEqualTo(200);
        Answer activeGrpc = grpc(confirm, confirmRequest(activeGrpcId, activeProof), ADMIN);
        assertThat(activeGrpc.status()).as(activeGrpc.code()).isEqualTo(200);
        assertThat(activeGrpc.body().path("tenantSubscription").path("status").asString())
                .isEqualTo("STATUS_ACTIVE");
        Answer activeAgain = grpc(confirm, confirmRequest(activeGrpcId, activeProof), ADMIN);
        assertThat(activeAgain.body()).as("activated once, answered unchanged").isEqualTo(activeGrpc.body());
    }

    @Test
    void startingACheckoutAnswersLikeRest() throws Exception {
        MethodDescriptor start = rpc("StartSubscriptionCheckout");
        String priced = plan("1200.00", true);
        for (String plan : List.of(priced, plan("0.00", true), plan("10.00", false), UUID.randomUUID().toString())) {
            // the gateway is closed (503 UNAVAILABLE); a free, retired or unknown plan is 422
            String key = UUID.randomUUID().toString();
            GrpcParityContract.assertSameAnswer(rest("POST", "/v1/platform/tenant-subscriptions:checkout", ADMIN,
                    "{\"planId\":\"" + plan + "\"}", "Idempotency-Key", key),
                    grpc(start, object().put("planId", plan).put("requestId", UUID.randomUUID().toString()), ADMIN));
        }
        Answer noKey = rest("POST", "/v1/platform/tenant-subscriptions:checkout", ADMIN,
                "{\"planId\":\"" + priced + "\"}");
        assertThat(noKey.status()).as("a money operation needs its key").isEqualTo(400);
        GrpcParityContract.assertSameAnswer(noKey, grpc(start, object().put("planId", priced), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/platform/tenant-subscriptions:checkout", SCHOOL_USER,
                "{\"planId\":\"" + priced + "\"}", "Idempotency-Key", UUID.randomUUID().toString()),
                grpc(start, object().put("planId", priced).put("requestId", UUID.randomUUID().toString()),
                        SCHOOL_USER));
    }

    @Test
    void theSignedWebhookIsRestOnly() throws Exception {
        MethodDescriptor webhook = rpc("ReceiveSubscriptionWebhook");
        ObjectNode delivery = object().put("rawBody", "e30=");
        delivery.putObject("headers").put("X-Razorpay-Signature", "00ff");
        for (String caller : Arrays.asList(null, ADMIN)) {
            Answer overGrpc = grpc(webhook, delivery, caller);
            assertThat(overGrpc.status()).isEqualTo(501);
            assertThat(overGrpc.code()).isEqualTo("NOT_IMPLEMENTED");
        }
    }

    private String subscription(String status, String order) {
        UUID id = UUID.randomUUID();
        Map<String, Object> params = new HashMap<>(Map.of("id", id, "tenant", TENANT, "user", UUID.randomUUID(),
                "status", status));
        params.put("order", order);
        http.sql(TENANT, "INSERT INTO platform.tenant_subscription (id, tenant_id, requested_by_user_id, status, "
                + "price, billing_cycle_months, gateway_name, gateway_txn_ref, gateway_status, row_version) VALUES "
                + "(:id, :tenant, :user, :status, 1200.00, 12, 'razorpay', :order, 'created', 1)", params);
        return id.toString();
    }

    private String plan(String price, boolean active) {
        UUID id = UUID.randomUUID();
        http.sqlGlobal("INSERT INTO platform.saas_plan (id, code, display_name, billing_cycle_months, price, "
                + "is_active, row_version) VALUES (:id, :code, 'Plan', 12, :price, :active, 1)", Map.of("id", id,
                "code", "grpc-" + id, "price", new BigDecimal(price), "active", active));
        return id.toString();
    }

    private static String confirmPath(String subscription) {
        return "/v1/platform/tenant-subscriptions/" + subscription + ":confirmCheckout";
    }

    private static String confirmation(String order, String payment, String signature) {
        return "{\"gatewayOrderId\":\"" + order + "\",\"gatewayPaymentId\":\"" + payment
                + "\",\"gatewaySignature\":\"" + signature + "\"}";
    }

    private static ObjectNode confirmRequest(String subscription, String body) throws Exception {
        return ((ObjectNode) JSON.readTree(body)).put("tenantSubscriptionId", subscription);
    }

    private static String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(KEY_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------------ platform staff: enquiries, demo data

    @Test
    void markingAnEnquiryContactedAnswersLikeRest() throws Exception {
        String overRestId = enquiry();
        String overGrpcId = enquiry();
        MethodDescriptor mark = rpc("MarkEnquiryContacted");
        String path = "/v1/platform/sales-enquiries/%s:markContacted";
        GrpcParityContract.assertSameAnswer(rest("POST", path.formatted(overGrpcId), ADMIN, "{}"),
                grpc(mark, keyed(mark, overGrpcId), ADMIN));
        String absent = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(rest("POST", path.formatted(absent), STAFF, "{}"),
                grpc(mark, keyed(mark, absent), STAFF));

        Answer overRest = rest("POST", path.formatted(overRestId), STAFF, "{\"reason\":\"Called back\"}");
        assertThat(overRest.status()).as(overRest.body().toString()).isEqualTo(200);
        Answer overGrpc = grpc(mark, keyed(mark, overGrpcId).put("reason", "Called back"), STAFF);
        assertThat(overGrpc.status()).as(overGrpc.code()).isEqualTo(200);
        assertThat(overGrpc.body().path("salesEnquiry").path("status").asString()).isEqualTo("STATUS_CONTACTED");
        GrpcParity.assertSameResource(rest("GET", "/v1/platform/sales-enquiries/" + overGrpcId, STAFF, null).body(),
                overGrpc.body().path("salesEnquiry"));
        GrpcParityContract.assertSameAnswer(rest("POST", path.formatted(overRestId), STAFF, "{}"),
                grpc(mark, keyed(mark, overGrpcId), STAFF));
    }

    private String enquiry() {
        UUID id = UUID.randomUUID();
        http.sqlGlobal("INSERT INTO platform.sales_enquiry (id, contact_name, email, phone, organisation_name, "
                + "designation, row_version) VALUES (:id, 'A. Rao', 'a.rao@example.org', '9000000000', "
                + "'Sunrise School', 'Principal', 1)", Map.of("id", id));
        return id.toString();
    }

    @Test
    void clearingDemoDataAnswersLikeRest() throws Exception {
        MethodDescriptor clear = rpc("ClearDemoData");
        String path = "/v1/platform/tenants/%s:clearDemoData";
        Answer overRest = rest("POST", path.formatted(TENANT), STAFF, "{}");
        assertThat(overRest.status()).as(overRest.body().toString()).isEqualTo(202);
        Answer overGrpc = grpc(clear, object().put("tenantId", TENANT.toString()), STAFF);
        assertThat(overGrpc.status()).as(overGrpc.code()).isEqualTo(200);
        JsonNode operation = overGrpc.body().path("result");
        assertThat(operation.path("kind").asString()).isEqualTo(overRest.body().path("kind").asString());
        assertThat(operation.path("status").asString()).isEqualTo("succeeded");
        // platform staff hold no tenant identity: the tenant's administrator reads what they started
        GrpcParity.assertSameResource(rest("GET", "/v1/platform/operations/" + operation.path("operationId")
                .asString(), ADMIN, null).body(), operation);
        UUID closed = UUID.randomUUID();
        http.sqlGlobal("INSERT INTO platform.tenant (id, code, name, status) VALUES (:id, 'grpc-closed', 'Closed', "
                + "'closed')", Map.of("id", closed));
        for (UUID tenant : List.of(UUID.randomUUID(), closed)) {
            GrpcParityContract.assertSameAnswer(rest("POST", path.formatted(tenant), STAFF, "{}"),
                    grpc(clear, object().put("tenantId", tenant.toString()), STAFF));
        }
        GrpcParityContract.assertSameAnswer(rest("POST", path.formatted(TENANT), ADMIN, "{}"),
                grpc(clear, object().put("tenantId", TENANT.toString()), ADMIN));
    }

    // ------------------------------------------------------------------ branding, search, reports, photos, storage

    @Test
    void brandingAnswersLikeRest() throws Exception {
        MethodDescriptor branding = rpc("GetBranding");
        Answer overRest = rest("GET", "/v1/platform/branding?tenantCode=grpc-a&institutionId=" + SCHOOL, null, null);
        assertThat(overRest.status()).as(overRest.body().toString()).isEqualTo(200);
        Answer overGrpc = grpc(branding, object().put("tenantCode", "grpc-a").put("institutionId",
                SCHOOL.toString()), null);
        assertThat(overGrpc.status()).as(overGrpc.code()).isEqualTo(200);
        GrpcParity.assertSameResource(overRest.body(), overGrpc.body().path("result"));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/platform/branding?tenantCode=nobody", null, null),
                grpc(branding, object().put("tenantCode", "nobody"), null));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/platform/branding", null, null),
                grpc(branding, object(), null));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/platform/branding?tenantCode=grpc-a&institutionId=x",
                null, null), grpc(branding, object().put("tenantCode", "grpc-a").put("institutionId", "x"), null));
    }

    @Test
    void globalSearchAnswersLikeRest() throws Exception {
        Answer tag = rest("POST", "/v1/platform/tags", ADMIN, "{\"name\":\"Artwork\",\"slug\":\"artwork\"}");
        assertThat(tag.status()).as(tag.body().toString()).isEqualTo(201);
        MethodDescriptor search = rpc("GlobalSearch");
        Answer overRest = rest("GET", "/v1/platform/search?query=artwork&entityTypes=platform.tag", ADMIN, null);
        assertThat(overRest.status()).as(overRest.body().toString()).isEqualTo(200);
        ObjectNode request = object().put("query", "artwork");
        request.putArray("entityTypes").add("platform.tag");
        Answer overGrpc = grpc(search, request, ADMIN);
        assertThat(overGrpc.status()).as(overGrpc.code()).isEqualTo(200);
        assertThat(ids(overGrpc.body().path("items"), "entityId")).contains(tag.body().path("id").asString())
                .isEqualTo(ids(overRest.body().path("data"), "entityId"));
        GrpcParity.assertSameResource(overRest.body().path("data").get(0), overGrpc.body().path("items").get(0));
        ObjectNode foreign = object().put("query", "artwork");
        foreign.putArray("entityTypes").add("sis.student");
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/platform/search?query=artwork&entityTypes=sis.student",
                ADMIN, null), grpc(search, foreign, ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/platform/search?query=a", ADMIN, null),
                grpc(search, object().put("query", "a"), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/platform/search", ADMIN, null),
                grpc(search, object(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/platform/search?query=artwork", NOBODY, null),
                grpc(search, object().put("query", "artwork"), NOBODY));
    }

    @Test
    void reportsAnswerLikeRest() throws Exception {
        MethodDescriptor definitions = rpc("ListReportDefinitions");
        for (String module : List.of("", "platform", "sis")) {
            Answer overRest = rest("GET", "/v1/platform/reports" + (module.isEmpty() ? "" : "?module=" + module),
                    ADMIN, null);
            assertThat(overRest.status()).as(overRest.body().toString()).isEqualTo(200);
            Answer overGrpc = grpc(definitions, module.isEmpty() ? object() : object().put("module", module), ADMIN);
            assertThat(ids(overGrpc.body().path("items"), "reportCode"))
                    .isEqualTo(ids(overRest.body().path("data"), "reportCode"));
            if (!overRest.body().path("data").isEmpty()) {
                GrpcParity.assertSameResource(overRest.body().path("data").get(0), overGrpc.body().path("items")
                        .get(0));
            }
        }
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/platform/reports", NOBODY, null),
                grpc(definitions, object(), NOBODY));

        MethodDescriptor run = rpc("RunReport");
        String auditReport = "{\"reportCode\":\"platform.audit_log\",\"format\":\"csv\",\"parameters\":"
                + "{\"from\":\"2026-10-01\",\"to\":\"2026-10-02\"}}";
        Answer noStore = rest("POST", "/v1/platform/reports:run", ADMIN, auditReport);
        assertThat(noStore.code()).isEqualTo("NOT_CONFIGURED");
        GrpcParityContract.assertSameAnswer(noStore, grpc(run, reportRequest(auditReport), ADMIN));
        for (String refused : List.of("{\"reportCode\":\"sis.students\",\"format\":\"csv\"}",
                "{\"reportCode\":\"platform.audit_log\",\"format\":\"csv\",\"parameters\":{\"from\":\"yesterday\"}}",
                "{\"reportCode\":\"platform.audit_log\"}")) {
            GrpcParityContract.assertSameAnswer(rest("POST", "/v1/platform/reports:run", ADMIN, refused),
                    grpc(run, reportRequest(refused), ADMIN));
        }
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/platform/reports:run", SCHOOL_USER, auditReport),
                grpc(run, reportRequest(auditReport), SCHOOL_USER));
    }

    /** A RunReport request from its REST body: the format as the proto enum's value name. */
    private static ObjectNode reportRequest(String body) throws Exception {
        ObjectNode request = (ObjectNode) JSON.readTree(body);
        if (request.has("format")) {
            request.put("format", "FORMAT_" + request.path("format").asString().toUpperCase(Locale.ROOT));
        }
        return request;
    }

    @Test
    void stockImagesAndMyStorageAnswerLikeRest() throws Exception {
        MethodDescriptor photos = rpc("SearchStockImages");
        Answer unreachable = rest("GET", "/v1/platform/stock-images?query=science%20lab", ADMIN, null);
        assertThat(unreachable.code()).isEqualTo("UNAVAILABLE");
        GrpcParityContract.assertSameAnswer(unreachable, grpc(photos, object().put("query", "science lab"), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/platform/stock-images", ADMIN, null),
                grpc(photos, object(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/platform/stock-images?query=lab", NOBODY, null),
                grpc(photos, object().put("query", "lab"), NOBODY));

        UUID user = UUID.randomUUID();
        String mine = token(user, TENANT, List.of("platform.storage_quota.get_my_storage_usage"), List.of("*"));
        http.sql(TENANT, "INSERT INTO platform.storage_quota (id, tenant_id, user_id, quota_bytes, used_bytes, "
                + "row_version) VALUES (:id, :tenant, :user, 5000, 1200, 1)",
                Map.of("id", UUID.randomUUID(), "tenant", TENANT, "user", user));
        MethodDescriptor usage = rpc("GetMyStorageUsage");
        Answer overRest = rest("GET", "/v1/platform/me/storage", mine, null);
        assertThat(overRest.status()).as(overRest.body().toString()).isEqualTo(200);
        Answer overGrpc = grpc(usage, object(), mine);
        assertThat(overGrpc.status()).as(overGrpc.code()).isEqualTo(200);
        GrpcParity.assertSameResource(overRest.body(), overGrpc.body().path("result"));
        String elsewhere = token(user, OTHER_TENANT, List.of("platform.storage_quota.get_my_storage_usage"),
                List.of("*"));
        for (String caller : List.of(elsewhere, NOBODY)) {
            GrpcParityContract.assertSameAnswer(rest("GET", "/v1/platform/me/storage", caller, null),
                    grpc(usage, object(), caller));
        }
    }

    // ------------------------------------------------------------------ long-running operations

    @Test
    void theLongRunningOperationsAnswerLikeRest() throws Exception {
        UUID user = UUID.randomUUID();
        String own = token(user, TENANT, List.of("platform.operation.read", "platform.operation.cancel_operation"),
                List.of(SCHOOL.toString()));
        String[] mine = {UUID.randomUUID().toString(), UUID.randomUUID().toString()};
        String theirs = UUID.randomUUID().toString();
        String insert = "INSERT INTO platform.operation (id, tenant_id, operation_name, status, progress_percent,"
                + " started_by_user_id, started_at, created_at, updated_at, row_version) VALUES ('%s', :tenant, '%s',"
                + " 'running', 40, '%s', now() - interval '%d minutes', now() - interval '%d minutes', now(), 1)";
        sql(TENANT, List.of(insert.formatted(mine[0], "platform.RunReport", user, 3, 3),
                insert.formatted(mine[1], "platform.RunReport", user, 2, 2),
                insert.formatted(theirs, "platform.ClearDemoData", UUID.randomUUID(), 1, 1)));

        MethodDescriptor get = rpc("GetOperation");
        Answer read = rest("GET", "/v1/platform/operations/" + mine[0], own, null);
        assertThat(read.status()).as(read.body().toString()).isEqualTo(200);
        Answer got = grpc(get, object().put("operationId", mine[0]), own);
        assertThat(got.status()).as(got.code()).isEqualTo(200);
        GrpcParity.assertSameResource(read.body(), got.body().path("result"));
        for (String token : List.of(own, NOBODY, STRANGER)) {
            GrpcParityContract.assertSameAnswer(rest("GET", "/v1/platform/operations/" + theirs, token, null),
                    grpc(get, object().put("operationId", theirs), token));
        }
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/platform/operations/not-a-uuid", own, null),
                grpc(get, object().put("operationId", "not-a-uuid"), own));

        MethodDescriptor list = rpc("ListOperations");
        Answer listed = rest("GET", "/v1/platform/operations", own, null);
        Answer listedGrpc = grpc(list, object(), own);
        assertThat(ids(listedGrpc.body().path("items"), "operationId")).containsExactly(mine[1], mine[0])
                .isEqualTo(ids(listed.body().path("data"), "operationId"));
        GrpcParity.assertSameResource(listed.body().path("data").get(0), listedGrpc.body().path("items").get(0));
        Answer paged = rest("GET", "/v1/platform/operations?size=1", own, null);
        ObjectNode firstPage = object();
        firstPage.putObject("pageable").put("size", 1);
        Answer pagedGrpc = grpc(list, firstPage, own);
        assertThat(ids(pagedGrpc.body().path("items"), "operationId")).containsExactly(mine[1])
                .isEqualTo(ids(paged.body().path("data"), "operationId"));
        GrpcParity.assertSameCursor(paged.body().path("paging").path("next").asString(),
                pagedGrpc.body().path("pagination").path("nextCursor").asString());
        Answer kind = rest("GET", "/v1/platform/operations?kind=platform.ClearDemoData", ADMIN, null);
        Answer kindGrpc = grpc(list, object().put("kind", "platform.ClearDemoData"), ADMIN);
        assertThat(ids(kindGrpc.body().path("items"), "operationId")).contains(theirs)
                .isEqualTo(ids(kind.body().path("data"), "operationId"));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/platform/operations", NOBODY, null),
                grpc(list, object(), NOBODY));

        MethodDescriptor cancel = rpc("CancelOperation");
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/platform/operations/" + theirs + ":cancel", own, null),
                grpc(cancel, object().put("operationId", theirs), own));
        Answer cancelled = rest("POST", "/v1/platform/operations/" + mine[0] + ":cancel", own, null);
        assertThat(cancelled.status()).as(cancelled.body().toString()).isEqualTo(202);
        Answer cancelledGrpc = grpc(cancel, object().put("operationId", mine[1]), own);
        assertThat(cancelledGrpc.status()).as(cancelledGrpc.code()).isEqualTo(200);
        GrpcParity.assertSameResource(rest("GET", "/v1/platform/operations/" + mine[1], own, null).body(),
                cancelledGrpc.body().path("result"));
        assertThat(cancelledGrpc.body().path("result").path("status").asString()).isEqualTo("cancelled");
        Answer again = rest("POST", "/v1/platform/operations/" + mine[0] + ":cancel", own, null);
        assertThat(again.status()).isEqualTo(409);
        GrpcParityContract.assertSameAnswer(again, grpc(cancel, object().put("operationId", mine[1]), own));
    }

    // ------------------------------------------------------------------ idempotency

    @Test
    void aCreateReplaysUnderItsRequestIdOnBoth() throws Exception {
        String body = "{\"institutionId\":\"0196f000-0000-7000-8000-00000000a001\",\"entityType\":\"lms.course\","
                + "\"entityId\":\"" + UUID.randomUUID() + "\",\"noteText\":\"Once\"}";
        String key = UUID.randomUUID().toString();
        Answer first = rest("POST", "/v1/platform/entity-notes", ADMIN, body, "Idempotency-Key", key);
        assertThat(first.status()).as(first.body().toString()).isEqualTo(201);
        Answer replayed = rest("POST", "/v1/platform/entity-notes", ADMIN, body, "Idempotency-Key", key);
        assertThat(replayed.body().path("id").asString()).isEqualTo(first.body().path("id").asString());

        MethodDescriptor create = rpc("CreateEntityNote");
        ObjectNode request = object().put("requestId", UUID.randomUUID().toString());
        request.set("entityNote", JSON.readTree(body));
        Answer firstGrpc = grpc(create, request, ADMIN);
        assertThat(firstGrpc.status()).as(firstGrpc.code()).isEqualTo(200);
        Answer replayedGrpc = grpc(create, request, ADMIN);
        assertThat(replayedGrpc.body()).isEqualTo(firstGrpc.body());
        Answer fresh = grpc(create, request.deepCopy().put("requestId", UUID.randomUUID().toString()), ADMIN);
        assertThat(fresh.status()).isEqualTo(200);
        assertThat(fresh.body().path("entityNote").path("id").asString())
                .isNotEqualTo(firstGrpc.body().path("entityNote").path("id").asString());
        // the key reused for another request is refused alike
        ((ObjectNode) request.path("entityNote")).put("noteText", "Twice");
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/platform/entity-notes", ADMIN,
                body.replace("Once", "Twice"), "Idempotency-Key", key), grpc(create, request, ADMIN));
    }

    // ------------------------------------------------------------------ imports

    /**
     * The import operations refuse alike on both transports; their successful runs and dry runs need a file store and
     * are compared in PlatformImportsComponentTest, which has one.
     */
    @Test
    void theImportOperationsAnswerLikeRest() throws Exception {
        MethodDescriptor template = rpc("GetImportTemplate");
        Answer noStore = rest("GET", "/v1/platform/imports/templates/students", ADMIN, null);
        assertThat(noStore.code()).isEqualTo("NOT_CONFIGURED");
        GrpcParityContract.assertSameAnswer(noStore, grpc(template, object().put("entityType", "students"), ADMIN));
        for (String type : List.of("lms_course_package", "teachers")) {
            GrpcParityContract.assertSameAnswer(rest("GET", "/v1/platform/imports/templates/" + type, ADMIN, null),
                    grpc(template, object().put("entityType", type), ADMIN));
        }
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/platform/imports/templates/students", NOBODY, null),
                grpc(template, object().put("entityType", "students"), NOBODY));

        MethodDescriptor start = rpc("StartImport");
        String absent = UUID.randomUUID().toString();
        String pdf = file();
        String csv = csvFile();
        Map<String, String> refusals = new LinkedHashMap<>();
        refusals.put("{\"entityType\":\"students\",\"fileId\":\"" + absent + "\"}", ADMIN);
        refusals.put("{\"entityType\":\"guardians\",\"fileId\":\"" + absent + "\",\"institutionId\":\"" + SCHOOL
                + "\"}", ADMIN);
        refusals.put("{\"entityType\":\"lms_course_package\",\"fileId\":\"" + absent + "\"}", ADMIN);
        refusals.put("{\"entityType\":\"students\",\"fileId\":\"" + absent + "\",\"institutionId\":\"" + SCHOOL
                + "\"}", ADMIN);
        refusals.put("{\"entityType\":\"students\",\"fileId\":\"" + pdf + "\",\"institutionId\":\"" + SCHOOL
                + "\",\"dryRun\":true}", ADMIN);
        refusals.put("{\"entityType\":\"students\",\"fileId\":\"" + csv + "\",\"institutionId\":\"" + SCHOOL
                + "\",\"dryRun\":true}", ADMIN);
        refusals.put("{\"entityType\":\"guardians\",\"fileId\":\"" + csv + "\"}", SCHOOL_USER);
        refusals.put("{\"entityType\":\"students\",\"fileId\":\"" + csv + "\",\"institutionId\":\"" + absent
                + "\"}", SCHOOL_USER);
        refusals.put("{\"entityType\":\"students\",\"fileId\":\"" + csv + "\",\"institutionId\":\"" + SCHOOL
                + "\"}", NOBODY);
        for (Map.Entry<String, String> refused : refusals.entrySet()) {
            Answer overRest = rest("POST", "/v1/platform/imports", refused.getValue(), refused.getKey());
            assertThat(overRest.status()).as(refused.getKey()).isGreaterThanOrEqualTo(400);
            GrpcParityContract.assertSameAnswer(overRest, grpc(start, importRequest(refused.getKey()),
                    refused.getValue()));
        }
        assertThat(grpc(start, object(), null).status()).as("authentication first").isEqualTo(401);
    }

    /** A StartImport request from its REST body: the entity type as the proto enum's value name. */
    private static ObjectNode importRequest(String body) throws Exception {
        ObjectNode request = (ObjectNode) JSON.readTree(body);
        request.put("entityType", "ENTITY_TYPE_" + request.path("entityType").asString().toUpperCase(Locale.ROOT));
        return request;
    }

    /** An uploaded CSV file of the tenant whose bytes would be in a store this context does not configure. */
    private String csvFile() {
        UUID id = UUID.randomUUID();
        http.sql(TENANT, "INSERT INTO platform.file_object (id, tenant_id, storage_disk, storage_key, file_name, "
                + "mime_type, size_bytes, status, row_version) VALUES (:id, :tenant, 'gcs', :key, 'students.csv', "
                + "'text/csv', 3, 'available', 1)", Map.of("id", id, "tenant", TENANT, "key", "t/" + id));
        return id.toString();
    }
}
