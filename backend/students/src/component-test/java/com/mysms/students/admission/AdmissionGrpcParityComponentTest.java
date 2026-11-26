package com.mysms.students.admission;

import com.mysms.students.StudentsApplication;
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
import com.mysms.students.admission.config.AdmissionDatabaseConfig;
import com.mysms.admission.v1.AdmissionProto;
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
import tools.jackson.databind.node.ObjectNode;

/**
 * Every RPC of admission's three gRPC services against the same REST operation, on real PostgreSQL: for each resource
 * of crud-spec.json the create, get, list, update and delete give the same resource over both transports, and both
 * refuse alike; every state transition of the admission cycle and the application is done and refused the same way;
 * enrolment answers its guards, the missing other services (503 in this module's test) and an enrolled application's
 * student alike; a request_id replays the first answer; and the module's operations are read and cancelled alike.
 */
@SpringBootTest(classes = StudentsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=AdmissionGrpcParityComponentTest")
class AdmissionGrpcParityComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final String ADMIN = token(TENANT, null, List.of("admission.*"));
    private static final String NOBODY = token(TENANT, null, List.of());
    private static final String STRANGER = token(OTHER_TENANT, null, List.of("admission.*"));
    /** The tenant of the workflow checks, so their rows never meet the standard operations' rows. */
    private static final UUID WORKFLOW_TENANT = UUID.randomUUID();
    private static final String INSTITUTION = "0196f000-0000-7000-8000-00000000a001";
    private static final String OFFICE = token(WORKFLOW_TENANT, null, List.of("admission.*"));
    private static final String NO_PERMISSION = token(WORKFLOW_TENANT, null, List.of());
    private static final String APPLICATIONS = "/v1/admission/applications";
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;


    @Autowired
    private ProtoBridge bridge;

    @Autowired
    @Qualifier(AdmissionDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "admission", "sis", "careers");
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
            channel = GrpcParity.channel("AdmissionGrpcParityComponentTest");
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

    // ---- the standard operations, resource by resource

    private GrpcParityContract contract() {
        return new GrpcParityContract(AdmissionProto.getDescriptor(), this::rest, this::channel, bridge,
                new GrpcParityContract.Callers(ADMIN, NOBODY, STRANGER));
    }

    @TestFactory
    List<DynamicNode> everyStandardRpcAnswersLikeRest() throws Exception {
        List<DynamicNode> tests = new ArrayList<>();
        for (GrpcParityContract.Resource resource : GrpcParityContract.resources("admission/crud-spec.json")) {
            tests.add(DynamicTest.dynamicTest(resource.name(), () -> {
                fixtures(TENANT, resource.fixtures());
                fixtures(OTHER_TENANT, resource.fixtures());
                contract().verify(resource);
            }));
        }
        return tests;
    }

    // ---- the state transitions of crud-spec.json

    @TestFactory
    List<DynamicNode> everyStateTransitionAnswersLikeRest() throws Exception {
        JsonNode spec = JSON.readTree(getClass().getClassLoader().getResourceAsStream("admission/crud-spec.json"));
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
                    ? AdmissionGrpcParityComponentTest.this.rest("POST", target, token, sent.toString(), "If-Match", etag)
                    : AdmissionGrpcParityComponentTest.this.rest("POST", target, token, sent.toString());
        }

        Answer grpc(String id, String token, String etag) throws Exception {
            ObjectNode sent = body.deepCopy();
            sent.put(idField, id);
            if (etag != null) {
                sent.put("etag", etag);
            }
            return AdmissionGrpcParityComponentTest.this.grpc(rpc, token, sent.toString());
        }
    }

    // ---- long-running operations

    @Test
    void operationsAreReadAndCancelledTheSameOverBoth() throws Exception {
        UUID running = UUID.randomUUID();
        UUID finished = UUID.randomUUID();
        String insert = "INSERT INTO admission.operation (id, tenant_id, operation_name, status, progress_percent,"
                + " started_by_user_id, started_at, finished_at, created_at, updated_at, row_version) VALUES ('%s', :tenant,"
                + " '%s', '%s', %s, '" + UUID.randomUUID() + "', now() - interval '%d minutes', %s, now(), now(), 1)";
        fixtures(WORKFLOW_TENANT, List.of(insert.formatted(running, "report.run", "running", "40", 3, "NULL"),
                insert.formatted(finished, "import.rows", "succeeded", "100", 1, "now()")));
        String operations = "/v1/admission/operations";

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

    // ---- enrolment and request_id

    private String application(String number) throws Exception {
        Answer created = rest("POST", APPLICATIONS, OFFICE, "{\"institutionId\":\"" + INSTITUTION
                + "\",\"applicationNumber\":\"" + number + "\",\"fullName\":\"Asha Rao\",\"dateOfBirth\":\"2019-01-01\","
                + "\"gender\":\"female\"}");
        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
        return created.body().path("id").asString();
    }

    private Answer enrollRest(String id, String token, String body) throws Exception {
        return rest("POST", APPLICATIONS + "/" + id + ":enroll", token, body, "Idempotency-Key",
                UUID.randomUUID().toString());
    }

    /** The same enrolment over gRPC: the body's fields beside the application id and a request_id of its own. */
    private Answer enrollGrpc(String id, String token, String body) throws Exception {
        ObjectNode request = (ObjectNode) JSON.readTree(body);
        request.put("applicationId", id);
        request.put("requestId", UUID.randomUUID().toString());
        return grpc("EnrollApplicant", token, request.toString());
    }

    @Test
    void enrolmentIsGuardedAndAnsweredTheSameOverBoth() throws Exception {
        String id = application("G-ENROLL-1");
        GrpcParityContract.assertSameAnswer(rest("POST", APPLICATIONS + "/" + id + ":enroll", OFFICE, "{}"),
                grpc("EnrollApplicant", OFFICE, "{\"applicationId\":\"" + id + "\"}"));
        assertThat(grpc("EnrollApplicant", OFFICE, "{\"applicationId\":\"" + id + "\"}").status())
                .as("enrolment needs a request_id").isEqualTo(400);
        GrpcParityContract.assertSameAnswer(enrollRest(id, OFFICE, "{}"), enrollGrpc(id, OFFICE, "{}"));
        fixtures(WORKFLOW_TENANT, List.of("UPDATE admission.application SET status = 'admitted' WHERE id = '" + id + "'"));
        GrpcParityContract.assertSameAnswer(enrollRest(id, NO_PERMISSION, "{}"), enrollGrpc(id, NO_PERMISSION, "{}"));
        Answer nowhere = enrollRest(id, OFFICE, "{}");
        assertThat(nowhere.status()).as(nowhere.body().toString()).isEqualTo(422);
        GrpcParityContract.assertSameAnswer(nowhere, enrollGrpc(id, OFFICE, "{}"));
        String both = "{\"classSectionId\":\"" + UUID.randomUUID() + "\",\"batchId\":\"" + UUID.randomUUID() + "\"}";
        GrpcParityContract.assertSameAnswer(enrollRest(id, OFFICE, both), enrollGrpc(id, OFFICE, both));
        fixtures(WORKFLOW_TENANT, List.of("UPDATE admission.application SET academic_year_id = '" + UUID.randomUUID()
                + "' WHERE id = '" + id + "'"));
        String placed = "{\"classSectionId\":\"" + UUID.randomUUID() + "\",\"rollNumber\":\"12\",\"createPortalAccounts\":true}";
        Answer unavailable = enrollRest(id, OFFICE, placed);
        assertThat(unavailable.status()).as("people and sis are not configured: " + unavailable.body()).isEqualTo(503);
        GrpcParityContract.assertSameAnswer(unavailable, enrollGrpc(id, OFFICE, placed));
        String absent = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(enrollRest(absent, OFFICE, placed), enrollGrpc(absent, OFFICE, placed));

        UUID student = UUID.randomUUID();
        fixtures(WORKFLOW_TENANT, List.of("UPDATE admission.application SET status = 'done', student_id = '" + student
                + "' WHERE id = '" + id + "'"));
        String section = "{\"classSectionId\":\"" + UUID.randomUUID() + "\"}";
        Answer enrolled = enrollRest(id, OFFICE, section);
        Answer enrolledGrpc = enrollGrpc(id, OFFICE, section);
        assertThat(enrolled.status()).as(enrolled.body().toString()).isEqualTo(200);
        assertThat(enrolledGrpc.status()).as(enrolledGrpc.code()).isEqualTo(200);
        assertThat(enrolledGrpc.body().path("studentId").asString()).isEqualTo(student.toString());
        assertThat(enrolledGrpc.body().has("operation")).as("no operation was started").isFalse();
        GrpcParity.assertSameResource(enrolled.body(), enrolledGrpc.body());
    }

    @Test
    void aRequestIdReplaysTheFirstAnswerAndRefusesADifferentRequest() throws Exception {
        String key = UUID.randomUUID().toString();
        String cycle = "{\"admissionCycle\":{\"institutionId\":\"" + INSTITUTION + "\",\"name\":\"%s\","
                + "\"startDate\":\"2027-01-10\"},\"requestId\":\"" + key + "\"}";
        Answer first = grpc("CreateAdmissionCycle", OFFICE, cycle.formatted("Replayed cycle"));
        assertThat(first.status()).as(first.code()).isEqualTo(200);
        Answer replay = grpc("CreateAdmissionCycle", OFFICE, cycle.formatted("Replayed cycle"));
        assertThat(replay.status()).as(replay.code()).isEqualTo(200);
        assertThat(replay.body()).as("the first answer, not a second row").isEqualTo(first.body());
        Answer listed = rest("GET", "/v1/admission/admission-cycles?filter=name%20%3D%20%22Replayed%20cycle%22", OFFICE,
                null);
        assertThat(listed.body().path("data")).hasSize(1);

        String restKey = UUID.randomUUID().toString();
        String body = "{\"institutionId\":\"" + INSTITUTION + "\",\"name\":\"%s\",\"startDate\":\"2027-01-10\"}";
        assertThat(rest("POST", "/v1/admission/admission-cycles", OFFICE, body.formatted("Keyed over REST"),
                "Idempotency-Key", restKey).status()).isEqualTo(201);
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/admission/admission-cycles", OFFICE,
                        body.formatted("Another request"), "Idempotency-Key", restKey),
                grpc("CreateAdmissionCycle", OFFICE, cycle.formatted("Another request")));
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/admission/admission-cycles", OFFICE,
                        body.formatted("Bad key"), "Idempotency-Key", "not-a-uuid"),
                grpc("CreateAdmissionCycle", OFFICE, cycle.formatted("Bad key").replace(key, "not-a-uuid")));
    }
}
