package com.mysms.academics.assessment;

import com.mysms.academics.AcademicsApplication;
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
import com.mysms.academics.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.v1.AssessmentProto;
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
 * Every RPC of assessment's fifteen gRPC services against the same REST operation, on real PostgreSQL: for each
 * resource of crud-spec.json serving the five standard operations the create, get, list, update and delete give the
 * same resource over both transports, and both refuse alike — 403 without the permission, 404 for an absent row and
 * another tenant's row, 409 for a duplicate, 412 for a stale etag, 428 without one, 422 for an invalid body; every
 * state transition of crud-spec.json moves and refuses alike; and the workflows answer alike: homework review and its
 * submission, the assignment and its submissions from every state, exam results and marks entry, the attendees and
 * rooms other services decide (503 here), and the results run (a 202 operation) with its marksheet, lines and result
 * cards.
 */
@SpringBootTest(classes = AcademicsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=AssessmentGrpcParityComponentTest")
class AssessmentGrpcParityComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final String ADMIN = token(TENANT, List.of("assessment.*"));
    private static final String NOBODY = token(TENANT, List.of());
    private static final String STRANGER = token(OTHER_TENANT, List.of("assessment.*"));
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;


    @Autowired
    private ProtoBridge bridge;

    @Autowired
    @Qualifier(AssessmentDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "assessment", "academics", "timetable", "attendance");
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
            return new School(tenant, token(tenant, List.of("assessment.*")), token(tenant, List.of()));
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
            channel = GrpcParity.channel("AssessmentGrpcParityComponentTest");
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

    /** An update: the {@code <Resource>Update} message of a REST PATCH body, with the row's id and etag. */
    private DynamicMessage updating(MethodDescriptor update, String id, String etag, ObjectNode changes) {
        DynamicMessage.Builder request = contract().changes(update, changes);
        set(request, "id", id);
        return set(request, "etag", etag).build();
    }

    /** A resource's crud-spec.json entry, for the kit's PATCH-state step on resources outside the standard flow. */
    private static GrpcParityContract.Resource described(String spec, String name) throws Exception {
        for (JsonNode resource : JSON.readTree(AssessmentGrpcParityComponentTest.class.getClassLoader().getResourceAsStream(spec))
                .path("resources")) {
            if (name.equals(resource.path("name").asString())) {
                return GrpcParityContract.Resource.of(resource);
            }
        }
        throw new IllegalArgumentException("no resource " + name + " in " + spec);
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
        return new GrpcParityContract(AssessmentProto.getDescriptor(), this::rest, this::channel, bridge,
                new GrpcParityContract.Callers(ADMIN, NOBODY, STRANGER));
    }

    /**
     * The resources serving all five standard operations; the others are verified by their own tests below. Read here
     * rather than by {@link GrpcParityContract#resources}, which expects an update body of every resource, while the
     * read-only ones of this module (approvals, marksheet lines) have none.
     */
    @TestFactory
    List<DynamicNode> everyStandardRpcAnswersLikeRest() throws Exception {
        List<DynamicNode> tests = new ArrayList<>();
        for (GrpcParityContract.Resource resource : GrpcParityContract.resources("assessment/crud-spec.json")) {
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
        JsonNode spec = JSON.readTree(getClass().getClassLoader().getResourceAsStream("assessment/crud-spec.json"));
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
        MethodDescriptor rpc = rpc(transition.path("operation").asString().substring("assessment.".length()));
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

    private static final String EXAMS = "/v1/assessment/exams";
    private static final String RESULTS = "/v1/assessment/exam-results";
    private static final String HOMEWORKS = "/v1/assessment/homeworks";
    private static final String ASSIGNMENTS = "/v1/assessment/assignments";
    private static final String SUBMISSIONS = "/v1/assessment/assignment-submissions";
    private static final String MARKSHEETS = "/v1/assessment/marksheets";
    private static final String OPERATIONS = "/v1/assessment/operations";

    private AssessmentFixtures fx() {
        return new AssessmentFixtures(TOKENS, database, port);
    }

    private static UUID institution(School school) {
        return UUID.fromString(school.id("institution"));
    }

    /**
     * One operation on two rows in the same state, one per transport: both answer alike, and where they succeed the
     * row moved over gRPC reads the same over REST and is in the state REST's row is in.
     */
    private void alikeOnTwins(School school, String token, String operation, String collection, String verb,
                              String overRestId, String overGrpcId, String body) throws Exception {
        MethodDescriptor rpc = rpc(operation);
        String idField = rpc.getInputType().getFields().get(0).getName();
        Answer overRest = rest("POST", collection + "/" + overRestId + ":" + verb, token, body);
        Answer overGrpc = grpc(rpc, call(rpc, idField, overGrpcId, json(body)), token);
        GrpcParityContract.assertSameAnswer(overRest, overGrpc);
        if (overRest.status() < 400) {
            JsonNode twin = rest("GET", collection + "/" + overGrpcId, school.admin(), null).body();
            sameResource(twin, resource(overGrpc));
            assertThat(twin.path("status").asString()).as(operation).isEqualTo(overRest.body().path("status").asString());
        }
    }

    /** A move REST refuses with the status: gRPC refuses it alike. */
    private void refusedAlike(int status, String token, String operation, String path, String id, String body)
            throws Exception {
        MethodDescriptor rpc = rpc(operation);
        Answer overRest = rest("POST", path, token, body);
        assertThat(overRest.status()).as(operation + ": " + overRest.body()).isEqualTo(status);
        GrpcParityContract.assertSameAnswer(overRest,
                grpc(rpc, call(rpc, rpc.getInputType().getFields().get(0).getName(), id, json(body)), token));
    }

    /** A move made over gRPC: the row it answers is the row REST reads (as the school's administrator). */
    private JsonNode movedOverGrpc(School school, String token, String operation, String collection, String id,
                                   String body) throws Exception {
        MethodDescriptor rpc = rpc(operation);
        JsonNode moved = resource(grpc(rpc, call(rpc, rpc.getInputType().getFields().get(0).getName(), id, json(body)),
                token));
        sameResource(rest("GET", collection + "/" + id, school.admin(), null).body(), moved);
        return moved;
    }

    private String assignment(School school, String status) {
        UUID id = UUID.randomUUID();
        fx().inTenant(school.tenant(), () -> database.jdbc().sql("INSERT INTO assessment.assignment (id, tenant_id, "
                        + "institution_id, title, class_section_id, max_marks, status) VALUES (:id, :tenantId, "
                        + ":institution, 'Essay', :section, 20, :status)")
                .param("id", id).param("institution", institution(school)).param("section", UUID.randomUUID())
                .param("status", status).update());
        return id.toString();
    }

    private String submission(School school, String assignment, String status) throws Exception {
        Answer made = rest("POST", SUBMISSIONS, school.admin(), "{\"assignmentId\":\"" + assignment + "\",\"studentId\":\""
                + UUID.randomUUID() + "\",\"content\":\"My essay\"}");
        assertThat(made.status()).as(made.body().toString()).isEqualTo(201);
        String id = made.body().path("id").asString();
        fixtures(school.tenant(), List.of("UPDATE assessment.assignment_submission SET status = '" + status
                + "' WHERE id = '" + id + "'"));
        return id;
    }

    @Test
    void homeworkGoesThroughReviewAndItsSubmissionIsCheckedAndAnsweredAlikeOverBoth() throws Exception {
        School school = School.open();
        AssessmentFixtures fx = fx();
        String teacher = fx.member(school.tenant(), institution(school), "assessment.homework.read",
                "assessment.homework.create", "assessment.homework.publish_homework");
        String[] homework = new String[3];
        for (int i = 0; i < homework.length; i++) {
            Answer made = rest("POST", HOMEWORKS, school.admin(), "{\"institutionId\":\"" + institution(school)
                    + "\",\"academicYearId\":\"" + UUID.randomUUID() + "\",\"classSectionId\":\"" + UUID.randomUUID()
                    + "\",\"description\":\"Read chapter " + i + "\"}");
            assertThat(made.status()).as(made.body().toString()).isEqualTo(201);
            homework[i] = made.body().path("id").asString();
        }
        // a teacher who may not approve sends it for review, on both transports
        alikeOnTwins(school, teacher, "PublishHomework", HOMEWORKS, "publish", homework[0], homework[1], "{}");
        assertThat(rest("GET", HOMEWORKS + "/" + homework[1], school.admin(), null).body().path("status").asString())
                .isEqualTo("pending");
        refusedAlike(409, teacher, "PublishHomework", HOMEWORKS + "/" + homework[1] + ":publish", homework[1], "{}");
        refusedAlike(403, teacher, "ApproveHomework", HOMEWORKS + "/" + homework[1] + ":approve", homework[1], "{}");
        refusedAlike(412, teacher, "PublishHomework", HOMEWORKS + "/" + homework[2] + ":publish", homework[2],
                "{\"etag\":\"\\\"99\\\"\"}");

        // the approval the workflow wrote is read alike (it has no create, update or delete)
        MethodDescriptor approvals = rpc("ListHomeworkApprovals");
        JsonNode listed = rest("GET", HOMEWORKS + "/" + homework[1] + "/approvals", school.admin(), null).body()
                .path("data");
        Answer listedGrpc = grpc(approvals, call(approvals, "homework_id", homework[1], null), school.admin());
        assertThat(listedGrpc.status()).isEqualTo(200);
        assertThat(listedGrpc.body().path("items")).hasSize(1).hasSize(listed.size());
        sameResource(listed.get(0), listedGrpc.body().path("items").get(0));
        String approval = listed.get(0).path("id").asString();
        MethodDescriptor getApproval = rpc("GetHomeworkApproval");
        sameResource(rest("GET", HOMEWORKS + "/" + homework[1] + "/approvals/" + approval, school.admin(), null).body(),
                resource(grpc(getApproval, call(getApproval, Map.of("homework_id", homework[1], "id", approval), null),
                        school.admin())));
        GrpcParityContract.assertSameAnswer(
                rest("GET", HOMEWORKS + "/" + homework[0] + "/approvals/" + approval, school.admin(), null),
                grpc(getApproval, call(getApproval, Map.of("homework_id", homework[0], "id", approval), null),
                        school.admin()));
        GrpcParityContract.assertSameAnswer(
                rest("GET", HOMEWORKS + "/" + homework[1] + "/approvals", school.nobody(), null),
                grpc(approvals, call(approvals, "homework_id", homework[1], null), school.nobody()));

        // the reviewer rejects one and approves the other; nothing is left to review on either
        alikeOnTwins(school, school.admin(), "RejectHomework", HOMEWORKS, "reject", homework[0], homework[1],
                "{\"reason\":\"Too long\"}");
        refusedAlike(409, school.admin(), "RejectHomework", HOMEWORKS + "/" + homework[1] + ":reject", homework[1], "{}");
        alikeOnTwins(school, teacher, "PublishHomework", HOMEWORKS, "publish", homework[0], homework[1], "{}");
        JsonNode approved = movedOverGrpc(school, school.admin(), "ApproveHomework", HOMEWORKS, homework[1],
                "{\"reason\":\"Good to go\"}");
        assertThat(approved.path("status").asString()).isEqualTo("STATUS_PUBLISHED");
        refusedAlike(409, school.admin(), "ApproveHomework", HOMEWORKS + "/" + homework[1] + ":approve", homework[1], "{}");

        // a submission is checked by the teacher and answered by the student who handed it in
        String handedIn = "/v1/assessment/homework-submissions";
        UUID studentUser = UUID.randomUUID();
        String student = fx.token(studentUser, school.tenant(), List.of(institution(school).toString()), List.of(),
                "assessment.homework_submission.read", "assessment.homework_submission.reply_to_homework_feedback");
        Answer made = rest("POST", handedIn, school.admin(), "{\"homeworkId\":\"" + homework[1] + "\",\"studentId\":\""
                + UUID.randomUUID() + "\"}");
        assertThat(made.status()).as(made.body().toString()).isEqualTo(201);
        String submission = made.body().path("id").asString();
        fx.inTenant(school.tenant(), () -> database.jdbc().sql("UPDATE assessment.homework_submission SET created_by = "
                + ":user WHERE tenant_id = :tenantId AND id = :id").param("user", studentUser)
                .param("id", UUID.fromString(submission)).update());
        String item = handedIn + "/" + submission;
        refusedAlike(409, student, "ReplyToHomeworkFeedback", item + ":reply", submission, "{\"studentReply\":\"Thanks\"}");
        refusedAlike(403, student, "CheckHomeworkSubmission", item + ":check", submission, "{}");
        refusedAlike(422, school.admin(), "CheckHomeworkSubmission", item + ":check", submission,
                "{\"teacherComments\":\"" + "x".repeat(4001) + "\"}");
        JsonNode checked = movedOverGrpc(school, school.admin(), "CheckHomeworkSubmission", handedIn, submission,
                "{\"teacherComments\":\" Show your working \"}");
        assertThat(checked.path("status").asString()).isEqualTo("STATUS_CHECKED");
        assertThat(checked.path("teacherComments").asString()).isEqualTo("Show your working");
        refusedAlike(412, school.admin(), "CheckHomeworkSubmission", item + ":check", submission,
                "{\"etag\":\"\\\"1\\\"\"}");
        refusedAlike(422, student, "ReplyToHomeworkFeedback", item + ":reply", submission, "{}");
        refusedAlike(403, school.admin(), "ReplyToHomeworkFeedback", item + ":reply", submission,
                "{\"studentReply\":\"Not mine\"}");
        JsonNode replied = movedOverGrpc(school, student, "ReplyToHomeworkFeedback", handedIn, submission,
                "{\"studentReply\":\" I will redo it \"}");
        assertThat(replied.path("studentReply").asString()).isEqualTo("I will redo it");
    }

    @Test
    void anAssignmentAndItsSubmissionsMoveAndAreRefusedAlikeOverBoth() throws Exception {
        School school = School.open();
        // every workflow operation of an assignment from every state it can be in, one row per transport
        for (String status : List.of("draft", "published", "ongoing", "completed", "cancelled")) {
            for (String[] move : new String[][] {{"PublishAssignment", "publish"}, {"ApproveAssignment", "approve"},
                {"RejectAssignment", "reject"}, {"FinishAssignment", "finish"}, {"CancelAssignment", "cancel"},
                {"ResetAssignment", "resetToDraft"}}) {
                alikeOnTwins(school, school.admin(), move[0], ASSIGNMENTS, move[1], assignment(school, status),
                        assignment(school, status), "{\"reason\":\"Parity\"}");
            }
            alikeOnTwins(school, school.admin(), "RequestAssignmentApproval", ASSIGNMENTS, "requestApproval",
                    assignment(school, status), assignment(school, status), "{}");
        }
        String assignment = assignment(school, "published");
        refusedAlike(403, school.nobody(), "FinishAssignment", ASSIGNMENTS + "/" + assignment + ":finish", assignment, "{}");
        refusedAlike(412, school.admin(), "FinishAssignment", ASSIGNMENTS + "/" + assignment + ":finish", assignment,
                "{\"etag\":\"\\\"99\\\"\"}");
        String absent = UUID.randomUUID().toString();
        refusedAlike(404, school.admin(), "ApproveAssignment", ASSIGNMENTS + "/" + absent + ":approve", absent, "{}");

        // and of a submission
        for (String status : List.of("draft", "submitted", "change_requested", "accepted", "rejected", "graded")) {
            for (String[] move : new String[][] {{"SubmitAssignmentSubmission", "submit", "{}"},
                {"AcceptAssignmentSubmission", "accept", "{}"},
                {"RequestSubmissionChanges", "requestChanges", "{\"reason\":\"Add sources\"}"},
                {"RejectAssignmentSubmission", "reject", "{\"reason\":\"Copied\"}"},
                {"ResetAssignmentSubmission", "resetToDraft", "{}"},
                {"GradeAssignmentSubmission", "grade", "{\"marks\":\"18.5\",\"feedback\":\" Good \"}"}}) {
                alikeOnTwins(school, school.admin(), move[0], SUBMISSIONS, move[1], submission(school, assignment, status),
                        submission(school, assignment, status), move[2]);
            }
        }
        String submitted = submission(school, assignment, "submitted");
        String item = SUBMISSIONS + "/" + submitted;
        refusedAlike(422, school.admin(), "GradeAssignmentSubmission", item + ":grade", submitted, "{\"marks\":\"20.01\"}");
        refusedAlike(422, school.admin(), "GradeAssignmentSubmission", item + ":grade", submitted, "{}");
        refusedAlike(403, school.nobody(), "GradeAssignmentSubmission", item + ":grade", submitted, "{\"marks\":\"1\"}");
        JsonNode graded = movedOverGrpc(school, school.admin(), "GradeAssignmentSubmission", SUBMISSIONS, submitted,
                "{\"marks\":\"18.5\",\"feedback\":\" Good \"}");
        assertThat(graded.path("status").asString()).isEqualTo("STATUS_GRADED");
        assertThat(graded.path("marksObtained").asString()).isEqualTo("18.50");
        assertThat(graded.path("feedback").asString()).isEqualTo("Good");
        refusedAlike(409, school.admin(), "GradeAssignmentSubmission", item + ":grade", submitted, "{\"marks\":\"1\"}");
    }

    private static String entries(String... entries) {
        return "{\"entries\":[" + String.join(",", entries) + "]}";
    }

    private static String entry(UUID student, String attendance, String marks) {
        return "{\"studentId\":\"" + student + "\",\"attendanceStatus\":\"" + attendance + "\""
                + (marks == null ? "" : ",\"marksObtained\":\"" + marks + "\"") + "}";
    }

    @Test
    void examResultsAreReadRecordedAndUpdatedAlikeOverBoth() throws Exception {
        School school = School.open();
        AssessmentFixtures fx = fx();
        UUID session = fx.session(school.tenant(), institution(school));
        UUID exam = fx.exam(school.tenant(), session, "held", "100", "40");
        UUID ann = UUID.randomUUID();
        UUID ben = UUID.randomUUID();
        UUID cara = UUID.randomUUID();
        for (UUID student : List.of(ann, ben, cara)) {
            fx.result(school.tenant(), exam, student, "expected", null, "100", null, null);
        }
        // read before the marks: every attendee is "expected"
        MethodDescriptor list = rpc("ListExamResults");
        JsonNode listed = rest("GET", RESULTS + "?sort=createTime", school.admin(), null).body().path("data");
        Answer listedGrpc = grpc(list, request(list).build(), school.admin());
        assertThat(listedGrpc.status()).as(String.valueOf(listedGrpc.code())).isEqualTo(200);
        assertThat(listedGrpc.body().path("items")).hasSize(3).hasSize(listed.size());
        Map<String, JsonNode> overGrpc = new HashMap<>();
        listedGrpc.body().path("items").forEach(row -> overGrpc.put(row.path("id").asString(), row));
        for (JsonNode row : listed) {
            assertThat(row.path("attendanceStatus").asString()).isEqualTo("expected");
            assertThat(overGrpc.get(row.path("id").asString()).path("attendanceStatus").asString())
                    .isEqualTo("ATTENDANCE_STATUS_EXPECTED");
            sameResource(row, overGrpc.get(row.path("id").asString()));
        }
        String first = listed.get(0).path("id").asString();
        MethodDescriptor get = rpc("GetExamResult");
        sameResource(rest("GET", RESULTS + "/" + first, school.admin(), null).body(),
                resource(grpc(get, call(get, "id", first, null), school.admin())));
        String absent = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(rest("GET", RESULTS + "/" + absent, school.admin(), null),
                grpc(get, call(get, "id", absent, null), school.admin()));
        GrpcParityContract.assertSameAnswer(rest("GET", RESULTS + "/" + first, school.nobody(), null),
                grpc(get, call(get, "id", first, null), school.nobody()));
        GrpcParityContract.assertSameAnswer(rest("GET", RESULTS + "/" + first, STRANGER, null),
                grpc(get, call(get, "id", first, null), STRANGER));
        GrpcParityContract.assertSameAnswer(rest("GET", RESULTS + "?filter=nope%20%3D%201", school.admin(), null),
                grpc(list, set(request(list), "filter", "nope = 1").build(), school.admin()));

        // assessment.RecordExamMarks: refused alike, then recorded over gRPC and read the same over REST
        String record = EXAMS + "/" + exam + ":recordMarks";
        refusedAlike(422, school.admin(), "RecordExamMarks", record, exam.toString(),
                entries(entry(ann, "present", null), entry(ben, "absent", "10")));
        refusedAlike(422, school.admin(), "RecordExamMarks", record, exam.toString(),
                entries(entry(UUID.randomUUID(), "present", "5")));
        refusedAlike(422, school.admin(), "RecordExamMarks", record, exam.toString(), "{}");
        refusedAlike(403, school.nobody(), "RecordExamMarks", record, exam.toString(), entries(entry(ann, "present", "1")));
        refusedAlike(404, school.admin(), "RecordExamMarks", EXAMS + "/" + absent + ":recordMarks", absent,
                entries(entry(ann, "present", "1")));
        MethodDescriptor recordMarks = rpc("RecordExamMarks");
        Answer recorded = grpc(recordMarks, call(recordMarks, "exam_id", exam.toString(), json(entries(
                entry(ann, "present", "72.50"), entry(ben, "absent", null), entry(cara, "exempted", null)))),
                school.admin());
        assertThat(recorded.status()).as(String.valueOf(recorded.code())).isEqualTo(200);
        assertThat(recorded.body().path("items")).hasSize(3);
        Map<String, String> attendance = new HashMap<>();
        for (JsonNode row : recorded.body().path("items")) {
            JsonNode overRest = rest("GET", RESULTS + "/" + row.path("id").asString(), school.admin(), null).body();
            sameResource(overRest, row);
            attendance.put(overRest.path("studentId").asString(), overRest.path("attendanceStatus").asString() + " "
                    + row.path("attendanceStatus").asString());
        }
        assertThat(attendance).containsEntry(ann.toString(), "present ATTENDANCE_STATUS_PRESENT")
                .containsEntry(ben.toString(), "absent ATTENDANCE_STATUS_ABSENT")
                .containsEntry(cara.toString(), "exempted ATTENDANCE_STATUS_EXEMPTED");

        // ExamResult has no Create and no Delete: its update is compared here
        JsonNode anns = rest("GET", RESULTS + "?filter=" + java.net.URLEncoder.encode("studentId = \"" + ann + "\"",
                StandardCharsets.UTF_8), school.admin(), null).body().path("data").get(0);
        String id = anns.path("id").asString();
        MethodDescriptor update = rpc("UpdateExamResult");
        ObjectNode changes = json("{\"note\":\"Rechecked\"}");
        for (String etag : List.of("\"99\"", "", anns.path("etag").asString())) {
            Answer overRest = etag.isEmpty() ? rest("PATCH", RESULTS + "/" + id, school.nobody(), changes.toString())
                    : rest("PATCH", RESULTS + "/" + id, school.nobody(), changes.toString(), "If-Match", etag);
            GrpcParityContract.assertSameAnswer(overRest, grpc(update, updating(update, id, etag, changes),
                    school.nobody()));
        }
        GrpcParityContract.assertSameAnswer(
                rest("PATCH", RESULTS + "/" + id, school.admin(), changes.toString(), "If-Match", "\"99\""),
                grpc(update, updating(update, id, "\"99\"", changes), school.admin()));
        GrpcParityContract.assertSameAnswer(rest("PATCH", RESULTS + "/" + id, school.admin(), changes.toString()),
                grpc(update, updating(update, id, "", changes), school.admin()));
        JsonNode updated = resource(grpc(update, updating(update, id, anns.path("etag").asString(), changes),
                school.admin()));
        assertThat(updated.path("note").asString()).isEqualTo("Rechecked");
        sameResource(rest("GET", RESULTS + "/" + id, school.admin(), null).body(), updated);
        new GrpcParityContract(AssessmentProto.getDescriptor(), this::rest, this::channel, bridge,
                new GrpcParityContract.Callers(school.admin(), school.nobody(), STRANGER))
                .patchStates(described("assessment/crud-spec.json", "ExamResult"), id, updated.path("etag").asString());
    }

    /** sis and facility are not running here: after every local check both transports answer 503. */
    @Test
    void anExamsAttendeesRoomsAndMarksEntryAreRefusedAndMovedAlikeOverBoth() throws Exception {
        School school = School.open();
        AssessmentFixtures fx = fx();
        UUID session = fx.session(school.tenant(), institution(school));
        String draft = fx.exam(school.tenant(), session, "draft", "100", "40").toString();
        String scheduled = fx.exam(school.tenant(), session, "scheduled", "100", "40").toString();
        String room = UUID.randomUUID().toString();
        refusedAlike(409, school.admin(), "PopulateExamResults", EXAMS + "/" + draft + ":populateResults", draft, "{}");
        refusedAlike(503, school.admin(), "PopulateExamResults", EXAMS + "/" + scheduled + ":populateResults", scheduled,
                "{}");
        refusedAlike(403, school.nobody(), "PopulateExamResults", EXAMS + "/" + scheduled + ":populateResults", scheduled,
                "{}");
        String allocate = EXAMS + "/" + scheduled + ":allocateRooms";
        refusedAlike(422, school.admin(), "AllocateExamRooms", allocate, scheduled, "{\"roomIds\":[]}");
        refusedAlike(422, school.admin(), "AllocateExamRooms", allocate, scheduled,
                "{\"roomIds\":[\"" + room + "\",\"" + room + "\"]}");
        refusedAlike(409, school.admin(), "AllocateExamRooms", EXAMS + "/" + draft + ":allocateRooms", draft,
                "{\"roomIds\":[\"" + room + "\"]}");
        refusedAlike(409, school.admin(), "AllocateExamRooms", allocate, scheduled, "{\"roomIds\":[\"" + room + "\"]}");
        fx.result(school.tenant(), UUID.fromString(scheduled), UUID.randomUUID(), "expected", null, "100", null, null);
        refusedAlike(503, school.admin(), "AllocateExamRooms", allocate, scheduled, "{\"roomIds\":[\"" + room + "\"]}");

        // the two moves crud-spec.json does not describe, from every state, one row per transport
        for (String status : List.of("draft", "scheduled", "held", "result_updated", "done", "cancelled")) {
            for (String[] move : new String[][] {{"MarkExamHeld", "markHeld"},
                {"MarkExamResultsUpdated", "markResultsUpdated"}}) {
                alikeOnTwins(school, school.admin(), move[0], EXAMS, move[1],
                        fx.exam(school.tenant(), session, status, "100", "40").toString(),
                        fx.exam(school.tenant(), session, status, "100", "40").toString(), "{}");
            }
        }
        JsonNode held = movedOverGrpc(school, school.admin(), "MarkExamHeld", EXAMS, scheduled, "{}");
        assertThat(held.path("status").asString()).isEqualTo("STATUS_HELD");
    }

    @Test
    void resultsAreGeneratedValidatedPublishedAndReadAlikeOverBoth() throws Exception {
        School school = School.open();
        AssessmentFixtures fx = fx();
        UUID tenant = school.tenant();
        UUID session = fx.session(tenant, institution(school));
        UUID scheme = fx.scheme(tenant, institution(school), "percentage_band",
                AssessmentFixtures.band("A", "75.00", "100.00", "10", true),
                AssessmentFixtures.band("C", "40.00", "74.99", "5", true),
                AssessmentFixtures.band("F", "0.00", "39.99", "0", false));
        UUID paper = fx.exam(tenant, session, "done", "100", "40");
        UUID student = UUID.randomUUID();
        fx.result(tenant, paper, student, "present", "80.00", "100", "pass", "4");
        fx.result(tenant, paper, UUID.randomUUID(), "exempted", null, "100", null, "4");
        String template = fx.template(tenant, session, scheme).toString();
        String generate = "/v1/assessment/result-templates/" + template + ":generateResults";

        // assessment.GenerateResults, a 202 operation: started over gRPC, replayed by its request_id, then refused alike
        refusedAlike(403, school.nobody(), "GenerateResults", generate, template, "{}");
        String absent = UUID.randomUUID().toString();
        refusedAlike(404, school.admin(), "GenerateResults",
                "/v1/assessment/result-templates/" + absent + ":generateResults", absent, "{}");
        MethodDescriptor generation = rpc("GenerateResults");
        DynamicMessage keyed = call(generation, Map.of("result_template_id", template, "request_id",
                UUID.randomUUID().toString()), null);
        JsonNode started = resource(grpc(generation, keyed, school.admin()));
        assertThat(started.path("status").asString()).isEqualTo("succeeded");
        assertThat(started.path("module").asString()).isEqualTo("assessment");
        String operation = started.path("operationId").asString();
        String marksheet = started.path("result").path("marksheetId").asString();
        assertThat(resource(grpc(generation, keyed, school.admin())).path("operationId").asString())
                .as("the same request_id answers the first run").isEqualTo(operation);
        refusedAlike(409, school.admin(), "GenerateResults", generate, template, "{}");

        MethodDescriptor get = rpc("GetOperation");
        sameResource(rest("GET", OPERATIONS + "/" + operation, school.admin(), null).body(),
                resource(grpc(get, call(get, "operation_id", operation, null), school.admin())));
        GrpcParityContract.assertSameAnswer(rest("GET", OPERATIONS + "/" + absent, school.admin(), null),
                grpc(get, call(get, "operation_id", absent, null), school.admin()));
        GrpcParityContract.assertSameAnswer(rest("GET", OPERATIONS + "/" + operation, STRANGER, null),
                grpc(get, call(get, "operation_id", operation, null), STRANGER));
        MethodDescriptor operations = rpc("ListOperations");
        Answer listed = grpc(operations, set(request(operations), "kind", "assessment.GenerateResults").build(),
                school.admin());
        JsonNode listedRest = rest("GET", OPERATIONS + "?kind=assessment.GenerateResults", school.admin(), null).body();
        assertThat(listed.status()).isEqualTo(200);
        assertThat(listed.body().path("items")).hasSize(listedRest.path("data").size()).isNotEmpty();
        assertThat(listed.body().path("items").get(0).path("operationId").asString())
                .isEqualTo(listedRest.path("data").get(0).path("operationId").asString());
        assertThat(grpc(operations, set(request(operations), "kind", "assessment.Nothing").build(), school.admin()).body()
                .path("items")).isEmpty();
        GrpcParityContract.assertSameAnswer(rest("GET", OPERATIONS, school.nobody(), null),
                grpc(operations, request(operations).build(), school.nobody()));
        MethodDescriptor cancel = rpc("CancelOperation");
        Answer cancelled = rest("POST", OPERATIONS + "/" + operation + ":cancel", school.admin(), null);
        assertThat(cancelled.status()).as("a finished run is not cancelled").isGreaterThanOrEqualTo(400);
        GrpcParityContract.assertSameAnswer(cancelled,
                grpc(cancel, call(cancel, "operation_id", operation, null), school.admin()));
        GrpcParityContract.assertSameAnswer(rest("POST", OPERATIONS + "/" + absent + ":cancel", school.admin(), null),
                grpc(cancel, call(cancel, "operation_id", absent, null), school.admin()));

        // the marksheet: validated over gRPC, then published with the etag that is REST's If-Match
        String item = MARKSHEETS + "/" + marksheet;
        refusedAlike(412, school.admin(), "ValidateMarksheet", item + ":validate", marksheet, "{\"etag\":\"\\\"99\\\"\"}");
        JsonNode validated = movedOverGrpc(school, school.admin(), "ValidateMarksheet", MARKSHEETS, marksheet, "{}");
        assertThat(validated.path("status").asString()).isEqualTo("STATUS_VALIDATED");
        refusedAlike(409, school.admin(), "ValidateMarksheet", item + ":validate", marksheet, "{}");
        MethodDescriptor publish = rpc("PublishMarksheet");
        ObjectNode visible = json("{\"visibleToGuardians\":true}");
        String etag = rest("GET", item, school.admin(), null).body().path("etag").asString();
        GrpcParityContract.assertSameAnswer(rest("POST", item + ":publish", school.admin(), visible.toString()),
                grpc(publish, call(publish, "marksheet_id", marksheet, visible), school.admin()));
        GrpcParityContract.assertSameAnswer(
                rest("POST", item + ":publish", school.admin(), visible.toString(), "If-Match", "\"99\""),
                grpc(publish, call(publish, "marksheet_id", marksheet, visible.deepCopy().put("etag", "\"99\"")),
                        school.admin()));
        GrpcParityContract.assertSameAnswer(
                rest("POST", item + ":publish", school.nobody(), visible.toString(), "If-Match", etag),
                grpc(publish, call(publish, "marksheet_id", marksheet, visible.deepCopy().put("etag", etag)),
                        school.nobody()));
        JsonNode published = resource(grpc(publish, call(publish, "marksheet_id", marksheet,
                visible.deepCopy().put("etag", etag)), school.admin()));
        JsonNode read = rest("GET", item, school.admin(), null).body();
        sameResource(read, published);
        assertThat(read.path("visibleToGuardians").asBoolean()).isTrue();

        // its lines (no create, update or delete) and a student's result cards read the same
        MethodDescriptor lines = rpc("ListMarksheetLines");
        JsonNode linesRest = rest("GET", "/v1/assessment/marksheet-lines", school.admin(), null).body().path("data");
        Answer linesGrpc = grpc(lines, request(lines).build(), school.admin());
        assertThat(linesGrpc.status()).isEqualTo(200);
        assertThat(linesGrpc.body().path("items")).hasSize(linesRest.size()).isNotEmpty();
        Map<String, JsonNode> lineById = new HashMap<>();
        linesGrpc.body().path("items").forEach(line -> lineById.put(line.path("id").asString(), line));
        for (JsonNode line : linesRest) {
            sameResource(line, lineById.get(line.path("id").asString()));
        }
        String line = linesRest.get(0).path("id").asString();
        MethodDescriptor getLine = rpc("GetMarksheetLine");
        sameResource(rest("GET", "/v1/assessment/marksheet-lines/" + line, school.admin(), null).body(),
                resource(grpc(getLine, call(getLine, "id", line, null), school.admin())));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/assessment/marksheet-lines/" + absent, school.admin(), null),
                grpc(getLine, call(getLine, "id", absent, null), school.admin()));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/assessment/marksheet-lines", school.nobody(), null),
                grpc(lines, request(lines).build(), school.nobody()));

        MethodDescriptor cards = rpc("GetStudentResults");
        String results = "/v1/assessment/students/" + student + "/results";
        JsonNode cardsRest = rest("GET", results, school.admin(), null).body().path("data");
        Answer cardsGrpc = grpc(cards, call(cards, "student_id", student.toString(), null), school.admin());
        assertThat(cardsGrpc.status()).as(String.valueOf(cardsGrpc.code())).isEqualTo(200);
        assertThat(cardsGrpc.body().path("items")).hasSize(1).hasSize(cardsRest.size());
        JsonNode card = cardsGrpc.body().path("items").get(0);
        assertThat(card.path("examSessionId").asString()).isEqualTo(session.toString());
        sameResource(cardsRest.get(0).path("marksheetLine"), card.path("marksheetLine"));
        assertThat(card.path("results")).hasSize(1).hasSize(cardsRest.get(0).path("results").size());
        sameResource(cardsRest.get(0).path("results").get(0), card.path("results").get(0));
        Answer bySession = grpc(cards, call(cards, Map.of("student_id", student.toString(), "exam_session_id",
                UUID.randomUUID().toString()), null), school.admin());
        assertThat(bySession.body().path("items")).as("another session has no card").hasSize(
                rest("GET", results + "?examSessionId=" + UUID.randomUUID(), school.admin(), null).body().path("data").size());
        GrpcParityContract.assertSameAnswer(rest("GET", results, school.nobody(), null),
                grpc(cards, call(cards, "student_id", student.toString(), null), school.nobody()));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/assessment/students/not-a-uuid/results", school.admin(), null),
                grpc(cards, call(cards, "student_id", "not-a-uuid", null), school.admin()));
    }
}
