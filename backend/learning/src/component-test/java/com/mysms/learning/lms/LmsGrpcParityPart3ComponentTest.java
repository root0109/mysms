package com.mysms.learning.lms;

import com.mysms.learning.LearningApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Descriptors.Descriptor;
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
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.lms.v1.LmsProto;
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
 * Every RPC of the third part of lms's gRPC services — external tools and their resources, the forums (sections,
 * topics, votes, report categories, reports), release rules, student alerts, content view sessions, bookmarks,
 * mentorships, plagiarism accounts and reports, question blank answers, hotspot regions and pools, import/export jobs,
 * the access log, the drive and the long-running operations — against the same operation over REST, on real PostgreSQL.
 * The resources of crud-spec.json with the five standard operations go through {@link GrpcParityContract}; those the
 * contract serves in part (no Get, no Delete, read-only, partitioned) through the same checks for the operations they
 * have; then the state transitions with their refusals, the operations this release does not serve (501 on both), the
 * operations store and a {@code request_id} replay.
 */
@SpringBootTest(classes = LearningApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=LmsGrpcParityPart3ComponentTest")
class LmsGrpcParityPart3ComponentTest {

    /** The resources of this part that have Create, Get, List, Update and Delete and list without a range. */
    private static final Set<String> STANDARD = Set.of("Bookmark", "DriveItem", "ExternalTool", "ExternalToolInstitution",
            "ExternalToolLink", "ExternalToolOption", "ExternalToolResource", "ExternalToolRole", "ExternalToolSetting",
            "ForumPost", "ForumPostReport", "ForumReportCategory", "ForumSection", "ForumTopic", "Mentorship",
            "PlagiarismAccount", "PlagiarismUserLink", "QuestionBlankAnswer", "QuestionHotspotRegion", "QuestionPool",
            "QuestionPoolItem", "ReleaseRule", "ReleaseRuleCondition", "ReleaseRuleTarget", "StudentAlert");
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

    /** A REST call; a JSON body unless the headers name another content type. */
    private Answer rest(String method, String path, String token, String body, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.setHeader("Content-Type", "application/json");
        }
        if (token != null) {
            request.setHeader("Authorization", "Bearer " + token);
        }
        for (int i = 0; i < headers.length; i += 2) {
            request.setHeader(headers[i], headers[i + 1]);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode json = response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body());
        return new Answer(response.statusCode(), response.statusCode() >= 400 ? json.path("code").asString() : null,
                json);
    }

    private Channel channel() {
        if (channel == null) {
            channel = GrpcParity.channel("LmsGrpcParityPart3ComponentTest");
        }
        return channel;
    }

    private Answer grpc(MethodDescriptor rpc, DynamicMessage.Builder request, String token) {
        return GrpcParity.call(channel(), rpc, request.build(), token);
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

    private GrpcParityContract contract() {
        return new GrpcParityContract(LmsProto.getDescriptor(), this::rest, this::channel, bridge,
                new GrpcParityContract.Callers(ADMIN, NOBODY, STRANGER));
    }

    // ---- crud-spec.json

    private static JsonNode spec(String name) throws Exception {
        JsonNode spec = JSON.readTree(LmsGrpcParityPart3ComponentTest.class.getClassLoader()
                .getResourceAsStream("lms/crud-spec.json"));
        for (JsonNode resource : spec.path("resources")) {
            if (name.equals(resource.path("name").asString())) {
                return resource;
            }
        }
        throw new IllegalArgumentException("no resource " + name + " in crud-spec.json");
    }

    private static List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.asString()));
        return values;
    }

    /** The resource's fixture rows in the tenant (and in the other tenant, for the stranger's refusals). */
    private JsonNode prepared(String name) throws Exception {
        JsonNode resource = spec(name);
        fixtures(TENANT, strings(resource.path("fixtures")));
        fixtures(OTHER_TENANT, strings(resource.path("fixtures")));
        return resource;
    }

    // ---- the standard operations, resource by resource

    @TestFactory
    List<DynamicNode> everyStandardRpcAnswersLikeRest() throws Exception {
        List<DynamicNode> tests = new ArrayList<>();
        for (GrpcParityContract.Resource resource : GrpcParityContract.resources("lms/crud-spec.json")) {
            if (STANDARD.contains(resource.name())) {
                tests.add(DynamicTest.dynamicTest(resource.name(), () -> {
                    prepared(resource.name());
                    contract().verify(resource);
                }));
            }
        }
        assertThat(tests).as("every standard resource of this part is in crud-spec.json").hasSize(STANDARD.size());
        return tests;
    }

    // ---- resources served in part

    /** An unlike is a create with {@code is_like} false, which the proto carries (an optional bool). */
    @Test
    void forumPostVotesWithoutGetAnswerLikeRest() throws Exception {
        JsonNode vote = prepared("ForumPostVote");
        assertThat(vote.path("create").path("isLike").asBoolean()).as("the spec creates an unlike").isFalse();
        partial(vote, "");
    }

    @Test
    void driveItemSharesWithoutGetAnswerLikeRest() throws Exception {
        partial(prepared("DriveItemShare"), "");
    }

    /** Partitioned by month: both transports refuse a list without the range, and list alike within it. */
    @Test
    void contentViewSessionsWithoutDeleteAnswerLikeRest() throws Exception {
        JsonNode resource = prepared("ContentViewSession");
        MethodDescriptor list = rpc("ListContentViewSessions");
        Answer unbounded = rest("GET", resource.path("collection").asString(), ADMIN, null);
        assertThat(unbounded.status()).isEqualTo(400);
        same(unbounded, grpc(list, request(list), ADMIN));
        partial(resource, "startedAt >= \"2025-12-11T09:00:00Z\" AND startedAt <= \"2026-02-09T09:00:00Z\"");
    }

    @Test
    void importExportJobsWithoutUpdateAndDeleteAnswerLikeRest() throws Exception {
        partial(prepared("ImportExportJob"), "");
    }

    /**
     * The standard operations a resource has ({@code ops} of crud-spec.json), with their refusals, over both
     * transports — {@link GrpcParityContract#verify} for a resource without one of the five: the row is read through
     * Get where there is one and through List otherwise.
     */
    private void partial(JsonNode resource, String range) throws Exception {
        String name = resource.path("name").asString();
        Set<String> ops = Set.copyOf(strings(resource.path("ops")));
        String collection = resource.path("collection").asString();
        ObjectNode body = (ObjectNode) resource.path("create");
        MethodDescriptor create = rpc("Create" + name);
        String field = resourceField(create);
        String json = create.getOutputType().findFieldByName(field).getJsonName();

        Answer created = grpc(create, createRequest(create, field, body, body), ADMIN);
        assertThat(created.status()).as("%s: %s", name, created.code()).isEqualTo(200);
        JsonNode row = created.body().path(json);
        String id = row.path("id").asString();
        JsonNode read = read(ops, collection, range, id);
        GrpcParity.assertSameResource(read, row);
        if (resource.path("duplicateConflicts").asBoolean()) {
            same(rest("POST", collection, ADMIN, body.toString()), grpc(create, createRequest(create, field, body, body), ADMIN));
        }
        Answer invalid = rest("POST", collection, ADMIN, "{}");
        assertThat(invalid.status()).as("%s: an empty body", name).isGreaterThanOrEqualTo(400);
        same(invalid, grpc(create, createRequest(create, field, JSON.createObjectNode(), body), ADMIN));
        same(rest("POST", collection, NOBODY, body.toString()), grpc(create, createRequest(create, field, body, body), NOBODY));

        if (ops.contains("Get")) {
            MethodDescriptor get = rpc("Get" + name);
            Answer got = grpc(get, keyed(get, body, id), ADMIN);
            assertThat(got.status()).as("%s get: %s", name, got.code()).isEqualTo(200);
            GrpcParity.assertSameResource(read, got.body().path(json));
            same(rest("GET", collection + "/" + UUID.randomUUID(), ADMIN, null),
                    grpc(get, keyed(get, body, UUID.randomUUID().toString()), ADMIN));
            same(rest("GET", collection + "/not-a-uuid", ADMIN, null), grpc(get, keyed(get, body, "not-a-uuid"), ADMIN));
            same(rest("GET", collection + "/" + id, NOBODY, null), grpc(get, keyed(get, body, id), NOBODY));
            same(rest("GET", collection + "/" + id, STRANGER, null), grpc(get, keyed(get, body, id), STRANGER));
        }

        MethodDescriptor list = contract().listRpc(name);
        Answer listed = rest("GET", collection + query(range, "size=1"), ADMIN, null);
        Answer listedGrpc = grpc(list, set(set(keyed(list, body, null), "filter", range), "pageable",
                pageable(list, 1)), ADMIN);
        assertThat(listedGrpc.status()).as("%s list: %s", name, listedGrpc.code()).isEqualTo(200);
        assertThat(listed.body().path("data")).hasSize(1);
        assertThat(listedGrpc.body().path("items")).hasSize(1);
        GrpcParity.assertSameResource(listed.body().path("data").get(0), listedGrpc.body().path("items").get(0));
        assertThat(listedGrpc.body().path("pagination").path("page").asInt())
                .isEqualTo(listed.body().path("paging").path("page").asInt());
        same(rest("GET", collection + "?filter=" + encode("nope = 1"), ADMIN, null),
                grpc(list, set(keyed(list, body, null), "filter", "nope = 1"), ADMIN));
        same(rest("GET", collection + query(range, ""), NOBODY, null),
                grpc(list, set(keyed(list, body, null), "filter", range), NOBODY));
        assertThat(grpc(list, set(keyed(list, body, null), "filter", range), STRANGER).body().path("items"))
                .as("another tenant's rows").isEmpty();

        String etag = read.path("etag").asString();
        if (ops.contains("Update")) {
            MethodDescriptor update = rpc("Update" + name);
            ObjectNode changes = (ObjectNode) resource.path("update");
            String item = collection + "/" + id;
            same(rest("PATCH", item, ADMIN, changes.toString(), "If-Match", "\"99\""),
                    grpc(update, updateRequest(update, body, id, changes, "99"), ADMIN));
            same(rest("PATCH", item, ADMIN, changes.toString()),
                    grpc(update, updateRequest(update, body, id, changes, ""), ADMIN));
            same(rest("PATCH", item, NOBODY, changes.toString(), "If-Match", etag),
                    grpc(update, updateRequest(update, body, id, changes, etag), NOBODY));
            Answer updated = grpc(update, updateRequest(update, body, id, changes, etag), ADMIN);
            assertThat(updated.status()).as("%s update: %s", name, updated.code()).isEqualTo(200);
            JsonNode reread = read(ops, collection, range, id);
            GrpcParity.assertSameResource(reread, updated.body().path(json));
            etag = contract().patchStates(GrpcParityContract.Resource.of(resource), id, reread.path("etag").asString());
        }
        if (ops.contains("Delete")) {
            MethodDescriptor delete = rpc("Delete" + name);
            String item = collection + "/" + id;
            same(rest("DELETE", item, ADMIN, null, "If-Match", "\"99\""),
                    grpc(delete, set(keyed(delete, body, id), "etag", "99"), ADMIN));
            same(rest("DELETE", item, ADMIN, null), grpc(delete, keyed(delete, body, id), ADMIN));
            same(rest("DELETE", item, NOBODY, null, "If-Match", etag),
                    grpc(delete, set(keyed(delete, body, id), "etag", etag), NOBODY));
            assertThat(grpc(delete, set(keyed(delete, body, id), "etag", etag), ADMIN).status()).as("%s delete", name)
                    .isEqualTo(200);
            assertThat(find(rest("GET", collection + query(range, ""), ADMIN, null).body().path("data"), id))
                    .as("the deleted row").isNull();
            same(rest("DELETE", item, ADMIN, null, "If-Match", etag),
                    grpc(delete, set(keyed(delete, body, id), "etag", etag), ADMIN));
        }
    }

    /** The row over REST: through Get, or found in the list where the resource has no Get. */
    private JsonNode read(Set<String> ops, String collection, String range, String id) throws Exception {
        if (ops.contains("Get")) {
            Answer got = rest("GET", collection + "/" + id, ADMIN, null);
            assertThat(got.status()).isEqualTo(200);
            return got.body();
        }
        JsonNode row = find(rest("GET", collection + query(range, "size=500"), ADMIN, null).body().path("data"), id);
        assertThat(row).as("row %s in the list", id).isNotNull();
        return row;
    }

    private static JsonNode find(JsonNode rows, String id) {
        for (JsonNode row : rows) {
            if (id.equals(row.path("id").asString())) {
                return row;
            }
        }
        return null;
    }

    private static String query(String filter, String rest) {
        String query = filter.isEmpty() ? rest : "filter=" + encode(filter) + (rest.isEmpty() ? "" : "&" + rest);
        return query.isEmpty() ? "" : "?" + query;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    // ---- building requests from REST bodies

    /** The request's field that holds the resource: its one message field of lms's own contract. */
    private static String resourceField(MethodDescriptor create) {
        for (FieldDescriptor field : create.getInputType().getFields()) {
            if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE
                    && field.getMessageType().getFile().equals(LmsProto.getDescriptor())) {
                return field.getName();
            }
        }
        throw new IllegalStateException(create.getFullName() + " carries no resource");
    }

    /** A create request: the resource converted from the REST body like a DTO, the path ids from {@code path}. */
    @SuppressWarnings("unchecked")
    private DynamicMessage.Builder createRequest(MethodDescriptor create, String field, ObjectNode body, ObjectNode path) {
        return withPathIds(bridge.reply(request(create), field, JSON.convertValue(body, Map.class)), path);
    }

    /** An update request: the {@code <Resource>Update} message of a REST PATCH body, with its parent ids, id and etag. */
    private DynamicMessage.Builder updateRequest(MethodDescriptor update, ObjectNode body, String id, ObjectNode changes,
                                                 String etag) {
        DynamicMessage.Builder request = withPathIds(contract().changes(update, changes), body);
        set(request, "id", id);
        return set(request, "etag", etag);
    }

    /** A request keyed by the row id (none for a list), with the parent ids a nested resource's path carries. */
    private static DynamicMessage.Builder keyed(MethodDescriptor rpc, ObjectNode body, String id) {
        DynamicMessage.Builder request = withPathIds(request(rpc), body);
        return id == null ? request : set(request, "id", id);
    }

    /**
     * The parent ids a REST path carries, from {@code body}: the plain string keys of the request, never a field an
     * update message changes (those have presence or are wrappers).
     */
    private static DynamicMessage.Builder withPathIds(DynamicMessage.Builder request, ObjectNode body) {
        for (FieldDescriptor field : request.getDescriptorForType().getFields()) {
            if (field.getName().endsWith("_id") && !"request_id".equals(field.getName())
                    && field.getJavaType() == FieldDescriptor.JavaType.STRING && !field.hasPresence()
                    && body.has(field.getJsonName())) {
                request.setField(field, body.path(field.getJsonName()).asString());
            }
        }
        return request;
    }

    private static DynamicMessage pageable(MethodDescriptor list, int size) {
        Descriptor type = list.getInputType().findFieldByName("pageable").getMessageType();
        return DynamicMessage.newBuilder(type).setField(type.findFieldByName("size"), size).build();
    }

    // ---- read-only resources (rows the workflows write)

    @Test
    void plagiarismReportsAnswerLikeRest() throws Exception {
        JsonNode resource = prepared("PlagiarismReport");
        fixtures(TENANT, strings(resource.path("seed")));
        String id = resource.path("seedId").asString();
        String collection = resource.path("collection").asString();
        MethodDescriptor get = rpc("GetPlagiarismReport");
        Answer read = rest("GET", collection + "/" + id, ADMIN, null);
        assertThat(read.status()).isEqualTo(200);
        Answer got = grpc(get, set(request(get), "id", id), ADMIN);
        assertThat(got.status()).as(got.code()).isEqualTo(200);
        GrpcParity.assertSameResource(read.body(), got.body().path("plagiarismReport"));
        same(rest("GET", collection + "/" + UUID.randomUUID(), ADMIN, null),
                grpc(get, set(request(get), "id", UUID.randomUUID().toString()), ADMIN));
        same(rest("GET", collection + "/not-a-uuid", ADMIN, null), grpc(get, set(request(get), "id", "not-a-uuid"), ADMIN));
        same(rest("GET", collection + "/" + id, NOBODY, null), grpc(get, set(request(get), "id", id), NOBODY));
        same(rest("GET", collection + "/" + id, STRANGER, null), grpc(get, set(request(get), "id", id), STRANGER));

        MethodDescriptor list = rpc("ListPlagiarismReports");
        Answer listed = rest("GET", collection, ADMIN, null);
        Answer listedGrpc = grpc(list, request(list), ADMIN);
        assertThat(listedGrpc.status()).as(listedGrpc.code()).isEqualTo(200);
        GrpcParity.assertSameResource(find(listed.body().path("data"), id), find(listedGrpc.body().path("items"), id));
        same(rest("GET", collection + "?filter=" + encode("nope = 1"), ADMIN, null),
                grpc(list, set(request(list), "filter", "nope = 1"), ADMIN));
        same(rest("GET", collection, NOBODY, null), grpc(list, request(list), NOBODY));
    }

    @Test
    void forumReadStatesAnswerLikeRest() throws Exception {
        JsonNode resource = prepared("ForumReadState");
        fixtures(TENANT, strings(resource.path("seed")));
        String id = resource.path("seedId").asString();
        String topic = resource.path("create").path("forumTopicId").asString();
        String collection = resource.path("collection").asString();
        MethodDescriptor list = rpc("ListForumReadStates");
        Answer listed = rest("GET", collection, ADMIN, null);
        Answer listedGrpc = grpc(list, set(request(list), "forum_topic_id", topic), ADMIN);
        assertThat(listedGrpc.status()).as(listedGrpc.code()).isEqualTo(200);
        assertThat(listedGrpc.body().path("items")).hasSize(listed.body().path("data").size());
        GrpcParity.assertSameResource(find(listed.body().path("data"), id), find(listedGrpc.body().path("items"), id));
        same(rest("GET", collection + "?filter=" + encode("nope = 1"), ADMIN, null),
                grpc(list, set(set(request(list), "forum_topic_id", topic), "filter", "nope = 1"), ADMIN));
        same(rest("GET", collection, NOBODY, null), grpc(list, set(request(list), "forum_topic_id", topic), NOBODY));
        String absent = UUID.randomUUID().toString();
        same(rest("GET", collection.replace(topic, absent), ADMIN, null),
                grpc(list, set(request(list), "forum_topic_id", absent), ADMIN));
        same(rest("GET", collection.replace(topic, "not-a-uuid"), ADMIN, null),
                grpc(list, set(request(list), "forum_topic_id", "not-a-uuid"), ADMIN));
    }

    // ---- the state transitions

    @Test
    void hidingAndUnhidingAForumTopicAnswersLikeRest() throws Exception {
        JsonNode resource = prepared("ForumTopic");
        String[] ids = new String[2];
        for (int i = 0; i < ids.length; i++) {
            Answer topic = rest("POST", "/v1/lms/forum-topics", ADMIN, resource.path("create").toString());
            assertThat(topic.status()).as(topic.body().toString()).isEqualTo(201);
            ids[i] = topic.body().path("id").asString();
        }
        transition("HideForumTopic", "/v1/lms/forum-topics/%s:hide", "forum_topic_id", "forumTopic", ids, "isHidden", true);
        transition("UnhideForumTopic", "/v1/lms/forum-topics/%s:unhide", "forum_topic_id", "forumTopic", ids, "isHidden",
                false);
    }

    @Test
    void cancellingAnImportExportJobAnswersLikeRest() throws Exception {
        JsonNode resource = prepared("ImportExportJob");
        String[] ids = new String[2];
        for (int i = 0; i < ids.length; i++) {
            Answer job = rest("POST", "/v1/lms/import-export-jobs", ADMIN, resource.path("create").toString());
            assertThat(job.status()).as(job.body().toString()).isEqualTo(201);
            ids[i] = job.body().path("id").asString();
        }
        fixtures(TENANT, List.of("UPDATE lms.import_export_job SET status = 'pending' WHERE tenant_id = :tenant"));
        transition("CancelImportExportJob", "/v1/lms/import-export-jobs/%s:cancel", "import_export_job_id",
                "importExportJob", ids, "status", "STATUS_CANCELLED");
    }

    /**
     * One state transition on two rows in its guarded state: refused alike for a stale etag, without the permission,
     * for an absent row, a malformed id and another tenant's caller; then the first row changed over REST and the
     * second over gRPC read the same; and a second attempt is refused alike (the row left the guarded state).
     */
    private void transition(String name, String path, String idField, String resourceJson, String[] ids, String sets,
                            Object value) throws Exception {
        MethodDescriptor rpc = rpc(name);
        same(rest("POST", path.formatted(ids[0]), ADMIN, "{\"etag\":\"\\\"99\\\"\"}"),
                grpc(rpc, set(set(request(rpc), idField, ids[0]), "etag", "\"99\""), ADMIN));
        same(rest("POST", path.formatted(ids[0]), NOBODY, "{}"), grpc(rpc, set(request(rpc), idField, ids[0]), NOBODY));
        same(rest("POST", path.formatted(ids[0]), STRANGER, "{}"), grpc(rpc, set(request(rpc), idField, ids[0]), STRANGER));
        String absent = UUID.randomUUID().toString();
        same(rest("POST", path.formatted(absent), ADMIN, "{}"), grpc(rpc, set(request(rpc), idField, absent), ADMIN));
        same(rest("POST", path.formatted("not-a-uuid"), ADMIN, "{}"),
                grpc(rpc, set(request(rpc), idField, "not-a-uuid"), ADMIN));

        Answer overRest = rest("POST", path.formatted(ids[0]), ADMIN, "{}");
        assertThat(overRest.status()).as("%s over REST: %s", name, overRest.body()).isEqualTo(200);
        String item = path.substring(0, path.indexOf(':'));
        String etag = rest("GET", item.formatted(ids[1]), ADMIN, null).body().path("etag").asString();
        Answer overGrpc = grpc(rpc, set(set(request(rpc), idField, ids[1]), "etag", etag), ADMIN);
        assertThat(overGrpc.status()).as("%s over gRPC: %s", name, overGrpc.code()).isEqualTo(200);
        JsonNode changed = overGrpc.body().path(resourceJson);
        // proto3 leaves a false out of the message
        assertThat(value instanceof Boolean ? (Object) changed.path(sets).asBoolean(false) : changed.path(sets).asString())
                .isEqualTo(value);
        GrpcParity.assertSameResource(rest("GET", item.formatted(ids[1]), ADMIN, null).body(), changed);
        assertThat(overRest.body().path(sets)).isEqualTo(rest("GET", item.formatted(ids[1]), ADMIN, null).body().path(sets));

        Answer again = rest("POST", path.formatted(ids[0]), ADMIN, "{}");
        assertThat(again.status()).as("%s twice", name).isEqualTo(409);
        same(again, grpc(rpc, set(request(rpc), idField, ids[1]), ADMIN));
    }

    /**
     * Signed provider callbacks answer alike on both transports. The LTI outcome callback carries the tool's OAuth
     * 1.0a signature in its own Authorization header, which the edge leaves to the operation (its contract declares
     * the header): REST reaches the operation with it, and both answer 503 while grade return is not configured. A
     * plagiarism callback that is none of the provider's events is taken and dropped on both.
     */
    @Test
    void theProviderWebhooksAnswerAlikeOnBoth() throws Exception {
        MethodDescriptor outcome = rpc("ReceiveToolOutcome");
        Answer outcomeRest = rest("POST", "/v1/lms/integrations/lti:outcome", null, "<imsx_POXEnvelopeRequest/>",
                "Content-Type", "application/xml", "Authorization",
                "OAuth oauth_consumer_key=\"tool\", oauth_signature_method=\"HMAC-SHA1\", oauth_signature=\"c2ln\"");
        assertThat(outcomeRest.status()).as("the edge lets the signed callback through").isEqualTo(503);
        assertThat(outcomeRest.code()).isEqualTo("LTI_NOT_CONFIGURED");
        same(outcomeRest, grpc(outcome, request(outcome), null));
        MethodDescriptor webhook = rpc("ReceivePlagiarismWebhook");
        Answer webhookRest = rest("POST", "/v1/lms/integrations/plagiarism/turnitin:webhook", null, "{}", "X-Signature",
                "sig", "X-Timestamp", "1767225600");
        assertThat(webhookRest.status()).as(String.valueOf(webhookRest.body())).isEqualTo(204);
        DynamicMessage.Builder sameBody = set(request(webhook), "provider", "turnitin");
        sameBody.setField(webhook.getInputType().findFieldByName("raw_body"),
                com.google.protobuf.ByteString.copyFromUtf8("{}"));
        same(webhookRest, grpc(webhook, sameBody, null));
    }

    // ---- the long-running operations

    @Test
    void theOperationsStoreAnswersLikeRest() throws Exception {
        String[] running = {UUID.randomUUID().toString(), UUID.randomUUID().toString()};
        String done = UUID.randomUUID().toString();
        String insert = "INSERT INTO lms.operation (id, tenant_id, operation_name, status, progress_percent,"
                + " started_by_user_id, started_at, finished_at, created_at, updated_at, row_version) VALUES ('%s', :tenant,"
                + " '%s', '%s', %s, '" + UUID.randomUUID() + "', now() - interval '%d minutes', %s, now(), now(), 1)";
        fixtures(TENANT, List.of(insert.formatted(running[0], "report.run", "running", "40", 4, "NULL"),
                insert.formatted(running[1], "report.run", "running", "NULL", 3, "NULL"),
                insert.formatted(done, "import.rows", "succeeded", "100", 2, "now()")));
        String base = "/v1/lms/operations";

        MethodDescriptor get = rpc("GetOperation");
        Answer read = rest("GET", base + "/" + done, ADMIN, null);
        assertThat(read.status()).as(read.body().toString()).isEqualTo(200);
        Answer got = grpc(get, set(request(get), "operation_id", done), ADMIN);
        assertThat(got.status()).as(got.code()).isEqualTo(200);
        GrpcParity.assertSameResource(read.body(), got.body().path("result"));
        same(rest("GET", base + "/" + UUID.randomUUID(), ADMIN, null),
                grpc(get, set(request(get), "operation_id", UUID.randomUUID().toString()), ADMIN));
        same(rest("GET", base + "/not-a-uuid", ADMIN, null), grpc(get, set(request(get), "operation_id", "not-a-uuid"), ADMIN));
        same(rest("GET", base + "/" + done, NOBODY, null), grpc(get, set(request(get), "operation_id", done), NOBODY));
        same(rest("GET", base + "/" + done, STRANGER, null), grpc(get, set(request(get), "operation_id", done), STRANGER));

        MethodDescriptor list = rpc("ListOperations");
        Answer first = rest("GET", base + "?size=2", ADMIN, null);
        Answer firstGrpc = grpc(list, set(request(list), "pageable", pageable(list, 2)), ADMIN);
        assertThat(firstGrpc.status()).as(firstGrpc.code()).isEqualTo(200);
        assertThat(operationIds(firstGrpc.body().path("items"))).hasSize(2)
                .isEqualTo(operationIds(first.body().path("data")));
        GrpcParity.assertSameResource(first.body().path("data").get(0), firstGrpc.body().path("items").get(0));
        String cursor = firstGrpc.body().path("pagination").path("nextCursor").asString();
        GrpcParity.assertSameCursor(first.body().path("paging").path("next").asString(), cursor);
        Descriptor pageable = list.getInputType().findFieldByName("pageable").getMessageType();
        Answer secondGrpc = grpc(list, set(request(list), "pageable", DynamicMessage.newBuilder(pageable)
                .setField(pageable.findFieldByName("page"), 2).setField(pageable.findFieldByName("size"), 2)
                .setField(pageable.findFieldByName("cursor"), cursor).build()), ADMIN);
        assertThat(operationIds(secondGrpc.body().path("items"))).isEqualTo(operationIds(
                rest("GET", first.body().path("paging").path("next").asString(), ADMIN, null).body().path("data")));
        assertThat(operationIds(grpc(list, set(request(list), "kind", "import.rows"), ADMIN).body().path("items")))
                .isEqualTo(operationIds(rest("GET", base + "?kind=import.rows", ADMIN, null).body().path("data")))
                .containsExactly(done);
        same(rest("GET", base, NOBODY, null), grpc(list, request(list), NOBODY));
        assertThat(grpc(list, request(list), STRANGER).body().path("items")).as("another tenant").isEmpty();

        MethodDescriptor cancel = rpc("CancelOperation");
        same(rest("POST", base + "/" + done + ":cancel", ADMIN, null),
                grpc(cancel, set(request(cancel), "operation_id", done), ADMIN));
        same(rest("POST", base + "/" + running[0] + ":cancel", NOBODY, null),
                grpc(cancel, set(request(cancel), "operation_id", running[0]), NOBODY));
        same(rest("POST", base + "/" + UUID.randomUUID() + ":cancel", ADMIN, null),
                grpc(cancel, set(request(cancel), "operation_id", UUID.randomUUID().toString()), ADMIN));
        Answer cancelledRest = rest("POST", base + "/" + running[0] + ":cancel", ADMIN, null);
        assertThat(cancelledRest.status()).as(cancelledRest.body().toString()).isEqualTo(202);
        Answer cancelledGrpc = grpc(cancel, set(request(cancel), "operation_id", running[1]), ADMIN);
        assertThat(cancelledGrpc.status()).as(cancelledGrpc.code()).isEqualTo(200);
        assertThat(cancelledGrpc.body().path("result").path("status").asString()).isEqualTo("cancelled");
        GrpcParity.assertSameResource(rest("GET", base + "/" + running[1], ADMIN, null).body(),
                cancelledGrpc.body().path("result"));
        Answer again = rest("POST", base + "/" + running[0] + ":cancel", ADMIN, null);
        assertThat(again.status()).isEqualTo(409);
        same(again, grpc(cancel, set(request(cancel), "operation_id", running[1]), ADMIN));
    }

    private static List<String> operationIds(JsonNode operations) {
        List<String> ids = new ArrayList<>();
        operations.forEach(operation -> ids.add(operation.path("operationId").asString()));
        return ids;
    }

    // ---- idempotency

    /** A keyed POST: the retry is answered with the first answer, the key reused for another request is refused. */
    @Test
    void aRetriedCreateWithItsRequestIdIsReplayedOnBoth() throws Exception {
        JsonNode resource = prepared("Bookmark");
        ObjectNode body = (ObjectNode) resource.path("create");
        ObjectNode other = body.deepCopy().put("userId", UUID.randomUUID().toString());
        MethodDescriptor create = rpc("CreateBookmark");
        String key = UUID.randomUUID().toString();
        Answer first = grpc(create, set(createRequest(create, "bookmark", body, body), "request_id", key), ADMIN);
        assertThat(first.status()).as(first.code()).isEqualTo(200);
        Answer retry = grpc(create, set(createRequest(create, "bookmark", body, body), "request_id", key), ADMIN);
        assertThat(retry.body()).as("the retry is the first answer").isEqualTo(first.body());

        String restKey = UUID.randomUUID().toString();
        Answer firstRest = rest("POST", "/v1/lms/bookmarks", ADMIN, body.toString(), "Idempotency-Key", restKey);
        assertThat(firstRest.status()).as(firstRest.body().toString()).isEqualTo(201);
        Answer retryRest = rest("POST", "/v1/lms/bookmarks", ADMIN, body.toString(), "Idempotency-Key", restKey);
        assertThat(retryRest.body().path("id")).isEqualTo(firstRest.body().path("id"));
        Answer reused = rest("POST", "/v1/lms/bookmarks", ADMIN, other.toString(), "Idempotency-Key", restKey);
        assertThat(reused.status()).isEqualTo(422);
        same(reused, grpc(create, set(createRequest(create, "bookmark", other, other), "request_id", key), ADMIN));
        same(rest("POST", "/v1/lms/bookmarks", ADMIN, body.toString(), "Idempotency-Key", "not-a-uuid"),
                grpc(create, set(createRequest(create, "bookmark", body, body), "request_id", "not-a-uuid"), ADMIN));
    }
}
