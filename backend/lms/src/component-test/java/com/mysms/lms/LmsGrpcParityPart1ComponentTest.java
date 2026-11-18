package com.mysms.lms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.FieldMask;
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
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.v1.LmsProto;
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
import java.util.Set;
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
 * Every RPC of the first part of lms's gRPC services (the course, lesson, quiz, question, assignment, exercise,
 * enrollment, cohort, timetable, live-class and learning-program services) against the same operation over REST, on
 * real PostgreSQL. For each resource of crud-spec.json these services own, the standard operations give the same
 * resource over both transports and both refuse alike — 403 without the permission, 404 for an absent row and another
 * tenant's row, 409 for a duplicate, 412 for a stale etag, 428 without one, 422 for an invalid body — and a standard
 * operation the release does not serve is 501 on both. Every RPC of these services is also called with a request for
 * nothing (unknown ids, an empty body) and must answer as REST does; the enrollment request's reject transition is
 * driven through its guard, its refusals and a request_id replay.
 */
@SpringBootTest(classes = LmsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=LmsGrpcParityPart1ComponentTest")
class LmsGrpcParityPart1ComponentTest {

    private static final Set<String> SERVICES = Set.of("CategoryService", "EvaluatorService", "LmsCourseService",
            "LessonService", "QuizService", "QuestionService", "QuizSubmissionService", "LmsAssignmentService",
            "LmsAssignmentSubmissionService", "ProgrammingExerciseService", "ExerciseSubmissionService",
            "ContentAuthorService", "EnrollmentService", "LessonProgressService", "ConferencingAccountService",
            "TimetableTemplateService", "CohortService", "TimetableEntryService", "TimetableLegendService",
            "CohortEnrollmentService", "LiveClassService", "LearningProgramService");
    private static final String COURSE = "affca88e-9a3d-5ad4-8f6b-621b239891b4";
    private static final String COURSE_FIXTURE = "INSERT INTO lms.course (tenant_id, institution_id, title, row_version, id)"
            + " VALUES (:tenant, '0196f000-0000-7000-8000-00000000a001', 'Parity course', 1, '" + COURSE
            + "') ON CONFLICT (id) DO NOTHING";
    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final String ADMIN = token(TENANT, List.of("lms.*"));
    private static final String NOBODY = token(TENANT, List.of());
    private static final String STRANGER = token(OTHER_TENANT, List.of("lms.*"));
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;


    @Autowired
    private ProtoBridge bridge;

    @Autowired
    @Qualifier(LmsDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "lms");
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
            channel = GrpcParity.channel("LmsGrpcParityPart1ComponentTest");
        }
        return channel;
    }

    private Answer grpc(MethodDescriptor rpc, DynamicMessage request, String token) {
        return GrpcParity.call(channel(), rpc, request, token);
    }

    private static DynamicMessage.Builder request(MethodDescriptor rpc) {
        return GrpcParityContract.request(rpc);
    }

    private static DynamicMessage.Builder set(DynamicMessage.Builder builder, String field, Object value) {
        return GrpcParityContract.set(builder, field, value);
    }

    private static void same(Answer overRest, Answer overGrpc) {
        GrpcParityContract.assertSameAnswer(overRest, overGrpc);
    }

    private void fixtures(UUID tenant, List<String> statements) {
        CallContext context = CallContext.anonymous(TRACE).withIdentity(tenant, null);
        CallContextHolder.callAs(context, () -> TestTransactions.of(database).write(() -> {
            statements.forEach(statement -> database.jdbc().sql(statement).param("tenant", tenant).update());
            return null;
        }));
    }

    private GrpcParityContract contract(String admin) {
        return new GrpcParityContract(LmsProto.getDescriptor(), this::rest, this::channel, bridge,
                new GrpcParityContract.Callers(admin, NOBODY, STRANGER));
    }

    /** The tenant's administrator, or the owner of the rows where only their owner reaches them. */
    private static String admin(JsonNode resource) {
        if (!resource.has("ownerOnly")) {
            return ADMIN;
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", resource.path("ownerOnly").asString());
        claims.put("tid", TENANT.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of("lms.*"));
        claims.put("tperms", List.of("lms.*"));
        claims.put("ins", List.of("*"));
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    /** An RPC of this part's services by name; null when no such RPC exists or another part serves it. */
    private static MethodDescriptor rpc(String name) {
        for (ServiceDescriptor service : LmsProto.getDescriptor().getServices()) {
            MethodDescriptor method = service.findMethodByName(name);
            if (method != null && SERVICES.contains(service.getName())) {
                return method;
            }
        }
        return null;
    }

    /** The List RPC of a resource: the one whose response's items are the resource. */
    private static MethodDescriptor listRpc(MethodDescriptor one) {
        for (MethodDescriptor method : one.getService().getMethods()) {
            FieldDescriptor items = method.getOutputType().findFieldByName("items");
            if (method.getName().startsWith("List") && items != null
                    && items.getMessageType().getName().equals(one.getName().replaceFirst("^(Create|Get|Update|Delete)", ""))) {
                return method;
            }
        }
        throw new IllegalStateException("no List RPC beside " + one.getFullName());
    }

    private static List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.asString()));
        return values;
    }

    // ---- the standard operations, resource by resource

    @TestFactory
    List<DynamicNode> everyStandardRpcAnswersLikeRest() throws Exception {
        JsonNode spec = JSON.readTree(getClass().getClassLoader().getResourceAsStream("crud-spec.json"));
        List<GrpcParityContract.Resource> resources = GrpcParityContract.resources("crud-spec.json");
        List<DynamicNode> tests = new ArrayList<>();
        for (int i = 0; i < resources.size(); i++) {
            GrpcParityContract.Resource resource = resources.get(i);
            JsonNode described = spec.path("resources").get(i);
            String name = resource.name();
            MethodDescriptor own = rpc("Get" + name) != null ? rpc("Get" + name) : rpc("Update" + name);
            if (own == null) {
                continue; // a resource of another part's services
            }
            Set<String> ops = Set.copyOf(strings(described.path("ops")));
            boolean whole = resource.standardOnly();
            tests.add(DynamicTest.dynamicTest(name, () -> {
                fixtures(TENANT, strings(described.path("fixtures")));
                fixtures(OTHER_TENANT, strings(described.path("fixtures")));
                String admin = admin(described);
                if (whole) {
                    contract(admin).verify(resource);
                } else {
                    verify(resource, described, ops, own, admin);
                }
            }));
        }
        assertThat(tests).as("the resources of this part's services").hasSizeGreaterThan(50);
        return tests;
    }

    /**
     * The standard operations of a resource that does not serve all five: the ones it serves answer the same resource
     * and the same refusals over both transports, from a row created over gRPC or — where rows are not created through
     * the API — the spec's seed row; the ones it does not serve are 501 on both.
     */
    private void verify(GrpcParityContract.Resource resource, JsonNode described, Set<String> ops, MethodDescriptor own,
            String admin)
            throws Exception {
        String name = resource.name();
        String collection = resource.collection();
        MethodDescriptor create = own.getService().findMethodByName("Create" + name);
        MethodDescriptor get = own.getService().findMethodByName("Get" + name);
        MethodDescriptor update = own.getService().findMethodByName("Update" + name);
        MethodDescriptor delete = own.getService().findMethodByName("Delete" + name);
        MethodDescriptor list = listRpc(own);
        String field = update != null ? resourceField(update) : create != null ? resourceField(create)
                : get.getOutputType().getFields().get(0).getName();
        ObjectNode body = resource.create();
        ObjectNode path = body.deepCopy();
        List<FieldDescriptor> parents = parentIds(own.getInputType());
        String[] segments = collection.split("/");
        for (int i = 0; i < parents.size(); i++) {
            // the parent ids of the path, by the names the RPCs carry them under
            path.put(parents.get(i).getJsonName(), segments[4 + 2 * i]);
        }

        String id;
        String etag = "\"1\"";
        if (ops.contains("Create")) {
            Answer created = grpc(create, createRequest(create, field, body, path), admin);
            assertThat(created.status()).as("%s: %s", name, created.code()).isEqualTo(200);
            JsonNode row = created.body().path(json(create.getOutputType(), field));
            id = row.path("id").asString();
            etag = row.path("etag").asString();
            if (resource.duplicateConflicts()) {
                same(rest("POST", collection, admin, body.toString()), grpc(create, createRequest(create, field, body, path),
                        admin));
            }
            Answer invalid = rest("POST", collection, admin, "{}");
            if (invalid.status() >= 400) {
                same(invalid, grpc(create, createRequest(create, field, JSON.createObjectNode(), path), admin));
            }
            same(rest("POST", collection, NOBODY, body.toString()), grpc(create, createRequest(create, field, body, path),
                    NOBODY));
        } else {
            id = described.path("seedId").asString();
            fixtures(TENANT, strings(described.path("seed")));
            if (create != null) {
                Answer unserved = rest("POST", collection, admin, body.toString());
                assertThat(unserved.status()).as("%s is not created through the API", name).isEqualTo(501);
                same(unserved, grpc(create, createRequest(create, field, body, path), admin));
            }
        }

        if (get != null) {
            Answer read = rest("GET", collection + "/" + id, admin, null);
            Answer got = grpc(get, keyed(get, path, id).build(), admin);
            same(read, got);
            if (ops.contains("Get")) {
                assertThat(read.status()).as("%s: %s", name, read.code()).isEqualTo(200);
                GrpcParity.assertSameResource(read.body(), got.body().path(json(get.getOutputType(), field)));
                etag = read.body().path("etag").asString();
                same(rest("GET", collection + "/" + UUID.randomUUID(), admin, null),
                        grpc(get, keyed(get, path, UUID.randomUUID().toString()).build(), admin));
                same(rest("GET", collection + "/not-a-uuid", admin, null), grpc(get, keyed(get, path, "not-a-uuid").build(),
                        admin));
                same(rest("GET", collection + "/" + id, NOBODY, null), grpc(get, keyed(get, path, id).build(), NOBODY));
                same(rest("GET", collection + "/" + id, STRANGER, null), grpc(get, keyed(get, path, id).build(), STRANGER));
            } else {
                assertThat(read.status()).as("%s is not read through the API", name).isEqualTo(501);
            }
        }

        Answer firstPage = rest("GET", collection + "?size=1", admin, null);
        Answer firstPageGrpc = grpc(list, set(keyed(list, path, null), "pageable", pageable(list, 1)).build(), admin);
        same(firstPage, firstPageGrpc);
        if (ops.contains("List")) {
            assertThat(firstPage.status()).as("%s list: %s", name, firstPage.code()).isEqualTo(200);
            assertThat(ids(firstPageGrpc.body().path("items"))).isEqualTo(ids(firstPage.body().path("data"))).isNotEmpty();
            GrpcParity.assertSameResource(firstPage.body().path("data").get(0), firstPageGrpc.body().path("items").get(0));
            assertThat(firstPageGrpc.body().path("pagination").path("page").asInt())
                    .isEqualTo(firstPage.body().path("paging").path("page").asInt());
            same(rest("GET", collection + "?filter=nope%20%3D%201", admin, null),
                    grpc(list, set(keyed(list, path, null), "filter", "nope = 1").build(), admin));
            same(rest("GET", collection, NOBODY, null), grpc(list, keyed(list, path, null).build(), NOBODY));
        } else {
            assertThat(firstPage.status()).as("%s is not listed through the API", name).isEqualTo(501);
        }

        if (update != null) {
            ObjectNode changes = resource.update();
            String item = collection + "/" + id;
            if (ops.contains("Update")) {
                same(rest("PATCH", item, admin, changes.toString(), "If-Match", "\"99\""),
                        grpc(update, updateRequest(update, field, path, id, changes, "99"), admin));
                same(rest("PATCH", item, admin, changes.toString()),
                        grpc(update, updateRequest(update, field, path, id, changes, ""), admin));
                same(rest("PATCH", item, NOBODY, changes.toString(), "If-Match", etag),
                        grpc(update, updateRequest(update, field, path, id, changes, etag), NOBODY));
                Answer updated = grpc(update, updateRequest(update, field, path, id, changes, etag), admin);
                assertThat(updated.status()).as("%s update: %s", name, updated.code()).isEqualTo(200);
                JsonNode row = updated.body().path(json(update.getOutputType(), field));
                etag = row.path("etag").asString();
                if (ops.contains("Get")) {
                    GrpcParity.assertSameResource(rest("GET", item, admin, null).body(), row);
                }
            } else {
                Answer unserved = rest("PATCH", item, admin, changes.toString(), "If-Match", etag);
                assertThat(unserved.status()).as("%s is not updated through the API", name).isEqualTo(501);
                same(unserved, grpc(update, updateRequest(update, field, path, id, changes, etag), admin));
            }
        }

        if (delete != null) {
            String item = collection + "/" + id;
            if (ops.contains("Delete")) {
                same(rest("DELETE", item, admin, null, "If-Match", "\"99\""),
                        grpc(delete, set(keyed(delete, path, id), "etag", "99").build(), admin));
                same(rest("DELETE", item, admin, null), grpc(delete, keyed(delete, path, id).build(), admin));
                same(rest("DELETE", item, NOBODY, null, "If-Match", etag),
                        grpc(delete, set(keyed(delete, path, id), "etag", etag).build(), NOBODY));
                assertThat(grpc(delete, set(keyed(delete, path, id), "etag", etag).build(), admin).status())
                        .as("%s delete", name).isEqualTo(200);
                same(rest("DELETE", item, admin, null, "If-Match", etag),
                        grpc(delete, set(keyed(delete, path, id), "etag", etag).build(), admin));
            } else {
                Answer unserved = rest("DELETE", item, admin, null, "If-Match", etag);
                assertThat(unserved.status()).as("%s is not deleted through the API", name).isEqualTo(501);
                same(unserved, grpc(delete, set(keyed(delete, path, id), "etag", etag).build(), admin));
            }
        }
    }

    // ---- every RPC, asked for nothing

    /**
     * Every RPC of this part's services with a request that names nothing — unknown ids, an empty body, a stale etag —
     * against its REST operation called the same way: both refuse alike (404, 422, 428 …), both serve an empty list, and
     * an operation the release does not serve is UNIMPLEMENTED where REST answers 501. REST validates a body before it
     * reaches a controller, so it may answer 400 or 422 for the empty body of an operation it does not serve.
     */
    @TestFactory
    List<DynamicNode> everyRpcAnswersARequestForNothingLikeRest() throws Exception {
        Map<String, JsonNode> operations = new HashMap<>();
        JSON.readTree(getClass().getClassLoader().getResourceAsStream("META-INF/mysms/operations/lms.json"))
                .forEach(operation -> operations.put(operation.path("operationId").asString(), operation));
        List<DynamicNode> tests = new ArrayList<>();
        for (ServiceDescriptor service : LmsProto.getDescriptor().getServices()) {
            if (!SERVICES.contains(service.getName())) {
                continue;
            }
            for (MethodDescriptor method : service.getMethods()) {
                JsonNode operation = operations.get("lms." + method.getName());
                assertThat(operation).as("the operation of %s", method.getFullName()).isNotNull();
                tests.add(DynamicTest.dynamicTest(method.getName(), () -> askedForNothing(method, operation)));
            }
        }
        assertThat(tests).as("the RPCs of this part's services").hasSize(363);
        return tests;
    }

    private void askedForNothing(MethodDescriptor method, JsonNode operation) throws Exception {
        String verb = operation.path("method").asString();
        boolean keyed = "required".equals(operation.path("idempotency").asString());
        String path = operation.path("path").asString().replaceAll("\\{[^}]+}", UUID.randomUUID().toString());
        List<String> headers = new ArrayList<>();
        if ("PATCH".equals(verb) || "DELETE".equals(verb)) {
            headers.addAll(List.of("If-Match", "\"1\""));
        }
        if (keyed) {
            headers.addAll(List.of("Idempotency-Key", UUID.randomUUID().toString()));
        }
        if (path.endsWith(":webhook")) {
            // a provider callback's contract requires its signature headers; the signature itself is not checked
            // before the provider is known, so an unknown provider answers alike on both transports
            headers.addAll(List.of("X-Signature", "00", "X-Timestamp", "1"));
        }
        Answer overRest = rest(verb, path, ADMIN, "POST".equals(verb) || "PATCH".equals(verb) ? "{}" : null,
                headers.toArray(String[]::new));

        DynamicMessage.Builder request = request(method);
        for (FieldDescriptor field : request.getDescriptorForType().getFields()) {
            if (field.getJavaType() == FieldDescriptor.JavaType.STRING && !field.isRepeated()
                    && ("id".equals(field.getName()) || field.getName().endsWith("_id"))
                    && (keyed || !"request_id".equals(field.getName()))) {
                request.setField(field, UUID.randomUUID().toString());
            } else if ("etag".equals(field.getName())) {
                request.setField(field, "\"1\"");
            } else if (method.getName().startsWith("Update") && field.getJavaType() == FieldDescriptor.JavaType.MESSAGE
                    && field.getMessageType().getFile().equals(LmsProto.getDescriptor())) {
                Descriptor resource = field.getMessageType();
                request.setField(field, DynamicMessage.newBuilder(resource)
                        .setField(resource.findFieldByName("id"), UUID.randomUUID().toString())
                        .setField(resource.findFieldByName("etag"), "\"1\"").build());
            }
        }
        Answer overGrpc = grpc(method, request.build(), ADMIN);
        if (overGrpc.status() == 501) {
            assertThat(overGrpc.code()).isEqualTo("NOT_IMPLEMENTED");
            assertThat(overRest.status()).as("REST %s %s: %s", verb, path, overRest.code()).isIn(501, 400, 422);
        } else if (overRest.status() == 422 && overGrpc.status() == 404) {
            // this sweep fills the id fields of the gRPC request but sends REST an empty body, so an operation
            // whose only required body fields are ids refuses the emptier REST request (422 NotNull) where the
            // gRPC one reaches the unknown row (404) — alike for what each request actually carried
            overRest.body().path("errors").forEach(error ->
                    assertThat(error.path("code").asString()).as("%s %s", verb, path).isEqualTo("NotNull"));
        } else {
            same(overRest, overGrpc);
        }
    }

    // ---- the workflow

    @Test
    void rejectingAnEnrollmentRequestAnswersLikeRest() throws Exception {
        fixtures(TENANT, List.of(COURSE_FIXTURE));
        String collection = "/v1/lms/courses/" + COURSE + "/enrollment-requests";
        String[] ids = new String[3];
        for (int i = 0; i < ids.length; i++) {
            // each request is its own student's: the requester comes from the token
            Answer made = rest("POST", collection, token(TENANT, List.of("lms.*")), "{\"courseId\":\"" + COURSE + "\"}");
            assertThat(made.status()).as(made.body().toString()).isEqualTo(201);
            ids[i] = made.body().path("id").asString();
        }
        fixtures(TENANT, List.of("UPDATE lms.enrollment_request SET status = 'pending' WHERE tenant_id = :tenant"));
        MethodDescriptor reject = rpc("RejectEnrollmentRequest");
        String reason = "{\"reason\":\"Parity reason\"}";

        // refusals: without the permission, an absent row, a malformed id, a stale etag
        same(rest("POST", collection + "/" + ids[0] + ":reject", NOBODY, reason), grpc(reject, rejection(reject, ids[0]).build(),
                NOBODY));
        String absent = UUID.randomUUID().toString();
        same(rest("POST", collection + "/" + absent + ":reject", ADMIN, reason), grpc(reject, rejection(reject, absent).build(),
                ADMIN));
        same(rest("POST", collection + "/not-a-uuid:reject", ADMIN, reason), grpc(reject, rejection(reject, "not-a-uuid").build(),
                ADMIN));
        same(rest("POST", collection + "/" + ids[0] + ":reject", STRANGER, reason),
                grpc(reject, rejection(reject, ids[0]).build(), STRANGER));
        Answer staleRest = rest("POST", collection + "/" + ids[0] + ":reject", ADMIN,
                "{\"reason\":\"Parity reason\",\"etag\":\"\\\"99\\\"\"}");
        assertThat(staleRest.status()).isEqualTo(412);
        same(staleRest, grpc(reject, set(rejection(reject, ids[0]), "etag", "\"99\"").build(), ADMIN));

        // the transition: one row over each transport, the same resource
        Answer rejectedRest = rest("POST", collection + "/" + ids[0] + ":reject", ADMIN, reason);
        Answer rejectedGrpc = grpc(reject, rejection(reject, ids[1]).build(), ADMIN);
        assertThat(rejectedRest.status()).as(rejectedRest.body().toString()).isEqualTo(200);
        assertThat(rejectedGrpc.status()).as(rejectedGrpc.code()).isEqualTo(200);
        JsonNode row = rejectedGrpc.body().path("enrollmentRequest");
        assertThat(row.path("status").asString()).isEqualTo("STATUS_REJECTED");
        assertThat(row.path("decidedAt").asString()).isNotEmpty();
        GrpcParity.assertSameResource(rest("GET", collection + "/" + ids[1], ADMIN, null).body(), row);

        // the guard: a rejected request is not rejected again, 409 on both
        Answer again = rest("POST", collection + "/" + ids[1] + ":reject", ADMIN, reason);
        assertThat(again.status()).isEqualTo(409);
        same(again, grpc(reject, rejection(reject, ids[1]).build(), ADMIN));
        same(again, grpc(reject, rejection(reject, ids[0]).build(), ADMIN));
    }

    /** A keyed request runs once: the retry with the same request_id is answered with the kept answer, not the guard. */
    @Test
    void aRetriedRejectionIsReplayedOnBoth() throws Exception {
        fixtures(TENANT, List.of(COURSE_FIXTURE));
        String collection = "/v1/lms/courses/" + COURSE + "/enrollment-requests";
        String[] ids = new String[2];
        for (int i = 0; i < ids.length; i++) {
            // each request is its own student's: the requester comes from the token
            Answer made = rest("POST", collection, token(TENANT, List.of("lms.*")), "{\"courseId\":\"" + COURSE + "\"}");
            assertThat(made.status()).as(made.body().toString()).isEqualTo(201);
            ids[i] = made.body().path("id").asString();
            fixtures(TENANT, List.of("UPDATE lms.enrollment_request SET status = 'pending' WHERE id = '" + ids[i] + "'"));
        }
        MethodDescriptor reject = rpc("RejectEnrollmentRequest");
        String key = UUID.randomUUID().toString();
        DynamicMessage keyed = set(rejection(reject, ids[0]), "request_id", key).build();
        Answer first = grpc(reject, keyed, ADMIN);
        Answer retried = grpc(reject, keyed, ADMIN);
        assertThat(first.status()).as(first.code()).isEqualTo(200);
        assertThat(retried.status()).as("the retry is replayed, not refused by the guard").isEqualTo(200);
        assertThat(retried.body()).isEqualTo(first.body());
        assertThat(grpc(reject, rejection(reject, ids[0]).build(), ADMIN).status()).as("without the key: the guard")
                .isEqualTo(409);

        String restKey = UUID.randomUUID().toString();
        String path = collection + "/" + ids[1] + ":reject";
        Answer firstRest = rest("POST", path, ADMIN, "{\"reason\":\"Parity reason\"}", "Idempotency-Key", restKey);
        Answer retriedRest = rest("POST", path, ADMIN, "{\"reason\":\"Parity reason\"}", "Idempotency-Key", restKey);
        same(firstRest, first);
        same(retriedRest, retried);
        assertThat(retriedRest.body()).isEqualTo(firstRest.body());

        // the key reused for another request: 422 IDEMPOTENCY_KEY_REUSED on both
        same(rest("POST", path, ADMIN, "{\"reason\":\"Another reason\"}", "Idempotency-Key", restKey),
                grpc(reject, set(set(rejection(reject, ids[0]), "reason", "Another reason"), "request_id", key).build(), ADMIN));
    }

    private static DynamicMessage.Builder rejection(MethodDescriptor reject, String id) {
        return set(set(set(request(reject), "course_id", COURSE), "enrollment_request_id", id), "reason", "Parity reason");
    }

    // ---- building requests from REST bodies

    /** The request's field that holds the resource: its one message field of the contract's own package. */
    private static String resourceField(MethodDescriptor rpc) {
        for (FieldDescriptor field : rpc.getInputType().getFields()) {
            if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE
                    && field.getMessageType().getFile().equals(LmsProto.getDescriptor())) {
                return field.getName();
            }
        }
        throw new IllegalStateException(rpc.getFullName() + " carries no resource");
    }

    private static String json(Descriptor type, String field) {
        return type.findFieldByName(field).getJsonName();
    }

    /** A create request: the resource converted from the REST body like a DTO, the path ids from {@code path}. */
    @SuppressWarnings("unchecked")
    private DynamicMessage createRequest(MethodDescriptor create, String field, ObjectNode body, ObjectNode path) {
        return withPathIds(bridge.reply(request(create), field, JSON.convertValue(body, Map.class)), path).build();
    }

    /** A request keyed by the row id (none for a list), with the parent ids a nested resource's path carries. */
    private static DynamicMessage.Builder keyed(MethodDescriptor rpc, ObjectNode body, String id) {
        DynamicMessage.Builder request = withPathIds(request(rpc), body);
        return id == null ? request : set(request, "id", id);
    }

    /** The parent ids a nested resource's request carries beside the row's own. */
    private static List<FieldDescriptor> parentIds(Descriptor request) {
        return request.getFields().stream().filter(field -> field.getName().endsWith("_id")
                && !"request_id".equals(field.getName()) && field.getJavaType() == FieldDescriptor.JavaType.STRING).toList();
    }

    private static DynamicMessage.Builder withPathIds(DynamicMessage.Builder request, ObjectNode body) {
        for (FieldDescriptor field : request.getDescriptorForType().getFields()) {
            if (field.getName().endsWith("_id") && !"request_id".equals(field.getName()) && body.has(field.getJsonName())) {
                request.setField(field, body.path(field.getJsonName()).asString());
            }
        }
        return request;
    }

    /** An update request: the row's id and etag in the resource, the changes, and a mask naming them. */
    @SuppressWarnings("unchecked")
    private DynamicMessage updateRequest(MethodDescriptor update, String field, ObjectNode body, String id,
                                         ObjectNode changes, String etag) {
        ObjectNode resource = changes.deepCopy();
        resource.put("id", id);
        resource.put("etag", etag);
        DynamicMessage.Builder request = withPathIds(bridge.reply(request(update), field,
                JSON.convertValue(resource, Map.class)), body);
        FieldMask.Builder mask = FieldMask.newBuilder();
        Descriptor type = update.getInputType().findFieldByName(field).getMessageType();
        changes.propertyNames().forEach(name -> type.getFields().stream()
                .filter(f -> f.getJsonName().equals(name)).forEach(f -> mask.addPaths(f.getName())));
        return set(request, "update_mask", mask.build()).build();
    }

    private static DynamicMessage pageable(MethodDescriptor list, int size) {
        Descriptor type = list.getInputType().findFieldByName("pageable").getMessageType();
        return DynamicMessage.newBuilder(type).setField(type.findFieldByName("size"), size).build();
    }

    private static List<String> ids(JsonNode items) {
        List<String> ids = new ArrayList<>();
        items.forEach(item -> ids.add(item.path("id").asString()));
        return ids;
    }
}
