package com.mysms.careers;

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
import com.mysms.careers.config.CareersDatabaseConfig;
import com.mysms.careers.v1.CareersProto;
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
 * Every RPC of careers' four gRPC services against the same operation over REST, on real PostgreSQL: for each resource
 * of crud-spec.json the create, get, list, update and delete give the same resource over both transports, and both
 * refuse alike — 403 without the permission, 404 for an absent row and another tenant's row, 409 for a duplicate, 412
 * for a stale etag, 428 without one, 422 for an invalid body; a job opening — which this release creates, changes and
 * deletes but does not read — row for row; its close and reopen transitions with their refusals, the five operations
 * this release does not serve (501), and a keyed create replayed by its {@code request_id}, the same way.
 */
@SpringBootTest(classes = CareersApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=CareersGrpcParityComponentTest")
class CareersGrpcParityComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final String ADMIN = token(TENANT, List.of("careers.*"));
    private static final String NOBODY = token(TENANT, List.of());
    private static final String STRANGER = token(OTHER_TENANT, List.of("careers.*"));
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;


    @Autowired
    private ProtoBridge bridge;

    @Autowired
    @Qualifier(CareersDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "careers");
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
        return token(UUID.randomUUID(), tenant, permissions);
    }

    private static String token(UUID user, UUID tenant, List<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.toString());
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
            channel = GrpcParity.channel("CareersGrpcParityComponentTest");
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
        return JSON.readTree(CareersGrpcParityComponentTest.class.getClassLoader().getResourceAsStream("crud-spec.json"));
    }

    private static List<String> fixturesOf(JsonNode resource) {
        List<String> fixtures = new ArrayList<>();
        resource.path("fixtures").forEach(statement -> fixtures.add(statement.asString()));
        return fixtures;
    }

    // ---- the standard operations, resource by resource

    private GrpcParityContract contract() {
        return new GrpcParityContract(CareersProto.getDescriptor(), this::rest, this::channel, bridge,
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
        assertThat(tests).hasSize(7);
        return tests;
    }

    // ---- the state transitions

    @TestFactory
    List<DynamicNode> everyStateTransitionAnswersLikeRest() throws Exception {
        List<DynamicNode> tests = new ArrayList<>();
        for (JsonNode resource : spec().path("resources")) {
            for (JsonNode transition : resource.path("transitions")) {
                tests.add(DynamicTest.dynamicTest(transition.path("operation").asString(),
                        () -> transitionAnswersLikeRest(resource, transition)));
            }
        }
        assertThat(tests).as("close and reopen a job opening").hasSize(2);
        return tests;
    }

    /**
     * One transition on two rows of the resource, one per transport: refused alike outside its guard state (409), for
     * a stale etag (412), without the permission (403), for another tenant's row and an absent row (404) and a
     * malformed id (400); then done on both, giving the same row; and refused alike once done (409).
     */
    private void transitionAnswersLikeRest(JsonNode resource, JsonNode transition) throws Exception {
        fixtures(TENANT, fixturesOf(resource));
        String collection = resource.path("collection").asString();
        String table = resource.path("table").asString();
        String operation = transition.path("operation").asString();
        MethodDescriptor rpc = rpc(operation.substring(operation.indexOf('.') + 1));
        String target = transition.path("sets").path("status").asString();
        ObjectNode body = (ObjectNode) transition.path("body");
        String[] ids = new String[2];
        for (int i = 0; i < 2; i++) {
            Answer created = rest("POST", collection, ADMIN, resource.path("create").toString());
            assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
            ids[i] = created.body().path("id").asString();
        }
        fixtures(TENANT, List.of("UPDATE " + table + " SET status = '" + target + "' WHERE tenant_id = :tenant AND id = '"
                + ids[0] + "'"));
        sameAnswer(transition, rpc, ids[0], body, null, ADMIN, 409);
        for (String id : ids) {
            List<String> prepare = new ArrayList<>();
            transition.path("prepare").forEach(statement -> prepare.add(statement.asString().replace("{id}", id)
                    + " AND tenant_id = :tenant"));
            fixtures(TENANT, prepare);
        }
        sameAnswer(transition, rpc, ids[0], body, "\"99\"", ADMIN, 412);
        sameAnswer(transition, rpc, ids[0], body, null, NOBODY, 403);
        sameAnswer(transition, rpc, ids[0], body, null, STRANGER, 404);
        sameAnswer(transition, rpc, UUID.randomUUID().toString(), body, null, ADMIN, 404);
        sameAnswer(transition, rpc, "not-a-uuid", body, null, ADMIN, 400);

        Answer overRest = rest("POST", path(transition, ids[0]), ADMIN, body.toString());
        Answer overGrpc = grpc(rpc, transitionRequest(rpc, ids[1], body, null), ADMIN);
        assertThat(overRest.status()).as(overRest.body().toString()).isEqualTo(200);
        assertThat(overGrpc.status()).as("%s: %s", operation, overGrpc.code()).isEqualTo(200);
        assertThat(overRest.body().path("status").asString()).isEqualTo(target);
        JsonNode row = overGrpc.body().path(rpc.getOutputType().getFields().get(0).getJsonName());
        assertThat(row.path("status").asString()).isEqualTo("STATUS_" + target.toUpperCase(Locale.ROOT));
        GrpcParity.assertSameResource(rest("GET", collection + "/" + ids[1], ADMIN, null).body(), row);
        if (!transition.path("guard").path("status").toString().contains("\"" + target + "\"")) {
            sameAnswer(transition, rpc, ids[0], body, null, ADMIN, 409);
            sameAnswer(transition, rpc, ids[1], body, null, ADMIN, 409);
        }
    }

    private static String path(JsonNode transition, String id) {
        return transition.path("path").asString().replace("{id}", id);
    }

    /** The transition's request: the path id (the request's first field), the REST body's fields, the etag if any. */
    private static DynamicMessage transitionRequest(MethodDescriptor rpc, String id, ObjectNode body, String etag) {
        DynamicMessage.Builder request = request(rpc);
        request.setField(rpc.getInputType().getFields().get(0), id);
        body.properties().forEach(field -> set(request, field.getKey(), field.getValue().asString()));
        if (etag != null) {
            set(request, "etag", etag);
        }
        return request.build();
    }

    private void sameAnswer(JsonNode transition, MethodDescriptor rpc, String id, ObjectNode body, String etag,
                            String token, int expected) throws Exception {
        ObjectNode restBody = body.deepCopy();
        if (etag != null) {
            restBody.put("etag", etag);
        }
        Answer overRest = rest("POST", path(transition, id), token, restBody.toString());
        assertThat(overRest.status()).as("%s over REST: %s", rpc.getName(), overRest.body()).isEqualTo(expected);
        GrpcParityContract.assertSameAnswer(overRest, grpc(rpc, transitionRequest(rpc, id, body, etag), token));
    }

    // ---- guest reads and hiring steps, and a replay

    @Test
    void guestReadsAndHiringStepsAnswerAlikeOnBoth() throws Exception {
        String opening = UUID.randomUUID().toString();
        String application = UUID.randomUUID().toString();
        // a guest who names no school reads nothing: 404 on both transports
        MethodDescriptor list = rpc("ListJobOpenings");
        same(rest("GET", "/v1/careers/job-openings", null, null), grpc(list, request(list).build(), null), 404);
        MethodDescriptor get = rpc("GetJobOpening");
        same(rest("GET", "/v1/careers/job-openings/" + opening, null, null),
                grpc(get, set(request(get), "id", opening).build(), null), 404);
        MethodDescriptor report = rpc("ReportJobOpening");
        same(rest("POST", "/v1/careers/job-openings/" + opening + ":report", ADMIN, "{\"reason\":\"Spam\"}"),
                grpc(report, set(set(request(report), "job_opening_id", opening), "reason", "Spam").build(), ADMIN),
                404);
        // no SMTP in this test: refused before the application is looked up
        MethodDescriptor email = rpc("EmailApplicant");
        same(rest("POST", "/v1/careers/job-applications/" + application + ":email", ADMIN,
                        "{\"subject\":\"Interview\",\"message\":\"Monday at ten.\"}"),
                grpc(email, set(set(set(request(email), "application_id", application), "subject", "Interview"), "message",
                        "Monday at ten.").build(), ADMIN), 503);
        MethodDescriptor status = rpc("UpdateApplicationStatus");
        DynamicMessage setStatus = set(set(set(request(status), "job_opening_id", opening), "job_application_id",
                application), "status", status.getInputType().findFieldByName("status").getEnumType()
                .findValueByName("STATUS_SHORTLISTED")).build();
        same(rest("POST", "/v1/careers/job-openings/" + opening + "/applications/" + application + ":setStatus", ADMIN,
                "{\"status\":\"shortlisted\"}"), grpc(status, setStatus, ADMIN), 404);
        assertThat(grpc(status, setStatus, null).status()).as("authentication first, as on REST").isEqualTo(401);
        assertThat(grpc(status, setStatus, NOBODY).status()).as("then the permission, as on REST").isEqualTo(403);
    }

    /** The applicant is the caller on both transports, and another applicant's application is absent on both. */
    @Test
    void anApplicantIsTheCallerAndReachesOnlyTheirOwnApplicationOnBoth() throws Exception {
        Answer opened = rest("POST", "/v1/careers/job-openings", ADMIN, """
                {"title":"Chemistry teacher","location":"Pune","countryId":"%s","employmentType":"full_time",
                 "description":"Teach chemistry","companyName":"Green Valley School","companyWebsite":"https://example.org",
                 "companyEmail":"jobs@example.org","companyLogoFileId":"%s"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID()));
        assertThat(opened.status()).as(opened.body().toString()).isEqualTo(201);
        String opening = opened.body().path("id").asString();
        UUID studentA = UUID.randomUUID();
        UUID studentB = UUID.randomUUID();
        List<String> applying = List.of("careers.job_application.create", "careers.job_application.read");
        String a = token(studentA, TENANT, applying);
        String b = token(studentB, TENANT, applying);
        String ofB = rest("POST", "/v1/careers/job-openings/" + opening + "/applications", b,
                "{\"resumeFileId\":\"" + UUID.randomUUID() + "\"}").body().path("id").asString();

        MethodDescriptor create = rpc("CreateJobApplication");
        DynamicMessage.Builder application = DynamicMessage.newBuilder(create.getInputType()
                .findFieldByName("job_application").getMessageType());
        Answer forged = grpc(create, set(set(request(create), "job_opening_id", opening), "job_application",
                set(set(application, "applicant_user_id", studentB.toString()), "resume_file_id",
                        UUID.randomUUID().toString()).build()).build(), a);
        assertThat(forged.status()).as(String.valueOf(forged.code())).isEqualTo(200);
        assertThat(forged.body().path("jobApplication").path("applicantUserId").asString())
                .as("the applicant comes from the token").isEqualTo(studentA.toString());

        MethodDescriptor get = rpc("GetJobApplication");
        Answer overGrpc = grpc(get, set(set(request(get), "job_opening_id", opening), "id", ofB).build(), a);
        same(rest("GET", "/v1/careers/job-openings/" + opening + "/applications/" + ofB, a, null), overGrpc, 404);
        String own = forged.body().path("jobApplication").path("id").asString();
        assertThat(grpc(get, set(set(request(get), "job_opening_id", opening), "id", own).build(), a).status())
                .isEqualTo(200);
    }

    private static void same(Answer overRest, Answer overGrpc, int expected) {
        assertThat(overRest.status()).as(String.valueOf(overRest.body())).isEqualTo(expected);
        GrpcParityContract.assertSameAnswer(overRest, overGrpc);
    }

    @Test
    void aKeyedCreateIsReplayedByItsRequestIdLikeRestsIdempotencyKey() throws Exception {
        MethodDescriptor create = rpc("CreateFunctionArea");
        DynamicMessage.Builder area = DynamicMessage.newBuilder(create.getInputType().findFieldByName("function_area")
                .getMessageType());
        String key = UUID.randomUUID().toString();
        DynamicMessage first = set(set(request(create), "function_area", set(set(area, "code", "REPLAY1"), "name",
                "Replayed area").build()), "request_id", key).build();
        Answer created = grpc(create, first, ADMIN);
        Answer replayed = grpc(create, first, ADMIN);
        assertThat(created.status()).as(String.valueOf(created.code())).isEqualTo(200);
        assertThat(replayed.body()).as("the first answer, not a 409 for the duplicate code").isEqualTo(created.body());

        String restKey = UUID.randomUUID().toString();
        String restBody = "{\"code\":\"REPLAY2\",\"name\":\"Replayed over REST\"}";
        Answer overRest = rest("POST", "/v1/careers/function-areas", ADMIN, restBody, "Idempotency-Key", restKey);
        assertThat(overRest.status()).isEqualTo(201);
        assertThat(rest("POST", "/v1/careers/function-areas", ADMIN, restBody, "Idempotency-Key", restKey).body().path("id"))
                .isEqualTo(overRest.body().path("id"));
        // the key reused for another request is refused alike
        DynamicMessage other = set(set(request(create), "function_area", set(set(area, "code", "REPLAY3"), "name",
                "Another area").build()), "request_id", key).build();
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/careers/function-areas", ADMIN,
                "{\"code\":\"REPLAY3\",\"name\":\"Another area\"}", "Idempotency-Key", restKey), grpc(create, other, ADMIN));
        assertThat(grpc(create, other, ADMIN).code()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
    }
}
