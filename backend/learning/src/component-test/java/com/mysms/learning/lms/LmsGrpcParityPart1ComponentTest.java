package com.mysms.learning.lms;

import com.mysms.learning.LearningApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
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
import com.mysms.learning.lms.config.LmsDatabaseConfig;
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
 * driven through its guard, its refusals and a request_id replay. The updates of resources outside crud-spec.json (a
 * cohort's activity, a lesson note, the tenant's settings) are compared by hand, with the kit's three PATCH states.
 */
@SpringBootTest(classes = LearningApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
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
        JsonNode spec = JSON.readTree(getClass().getClassLoader().getResourceAsStream("lms/crud-spec.json"));
        List<GrpcParityContract.Resource> resources = GrpcParityContract.resources("lms/crud-spec.json");
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
        String field = update != null ? update.getOutputType().getFields().get(0).getName()
                : create != null ? resourceField(create)
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
                        grpc(update, updateRequest(update, path, id, changes, "99"), admin));
                same(rest("PATCH", item, admin, changes.toString()),
                        grpc(update, updateRequest(update, path, id, changes, ""), admin));
                same(rest("PATCH", item, NOBODY, changes.toString(), "If-Match", etag),
                        grpc(update, updateRequest(update, path, id, changes, etag), NOBODY));
                Answer updated = grpc(update, updateRequest(update, path, id, changes, etag), admin);
                assertThat(updated.status()).as("%s update: %s", name, updated.code()).isEqualTo(200);
                JsonNode row = updated.body().path(json(update.getOutputType(), field));
                if (ops.contains("Get")) {
                    GrpcParity.assertSameResource(rest("GET", item, admin, null).body(), row);
                }
                etag = contract(admin).patchStates(resource, id, row.path("etag").asString());
            } else {
                Answer unserved = rest("PATCH", item, admin, changes.toString(), "If-Match", etag);
                assertThat(unserved.status()).as("%s is not updated through the API", name).isEqualTo(501);
                same(unserved, grpc(update, updateRequest(update, path, id, changes, etag), admin));
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
        boolean patch = method.getInputType().getName().endsWith("Update"); // its optional ids are changes, not keys
        for (FieldDescriptor field : request.getDescriptorForType().getFields()) {
            if (field.getJavaType() == FieldDescriptor.JavaType.STRING && !field.isRepeated()
                    && !(patch && field.hasPresence())
                    && ("id".equals(field.getName()) || field.getName().endsWith("_id"))
                    && (keyed || !"request_id".equals(field.getName()))) {
                request.setField(field, UUID.randomUUID().toString());
            } else if ("etag".equals(field.getName())) {
                request.setField(field, "\"1\"");
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

    // ---- updates of resources outside crud-spec.json (ARC-9 review T1)

    private static final String INSTITUTION = "0196f000-0000-7000-8000-00000000a001";

    /** A cohort's activity points at another quiz alike over both, and passes the three PATCH states over gRPC. */
    @Test
    void aCohortActivityIsUpdatedAlikeOverBoth() throws Exception {
        String cohort = UUID.randomUUID().toString();
        String quiz = UUID.randomUUID().toString();
        String other = UUID.randomUUID().toString();
        fixtures(TENANT, List.of("INSERT INTO lms.cohort (tenant_id, institution_id, title, start_date, end_date, "
                + "timezone, row_version, id) VALUES (:tenant, '" + INSTITUTION + "', 'Parity cohort', '2026-01-10', "
                + "'2026-01-20', 'UTC', 1, '" + cohort + "')", quiz(quiz), quiz(other)));
        String collection = "/v1/lms/cohorts/" + cohort + "/activities";
        Answer created = rest("POST", collection, ADMIN, "{\"quizId\":\"" + quiz + "\"}");
        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
        GrpcParityContract.Resource resource = handled("CohortActivity", collection,
                "{\"quizId\":\"" + other + "\"}", null, "assignmentId");
        String id = created.body().path("id").asString();
        String etag = updatedAlike(resource, id, created.body().path("etag").asString());
        contract(ADMIN).patchStates(resource, id, etag);
    }

    /**
     * A note is changed by its writer alike over both; a required color sent UNSPECIFIED is refused like REST's
     * null, and clearing the highlight keeps the note.
     */
    @Test
    void aLessonNoteIsUpdatedAlikeOverBoth() throws Exception {
        String course = UUID.randomUUID().toString();
        String chapter = UUID.randomUUID().toString();
        String lesson = UUID.randomUUID().toString();
        fixtures(TENANT, List.of("INSERT INTO lms.course (tenant_id, institution_id, title, row_version, id) VALUES "
                + "(:tenant, '" + INSTITUTION + "', 'Notes course', 1, '" + course + "')",
                "INSERT INTO lms.chapter (tenant_id, course_id, title, sort_order, row_version, id) VALUES (:tenant, '"
                        + course + "', 'Notes chapter', 1, 1, '" + chapter + "')",
                "INSERT INTO lms.lesson (tenant_id, chapter_id, title, sort_order, row_version, id) VALUES (:tenant, '"
                        + chapter + "', 'Notes lesson', 1, 1, '" + lesson + "')"));
        String collection = "/v1/lms/lessons/" + lesson + "/notes";
        Answer created = rest("POST", collection, ADMIN,
                "{\"note\":\"Remember this\",\"highlightedText\":\"A line\",\"color\":\"green\"}");
        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
        GrpcParityContract.Resource resource = handled("LessonNote", collection, "{\"note\":\"Remember that\"}",
                "color", "highlightedText");
        String id = created.body().path("id").asString();
        String etag = updatedAlike(resource, id, created.body().path("etag").asString());
        contract(ADMIN).patchStates(resource, id, etag);
    }

    /**
     * The tenant's settings singleton over gRPC as over REST: a stale, missing or unpermitted update is refused alike;
     * the first save writes version 1 and returns only the mask of the write-only image-search key; an empty message
     * changes nothing; leaving the key out keeps it, and a wrapper holding null removes it.
     */
    @Test
    void theSettingsSingletonIsUpdatedAlikeOverBothAndKeepsItsKeyWriteOnly() throws Exception {
        String admin = token(UUID.randomUUID(), List.of("lms.*"));
        MethodDescriptor update = LmsProto.getDescriptor().findServiceByName("SettingsService")
                .findMethodByName("UpdateSettings");
        String settings = "/v1/lms/settings";
        ObjectNode first = (ObjectNode) JSON.readTree("{\"disableSignup\":false,\"unsplashAccessKey\":\"key-123456\"}");
        same(rest("PATCH", settings, admin, first.toString(), "If-Match", "\"7\""),
                grpc(update, settings(update, first, "7"), admin));
        same(rest("PATCH", settings, admin, first.toString()), grpc(update, settings(update, first, ""), admin));
        same(rest("PATCH", settings, NOBODY, first.toString(), "If-Match", "\"0\""),
                grpc(update, settings(update, first, "0"), NOBODY));

        JsonNode saved = settingsRow(grpc(update, settings(update, first, "0"), admin));
        GrpcParity.assertSameResource(rest("GET", settings, admin, null).body(), saved);
        assertThat(saved.path("etag").asString()).isEqualTo("\"1\"");
        assertThat(saved.path("disableSignup").asBoolean()).isFalse();
        assertThat(saved.path("unsplashAccessKey").isMissingNode()).as("the plaintext never returns").isTrue();
        assertThat(saved.path("unsplashAccessKeyMasked").asString()).endsWith("3456");

        JsonNode unchanged = settingsRow(grpc(update, settings(update, JSON.createObjectNode(), "\"1\""), admin));
        assertThat(unchanged.path("etag").asString()).as("an empty update changes nothing").isEqualTo("\"1\"");
        GrpcParity.assertSameResource(rest("GET", settings, admin, null).body(), unchanged);

        JsonNode kept = settingsRow(grpc(update, settings(update, (ObjectNode) JSON.readTree(
                "{\"disableSignup\":true}"), "\"1\""), admin));
        assertThat(kept.path("disableSignup").asBoolean()).isTrue();
        assertThat(kept.path("unsplashAccessKeyMasked").asString()).as("an absent key keeps the stored one")
                .endsWith("3456");

        JsonNode removed = settingsRow(grpc(update, settings(update, (ObjectNode) JSON.readTree(
                "{\"unsplashAccessKey\":null}"), kept.path("etag").asString()), admin));
        assertThat(removed.path("unsplashAccessKeyMasked").isMissingNode()).as("null removes the key").isTrue();
        assertThat(removed.path("etag").asString()).isEqualTo("\"3\"");
        GrpcParity.assertSameResource(rest("GET", settings, admin, null).body(), removed);
    }

    /** A quiz of the parity institution. */
    private static String quiz(String id) {
        return "INSERT INTO lms.quiz (tenant_id, title, passing_percentage, total_marks, institution_id, row_version, id)"
                + " VALUES (:tenant, 'Parity quiz', 1, 2, '" + INSTITUTION + "', 1, '" + id + "')";
    }

    /** A resource outside crud-spec.json for the kit's update steps: its collection, update body and PATCH entry. */
    private static GrpcParityContract.Resource handled(String name, String collection, String update, String keeps,
                                                       String clears) {
        ObjectNode described = JSON.createObjectNode();
        List.of("Create", "Get", "List", "Update", "Delete").forEach(described.putArray("ops")::add);
        ObjectNode patch = described.putObject("patch").put("clears", clears);
        if (keeps != null) {
            patch.put("keeps", keeps);
        }
        return new GrpcParityContract.Resource(name, collection, JSON.createObjectNode(),
                (ObjectNode) JSON.readTree(update), false, described);
    }

    /**
     * The update of a row at {@code etag} refused alike for a stale etag, none and a caller without the permission,
     * then applied over gRPC and read back alike over REST; the row's new ETag.
     */
    private String updatedAlike(GrpcParityContract.Resource resource, String id, String etag) throws Exception {
        GrpcParityContract kit = contract(ADMIN);
        MethodDescriptor update = kit.rpc("Update" + resource.name());
        String item = resource.collection() + "/" + id;
        ObjectNode changes = resource.update();
        same(rest("PATCH", item, ADMIN, changes.toString(), "If-Match", "\"99\""),
                grpc(update, kit.updateRequest(update, resource, id, changes, "99"), ADMIN));
        same(rest("PATCH", item, ADMIN, changes.toString()),
                grpc(update, kit.updateRequest(update, resource, id, changes, ""), ADMIN));
        same(rest("PATCH", item, NOBODY, changes.toString(), "If-Match", etag),
                grpc(update, kit.updateRequest(update, resource, id, changes, etag), NOBODY));
        Answer updated = grpc(update, kit.updateRequest(update, resource, id, changes, etag), ADMIN);
        assertThat(updated.status()).as("%s update: %s", resource.name(), updated.code()).isEqualTo(200);
        JsonNode row = updated.body().properties().iterator().next().getValue();
        GrpcParity.assertSameResource(rest("GET", item, ADMIN, null).body(), row);
        changes.properties().forEach(change -> assertThat(row.path(change.getKey()).asString()).as(change.getKey())
                .isEqualTo(change.getValue().asString()));
        return row.path("etag").asString();
    }

    /** An UpdateSettings message: the changes of a REST PATCH body and the etag. */
    private DynamicMessage settings(MethodDescriptor update, ObjectNode changes, String etag) {
        return set(contract(ADMIN).changes(update, changes), "etag", etag).build();
    }

    /** The settings an UpdateSettings answer carries. */
    private static JsonNode settingsRow(Answer answer) {
        assertThat(answer.status()).as("UpdateSettings: %s", answer.code()).isEqualTo(200);
        return answer.body().path("settings");
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
        return request.getFields().stream().filter(LmsGrpcParityPart1ComponentTest::parentId).toList();
    }

    /**
     * Whether a request field is a parent id the REST path carries: a plain string key, never a field an update
     * message changes (those have presence or are wrappers).
     */
    private static boolean parentId(FieldDescriptor field) {
        return field.getName().endsWith("_id") && !"request_id".equals(field.getName())
                && field.getJavaType() == FieldDescriptor.JavaType.STRING && !field.hasPresence();
    }

    private static DynamicMessage.Builder withPathIds(DynamicMessage.Builder request, ObjectNode body) {
        for (FieldDescriptor field : request.getDescriptorForType().getFields()) {
            if (parentId(field) && body.has(field.getJsonName())) {
                request.setField(field, body.path(field.getJsonName()).asString());
            }
        }
        return request;
    }

    /** An update request: the {@code <Resource>Update} message of a REST PATCH body, with its parent ids, id and etag. */
    private DynamicMessage updateRequest(MethodDescriptor update, ObjectNode path, String id, ObjectNode changes,
                                         String etag) {
        DynamicMessage.Builder request = withPathIds(contract(ADMIN).changes(update, changes), path);
        set(request, "id", id);
        return set(request, "etag", etag).build();
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
