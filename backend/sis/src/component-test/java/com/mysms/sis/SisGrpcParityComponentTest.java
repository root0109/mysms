package com.mysms.sis;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;
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
import com.mysms.sis.config.SisDatabaseConfig;
import com.mysms.sis.v1.SisProto;
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
import java.util.Locale;
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
 * Every RPC of sis's fourteen gRPC services against the same operation over REST, on real PostgreSQL: the standard
 * operations and state transitions give the same resource and the same refusals on both transports, and the workflow
 * operations answer alike, including the 503 of the parts that need a service this test does not run (iam, academics).
 */
@SpringBootTest(classes = SisApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=SisGrpcParityComponentTest")
class SisGrpcParityComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final String ADMIN = token(TENANT, null, List.of("sis.*"));
    private static final String NOBODY = token(TENANT, null, List.of());
    private static final String STRANGER = token(OTHER_TENANT, null, List.of("sis.*"));
    /** The tenant of the workflow checks, so their rows never meet the standard operations' rows. */
    private static final UUID WORKFLOW_TENANT = UUID.randomUUID();
    private static final String INSTITUTION = "0196f000-0000-7000-8000-00000000a001";
    private static final String OFFICE = token(WORKFLOW_TENANT, null, List.of("sis.*"));
    private static final String NO_PERMISSION = token(WORKFLOW_TENANT, null, List.of());
    private static final String STUDENTS = "/v1/sis/students/";
    private static final String YEAR = "0196f000-0000-7000-8000-00000000b001";
    private static final String CLASS_SECTION = "0196f000-0000-7000-8000-00000000b002";
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;


    @Autowired
    private ProtoBridge bridge;

    @Autowired
    @Qualifier(SisDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "sis");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @AfterAll
    static void closeChannel() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    /** A tenant-wide caller holding the permissions, linked to the person when one is given (the {@code pid} claim). */
    private static String token(UUID tenant, UUID person, List<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", UUID.randomUUID().toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", permissions);
        claims.put("tperms", permissions);
        claims.put("ins", List.of("*"));
        if (person != null) {
            claims.put("pid", person.toString());
        }
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
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

    private Channel channel() {
        if (channel == null) {
            channel = GrpcParity.channel("SisGrpcParityComponentTest");
        }
        return channel;
    }

    /** Calls an RPC with the request given as protobuf JSON (lowerCamelCase names, enum value names). */
    private Answer grpc(String rpc, String token, String requestJson) throws Exception {
        MethodDescriptor method = contract().rpc(rpc);
        DynamicMessage.Builder request = GrpcParityContract.request(method);
        JsonFormat.parser().merge(requestJson, request);
        return GrpcParity.call(channel(), method, request.build(), token);
    }

    private void fixtures(UUID tenant, List<String> statements) {
        CallContext context = CallContext.anonymous(TRACE).withIdentity(tenant, null);
        CallContextHolder.callAs(context, () -> TestTransactions.of(database).write(() -> {
            statements.forEach(statement -> database.jdbc().sql(statement).param("tenant", tenant).update());
            return null;
        }));
    }

    // ------------------------------------------------------------------ the standard operations, resource by resource

    private GrpcParityContract contract() {
        return new GrpcParityContract(SisProto.getDescriptor(), this::rest, this::channel, bridge,
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

    // ------------------------------------------------------------------ the state transitions of crud-spec.json

    @TestFactory
    List<DynamicNode> everyStateTransitionAnswersLikeRest() throws Exception {
        JsonNode spec = JSON.readTree(getClass().getClassLoader().getResourceAsStream("crud-spec.json"));
        List<DynamicNode> tests = new ArrayList<>();
        for (JsonNode resource : spec.path("resources")) {
            if (resource.path("transitions").isEmpty()) {
                continue;
            }
            tests.add(DynamicTest.dynamicTest(resource.path("name").asString() + " transitions", () -> {
                List<String> fixtures = new ArrayList<>();
                resource.path("fixtures").forEach(statement -> fixtures.add(statement.asString()));
                fixtures(TENANT, fixtures);
                Answer created = rest("POST", resource.path("collection").asString(), ADMIN,
                        distinct((ObjectNode) resource.path("create")).toString());
                assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
                String collection = resource.path("collection").asString();
                String id = created.body().path("id").asString();
                for (JsonNode transition : resource.path("transitions")) {
                    transition(collection, id, transition);
                }
                // the row goes, so the standard operations can create theirs in the same slot (subject registrations)
                rest("DELETE", collection + "/" + id, ADMIN, null, "If-Match",
                        rest("GET", collection + "/" + id, ADMIN, null).body().path("etag").asString());
            }));
        }
        return tests;
    }

    /**
     * The create body with its natural keys changed, so the row never meets the one the standard operations create
     * from the same body in the same tenant (an application number is unique per institution across tenants).
     */
    private static ObjectNode distinct(ObjectNode body) {
        ObjectNode copy = body.deepCopy();
        for (String key : List.of("code", "name", "applicationNumber")) {
            if (copy.has(key)) {
                String value = copy.path(key).asString();
                copy.put(key, value.substring(0, value.length() - 1) + (value.endsWith("T") ? "U" : "T"));
            }
        }
        return copy;
    }

    /**
     * One transition of a row over both transports: refused alike without the permission, with a stale etag, without
     * the etag where If-Match is required, and for an absent row; done over gRPC it leaves the row REST then reads;
     * and a repeat is answered alike.
     */
    private void transition(String collection, String id, JsonNode transition) throws Exception {
        String name = transition.path("operation").asString();
        String rpc = name.substring(name.indexOf('.') + 1);
        MethodDescriptor method = contract().rpc(rpc);
        String idField = method.getInputType().getFields().get(0).getJsonName();
        String resourceField = method.getOutputType().getFields().get(0).getJsonName();
        boolean ifMatch = transition.path("ifMatch").asBoolean();
        String path = transition.path("path").asString();
        List<String> prepare = new ArrayList<>();
        transition.path("prepare").forEach(statement -> prepare.add(statement.asString().replace("{id}", id)));
        fixtures(TENANT, prepare);

        Step step = new Step(rpc, path, idField, (ObjectNode) transition.path("body"), ifMatch);
        GrpcParityContract.assertSameAnswer(step.rest(id, NOBODY, null), step.grpc(id, NOBODY, null));
        GrpcParityContract.assertSameAnswer(step.rest(id, ADMIN, "\"99\""), step.grpc(id, ADMIN, "\"99\""));
        String absent = UUID.randomUUID().toString();
        String etag = rest("GET", collection + "/" + id, ADMIN, null).body().path("etag").asString();
        GrpcParityContract.assertSameAnswer(step.rest(absent, ADMIN, etag), step.grpc(absent, ADMIN, etag));
        if (ifMatch) {
            Answer missing = step.rest(id, ADMIN, null);
            assertThat(missing.status()).isEqualTo(428);
            GrpcParityContract.assertSameAnswer(missing, step.grpc(id, ADMIN, null));
        }

        Answer done = step.grpc(id, ADMIN, ifMatch ? etag : null);
        assertThat(done.status()).as("%s: %s", name, done.code()).isEqualTo(200);
        Answer read = rest("GET", collection + "/" + id, ADMIN, null);
        transition.path("sets").properties().forEach(set -> assertThat(read.body().path(set.getKey()))
                .as("%s sets %s", name, set.getKey()).isEqualTo(set.getValue()));
        GrpcParity.assertSameResource(read.body(), done.body().path(resourceField));

        Answer again = step.rest(id, ADMIN, ifMatch ? read.body().path("etag").asString() : null);
        String current = rest("GET", collection + "/" + id, ADMIN, null).body().path("etag").asString();
        GrpcParityContract.assertSameAnswer(again, step.grpc(id, ADMIN, ifMatch ? current : null));
    }

    /** A workflow step as each transport takes it: the etag in the body, or as If-Match / the etag field. */
    private final class Step {

        private final String rpc;
        private final String path;
        private final String idField;
        private final ObjectNode body;
        private final boolean ifMatch;

        Step(String rpc, String path, String idField, ObjectNode body, boolean ifMatch) {
            this.rpc = rpc;
            this.path = path;
            this.idField = idField;
            this.body = body;
            this.ifMatch = ifMatch;
        }

        Answer rest(String id, String token, String etag) throws Exception {
            ObjectNode sent = body.deepCopy();
            if (etag != null && !ifMatch) {
                sent.put("etag", etag);
            }
            String target = path.replace("{id}", id);
            return etag != null && ifMatch
                    ? SisGrpcParityComponentTest.this.rest("POST", target, token, sent.toString(), "If-Match", etag)
                    : SisGrpcParityComponentTest.this.rest("POST", target, token, sent.toString());
        }

        Answer grpc(String id, String token, String etag) throws Exception {
            ObjectNode sent = body.deepCopy();
            sent.put(idField, id);
            if (etag != null) {
                sent.put("etag", etag);
            }
            return SisGrpcParityComponentTest.this.grpc(rpc, token, sent.toString());
        }
    }

    // ------------------------------------------------------------------ long-running operations

    @Test
    void operationsAreReadAndCancelledTheSameOverBoth() throws Exception {
        UUID running = UUID.randomUUID();
        UUID finished = UUID.randomUUID();
        String insert = "INSERT INTO sis.operation (id, tenant_id, operation_name, status, progress_percent,"
                + " started_by_user_id, started_at, finished_at, created_at, updated_at, row_version) VALUES ('%s', :tenant,"
                + " '%s', '%s', %s, '" + UUID.randomUUID() + "', now() - interval '%d minutes', %s, now(), now(), 1)";
        fixtures(WORKFLOW_TENANT, List.of(insert.formatted(running, "report.run", "running", "40", 3, "NULL"),
                insert.formatted(finished, "import.rows", "succeeded", "100", 1, "now()")));
        String operations = "/v1/sis/operations";

        Answer listed = rest("GET", operations, OFFICE, null);
        Answer listedGrpc = grpc("ListOperations", OFFICE, "{}");
        assertThat(listed.status()).as(listed.body().toString()).isEqualTo(200);
        assertThat(listedGrpc.status()).as(listedGrpc.code()).isEqualTo(200);
        assertThat(listed.body().path("data")).hasSize(2);
        assertThat(listedGrpc.body().path("items")).hasSize(2);
        for (int i = 0; i < 2; i++) {
            GrpcParity.assertSameResource(listed.body().path("data").get(i), listedGrpc.body().path("items").get(i));
        }
        Answer byKind = grpc("ListOperations", OFFICE, "{\"kind\":\"import.rows\",\"pageable\":{\"size\":1}}");
        assertThat(byKind.body().path("items")).hasSize(1);
        assertThat(byKind.body().path("items").get(0).path("operationId").asString()).isEqualTo(finished.toString());
        GrpcParityContract.assertSameAnswer(rest("GET", operations, NO_PERMISSION, null),
                grpc("ListOperations", NO_PERMISSION, "{}"));

        String keyed = "{\"operationId\":\"%s\"}";
        GrpcParity.assertSameResource(rest("GET", operations + "/" + running, OFFICE, null).body(),
                grpc("GetOperation", OFFICE, keyed.formatted(running)).body().path("result"));
        for (String id : List.of(UUID.randomUUID().toString(), "not-a-uuid")) {
            GrpcParityContract.assertSameAnswer(rest("GET", operations + "/" + id, OFFICE, null),
                    grpc("GetOperation", OFFICE, keyed.formatted(id)));
            GrpcParityContract.assertSameAnswer(rest("POST", operations + "/" + id + ":cancel", OFFICE, null),
                    grpc("CancelOperation", OFFICE, keyed.formatted(id)));
        }
        GrpcParityContract.assertSameAnswer(rest("POST", operations + "/" + running + ":cancel", NO_PERMISSION, null),
                grpc("CancelOperation", NO_PERMISSION, keyed.formatted(running)));
        GrpcParityContract.assertSameAnswer(rest("POST", operations + "/" + finished + ":cancel", OFFICE, null),
                grpc("CancelOperation", OFFICE, keyed.formatted(finished)));
        Answer cancelled = grpc("CancelOperation", OFFICE, keyed.formatted(running));
        assertThat(cancelled.status()).as(cancelled.code()).isEqualTo(200);
        assertThat(cancelled.body().path("result").path("status").asString()).isEqualTo("cancelled");
        GrpcParity.assertSameResource(rest("GET", operations + "/" + running, OFFICE, null).body(),
                cancelled.body().path("result"));
        GrpcParityContract.assertSameAnswer(rest("POST", operations + "/" + running + ":cancel", OFFICE, null),
                grpc("CancelOperation", OFFICE, keyed.formatted(running)));
    }

    // ------------------------------------------------------------------ the workflow operations

    /** An active student of the school placed in the class of the year; answers the id. */
    private String student(String admissionNumber) {
        String id = UUID.randomUUID().toString();
        fixtures(WORKFLOW_TENANT, List.of("INSERT INTO sis.student (id, tenant_id, person_id, institution_id,"
                + " admission_number, admission_date, row_version) VALUES ('" + id + "', :tenant, '" + UUID.randomUUID()
                + "', '" + INSTITUTION + "', '" + admissionNumber + "', DATE '2020-06-01', 1)",
                "INSERT INTO sis.student_enrollment (id, tenant_id, student_id, academic_year_id, class_section_id,"
                + " effective_from, enrolled_on, row_version) VALUES ('" + UUID.randomUUID() + "', :tenant, '" + id
                + "', '" + YEAR + "', '" + CLASS_SECTION + "', DATE '2026-04-01', DATE '2026-04-01', 1)"));
        return id;
    }

    /** A guardian linked to the person, with an active link to each student; answers the id. */
    private String guardian(UUID person, String... students) {
        String id = UUID.randomUUID().toString();
        String relationship = UUID.randomUUID().toString();
        List<String> statements = new ArrayList<>(List.of("INSERT INTO sis.relationship_type (id, tenant_id, code, name,"
                + " row_version) VALUES ('" + relationship + "', :tenant, 'R" + relationship.substring(0, 8) + "', 'Mother "
                + relationship.substring(0, 8) + "', 1)", "INSERT INTO sis.guardian (id, tenant_id, person_id, email,"
                + " row_version) VALUES ('" + id + "', :tenant, '" + person + "', 'g" + id.substring(0, 8)
                + "@example.test', 1)"));
        for (String student : students) {
            statements.add("INSERT INTO sis.student_guardian (id, tenant_id, student_id, guardian_id,"
                    + " relationship_type_id, status, row_version) VALUES ('" + UUID.randomUUID() + "', :tenant, '" + student
                    + "', '" + id + "', '" + relationship + "', 'active', 1)");
        }
        fixtures(WORKFLOW_TENANT, statements);
        return id;
    }

    /** The same step over both transports: REST's body beside the path id, which gRPC carries in {@code idField}. */
    private void assertSameStep(String path, String rpc, String idField, String id, String token, String body)
            throws Exception {
        ObjectNode request = (ObjectNode) JSON.readTree(body);
        request.put(idField, id);
        GrpcParityContract.assertSameAnswer(rest("POST", path, token, body), grpc(rpc, token, grpcEnums(request)));
    }

    /** The request as protobuf JSON: a student status is its enum value name there. */
    private static String grpcEnums(ObjectNode request) {
        if (request.has("status")) {
            request.put("status", "STATUS_" + request.path("status").asString().toUpperCase(Locale.ROOT));
        }
        return request.toString();
    }

    @Test
    void aStudentsStatusSummaryCertificateAndPortalAccountAnswerLikeRest() throws Exception {
        String student = student("P-1");
        String status = STUDENTS + student + ":changeStatus";
        for (String refused : List.of("{\"status\":\"withdrawn\"}", "{\"status\":\"inactive\",\"exitDate\":\"2026-09-01\"}",
                "{\"status\":\"withdrawn\",\"exitDate\":\"2026-09-01\",\"etag\":\"\\\"7\\\"\"}",
                "{\"status\":\"active\"}", "{}")) {
            Answer overRest = rest("POST", status, OFFICE, refused);
            assertThat(overRest.status()).as(refused).isGreaterThanOrEqualTo(400);
            assertSameStep(status, "ChangeStudentStatus", "studentId", student, OFFICE, refused);
        }
        String leaving = "{\"status\":\"transferred\",\"exitDate\":\"2026-09-01\",\"exitReason\":\"Family moved\"}";
        assertSameStep(status, "ChangeStudentStatus", "studentId", student, NO_PERMISSION, leaving);
        String absent = UUID.randomUUID().toString();
        assertSameStep(STUDENTS + absent + ":changeStatus", "ChangeStudentStatus", "studentId", absent, OFFICE, leaving);
        Answer left = grpc("ChangeStudentStatus", OFFICE, "{\"studentId\":\"" + student + "\",\"status\":"
                + "\"STATUS_TRANSFERRED\",\"exitDate\":\"2026-09-01\",\"exitReason\":\"Family moved\"}");
        assertThat(left.status()).as(left.code()).isEqualTo(200);
        Answer read = rest("GET", STUDENTS + student, OFFICE, null);
        assertThat(read.body().path("status").asString()).isEqualTo("transferred");
        assertThat(read.body().path("exitDate").asString()).isEqualTo("2026-09-01");
        GrpcParity.assertSameResource(read.body(), left.body().path("student"));
        assertSameStep(status, "ChangeStudentStatus", "studentId", student, OFFICE, leaving);

        String current = student("P-2");
        Answer summary = rest("GET", STUDENTS + current + ":summary", OFFICE, null);
        Answer summaryGrpc = grpc("GetStudentSummary", OFFICE, "{\"studentId\":\"" + current + "\"}");
        assertThat(summary.status()).as(summary.body().toString()).isEqualTo(200);
        assertThat(summaryGrpc.status()).as(summaryGrpc.code()).isEqualTo(200);
        assertThat(summaryGrpc.body().path("result").path("currentEnrollment").path("academicYearId").asString())
                .isEqualTo(YEAR);
        GrpcParity.assertSameResource(summary.body(), summaryGrpc.body().path("result"));
        String otherYear = UUID.randomUUID().toString();
        Answer inYear = grpc("GetStudentSummary", OFFICE, "{\"studentId\":\"" + current + "\",\"academicYearId\":\""
                + otherYear + "\"}");
        assertThat(inYear.body().path("result").has("currentEnrollment")).as("no placement in that year").isFalse();
        GrpcParity.assertSameResource(rest("GET", STUDENTS + current + ":summary?academicYearId=" + otherYear, OFFICE,
                null).body(), inYear.body().path("result"));
        GrpcParityContract.assertSameAnswer(rest("GET", STUDENTS + current + ":summary?academicYearId=nope", OFFICE, null),
                grpc("GetStudentSummary", OFFICE, "{\"studentId\":\"" + current + "\",\"academicYearId\":\"nope\"}"));
        GrpcParityContract.assertSameAnswer(rest("GET", STUDENTS + absent + ":summary", OFFICE, null),
                grpc("GetStudentSummary", OFFICE, "{\"studentId\":\"" + absent + "\"}"));
        GrpcParityContract.assertSameAnswer(rest("GET", STUDENTS + current + ":summary", NO_PERMISSION, null),
                grpc("GetStudentSummary", NO_PERMISSION, "{\"studentId\":\"" + current + "\"}"));

        Answer issued = grpc("IssueBonafideCertificate", OFFICE, "{\"studentId\":\"" + current + "\",\"purpose\":"
                + "\"Passport application\"}");
        assertThat(issued.status()).as(issued.code()).isEqualTo(200);
        JsonNode operation = issued.body().path("result");
        assertThat(operation.path("status").asString()).isEqualTo("succeeded");
        GrpcParity.assertSameResource(rest("GET", "/v1/sis/operations/" + operation.path("operationId").asString(),
                OFFICE, null).body(), operation);
        assertThat(rest("POST", STUDENTS + current + ":issueBonafideCertificate", OFFICE, "{}").status()).isEqualTo(202);
        String certificate = ":issueBonafideCertificate";
        assertSameStep(STUDENTS + student + certificate, "IssueBonafideCertificate", "studentId", student, OFFICE, "{}");
        assertSameStep(STUDENTS + absent + certificate, "IssueBonafideCertificate", "studentId", absent, OFFICE, "{}");
        assertSameStep(STUDENTS + current + certificate, "IssueBonafideCertificate", "studentId", current, NO_PERMISSION,
                "{}");

        String account = STUDENTS + current + ":createPortalAccount";
        assertThat(rest("POST", account, OFFICE, "{}").status()).as("iam is not running here").isEqualTo(503);
        for (String body : List.of("{}", "{\"sendInvite\":true}")) {
            assertSameStep(account, "CreateStudentPortalAccount", "studentId", current, OFFICE, body);
        }
        assertSameStep(STUDENTS + absent + ":createPortalAccount", "CreateStudentPortalAccount", "studentId", absent,
                OFFICE, "{}");
        assertSameStep(account, "CreateStudentPortalAccount", "studentId", current, NO_PERMISSION, "{}");
    }

    @Test
    void aGuardiansChildrenAndPortalAccountAndTheClassRosterAnswerLikeRest() throws Exception {
        String first = student("Q-1");
        String second = student("Q-2");
        UUID person = UUID.randomUUID();
        String parent = guardian(person, first, second);
        String lonely = guardian(UUID.randomUUID());
        String me = token(WORKFLOW_TENANT, person, List.of("sis.guardian.list_my_children"));

        Answer children = rest("GET", "/v1/sis/me/children?size=1", me, null);
        Answer childrenGrpc = grpc("ListMyChildren", me, "{\"pageable\":{\"size\":1}}");
        assertThat(children.status()).as(children.body().toString()).isEqualTo(200);
        assertThat(childrenGrpc.status()).as(childrenGrpc.code()).isEqualTo(200);
        assertThat(childrenGrpc.body().path("items")).hasSize(1);
        GrpcParity.assertSameResource(children.body().path("data").get(0), childrenGrpc.body().path("items").get(0));
        assertThat(children.body().path("paging").path("next").asString()).contains("cursor=");
        String cursor = childrenGrpc.body().path("pagination").path("nextCursor").asString();
        assertThat(cursor).as("a next page by cursor, as on REST").isNotEmpty();
        Answer next = grpc("ListMyChildren", me, "{\"pageable\":{\"size\":1,\"page\":2,\"cursor\":\"" + cursor + "\"}}");
        assertThat(next.body().path("items")).hasSize(1);
        assertThat(List.of(childrenGrpc.body().path("items").get(0).path("student").path("id").asString(),
                next.body().path("items").get(0).path("student").path("id").asString()))
                .containsExactlyInAnyOrder(first, second);
        assertThat(grpc("ListMyChildren", OFFICE, "{}").body().path("items")).as("nobody's guardian").isEmpty();
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/sis/me/children", NO_PERMISSION, null),
                grpc("ListMyChildren", NO_PERMISSION, "{}"));

        String guardians = "/v1/sis/guardians/";
        Answer noChild = rest("POST", guardians + lonely + ":createPortalAccount", OFFICE, "{}");
        assertThat(noChild.status()).as("a guardian of no student").isEqualTo(409);
        assertSameStep(guardians + lonely + ":createPortalAccount", "CreatePortalAccount", "guardianId", lonely, OFFICE,
                "{}");
        assertThat(rest("POST", guardians + parent + ":createPortalAccount", OFFICE, "{}").status())
                .as("iam is not running here").isEqualTo(503);
        assertSameStep(guardians + parent + ":createPortalAccount", "CreatePortalAccount", "guardianId", parent, OFFICE,
                "{}");
        assertSameStep(guardians + parent + ":createPortalAccount", "CreatePortalAccount", "guardianId", parent,
                NO_PERMISSION, "{}");

        String roster = "/v1/sis/class-sections/" + CLASS_SECTION + ":roster";
        Answer listed = rest("GET", roster + "?size=500", OFFICE, null);
        Answer listedGrpc = grpc("ListClassRoster", OFFICE, "{\"classSectionId\":\"" + CLASS_SECTION
                + "\",\"pageable\":{\"size\":500}}");
        assertThat(listed.status()).as(listed.body().toString()).isEqualTo(200);
        assertThat(listedGrpc.status()).as(listedGrpc.code()).isEqualTo(200);
        assertThat(listed.body().path("data").size()).isGreaterThanOrEqualTo(2);
        assertThat(listedGrpc.body().path("items")).hasSize(listed.body().path("data").size());
        for (int i = 0; i < listed.body().path("data").size(); i++) {
            GrpcParity.assertSameResource(listed.body().path("data").get(i), listedGrpc.body().path("items").get(i));
        }
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/sis/class-sections/nope:roster", OFFICE, null),
                grpc("ListClassRoster", OFFICE, "{\"classSectionId\":\"nope\"}"));
        GrpcParityContract.assertSameAnswer(rest("GET", roster, NO_PERMISSION, null),
                grpc("ListClassRoster", NO_PERMISSION, "{\"classSectionId\":\"" + CLASS_SECTION + "\"}"));
    }

    private String promotion(String student, String status) {
        String id = UUID.randomUUID().toString();
        fixtures(WORKFLOW_TENANT, List.of("INSERT INTO sis.promotion (id, tenant_id, student_id, institution_id,"
                + " from_academic_year_id, from_grade_level_id, to_academic_year_id, to_grade_level_id, status, row_version)"
                + " VALUES ('" + id + "', :tenant, '" + student + "', '" + INSTITUTION + "', '" + YEAR + "', '"
                + UUID.randomUUID() + "', '" + UUID.randomUUID() + "', '" + UUID.randomUUID() + "', '" + status + "', 1)"));
        return id;
    }

    @Test
    void promotionsAreProposedAndAppliedTheSameOverBoth() throws Exception {
        String propose = "{\"fromClassSectionId\":\"" + CLASS_SECTION + "\",\"toAcademicYearId\":\"" + UUID.randomUUID()
                + "\",\"toGradeLevelId\":\"" + UUID.randomUUID() + "\"}";
        Answer unavailable = rest("POST", "/v1/sis/promotions:propose", OFFICE, propose);
        assertThat(unavailable.status()).as("academics is not running here").isEqualTo(503);
        GrpcParityContract.assertSameAnswer(unavailable, grpc("ProposePromotions", OFFICE, propose));
        Answer invalid = rest("POST", "/v1/sis/promotions:propose", OFFICE, "{}");
        assertThat(invalid.status()).isEqualTo(422);
        GrpcParityContract.assertSameAnswer(invalid, grpc("ProposePromotions", OFFICE, "{}"));
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/sis/promotions:propose", NO_PERMISSION, propose),
                grpc("ProposePromotions", NO_PERMISSION, propose));

        String approved = promotion(student("R-1"), "approved");
        String proposed = promotion(student("R-2"), "proposed");
        String apply = "/v1/sis/promotions:apply";
        String ids = "{\"promotionIds\":[\"%s\"]}";
        for (String body : List.of("{\"promotionIds\":[]}", ids.formatted(proposed), ids.formatted(UUID.randomUUID()))) {
            Answer refused = rest("POST", apply, OFFICE, body);
            assertThat(refused.status()).as(body).isGreaterThanOrEqualTo(400);
            GrpcParityContract.assertSameAnswer(refused, grpc("ApplyPromotions", OFFICE, body));
        }
        GrpcParityContract.assertSameAnswer(rest("POST", apply, NO_PERMISSION, ids.formatted(approved)),
                grpc("ApplyPromotions", NO_PERMISSION, ids.formatted(approved)));
        Answer applied = grpc("ApplyPromotions", OFFICE, ids.formatted(approved));
        assertThat(applied.status()).as(applied.code()).isEqualTo(200);
        assertThat(applied.body().path("result").path("status").asString()).isEqualTo("succeeded");
        assertThat(rest("GET", "/v1/sis/promotions/" + approved, OFFICE, null).body().path("status").asString())
                .isEqualTo("applied");
        Answer again = rest("POST", apply, OFFICE, ids.formatted(approved));
        assertThat(again.status()).as("applied once").isEqualTo(409);
        GrpcParityContract.assertSameAnswer(again, grpc("ApplyPromotions", OFFICE, ids.formatted(approved)));
    }

    /** A subject registration of a new student in the state, with the number of lines; answers the id. */
    private String registration(String status, int lines) {
        String student = student("S-" + UUID.randomUUID().toString().substring(0, 8));
        String enrollment = UUID.randomUUID().toString();
        String id = UUID.randomUUID().toString();
        List<String> statements = new ArrayList<>(List.of("INSERT INTO sis.program_enrollment (id, tenant_id, student_id,"
                + " course_id, row_version) VALUES ('" + enrollment + "', :tenant, '" + student + "', '" + UUID.randomUUID()
                + "', 1)", "INSERT INTO sis.subject_registration (id, tenant_id, student_id, course_id, academic_year_id,"
                + " program_enrollment_id, status, row_version) VALUES ('" + id + "', :tenant, '" + student + "', '"
                + UUID.randomUUID() + "', '" + YEAR + "', '" + enrollment + "', '" + status + "', 1)"));
        for (int i = 0; i < lines; i++) {
            statements.add("INSERT INTO sis.subject_registration_line (id, tenant_id, subject_registration_id, subject_id,"
                    + " course_subject_id, selection_type, row_version) VALUES ('" + UUID.randomUUID() + "', :tenant, '" + id
                    + "', '" + UUID.randomUUID() + "', '" + UUID.randomUUID() + "', 'compulsory', 1)");
        }
        fixtures(WORKFLOW_TENANT, statements);
        return id;
    }

    @Test
    void aSubjectRegistrationIsApprovedAndFilledTheSameOverBoth() throws Exception {
        String submitted = registration("submitted", 2);
        String empty = registration("submitted", 0);
        String draft = registration("draft", 1);
        String registrations = "/v1/sis/subject-registrations/";
        String field = "subjectRegistrationId";
        assertThat(rest("POST", registrations + empty + ":approve", OFFICE, "{}").status()).as("lists no subjects")
                .isEqualTo(409);
        assertSameStep(registrations + empty + ":approve", "ApproveSubjectRegistration", field, empty, OFFICE, "{}");
        assertSameStep(registrations + draft + ":approve", "ApproveSubjectRegistration", field, draft, OFFICE, "{}");
        assertSameStep(registrations + submitted + ":approve", "ApproveSubjectRegistration", field, submitted, OFFICE,
                "{\"etag\":\"\\\"9\\\"\"}");
        assertSameStep(registrations + submitted + ":approve", "ApproveSubjectRegistration", field, submitted,
                NO_PERMISSION, "{}");
        Answer approved = grpc("ApproveSubjectRegistration", OFFICE, "{\"subjectRegistrationId\":\"" + submitted + "\"}");
        assertThat(approved.status()).as(approved.code()).isEqualTo(200);
        Answer read = rest("GET", registrations + submitted, OFFICE, null);
        assertThat(read.body().path("status").asString()).isEqualTo("approved");
        GrpcParity.assertSameResource(read.body(), approved.body().path("subjectRegistration"));
        assertSameStep(registrations + submitted + ":approve", "ApproveSubjectRegistration", field, submitted, OFFICE,
                "{}");

        Answer filled = rest("POST", registrations + submitted + ":loadCourseSubjects", OFFICE, "{}");
        assertThat(filled.status()).as("only a draft is filled").isEqualTo(409);
        assertSameStep(registrations + submitted + ":loadCourseSubjects", "LoadCourseSubjects", field, submitted, OFFICE,
                "{}");
        assertThat(rest("POST", registrations + draft + ":loadCourseSubjects", OFFICE, "{}").status())
                .as("academics is not running here").isEqualTo(503);
        assertSameStep(registrations + draft + ":loadCourseSubjects", "LoadCourseSubjects", field, draft, OFFICE, "{}");
        String absent = UUID.randomUUID().toString();
        assertSameStep(registrations + absent + ":loadCourseSubjects", "LoadCourseSubjects", field, absent, OFFICE, "{}");
        assertSameStep(registrations + draft + ":loadCourseSubjects", "LoadCourseSubjects", field, draft, NO_PERMISSION,
                "{}");
    }

    /**
     * A leave decision carries an idempotency key over gRPC as REST's Idempotency-Key: retried with its request_id, the
     * approval answers its first result (no second decision); a malformed key is refused alike.
     */
    @Test
    void aLeaveDecisionRetriedWithItsRequestIdAnswersTheFirstResult() throws Exception {
        String leaves = "/v1/sis/student-leave-requests/";
        Answer created = rest("POST", "/v1/sis/student-leave-requests", OFFICE, "{\"studentId\":\"" + student("LEAVE-1")
                + "\",\"fromDate\":\"2026-05-04\",\"toDate\":\"2026-05-06\"}");
        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
        String id = created.body().path("id").asString();
        fixtures(WORKFLOW_TENANT, List.of("UPDATE sis.student_leave_request SET status = 'pending' WHERE id = '" + id
                + "'"));
        String etag = rest("GET", leaves + id, OFFICE, null).body().path("etag").asString();
        String approval = "{\"studentLeaveRequestId\":\"" + id + "\",\"etag\":" + JSON.writeValueAsString(etag)
                + ",\"decisionNote\":\"Family event\",\"requestId\":\"%s\"}";
        GrpcParityContract.assertSameAnswer(rest("POST", leaves + id + ":approve", OFFICE, "{}", "If-Match", etag,
                "Idempotency-Key", "not-a-uuid"), grpc("ApproveStudentLeaveRequest", OFFICE,
                approval.formatted("not-a-uuid")));

        String key = UUID.randomUUID().toString();
        Answer approved = grpc("ApproveStudentLeaveRequest", OFFICE, approval.formatted(key));
        assertThat(approved.status()).as(approved.code()).isEqualTo(200);
        assertThat(approved.body().path("studentLeaveRequest").path("status").asString()).isEqualTo("approved");
        Answer replayed = grpc("ApproveStudentLeaveRequest", OFFICE, approval.formatted(key));
        assertThat(replayed.status()).as("the first answer, not a refusal of a second decision").isEqualTo(200);
        assertThat(replayed.body()).isEqualTo(approved.body());
        assertThat(grpc("RejectStudentLeaveRequest", OFFICE, approval.formatted(UUID.randomUUID())).status())
                .as("a new key is a new decision, refused: the request moved on since that etag").isEqualTo(412);
    }
}
