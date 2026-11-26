package com.mysms.identity.iam;

import com.mysms.identity.IdentityApplication;
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
import com.mysms.identity.iam.config.IamDatabaseConfig;
import com.mysms.iam.v1.IamProto;
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
 * Every RPC of iam's six gRPC services against the same operation over REST, on real PostgreSQL: for each resource of
 * crud-spec.json the create, get, list, update and
 * delete give the same resource over both transports and refuse alike (403 without the permission, 404 for an absent
 * row and another tenant's row, 409 for a duplicate, 412 for a stale etag, 428 without one, 422 for an invalid body);
 * the account's state transitions move and refuse alike; the setting values, the two catalogues and the operations
 * answer alike; and the account operations — sign-in, refresh (also with a personal API token), sign-out, the caller's
 * own settings and second factors, recovery and verification, impersonation, API tokens, role grants and the role
 * switch, the login-name check and the user import — answer and refuse as REST does, record the caller's address and
 * client like REST, and hand out exactly REST's tokens and secrets. A request_id replays like an Idempotency-Key.
 */
@SpringBootTest(classes = IdentityApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IamGrpcParityComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    /** A real gRPC port: iam records the calling peer's address, which only a socket has. */
    private static final int GRPC_PORT = freePort();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String TENANT_CODE = "parity";
    private static final String ADMIN_PASSWORD = "bootstrap-admin-secret-7";
    private static final String READ_USERS = "iam.user_account.read";
    /** The test calls the service on localhost, over IPv4 or IPv6. */
    private static final List<String> LOOPBACK = List.of("127.0.0.1", "::1");
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final String ADMIN = token(TENANT, List.of("*"));
    private static final String NOBODY = token(TENANT, List.of());
    private static final String STRANGER = token(OTHER_TENANT, List.of("*"));
    /** The resources whose standard operations are compared by their own tests below. */
    private static final Set<String> OWN_TESTS = Set.of("UserSession");
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;


    @Autowired
    private ProtoBridge bridge;

    @Autowired
    @Qualifier(IamDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    private IamHttp iam;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "iam", "people");
        registry.add("grpc.server.port", () -> GRPC_PORT);
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.iam.token.signing-key", TOKENS::privateKeyPem);
        registry.add("mysms.iam.bootstrap.tenant-code", () -> TENANT_CODE);
        registry.add("mysms.iam.bootstrap.admin-username", () -> "admin");
        registry.add("mysms.iam.bootstrap.admin-password", () -> ADMIN_PASSWORD);
    }

    @BeforeEach
    void connect() {
        iam = new IamHttp(port, database, TENANT_CODE);
    }

    private static int freePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    @AfterAll
    static void closeChannel() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    /** A tenant-wide caller of the tenant holding the permissions (every institution, tenant-level rows). */
    private static String token(UUID tenant, List<String> permissions) {
        return tokenOf(UUID.randomUUID(), tenant, permissions);
    }

    private static String tokenOf(UUID user, UUID tenant, List<String> permissions) {
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
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
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
            channel = GrpcParity.channel(GRPC_PORT);
        }
        return channel;
    }

    private GrpcParityContract contract() {
        return new GrpcParityContract(IamProto.getDescriptor(), this::rest, this::channel, bridge,
                new GrpcParityContract.Callers(ADMIN, NOBODY, STRANGER));
    }

    /**
     * An RPC called with its request written as REST JSON: each property is the request field of that JSON name,
     * converted through the service's own bridge (so a resource travels as its REST body, an enum as its REST value).
     */
    @SuppressWarnings("unchecked")
    private Answer grpc(String rpc, String token, String json) {
        MethodDescriptor method = contract().rpc(rpc);
        DynamicMessage.Builder request = GrpcParityContract.request(method);
        JSON.readTree(json).properties().forEach(property -> {
            FieldDescriptor field = method.getInputType().getFields().stream()
                    .filter(f -> f.getJsonName().equals(property.getKey())).findFirst()
                    .orElseThrow(() -> new AssertionError(rpc + " has no field " + property.getKey()));
            bridge.reply(request, field.getName(), JSON.convertValue(property.getValue(), Object.class));
        });
        return GrpcParity.call(channel(), method, request.build(), token);
    }

    private static void same(Answer overRest, Answer overGrpc) {
        GrpcParityContract.assertSameAnswer(overRest, overGrpc);
    }

    private static JsonNode ok(Answer answer) {
        assertThat(answer.status()).as("%s %s", answer.code(), answer.body()).isLessThan(300);
        return answer.body();
    }

    private static List<String> ids(JsonNode items) {
        List<String> ids = new ArrayList<>();
        items.forEach(item -> ids.add(item.path("id").asString()));
        return ids;
    }

    private static List<String> operations(JsonNode items) {
        List<String> ids = new ArrayList<>();
        items.forEach(item -> ids.add(item.path("operationId").asString()));
        return ids;
    }

    private void fixtures(UUID tenant, List<String> statements) {
        CallContext context = CallContext.anonymous(TRACE).withIdentity(tenant, null);
        CallContextHolder.callAs(context, () -> TestTransactions.of(database).write(() -> {
            statements.forEach(statement -> database.jdbc().sql(statement).param("tenant", tenant).update());
            return null;
        }));
    }

    private static JsonNode spec() throws Exception {
        return JSON.readTree(IamGrpcParityComponentTest.class.getClassLoader().getResourceAsStream("iam/crud-spec.json"));
    }

    private static List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.asString()));
        return values;
    }

    // ---- the standard operations, resource by resource

    @TestFactory
    List<DynamicNode> everyStandardRpcAnswersLikeRest() throws Exception {
        List<DynamicNode> tests = new ArrayList<>();
        for (GrpcParityContract.Resource resource : GrpcParityContract.resources("iam/crud-spec.json")) {
            if (OWN_TESTS.contains(resource.name())) {
                continue;
            }
            tests.add(DynamicTest.dynamicTest(resource.name(), () -> {
                fixtures(TENANT, resource.fixtures());
                fixtures(OTHER_TENANT, resource.fixtures());
                contract().verify(resource);
            }));
        }
        assertThat(tests).hasSize(8);
        return tests;
    }

    /** UserSession has no Create (a sign-in starts one) and is its owner's alone: read and ended from a seeded row. */
    @Test
    void aSessionIsReadListedAndEndedAlikeOverBoth() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        fixtures(tenant, List.of(
                "INSERT INTO iam.user_account (tenant_id, username, row_version, id) VALUES (:tenant, 'holder', 1, '"
                        + user + "')",
                "INSERT INTO iam.user_session (user_id, session_ref, tenant_id, row_version, id) VALUES ('" + user
                        + "', 'ref-1', :tenant, 1, '" + session + "')"));
        String self = tokenOf(user, tenant, List.of("*"));
        String other = token(tenant, List.of("*"));
        String path = "/v1/iam/users/" + user + "/sessions";
        String key = "{\"userAccountId\":\"" + user + "\",\"id\":\"" + session + "\"";

        Answer read = rest("GET", path + "/" + session, self, null);
        GrpcParity.assertSameResource(ok(read), ok(grpc("GetUserSession", self, key + "}")).path("userSession"));
        same(rest("GET", path + "/" + session, other, null), grpc("GetUserSession", other, key + "}"));
        same(rest("GET", path + "/" + session, STRANGER, null), grpc("GetUserSession", STRANGER, key + "}"));
        Answer listed = rest("GET", path, self, null);
        JsonNode listedGrpc = ok(grpc("ListUserSessions", self, "{\"userAccountId\":\"" + user + "\"}"));
        assertThat(ids(listedGrpc.path("items"))).isEqualTo(ids(listed.body().path("data"))).hasSize(1);
        GrpcParity.assertSameResource(listed.body().path("data").get(0), listedGrpc.path("items").get(0));

        same(rest("DELETE", path + "/" + session, self, null, "If-Match", "\"99\""),
                grpc("DeleteUserSession", self, key + ",\"etag\":\"99\"}"));
        same(rest("DELETE", path + "/" + session, self, null), grpc("DeleteUserSession", self, key + "}"));
        String nobody = token(tenant, List.of());
        same(rest("DELETE", path + "/" + session, nobody, null, "If-Match", read.body().path("etag").asString()),
                grpc("DeleteUserSession", nobody, key + ",\"etag\":"
                        + JSON.writeValueAsString(read.body().path("etag").asString()) + "}"));
        ok(grpc("DeleteUserSession", self, key + ",\"etag\":" + JSON.writeValueAsString(read.body().path("etag")
                .asString()) + "}"));
        same(rest("GET", path + "/" + session, self, null), grpc("GetUserSession", self, key + "}"));
    }

    // ---- the state transitions of crud-spec.json

    /**
     * Every transition crud-spec.json describes, on an account of a tenant of its own: both transports refuse alike
     * for an absent account, without the permission and with a stale etag; the move made over gRPC reads the same over
     * REST and sets what the specification says; the move repeated is answered alike; and REST makes the same move
     * from the same prepared state.
     */
    @TestFactory
    List<DynamicNode> everyTransitionAnswersLikeRest() throws Exception {
        List<DynamicNode> tests = new ArrayList<>();
        for (JsonNode resource : spec().path("resources")) {
            for (JsonNode transition : resource.path("transitions")) {
                tests.add(DynamicTest.dynamicTest(transition.path("operation").asString(),
                        () -> verifyTransition(resource, transition)));
            }
        }
        assertThat(tests).hasSize(3);
        return tests;
    }

    private void verifyTransition(JsonNode resource, JsonNode transition) throws Exception {
        UUID tenant = UUID.randomUUID();
        String admin = token(tenant, List.of("*"));
        String nobody = token(tenant, List.of());
        String collection = resource.path("collection").asString();
        Answer created = rest("POST", collection, admin, resource.path("create").toString());
        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
        String id = created.body().path("id").asString();
        String rpc = transition.path("operation").asString().substring("iam.".length());
        String path = transition.path("path").asString();
        ObjectNode body = (ObjectNode) transition.path("body");
        List<String> prepare = strings(transition.path("prepare")).stream().map(sql -> sql.replace("{id}", id))
                .toList();
        fixtures(tenant, prepare);

        String absent = UUID.randomUUID().toString();
        same(rest("POST", path.replace("{id}", absent), admin, body.toString()),
                grpc(rpc, admin, withId(body, absent).toString()));
        same(rest("POST", path.replace("{id}", id), nobody, body.toString()),
                grpc(rpc, nobody, withId(body, id).toString()));
        ObjectNode stale = body.deepCopy().put("etag", "\"99\"");
        same(rest("POST", path.replace("{id}", id), admin, stale.toString()),
                grpc(rpc, admin, withId(stale, id).toString()));

        JsonNode moved = ok(grpc(rpc, admin, withId(body, id).toString())).path("userAccount");
        JsonNode read = rest("GET", collection + "/" + id, admin, null).body();
        GrpcParity.assertSameResource(read, moved);
        transition.path("sets").properties().forEach(set -> assertThat(read.path(set.getKey()).asString())
                .as(set.getKey()).isEqualTo(set.getValue().asString()));
        same(rest("POST", path.replace("{id}", id), admin, body.toString()),
                grpc(rpc, admin, withId(body, id).toString()));

        fixtures(tenant, prepare);
        Answer overRest = rest("POST", path.replace("{id}", id), admin, body.toString());
        assertThat(overRest.status()).as(overRest.body().toString()).isEqualTo(200);
        transition.path("sets").properties().forEach(set -> assertThat(overRest.body().path(set.getKey()).asString())
                .as(set.getKey()).isEqualTo(set.getValue().asString()));
    }

    private static ObjectNode withId(ObjectNode body, String id) {
        return body.deepCopy().put("userAccountId", id);
    }

    // ---- settings, catalogues, operations, idempotency

    @Test
    void settingValuesAnswerLikeRest() throws Exception {
        String admin = iam.token("admin", ADMIN_PASSWORD);
        iam.user("valueless", null);
        String nobody = iam.token("valueless", IamHttp.PASSWORD);
        String body = "{\"scopeType\":\"tenant\",\"settingKey\":\"ui.theme\",\"value\":\"dark\"}";
        JsonNode created = ok(grpc("CreateSettingValue", admin, "{\"settingValue\":" + body + "}")).path("settingValue");
        String id = created.path("id").asString();
        String path = "/v1/iam/setting-values";
        Answer read = rest("GET", path + "/" + id, admin, null);
        GrpcParity.assertSameResource(ok(read), created);
        GrpcParity.assertSameResource(read.body(),
                ok(grpc("GetSettingValue", admin, "{\"id\":\"" + id + "\"}")).path("settingValue"));
        same(rest("POST", path, admin, body), grpc("CreateSettingValue", admin, "{\"settingValue\":" + body + "}"));
        same(rest("POST", path, admin, "{}"), grpc("CreateSettingValue", admin, "{}"));
        same(rest("POST", path, nobody, body), grpc("CreateSettingValue", nobody, "{\"settingValue\":" + body + "}"));
        same(rest("GET", path + "/" + id, STRANGER, null), grpc("GetSettingValue", STRANGER, "{\"id\":\"" + id + "\"}"));
        Answer listed = rest("GET", path, admin, null);
        JsonNode listedGrpc = ok(grpc("ListSettingValues", admin, "{}"));
        assertThat(ids(listedGrpc.path("items"))).isEqualTo(ids(listed.body().path("data"))).contains(id);
        same(rest("GET", path + "?filter=nope%20%3D%201", admin, null),
                grpc("ListSettingValues", admin, "{\"filter\":\"nope = 1\"}"));

        String etag = JSON.writeValueAsString(read.body().path("etag").asString());
        String update = "{\"updateMask\":\"value\",\"settingValue\":{\"value\":\"light\",\"id\":\"" + id + "\",\"etag\":";
        same(rest("PATCH", path + "/" + id, admin, "{\"value\":\"light\"}", "If-Match", "\"99\""),
                grpc("UpdateSettingValue", admin, update + "\"99\"}}"));
        same(rest("PATCH", path + "/" + id, admin, "{\"value\":\"light\"}"),
                grpc("UpdateSettingValue", admin, update + "\"\"}}"));
        same(rest("PATCH", path + "/" + id, admin, "{\"value\":\"nope\"}", "If-Match", read.body().path("etag")
                .asString()), grpc("UpdateSettingValue", admin, update.replace("light", "nope") + etag + "}}"));
        JsonNode updated = ok(grpc("UpdateSettingValue", admin, update + etag + "}}")).path("settingValue");
        Answer reread = rest("GET", path + "/" + id, admin, null);
        GrpcParity.assertSameResource(reread.body(), updated);
        assertThat(updated.path("value").asString()).isEqualTo("light");
        same(rest("DELETE", path + "/" + id, admin, null), grpc("DeleteSettingValue", admin, "{\"id\":\"" + id + "\"}"));
        ok(grpc("DeleteSettingValue", admin, "{\"id\":\"" + id + "\",\"etag\":"
                + JSON.writeValueAsString(reread.body().path("etag").asString()) + "}"));
        same(rest("GET", path + "/" + id, admin, null), grpc("GetSettingValue", admin, "{\"id\":\"" + id + "\"}"));
    }

    @Test
    void theCataloguesReadAlike() throws Exception {
        for (String[] catalogue : new String[][] {{"/v1/iam/permissions", "Permission", "Permissions"},
            {"/v1/iam/setting-definitions", "SettingDefinition", "SettingDefinitions"}}) {
            String path = catalogue[0];
            Answer page = rest("GET", path + "?size=2", ADMIN, null);
            JsonNode pageGrpc = ok(grpc("List" + catalogue[2], ADMIN, "{\"pageable\":{\"size\":2}}"));
            assertThat(ids(pageGrpc.path("items"))).isEqualTo(ids(page.body().path("data"))).hasSize(2);
            GrpcParity.assertSameResource(page.body().path("data").get(0), pageGrpc.path("items").get(0));
            String id = page.body().path("data").get(1).path("id").asString();
            String field = Character.toLowerCase(catalogue[1].charAt(0)) + catalogue[1].substring(1);
            GrpcParity.assertSameResource(ok(rest("GET", path + "/" + id, ADMIN, null)),
                    ok(grpc("Get" + catalogue[1], ADMIN, "{\"id\":\"" + id + "\"}")).path(field));
            String absent = UUID.randomUUID().toString();
            same(rest("GET", path + "/" + absent, ADMIN, null), grpc("Get" + catalogue[1], ADMIN, "{\"id\":\"" + absent
                    + "\"}"));
            same(rest("GET", path, null, null), grpc("List" + catalogue[2], null, "{}"));
            same(rest("GET", path + "?filter=nope%20%3D%201", ADMIN, null),
                    grpc("List" + catalogue[2], ADMIN, "{\"filter\":\"nope = 1\"}"));
        }
    }

    /**
     * The API-permission catalogue, here iam's own as its publisher sent it at start-up, reads alike for platform staff
     * and is refused alike to a tenant user and to a caller without a token.
     */
    @Test
    void theApiPermissionCatalogueReadsAlike() throws Exception {
        String staff = TOKENS.platformOperator(UUID.randomUUID(), "iam.api_permission.read");
        String path = "/v1/iam/api-permissions?size=3&filter=moduleCode%20%3D%20%22iam%22";
        long until = System.currentTimeMillis() + 20_000;
        Answer page = rest("GET", path, staff, null);
        while (page.body().path("data").size() < 3 && System.currentTimeMillis() < until) {
            Thread.sleep(100);
            page = rest("GET", path, staff, null);
        }
        JsonNode pageGrpc = ok(grpc("ListApiPermissions", staff,
                "{\"pageable\":{\"size\":3},\"filter\":\"moduleCode = \\\"iam\\\"\"}"));
        assertThat(ids(pageGrpc.path("items"))).isEqualTo(ids(ok(page).path("data"))).hasSize(3);
        GrpcParity.assertSameResource(page.body().path("data").get(0), pageGrpc.path("items").get(0));
        same(rest("GET", "/v1/iam/api-permissions", ADMIN, null), grpc("ListApiPermissions", ADMIN, "{}"));
        same(rest("GET", "/v1/iam/api-permissions", null, null), grpc("ListApiPermissions", null, "{}"));
    }

    /** A request_id is REST's Idempotency-Key: a retry replays the first answer, another request with it is 422. */
    @Test
    void aRequestIdReplaysTheFirstAnswer() {
        String key = UUID.randomUUID().toString();
        JsonNode first = ok(grpc("CreateRole", ADMIN, "{\"requestId\":\"" + key + "\",\"role\":{\"code\":\"REPLAY1\"}}"));
        JsonNode again = ok(grpc("CreateRole", ADMIN, "{\"requestId\":\"" + key + "\",\"role\":{\"code\":\"REPLAY1\"}}"));
        assertThat(again.path("role").path("id").asString()).isEqualTo(first.path("role").path("id").asString());
        Answer reused = grpc("CreateRole", ADMIN, "{\"requestId\":\"" + key + "\",\"role\":{\"code\":\"REPLAY2\"}}");
        assertThat(reused.status()).isEqualTo(422);
        assertThat(reused.code()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
        assertThat(grpc("CreateRole", ADMIN, "{\"requestId\":\"not-a-uuid\",\"role\":{\"code\":\"REPLAY3\"}}").status())
                .isEqualTo(400);
    }

    // ---- signing in and out

    /**
     * Sign-in, refresh and sign-out: the same refusals, the same account, tokens issued over gRPC working over REST
     * and back, the session recording the caller's address and client as REST's does, and a refresh token rotated
     * once only, whichever transport presents it.
     */
    @Test
    void signingInRefreshingAndOutAnswersLikeRest() throws Exception {
        iam.user("signer", null, READ_USERS);
        String login = "{\"tenantCode\":\"" + TENANT_CODE + "\",\"identifier\":\"signer\",\"password\":\"";
        same(rest("POST", "/v1/iam/auth:login", null, login + "wrong-password-1\"}"),
                grpc("Login", null, login + "wrong-password-1\"}"));
        same(rest("POST", "/v1/iam/auth:login", null, "{}"), grpc("Login", null, "{}"));
        same(rest("POST", "/v1/iam/auth:login", null, "{\"tenantCode\":\"nope\",\"identifier\":\"signer\","
                + "\"password\":\"x\"}"), grpc("Login", null, "{\"tenantCode\":\"nope\",\"identifier\":\"signer\","
                + "\"password\":\"x\"}"));

        JsonNode overRest = ok(rest("POST", "/v1/iam/auth:login", null, login + IamHttp.PASSWORD + "\"}"));
        JsonNode overGrpc = ok(grpc("Login", null, login + IamHttp.PASSWORD + "\"}")).path("result");
        assertThat(overGrpc.path("user").path("id").asString()).isEqualTo(overRest.path("user").path("id").asString());
        assertThat(overGrpc.path("user").path("username").asString()).isEqualTo("signer");
        JsonNode tokens = overGrpc.path("tokens");
        assertThat(tokens.path("tokenType").asString()).isEqualTo(overRest.path("tokens").path("tokenType").asString());
        assertThat(tokens.path("expiresInSeconds").asInt()).isEqualTo(overRest.path("tokens").path("expiresInSeconds")
                .asInt());
        String access = tokens.path("accessToken").asString();
        UUID session = UUID.fromString(IamHttp.claims(access).path("sid").asString());
        Map<String, Object> origin = iam.sql(() -> iam.jdbc().sql("SELECT host(ip_address) AS ip, user_agent FROM "
                + "iam.user_session WHERE id = :id").param("id", session).query().singleRow());
        assertThat(origin.get("ip")).as("the peer's address, like getRemoteAddr()").isIn(LOOPBACK);
        assertThat(String.valueOf(origin.get("user_agent"))).as("the client's user-agent").startsWith("grpc-java");
        assertThat(ok(rest("GET", "/v1/iam/me", access, null)).path("user").path("username").asString())
                .isEqualTo("signer");

        Answer me = rest("GET", "/v1/iam/me", access, null);
        Answer meGrpc = grpc("GetMe", access, "{}");
        same(me, meGrpc);
        GrpcParity.assertSameResource(me.body().path("user"), meGrpc.body().path("result").path("user"));
        assertThat(meGrpc.body().path("result").path("permissions").toString())
                .isEqualTo(me.body().path("permissions").toString());

        String grpcRefresh = tokens.path("refreshToken").asString();
        String restRefresh = overRest.path("tokens").path("refreshToken").asString();
        JsonNode rotated = ok(grpc("RefreshToken", null, "{\"refreshToken\":\"" + restRefresh + "\"}")).path("result");
        assertThat(rotated.path("refreshToken").asString()).isNotEqualTo(restRefresh);
        ok(rest("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + grpcRefresh + "\"}"));
        same(rest("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + grpcRefresh + "\"}"),
                grpc("RefreshToken", null, "{\"refreshToken\":\"" + restRefresh + "\"}"));
        same(rest("POST", "/v1/iam/auth:refresh", null, "{}"), grpc("RefreshToken", null, "{}"));

        String change = "{\"currentPassword\":\"not-the-password-1\",\"newPassword\":\"another-strong-pass-4\"}";
        same(rest("POST", "/v1/iam/auth:changePassword", access, change), grpc("ChangePassword", access, change));

        String fresh = IamHttp.access(iam.signIn("signer", IamHttp.PASSWORD));
        same(rest("POST", "/v1/iam/auth:logout", null, "{}"), grpc("Logout", null, "{}"));
        ok(grpc("Logout", access, "{}"));
        ok(rest("POST", "/v1/iam/auth:logout", fresh, "{\"allDevices\":true}"));
        same(rest("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + rotated.path("refreshToken")
                .asString() + "\"}"), grpc("RefreshToken", null, "{\"refreshToken\":\"" + rotated.path("refreshToken")
                .asString() + "\"}"));
    }

    // ---- the caller's own settings and second factors

    @Test
    void theCallersSettingsAndSecondFactorsAnswerLikeRest() throws Exception {
        iam.user("selfie", null, "iam.mfa_credential.enroll", "iam.mfa_credential.read", "iam.mfa_credential.disable",
                "iam.mfa_credential.regenerate_recovery_codes");
        String me = iam.token("selfie", IamHttp.PASSWORD);

        Answer settings = rest("GET", "/v1/iam/me/settings", me, null);
        JsonNode settingsGrpc = ok(grpc("ListMySettings", me, "{}"));
        assertThat(settingsGrpc.path("items")).hasSize(ok(settings).path("items").size());
        GrpcParity.assertSameResource(settings.body().path("items").get(0), settingsGrpc.path("items").get(0));
        JsonNode set = ok(grpc("SetMySetting", me, "{\"settingKey\":\"ui.theme\",\"value\":\"dark\"}")).path("setting");
        GrpcParity.assertSameResource(ok(rest("PUT", "/v1/iam/me/settings/ui.theme", me, "{\"value\":\"dark\"}")), set);
        assertThat(set.path("sourceScope").asString()).isEqualTo("user");
        same(rest("PUT", "/v1/iam/me/settings/ui.textScale", me, "{\"value\":\"500\"}"),
                grpc("SetMySetting", me, "{\"settingKey\":\"ui.textScale\",\"value\":\"500\"}"));
        same(rest("PUT", "/v1/iam/me/settings/no.such", me, "{\"value\":\"x\"}"),
                grpc("SetMySetting", me, "{\"settingKey\":\"no.such\",\"value\":\"x\"}"));
        ok(grpc("ResetMySetting", me, "{\"settingKey\":\"ui.theme\"}"));
        same(rest("DELETE", "/v1/iam/me/settings/no.such", me, null), grpc("ResetMySetting", me,
                "{\"settingKey\":\"no.such\"}"));
        assertThat(ok(grpc("ListMySettings", me, "{}")).path("items").toString()).doesNotContain("\"sourceScope\":"
                + "\"user\"");

        JsonNode enrolled = ok(grpc("EnrollMfa", me, "{\"credentialType\":\"totp\",\"label\":\"phone\"}"));
        assertThat(enrolled.path("mfaCredentialId").asString()).isNotEmpty();
        assertThat(enrolled.path("secret").asString()).as("the secret, once, as on REST").isNotEmpty();
        assertThat(enrolled.path("otpauthUrl").asString()).startsWith("otpauth://");
        Answer credentials = rest("GET", "/v1/iam/mfa", me, null);
        JsonNode credentialsGrpc = ok(grpc("ListMfaCredentials", me, "{}"));
        assertThat(ids(credentialsGrpc.path("items"))).isEqualTo(ids(ok(credentials).path("items")))
                .contains(enrolled.path("mfaCredentialId").asString());
        assertThat(credentialsGrpc.toString()).doesNotContain(enrolled.path("secret").asString());
        same(rest("POST", "/v1/iam/mfa:enroll", me, "{\"credentialType\":\"totp\"}"),
                grpc("EnrollMfa", me, "{\"credentialType\":\"totp\"}"));
        same(rest("POST", "/v1/iam/mfa:enroll", me, "{}"), grpc("EnrollMfa", me, "{}"));
        String disable = "{\"mfaCredentialId\":\"" + enrolled.path("mfaCredentialId").asString()
                + "\",\"code\":\"000000\"}";
        same(rest("POST", "/v1/iam/mfa:disable", me, disable), grpc("DisableMfa", me, disable));
        // both take an idempotency key, and refuse a malformed one alike
        String keyed = disable.replace("}", ",\"requestId\":\"not-a-uuid\"}");
        same(rest("POST", "/v1/iam/mfa:disable", me, disable, "Idempotency-Key", "not-a-uuid"),
                grpc("DisableMfa", me, keyed));
        same(rest("POST", "/v1/iam/mfa:regenerateRecoveryCodes", me, "{\"code\":\"000000\"}"),
                grpc("RegenerateRecoveryCodes", me, "{\"code\":\"000000\"}"));
        String complete = "{\"mfaToken\":\"" + UUID.randomUUID() + ".nope\",\"code\":\"000000\"}";
        same(rest("POST", "/v1/iam/mfa:complete", null, complete), grpc("CompleteMfa", null, complete));
        same(rest("POST", "/v1/iam/mfa:complete", null, complete, "Idempotency-Key", "not-a-uuid"),
                grpc("CompleteMfa", null, complete.replace("}", ",\"requestId\":\"not-a-uuid\"}")));
        same(rest("GET", "/v1/iam/mfa", null, null), grpc("ListMfaCredentials", null, "{}"));
    }

    // ---- recovery, verification, sign-up, terms

    @Test
    void recoveryVerificationAndSignUpAnswerLikeRest() throws Exception {
        UUID holder = iam.user("holder", "holder@school.test", READ_USERS);
        String me = iam.token("holder", IamHttp.PASSWORD);
        String admin = iam.token("admin", ADMIN_PASSWORD);
        String reset = "{\"tenantCode\":\"" + TENANT_CODE + "\",\"identifier\":\"nobody@school.test\"}";
        same(rest("POST", "/v1/iam/auth:requestPasswordReset", null, reset),
                grpc("RequestPasswordReset", null, reset));
        same(rest("POST", "/v1/iam/auth:requestPasswordReset", null, "{}"), grpc("RequestPasswordReset", null, "{}"));
        String token = "{\"token\":\"" + UUID.randomUUID() + ".nope\",\"newPassword\":\"another-strong-pass-4\"}";
        same(rest("POST", "/v1/iam/auth:resetPassword", null, token), grpc("ResetPassword", null, token));
        same(rest("POST", "/v1/iam/auth:requestVerification", me, "{\"channel\":\"email\"}"),
                grpc("RequestVerification", me, "{\"channel\":\"email\"}"));
        same(rest("POST", "/v1/iam/auth:requestVerification", me, "{}"), grpc("RequestVerification", me, "{}"));
        String confirm = "{\"channel\":\"email\",\"code\":\"000000\"}";
        same(rest("POST", "/v1/iam/auth:confirmVerification", me, confirm), grpc("ConfirmVerification", me, confirm));
        String signUp = "{\"tenantCode\":\"" + TENANT_CODE + "\",\"institutionId\":\"" + UUID.randomUUID()
                + "\",\"email\":\"new@school.test\",\"fullName\":\"New One\",\"acceptTerms\":true}";
        same(rest("POST", "/v1/iam/auth:signUp", null, signUp), grpc("SignUp", null, signUp));
        same(rest("POST", "/v1/iam/auth:signUp", null, "{}"), grpc("SignUp", null, "{}"));
        same(rest("POST", "/v1/iam/auth:signUp", null, signUp.replaceAll("\"institutionId\":\"[^\"]+\",", "")),
                grpc("SignUp", null, signUp.replaceAll("\"institutionId\":\"[^\"]+\",", "")));
        same(rest("GET", "/v1/iam/tenant-codes/" + TENANT_CODE + "/sign-up-institutions", null, null),
                grpc("ListSignUpInstitutions", null, "{\"tenantCode\":\"" + TENANT_CODE + "\"}"));
        same(rest("GET", "/v1/iam/tenant-codes/nowhere/sign-up-institutions", null, null),
                grpc("ListSignUpInstitutions", null, "{\"tenantCode\":\"nowhere\"}"));

        JsonNode accepted = ok(grpc("AcceptTerms", me, "{\"termsVersion\":\"2026-11\"}")).path("userAccount");
        GrpcParity.assertSameResource(ok(rest("GET", "/v1/iam/users/" + holder, admin, null)), accepted);
        same(rest("POST", "/v1/iam/me:acceptTerms", me, "{}"), grpc("AcceptTerms", me, "{}"));

        String path = "/v1/iam/users/" + holder;
        String credentials = "{\"temporaryPassword\":\"temporary-strong-pass-5\"}";
        JsonNode changed = ok(grpc("SetUserCredentials", admin, "{\"userAccountId\":\"" + holder + "\","
                + "\"temporaryPassword\":\"temporary-strong-pass-5\"}")).path("userAccount");
        GrpcParity.assertSameResource(ok(rest("GET", path, admin, null)), changed);
        same(rest("POST", path + ":setCredentials", me, credentials), grpc("SetUserCredentials", me,
                "{\"userAccountId\":\"" + holder + "\",\"temporaryPassword\":\"temporary-strong-pass-5\"}"));
        same(rest("POST", path + ":forcePasswordReset", admin, "{}"),
                grpc("ForcePasswordReset", admin, "{\"userAccountId\":\"" + holder + "\"}"));
        same(rest("POST", path + ":forcePasswordReset", admin, "{\"etag\":\"\\\"99\\\"\"}"),
                grpc("ForcePasswordReset", admin, "{\"userAccountId\":\"" + holder + "\",\"etag\":\"\\\"99\\\"\"}"));
    }

    // ---- impersonation, API tokens, roles, import

    @Test
    void impersonationAnswersLikeRest() throws Exception {
        String admin = iam.token("admin", ADMIN_PASSWORD);
        UUID administrator = iam.admin();
        UUID pupil = iam.user("pupil", null, READ_USERS);
        String reason = "{\"reason\":\"Ticket 7\"}";
        same(rest("POST", "/v1/iam/users/" + administrator + ":impersonate", admin, reason),
                grpc("StartImpersonation", admin, "{\"userAccountId\":\"" + administrator + "\",\"reason\":\"x\"}"));
        same(rest("POST", "/v1/iam/users/" + pupil + ":impersonate", admin, "{\"reason\":\" \"}"),
                grpc("StartImpersonation", admin, "{\"userAccountId\":\"" + pupil + "\",\"reason\":\" \"}"));
        String absent = UUID.randomUUID().toString();
        same(rest("POST", "/v1/iam/users/" + absent + ":impersonate", admin, reason),
                grpc("StartImpersonation", admin, "{\"userAccountId\":\"" + absent + "\",\"reason\":\"x\"}"));

        JsonNode overRest = ok(rest("POST", "/v1/iam/users/" + pupil + ":impersonate", admin, reason));
        JsonNode overGrpc = ok(grpc("StartImpersonation", admin, "{\"userAccountId\":\"" + pupil
                + "\",\"reason\":\"Ticket 7\"}")).path("result");
        assertThat(overGrpc.path("impersonatedUserId").asString()).isEqualTo(pupil.toString())
                .isEqualTo(overRest.path("impersonatedUserId").asString());
        assertThat(overGrpc.has("refreshToken")).as("no more than REST hands out").isFalse();
        String acting = overGrpc.path("accessToken").asString();
        JsonNode claims = IamHttp.claims(acting);
        assertThat(claims.path("act").path("sub").asString()).isEqualTo(administrator.toString());
        Map<String, Object> origin = iam.sql(() -> iam.jdbc().sql("SELECT host(ip_address) AS ip, user_agent FROM "
                + "iam.user_session WHERE id = :id").param("id", UUID.fromString(claims.path("sid").asString()))
                .query().singleRow());
        assertThat(origin.get("ip")).isIn(LOOPBACK);
        assertThat(String.valueOf(origin.get("user_agent"))).startsWith("grpc-java");

        same(rest("POST", "/v1/iam/auth:stopImpersonation", admin, "{}"), grpc("StopImpersonation", admin, "{}"));
        ok(grpc("StopImpersonation", acting, "{}"));
        String restActing = overRest.path("accessToken").asString();
        ok(rest("POST", "/v1/iam/auth:stopImpersonation", restActing, "{}"));
        same(rest("POST", "/v1/iam/auth:stopImpersonation", restActing, "{}"), grpc("StopImpersonation", acting, "{}"));
    }

    @Test
    void anApiTokenIsCreatedExchangedListedAndRevokedAlike() throws Exception {
        UUID coder = iam.user("coder", null, "iam.user_account.create_api_token", READ_USERS);
        UUID other = iam.user("bystander", null);
        String token = iam.token("coder", IamHttp.PASSWORD);
        String owner = "{\"userAccountId\":\"" + coder + "\"";
        same(rest("POST", "/v1/iam/users/" + other + ":createApiToken", token, "{\"name\":\"x\"}"),
                grpc("CreateApiToken", token, "{\"userAccountId\":\"" + other + "\",\"name\":\"x\"}"));
        String refused = "\"name\":\"CI\",\"abilities\":[\"iam.role.delete\"],\"expiresAt\":\"2000-01-01T00:00:00Z\"}";
        same(rest("POST", "/v1/iam/users/" + coder + ":createApiToken", token, "{" + refused),
                grpc("CreateApiToken", token, owner + "," + refused));

        JsonNode created = ok(grpc("CreateApiToken", token, owner + ",\"name\":\"CI\",\"abilities\":[\"" + READ_USERS
                + "\"]}")).path("result");
        String secret = created.path("token").asString();
        assertThat(secret).startsWith("mysms_pat_" + iam.tenant() + ".");
        String id = created.path("authToken").path("id").asString();
        Answer listed = rest("GET", "/v1/iam/users/" + coder + "/api-tokens", token, null);
        JsonNode listedGrpc = ok(grpc("ListAuthTokens", token, owner + "}"));
        assertThat(ids(listedGrpc.path("items"))).isEqualTo(ids(ok(listed).path("data"))).containsExactly(id);
        GrpcParity.assertSameResource(listed.body().path("data").get(0), listedGrpc.path("items").get(0));
        GrpcParity.assertSameResource(listed.body().path("data").get(0), created.path("authToken"));
        assertThat(listedGrpc.toString()).doesNotContain(secret);
        String ip = iam.sql(() -> iam.jdbc().sql("SELECT host(ip_address) FROM iam.auth_token WHERE id = :id")
                .param("id", UUID.fromString(id)).query(String.class).single());
        assertThat(ip).isIn(LOOPBACK);

        JsonNode exchanged = ok(grpc("RefreshToken", null, "{\"refreshToken\":\"" + secret + "\"}")).path("result");
        assertThat(exchanged.path("refreshToken").asString()).as("an API token is not rotated").isEqualTo(secret);
        assertThat(IamHttp.claims(exchanged.path("accessToken").asString()).path("abl").toString())
                .isEqualTo("[\"" + READ_USERS + "\"]");

        String key = owner + ",\"id\":\"" + id + "\"";
        String path = "/v1/iam/users/" + coder + "/api-tokens/" + id;
        same(rest("DELETE", path, token, null), grpc("DeleteAuthToken", token, key + "}"));
        same(rest("DELETE", path, token, null, "If-Match", "\"7\""), grpc("DeleteAuthToken", token, key
                + ",\"etag\":\"7\"}"));
        ok(grpc("DeleteAuthToken", token, key + ",\"etag\":" + JSON.writeValueAsString(listed.body().path("data")
                .get(0).path("etag").asString()) + "}"));
        same(rest("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + secret + "\"}"),
                grpc("RefreshToken", null, "{\"refreshToken\":\"" + secret + "\"}"));
    }

    @Test
    void grantsTheRoleSwitchAndTheLoginNameCheckAnswerLikeRest() throws Exception {
        String admin = iam.token("admin", ADMIN_PASSWORD);
        UUID target = iam.user("target", null);
        iam.role("clerk", READ_USERS);
        String grants = "\"grants\":[{\"roleCode\":\"clerk\",\"scopeType\":\"tenant\"}]}";
        JsonNode set = ok(grpc("SetUserRoles", admin, "{\"userAccountId\":\"" + target + "\"," + grants));
        Answer roles = rest("GET", "/v1/iam/users/" + target + "/roles", admin, null);
        assertThat(ids(set.path("items"))).isEqualTo(ids(ok(roles).path("data"))).hasSize(1);
        GrpcParity.assertSameResource(roles.body().path("data").get(0), set.path("items").get(0));
        String unknown = "\"grants\":[{\"roleCode\":\"nope\",\"scopeType\":\"tenant\"}]}";
        same(rest("POST", "/v1/iam/users/" + target + ":setRoles", admin, "{" + unknown),
                grpc("SetUserRoles", admin, "{\"userAccountId\":\"" + target + "\"," + unknown));
        same(rest("POST", "/v1/iam/users/" + iam.admin() + ":setRoles", admin, "{\"grants\":[]}"),
                grpc("SetUserRoles", admin, "{\"userAccountId\":\"" + iam.admin() + "\"}"));
        // no grants at all is REST's [] (every grant taken away), never a missing list
        JsonNode cleared = ok(grpc("SetUserRoles", admin, "{\"userAccountId\":\"" + target + "\"}"));
        assertThat(cleared.path("items")).isEmpty();
        assertThat(ok(rest("GET", "/v1/iam/users/" + target + "/roles", admin, null)).path("data")).isEmpty();

        UUID multi = iam.user("multi", null, "iam.role.read");
        UUID second = iam.role("reviewer", "iam.user_device.read");
        iam.grant(multi, second, "tenant", null);
        String token = iam.token("multi", IamHttp.PASSWORD);
        String notMine = "{\"roleId\":\"" + UUID.randomUUID() + "\"}";
        same(rest("POST", "/v1/iam/me:switchRole", token, notMine), grpc("SwitchRole", token, notMine));
        JsonNode switched = ok(grpc("SwitchRole", token, "{\"roleId\":\"" + second + "\"}")).path("result");
        assertThat(switched.path("me").path("activeRole").path("roleCode").asString()).isEqualTo("reviewer");
        assertThat(IamHttp.claims(switched.path("tokens").path("accessToken").asString()).path("rgr").toString())
                .isEqualTo("[\"" + second + "@tenant\"]");
        JsonNode overRest = ok(rest("POST", "/v1/iam/me:switchRole", token, "{\"roleId\":\"" + second + "\"}"));
        GrpcParity.assertSameResource(overRest.path("me").path("user"), switched.path("me").path("user"));

        Answer taken = rest("GET", "/v1/iam/users:checkUsername?username=TARGET", admin, null);
        JsonNode takenGrpc = ok(grpc("CheckUsernameAvailable", admin, "{\"username\":\"TARGET\"}")).path("result");
        GrpcParity.assertSameResource(ok(taken), takenGrpc);
        assertThat(takenGrpc.path("available").asBoolean()).isFalse();
        same(rest("GET", "/v1/iam/users:checkUsername", admin, null), grpc("CheckUsernameAvailable", admin, "{}"));
        same(rest("GET", "/v1/iam/users:checkUsername?username=x", token, null),
                grpc("CheckUsernameAvailable", token, "{\"username\":\"x\"}"));
    }

    @Test
    void anImportAndItsOperationAnswerLikeRest() throws Exception {
        String admin = iam.token("admin", ADMIN_PASSWORD);
        iam.role("importrole", READ_USERS);
        String rows = "\"rows\":[{\"username\":\"imp1\",\"email\":\"imp1@school.test\",\"userType\":\"teacher\","
                + "\"grants\":[{\"roleCode\":\"importrole\",\"scopeType\":\"tenant\"}]},{\"mobileNo\":\"9000000001\"}]";
        JsonNode dryRun = ok(grpc("ImportUsers", admin, "{" + rows + ",\"validateOnly\":true}")).path("result");
        assertThat(dryRun.path("result").path("userAccountIds").size()).isZero();
        JsonNode imported = ok(grpc("ImportUsers", admin, "{" + rows + "}")).path("result");
        assertThat(imported.path("status").asString()).isEqualTo("succeeded");
        assertThat(imported.path("result").path("userAccountIds")).hasSize(2);
        same(rest("POST", "/v1/iam/users:import", admin, "{" + rows + "}"), grpc("ImportUsers", admin, "{" + rows + "}"));
        same(rest("POST", "/v1/iam/users:import", admin, "{\"rows\":[{\"userType\":\"teacher\"}]}"),
                grpc("ImportUsers", admin, "{\"rows\":[{\"userType\":\"teacher\"}]}"));

        String operation = imported.path("operationId").asString();
        String path = "/v1/iam/operations/" + operation;
        GrpcParity.assertSameResource(ok(rest("GET", path, admin, null)),
                ok(grpc("GetOperation", admin, "{\"operationId\":\"" + operation + "\"}")).path("result"));
        String absent = UUID.randomUUID().toString();
        same(rest("GET", "/v1/iam/operations/" + absent, admin, null),
                grpc("GetOperation", admin, "{\"operationId\":\"" + absent + "\"}"));
        Answer listed = rest("GET", "/v1/iam/operations", admin, null);
        JsonNode listedGrpc = ok(grpc("ListOperations", admin, "{}"));
        assertThat(operations(listedGrpc.path("items"))).isEqualTo(operations(ok(listed).path("data")))
                .contains(operation);
        String kind = listed.body().path("data").get(0).path("kind").asString();
        assertThat(operations(ok(grpc("ListOperations", admin, "{\"kind\":\"" + kind + "\"}")).path("items")))
                .isEqualTo(operations(ok(rest("GET", "/v1/iam/operations?kind=" + kind, admin, null)).path("data")));
        same(rest("POST", path + ":cancel", admin, "{}"), grpc("CancelOperation", admin, "{\"operationId\":\""
                + operation + "\"}"));
        same(rest("GET", path, NOBODY, null), grpc("GetOperation", NOBODY, "{\"operationId\":\"" + operation + "\"}"));
    }
}
