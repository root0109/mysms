package com.mysms.iam;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.iam.config.IamDatabaseConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Access management cannot be used to escalate, end to end over HTTP against real
 * PostgreSQL: a permission goes into a role only from someone holding it tenant-wide, a role is granted only by someone
 * holding all its permissions at the grant's reach, nobody changes their own access, system roles are fixed, and a
 * deleted account is signed out everywhere in the same transaction.
 */
@SpringBootTest(classes = IamApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IamAccessControlComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID SCHOOL_ADMIN = UUID.randomUUID();
    private static final UUID SCHOOL_A = UUID.randomUUID();
    private static final UUID SCHOOL_B = UUID.randomUUID();
    private static final String GRANTED = "sis.student.read";

    private static String roleId;
    private static String targetUser;

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(IamDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "iam");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.iam.token.signing-key", TOKENS::privateKeyPem);
    }

    private record Reply(int status, JsonNode body, String text) {
    }

    private Reply call(String method, String path, String token, String body, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .header("Authorization", "Bearer " + token);
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode json = response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body());
        return new Reply(response.statusCode(), json, method + " " + path + " -> " + response.body());
    }

    /** A tenant user's token with the given institution reach and tenant-wide permissions. */
    private static String token(UUID user, List<String> ins, List<String> tenantWide, String... perms) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.toString());
        claims.put("tid", TENANT.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of(perms));
        claims.put("ins", ins);
        claims.put("tperms", tenantWide);
        return TOKENS.custom(claims, Instant.now().plusSeconds(600));
    }

    private static String admin() {
        return token(ADMIN, List.of("*"), List.of("*"), "*");
    }

    private <T> T as(Supplier<T> work) {
        CallContext context = CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736").withIdentity(TENANT, null);
        return CallContextHolder.callAs(context, () -> TestTransactions.of(database).write(work::get));
    }

    private String permissionId(String code) {
        return as(() -> database.jdbc().sql("SELECT id FROM iam.permission WHERE code = :code").param("code", code)
                .query(UUID.class).single()).toString();
    }

    @Test
    @Order(1)
    void rolesTakeOnlyPermissionsTheCallerHoldsTenantWide() throws Exception {
        Reply role = call("POST", "/v1/iam/roles", admin(), "{\"code\":\"registrar\",\"displayName\":\"Registrar\"}");
        assertThat(role.status()).as(role.text()).isEqualTo(201);
        roleId = role.body().path("id").asString();
        String body = "{\"permissionId\":\"" + permissionId(GRANTED) + "\"}";
        // holds every iam permission tenant-wide, but the student permission only through a school grant
        String partial = token(SCHOOL_ADMIN, List.of("*", SCHOOL_A.toString()), List.of("iam.*"), "iam.*", GRANTED);
        Reply refused = call("POST", "/v1/iam/roles/" + roleId + "/permissions", partial, body);
        assertThat(refused.status()).as(refused.text()).isEqualTo(403);
        assertThat(refused.body().path("detail").asString()).contains(GRANTED);
        Reply added = call("POST", "/v1/iam/roles/" + roleId + "/permissions", admin(), body);
        assertThat(added.status()).as(added.text()).isEqualTo(201);
    }

    @Test
    @Order(2)
    void rolesAreGrantedOnlyWithinTheCallersReachAndNeverToOneself() throws Exception {
        Reply user = call("POST", "/v1/iam/users", admin(), "{\"username\":\"clerk1\",\"userType\":\"staff\"}");
        assertThat(user.status()).as(user.text()).isEqualTo(201);
        targetUser = user.body().path("id").asString();
        // may grant roles tenant-wide, holds the role's permission at school A only
        String schoolAdmin = token(SCHOOL_ADMIN, List.of("*", SCHOOL_A.toString()), List.of("iam.*"), "iam.*", GRANTED);
        String grants = "/v1/iam/users/" + targetUser + "/roles";
        Reply atA = call("POST", grants, schoolAdmin, grant("institution", SCHOOL_A));
        assertThat(atA.status()).as(atA.text()).isEqualTo(201);
        Reply atB = call("POST", grants, schoolAdmin, grant("institution", SCHOOL_B));
        assertThat(atB.status()).as("school B is outside the caller's reach: " + atB.text()).isEqualTo(403);
        Reply tenantWide = call("POST", grants, schoolAdmin, grant("tenant", null));
        assertThat(tenantWide.status()).as("a tenant-wide grant needs the permission tenant-wide").isEqualTo(403);
        Reply department = call("POST", grants, schoolAdmin, grant("department", UUID.randomUUID()));
        assertThat(department.status()).as("units this module cannot place need the tenant-wide reach").isEqualTo(403);
        String lacking = token(SCHOOL_ADMIN, List.of("*"), List.of("iam.*"), "iam.*");
        assertThat(call("POST", grants, lacking, grant("institution", SCHOOL_A)).status())
                .as("a role whose permissions the caller does not hold").isEqualTo(403);
        String self = token(UUID.fromString(targetUser), List.of("*"), List.of("*"), "*");
        Reply own = call("POST", grants, self, grant("tenant", null));
        assertThat(own.status()).as("nobody changes their own access: " + own.text()).isEqualTo(403);
        Reply direct = call("POST", "/v1/iam/users/" + targetUser + "/permissions", schoolAdmin,
                "{\"permissionId\":\"" + permissionId(GRANTED) + "\",\"effect\":\"grant\"}");
        assertThat(direct.status()).as("direct grants apply tenant-wide: " + direct.text()).isEqualTo(403);
        Reply directByAdmin = call("POST", "/v1/iam/users/" + targetUser + "/permissions", admin(),
                "{\"permissionId\":\"" + permissionId(GRANTED) + "\",\"effect\":\"grant\"}");
        assertThat(directByAdmin.status()).as(directByAdmin.text()).isEqualTo(201);
    }

    private String grant(String scopeType, UUID scopeId) {
        return "{\"roleId\":\"" + roleId + "\",\"scopeType\":\"" + scopeType + "\""
                + (scopeId == null ? "" : ",\"scopeId\":\"" + scopeId + "\"") + "}";
    }

    @Test
    @Order(3)
    void systemRolesAreFixed() throws Exception {
        Reply created = call("POST", "/v1/iam/roles", admin(), "{\"code\":\"sneaky\",\"isSystem\":true}");
        assertThat(created.status()).as(created.text()).isEqualTo(422);
        UUID system = UUID.randomUUID();
        as(() -> database.jdbc().sql("""
                INSERT INTO iam.role (id, tenant_id, code, display_name, is_system, is_active, row_version)
                VALUES (:id, :tenant, 'administrator', 'Administrator', true, true, 1)
                """).param("id", system).param("tenant", TENANT).update());
        Reply renamed = call("PATCH", "/v1/iam/roles/" + system, admin(), "{\"displayName\":\"Boss\"}", "If-Match",
                "\"1\"");
        assertThat(renamed.status()).as(renamed.text()).isEqualTo(409);
        assertThat(call("DELETE", "/v1/iam/roles/" + system, admin(), null, "If-Match", "\"1\"").status()).isEqualTo(409);
        Reply widened = call("POST", "/v1/iam/roles/" + system + "/permissions", admin(),
                "{\"permissionId\":\"" + permissionId(GRANTED) + "\"}");
        assertThat(widened.status()).as(widened.text()).isEqualTo(409);
    }

    @Test
    @Order(4)
    void aDeletedAccountIsSignedOutEverywhere() throws Exception {
        String self = token(UUID.fromString(targetUser), List.of("*"), List.of("*"), "*");
        Reply own = call("DELETE", "/v1/iam/users/" + targetUser, self, null, "If-Match", "\"1\"");
        assertThat(own.status()).as("nobody deletes their own account: " + own.text()).isEqualTo(403);
        Reply blocked = call("DELETE", "/v1/iam/users/" + targetUser, admin(), null, "If-Match", "\"1\"");
        assertThat(blocked.status()).as("live role grants still reference the account: " + blocked.text())
                .isEqualTo(409);
        for (String kind : List.of("roles", "permissions")) {
            for (JsonNode row : call("GET", "/v1/iam/users/" + targetUser + "/" + kind, admin(), null).body()
                    .path("data")) {
                Reply removed = call("DELETE", "/v1/iam/users/" + targetUser + "/" + kind + "/" + row.path("id")
                        .asString(), admin(), null, "If-Match", row.path("etag").asString());
                assertThat(removed.status()).as(removed.text()).isEqualTo(204);
            }
        }
        UUID session = UUID.randomUUID();
        as(() -> {
            database.jdbc().sql("""
                    INSERT INTO iam.user_session (id, tenant_id, user_id, session_ref, started_at, last_seen_at)
                    VALUES (:id, :tenant, :user, :id, now(), now())
                    """).param("id", session).param("tenant", TENANT).param("user", UUID.fromString(targetUser)).update();
            return database.jdbc().sql("""
                    INSERT INTO iam.auth_token (id, tenant_id, user_id, token_type, name, token_hash, expires_at)
                    VALUES (:id, :tenant, :user, 'refresh', :family, 'hash-of-a-refresh-token', now() + interval '1 day')
                    """).param("id", UUID.randomUUID()).param("tenant", TENANT).param("user", UUID.fromString(targetUser))
                    .param("family", session.toString()).update();
        });
        Reply deleted = call("DELETE", "/v1/iam/users/" + targetUser, admin(), null, "If-Match", "\"1\"");
        assertThat(deleted.status()).as(deleted.text()).isEqualTo(204);
        Map<String, Object> left = as(() -> database.jdbc().sql("""
                SELECT (SELECT count(*) FROM iam.auth_token WHERE user_id = :user AND revoked_at IS NULL) AS live_tokens,
                       (SELECT count(*) FROM iam.user_session WHERE user_id = :user AND ended_at IS NULL) AS open_sessions
                """).param("user", UUID.fromString(targetUser)).query().singleRow());
        assertThat(((Number) left.get("live_tokens")).intValue()).isZero();
        assertThat(((Number) left.get("open_sessions")).intValue()).isZero();
    }
}
