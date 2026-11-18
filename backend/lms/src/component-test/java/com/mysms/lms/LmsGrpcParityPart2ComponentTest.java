package com.mysms.lms;

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
 * The RPCs of twenty-two of lms's gRPC services (certificates and badges, settings, learning objectives, grading,
 * content items, the gradebook and its logs, assignment groups and peer reviews, rubrics, discussion prompts,
 * participation, resources, e-books and annotations) against the same operation over REST, on real PostgreSQL: for each
 * of their resources in crud-spec.json the standard operations give the same resource over both transports and both
 * refuse alike — 403 without the permission, 404 for an absent row and another tenant's row, 409 for a duplicate, 412
 * for a stale etag, 428 without one, 422 for an invalid body; the read-only resources are read from a seeded row; a
 * group request is accepted and rejected the same way; a create replays under its idempotency key on both; and the
 * operations this release does not serve are 501 on both.
 */
@SpringBootTest(classes = LmsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=LmsGrpcParityPart2ComponentTest")
class LmsGrpcParityPart2ComponentTest {

    /** The resources of crud-spec.json whose services this test covers. */
    private static final Set<String> RESOURCES = Set.of("Annotation", "AnnotationShare", "AssignmentGroup",
            "AssignmentGroupMember", "Badge", "BadgeAward", "Certificate", "CertificateEvaluation", "CertificateRequest",
            "ContentItem", "ContentItemRating", "DiscussionPrompt", "Ebook", "GradeChangeLog", "GradebookEntry",
            "GradingCategory", "ItemOverride", "LearningObjective", "ParticipationActivity", "PeerReview",
            "ReassignmentLog", "RelatedMaterial", "ReleaseGrant", "ResourceCategory", "Rubric", "RubricAssessment",
            "RubricCell", "RubricCriterion", "RubricLevel", "SettingsPrimaryCountry", "SettingsSidebarItem",
            "WebResource");
    private static final String MEMBERS = "/v1/lms/assignment-group-members";
    private static final String GROUP = "5ae2fc4d-faac-562d-b6c8-bc528a768ee7";
    private static final String CONTENT_ITEM = "33f45148-7c83-54fd-8625-2b783212edb7";

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
            channel = GrpcParity.channel("LmsGrpcParityPart2ComponentTest");
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

    // ---- the standard operations, resource by resource

    private GrpcParityContract contract() {
        return new GrpcParityContract(LmsProto.getDescriptor(), this::rest, this::channel, bridge,
                new GrpcParityContract.Callers(ADMIN, NOBODY, STRANGER));
    }

    /**
     * The standard operations of each resource these services own, as many as it serves: from a row created over gRPC,
     * or from the spec's seeded row where the API creates none.
     */
    @TestFactory
    List<DynamicNode> everyStandardRpcAnswersLikeRest() throws Exception {
        List<DynamicNode> tests = new ArrayList<>();
        for (GrpcParityContract.Resource resource : GrpcParityContract.resources("crud-spec.json")) {
            if (!RESOURCES.contains(resource.name())) {
                continue;
            }
            tests.add(DynamicTest.dynamicTest(resource.name(), () -> {
                fixtures(TENANT, resource.fixtures());
                fixtures(OTHER_TENANT, resource.fixtures());
                if (!resource.serves("Create")) {
                    fixtures(TENANT, resource.seed());
                }
                contract().verify(resource);
            }));
        }
        assertThat(tests).hasSize(RESOURCES.size());
        return tests;
    }

    /** A content item of kind exercise is created and read alike: ITEM_KIND_EXERCISE over gRPC, "exercise" over REST. */
    @Test
    @SuppressWarnings("unchecked")
    void anExerciseItemIsCreatedAndReadAlike() throws Exception {
        GrpcParityContract.Resource item = GrpcParityContract.resources("crud-spec.json").stream()
                .filter(resource -> "ContentItem".equals(resource.name())).findFirst().orElseThrow();
        fixtures(TENANT, item.fixtures());
        String exercise = UUID.randomUUID().toString();
        fixtures(TENANT, List.of("INSERT INTO lms.programming_exercise (tenant_id, title, problem_statement, language,"
                + " row_version, id) VALUES (:tenant, 'Parity exercise', 'Add two numbers.', 'python', 1, '" + exercise
                + "')"));
        ObjectNode body = item.create().deepCopy().put("itemKind", "exercise").put("exerciseId", exercise);
        body.remove("lessonId");
        MethodDescriptor create = rpc("CreateContentItem");
        Answer created = grpc(create, bridge.reply(request(create), "content_item", JSON.convertValue(body, Map.class))
                .build(), ADMIN);
        assertThat(created.status()).as(String.valueOf(created.code())).isEqualTo(200);
        JsonNode row = created.body().path("contentItem");
        assertThat(row.path("itemKind").asString()).isEqualTo("ITEM_KIND_EXERCISE");
        Answer read = rest("GET", item.collection() + "/" + row.path("id").asString(), ADMIN, null);
        assertThat(read.body().path("itemKind").asString()).isEqualTo("exercise");
        GrpcParity.assertSameResource(read.body(), row);
        MethodDescriptor get = rpc("GetContentItem");
        GrpcParity.assertSameResource(read.body(), grpc(get, set(request(get), "id", row.path("id").asString()).build(),
                ADMIN).body().path("contentItem"));
    }

    // ---- the workflow of a group request

    private String memberRequest() throws Exception {
        Answer member = rest("POST", MEMBERS, ADMIN, "{\"assignmentGroupId\":\"" + GROUP + "\",\"userId\":\""
                + UUID.randomUUID() + "\",\"contentItemId\":\"" + CONTENT_ITEM + "\"}");
        assertThat(member.status()).as(String.valueOf(member.body())).isEqualTo(201);
        String id = member.body().path("id").asString();
        fixtures(TENANT, List.of("UPDATE lms.assignment_group_member SET status = 'requested' WHERE tenant_id = :tenant"
                + " AND id = '" + id + "'"));
        return id;
    }

    private static DynamicMessage member(MethodDescriptor rpc, String id, String etag, String reason) {
        DynamicMessage.Builder request = set(request(rpc), "assignment_group_member_id", id);
        if (etag != null) {
            set(request, "etag", etag);
        }
        if (reason != null) {
            set(request, "reason", reason);
        }
        return request.build();
    }

    @Test
    void acceptingAndRejectingAGroupRequestAnswersLikeRest() throws Exception {
        for (GrpcParityContract.Resource resource : GrpcParityContract.resources("crud-spec.json")) {
            if ("AssignmentGroupMember".equals(resource.name())) {
                fixtures(TENANT, resource.fixtures());
            }
        }
        for (String[] transition : List.of(new String[] {"AcceptGroupRequest", ":accept", "{}", null, "STATUS_ACTIVE"},
                new String[] {"RejectGroupRequest", ":reject", "{\"reason\":\"Not this group\"}", "Not this group",
                    "STATUS_REJECTED"})) {
            MethodDescriptor rpc = rpc(transition[0]);
            String action = transition[1];
            String overRest = memberRequest();
            String overGrpc = memberRequest();
            String absent = UUID.randomUUID().toString();
            // refusals: no permission, an absent row, another tenant's caller, a malformed id, a stale etag
            GrpcParityContract.assertSameAnswer(rest("POST", MEMBERS + "/" + overRest + action, NOBODY, transition[2]),
                    grpc(rpc, member(rpc, overGrpc, null, transition[3]), NOBODY));
            GrpcParityContract.assertSameAnswer(rest("POST", MEMBERS + "/" + absent + action, ADMIN, transition[2]),
                    grpc(rpc, member(rpc, absent, null, transition[3]), ADMIN));
            GrpcParityContract.assertSameAnswer(rest("POST", MEMBERS + "/" + overRest + action, STRANGER, transition[2]),
                    grpc(rpc, member(rpc, overGrpc, null, transition[3]), STRANGER));
            GrpcParityContract.assertSameAnswer(rest("POST", MEMBERS + "/not-a-uuid" + action, ADMIN, transition[2]),
                    grpc(rpc, member(rpc, "not-a-uuid", null, transition[3]), ADMIN));
            Answer stale = rest("POST", MEMBERS + "/" + overRest + action, ADMIN, "{\"etag\":\"\\\"99\\\"\"}");
            assertThat(stale.status()).isEqualTo(412);
            GrpcParityContract.assertSameAnswer(stale, grpc(rpc, member(rpc, overGrpc, "\"99\"", null), ADMIN));
            // the transition itself, then the guard: a request already decided is refused alike
            Answer changedRest = rest("POST", MEMBERS + "/" + overRest + action, ADMIN, transition[2]);
            Answer changedGrpc = grpc(rpc, member(rpc, overGrpc, null, transition[3]), ADMIN);
            assertThat(changedRest.status()).as(String.valueOf(changedRest.body())).isEqualTo(200);
            assertThat(changedGrpc.status()).as(String.valueOf(changedGrpc.code())).isEqualTo(200);
            JsonNode changed = changedGrpc.body().path("assignmentGroupMember");
            assertThat(changed.path("status").asString()).isEqualTo(transition[4]);
            GrpcParity.assertSameResource(rest("GET", MEMBERS + "/" + overGrpc, ADMIN, null).body(), changed);
            Answer again = rest("POST", MEMBERS + "/" + overRest + action, ADMIN, transition[2]);
            assertThat(again.status()).isEqualTo(409);
            GrpcParityContract.assertSameAnswer(again, grpc(rpc, member(rpc, overGrpc, null, transition[3]), ADMIN));
        }
    }

    // ---- idempotency

    @Test
    void aCreateReplaysUnderItsIdempotencyKeyOnBoth() throws Exception {
        String key = UUID.randomUUID().toString();
        String body = "{\"code\":\"REPLAY1\",\"name\":\"Replayed over REST\"}";
        Answer first = rest("POST", "/v1/lms/resource-categories", ADMIN, body, "Idempotency-Key", key);
        Answer replayed = rest("POST", "/v1/lms/resource-categories", ADMIN, body, "Idempotency-Key", key);
        assertThat(first.status()).as(String.valueOf(first.body())).isEqualTo(201);
        assertThat(replayed.status()).isEqualTo(201);
        assertThat(replayed.body().path("id").asString()).isEqualTo(first.body().path("id").asString());

        MethodDescriptor create = rpc("CreateResourceCategory");
        String requestId = UUID.randomUUID().toString();
        DynamicMessage request = set(bridge.reply(request(create), "resource_category",
                Map.of("code", "REPLAY2", "name", "Replayed over gRPC")), "request_id", requestId).build();
        Answer firstGrpc = grpc(create, request, ADMIN);
        Answer replayedGrpc = grpc(create, request, ADMIN);
        assertThat(firstGrpc.status()).as(String.valueOf(firstGrpc.code())).isEqualTo(200);
        assertThat(replayedGrpc.status()).as("a replay, not the 409 of a second create").isEqualTo(200);
        assertThat(replayedGrpc.body().path("resourceCategory").path("id").asString())
                .isEqualTo(firstGrpc.body().path("resourceCategory").path("id").asString());
        // the same key with another request is refused alike
        DynamicMessage other = set(bridge.reply(request(create), "resource_category",
                Map.of("code", "REPLAY3", "name", "Another request")), "request_id", requestId).build();
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/lms/resource-categories", ADMIN,
                "{\"code\":\"REPLAY4\",\"name\":\"Another request\"}", "Idempotency-Key", key), grpc(create, other, ADMIN));
    }

    // ---- the operations answered without a token

    @Test
    void thePublicOperationsNotServedAre501OnBothWithoutAToken() throws Exception {
        String certificate = UUID.randomUUID().toString();
        MethodDescriptor verify = rpc("VerifyCertificate");
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/lms/certificates/" + certificate + ":verify", null, null),
                grpc(verify, set(request(verify), "certificate_id", certificate).build(), null));
        MethodDescriptor settings = rpc("GetPublicSettings");
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/lms/public-settings", null, null),
                grpc(settings, request(settings).build(), null));
        // a provider's signed callback: an unknown provider is 404 on both transports, before any signature check
        MethodDescriptor webhook = rpc("ReceiveEbookConversion");
        Answer overRest = rest("POST", "/v1/lms/integrations/conversion/acme:webhook", null, "{}", "X-Signature", "none",
                "X-Timestamp", "1");
        assertThat(overRest.status()).isEqualTo(404);
        GrpcParityContract.assertSameAnswer(overRest, grpc(webhook, set(request(webhook), "provider", "acme").build(), null));
        MethodDescriptor download = rpc("DownloadCertificate");
        assertThat(grpc(download, set(request(download), "certificate_id", certificate).build(), null).status())
                .as("authentication first, as on REST").isEqualTo(401);
    }
}
