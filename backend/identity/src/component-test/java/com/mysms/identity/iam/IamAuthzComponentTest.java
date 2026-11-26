package com.mysms.identity.iam;

import com.mysms.identity.IdentityApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.identity.iam.business.AuthzService;
import com.mysms.identity.iam.config.IamDatabaseConfig;
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
import java.util.stream.Stream;
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
 * What the modules' permissions filters read from iam, against real PostgreSQL: a role answers its own permissions
 * plus the codes every role holds, several roles are answered in one call, roles of another tenant stay invisible,
 * and every change to a role, a role permission, a role grant or a direct grant raises the tenant's authorisation
 * version exactly once. The changes are made through the real REST edge; the answer is read from the business
 * service, because {@code GetRolePermissions} is not reachable over gRPC until the interceptor lets iam's
 * authorisation service through as infrastructure (ROADMAP SEC-0 item 4a).
 */
@SpringBootTest(classes = IdentityApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IamAuthzComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final UUID ADMIN = UUID.randomUUID();
    private static final String GRANTED = "sis.student.read";
    private static final String ALSO_GRANTED = "sis.student.create";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(IamDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private AuthzService authz;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "iam", "people");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.iam.token.signing-key", TOKENS::privateKeyPem);
    }

    private record Reply(int status, JsonNode body, String text) {
    }

    private Reply call(String method, String path, String body, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .header("Authorization", "Bearer " + admin());
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

    /** An administrator of TENANT: every permission, tenant-wide. */
    private static String admin() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", ADMIN.toString());
        claims.put("tid", TENANT.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of("*"));
        claims.put("ins", List.of("*"));
        claims.put("tperms", List.of("*"));
        return TOKENS.custom(claims, Instant.now().plusSeconds(600));
    }

    private <T> T as(UUID tenant, Supplier<T> work) {
        CallContext context = CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736").withIdentity(tenant, null);
        return CallContextHolder.callAs(context, work::get);
    }

    private <T> T inTenant(UUID tenant, Supplier<T> work) {
        return as(tenant, () -> TestTransactions.of(database).write(work::get));
    }

    private String permissionId(String code) {
        return inTenant(TENANT, () -> database.jdbc().sql("SELECT id FROM iam.permission WHERE code = :code")
                .param("code", code).query(UUID.class).single()).toString();
    }

    /** The codes every role carries implicitly, as the permission catalogue states them. */
    private List<String> selfServiceCodes() {
        return inTenant(TENANT, () -> database.jdbc().sql(
                "SELECT code FROM iam.permission WHERE applies_to_all_roles AND deleted_at IS NULL ORDER BY code")
                .query(String.class).list());
    }

    private long version() {
        return as(TENANT, () -> authz.rolePermissions(TENANT, List.of())).authzVersion();
    }

    private AuthzService.RoleAuthz resolve(UUID... roleIds) {
        return as(TENANT, () -> authz.rolePermissions(TENANT, List.of(roleIds)));
    }

    private String createRole(String code) throws Exception {
        Reply role = call("POST", "/v1/iam/roles", "{\"code\":\"" + code + "\",\"displayName\":\"" + code + "\"}");
        assertThat(role.status()).as(role.text()).isEqualTo(201);
        return role.body().path("id").asString();
    }

    private void grantToRole(String roleId, String code) throws Exception {
        Reply added = call("POST", "/v1/iam/roles/" + roleId + "/permissions",
                "{\"permissionId\":\"" + permissionId(code) + "\"}");
        assertThat(added.status()).as(added.text()).isEqualTo(201);
    }

    @Test
    @Order(1)
    void aRoleAnswersItsOwnPermissionsAndTheCodesEveryRoleHolds() throws Exception {
        String roleId = createRole("registrar");
        grantToRole(roleId, GRANTED);

        AuthzService.RoleAuthz answer = resolve(UUID.fromString(roleId));

        assertThat(answer.roles()).singleElement().satisfies(role -> {
            assertThat(role.roleId()).hasToString(roleId);
            assertThat(role.permissionCodes()).containsExactly(GRANTED);
        });
        assertThat(answer.commonCodes()).as("the codes every role holds come once, apart")
                .isNotEmpty().isEqualTo(selfServiceCodes());
        assertThat(as(TENANT, () -> authz.rolePermissions(TENANT, List.of())).commonCodes())
                .as("also to a caller without any role grant").isEqualTo(selfServiceCodes());
        assertThat(answer.authzVersion()).as("the role and its permission raised it").isEqualTo(2);
    }

    @Test
    @Order(2)
    void severalRolesAreAnsweredInOneCallAndUnknownOnesAreLeftOut() throws Exception {
        String registrar = roleIdOf("registrar");
        String librarian = createRole("librarian");
        grantToRole(librarian, ALSO_GRANTED);

        AuthzService.RoleAuthz answer =
                resolve(UUID.fromString(registrar), UUID.fromString(librarian), UUID.randomUUID());

        assertThat(answer.roles()).hasSize(2);
        assertThat(codesOf(answer, registrar)).contains(GRANTED).doesNotContain(ALSO_GRANTED);
        assertThat(codesOf(answer, librarian)).contains(ALSO_GRANTED).doesNotContain(GRANTED);
    }

    @Test
    @Order(3)
    void aRoleOfAnotherTenantNeverAnswersItsPermissions() {
        UUID foreign = UUID.randomUUID();
        inTenant(OTHER_TENANT, () -> database.jdbc().sql("""
                INSERT INTO iam.role (id, tenant_id, code, display_name, is_system, is_active, row_version)
                VALUES (:id, :tenant, 'registrar', 'Registrar', false, true, 1)
                """).param("id", foreign).param("tenant", OTHER_TENANT).update());

        assertThat(resolve(foreign).roles()).as("another tenant's role id resolves to nothing").isEmpty();
        assertThatThrownBy(() -> as(TENANT, () -> authz.rolePermissions(OTHER_TENANT, List.of(foreign))))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    @Order(4)
    void aDeactivatedRoleCarriesNoPermissions() throws Exception {
        String roleId = createRole("bursar");
        grantToRole(roleId, GRANTED);
        assertThat(resolve(UUID.fromString(roleId)).roles()).hasSize(1);

        Reply deactivated = call("PATCH", "/v1/iam/roles/" + roleId, "{\"isActive\":false}", "If-Match", "\"1\"");
        assertThat(deactivated.status()).as(deactivated.text()).isEqualTo(200);

        assertThat(resolve(UUID.fromString(roleId)).roles()).isEmpty();
    }

    @Test
    @Order(5)
    void everyChangeToTheTenantsAccessRaisesItsVersionOnce() throws Exception {
        long start = version();

        String roleId = createRole("bookkeeper");
        assertThat(version()).as("role created").isEqualTo(start + 1);

        Reply renamed = call("PATCH", "/v1/iam/roles/" + roleId, "{\"displayName\":\"Bookkeeper\"}", "If-Match",
                "\"1\"");
        assertThat(renamed.status()).as(renamed.text()).isEqualTo(200);
        assertThat(version()).as("role updated").isEqualTo(start + 2);

        Reply added = call("POST", "/v1/iam/roles/" + roleId + "/permissions",
                "{\"permissionId\":\"" + permissionId(GRANTED) + "\"}");
        assertThat(added.status()).as(added.text()).isEqualTo(201);
        String rolePermissionId = added.body().path("id").asString();
        assertThat(version()).as("permission granted to the role").isEqualTo(start + 3);

        Reply user = call("POST", "/v1/iam/users", "{\"username\":\"bookkeeper1\",\"userType\":\"staff\"}");
        assertThat(user.status()).as(user.text()).isEqualTo(201);
        String userId = user.body().path("id").asString();
        assertThat(version()).as("an account alone changes nobody's access").isEqualTo(start + 3);

        Reply granted = call("POST", "/v1/iam/users/" + userId + "/roles",
                "{\"roleId\":\"" + roleId + "\",\"scopeType\":\"tenant\"}");
        assertThat(granted.status()).as(granted.text()).isEqualTo(201);
        String userRoleId = granted.body().path("id").asString();
        assertThat(version()).as("role granted to a user").isEqualTo(start + 4);

        Reply direct = call("POST", "/v1/iam/users/" + userId + "/permissions",
                "{\"permissionId\":\"" + permissionId(ALSO_GRANTED) + "\",\"effect\":\"grant\"}");
        assertThat(direct.status()).as(direct.text()).isEqualTo(201);
        String userPermissionId = direct.body().path("id").asString();
        assertThat(version()).as("direct grant").isEqualTo(start + 5);

        Reply denied = call("PATCH", "/v1/iam/users/" + userId + "/permissions/" + userPermissionId,
                "{\"effect\":\"deny\"}", "If-Match", "\"1\"");
        assertThat(denied.status()).as(denied.text()).isEqualTo(200);
        assertThat(version()).as("the direct grant turned into a deny").isEqualTo(start + 6);

        assertThat(call("DELETE", "/v1/iam/users/" + userId + "/permissions/" + userPermissionId, null,
                "If-Match", "\"2\"").status()).isEqualTo(204);
        assertThat(version()).as("direct grant withdrawn").isEqualTo(start + 7);

        assertThat(call("DELETE", "/v1/iam/users/" + userId + "/roles/" + userRoleId, null, "If-Match", "\"1\"")
                .status()).isEqualTo(204);
        assertThat(version()).as("role grant withdrawn").isEqualTo(start + 8);

        assertThat(call("DELETE", "/v1/iam/roles/" + roleId + "/permissions/" + rolePermissionId, null,
                "If-Match", "\"1\"").status()).isEqualTo(204);
        assertThat(version()).as("permission revoked from the role").isEqualTo(start + 9);

        assertThat(call("DELETE", "/v1/iam/roles/" + roleId, null, "If-Match", "\"2\"").status()).isEqualTo(204);
        assertThat(version()).as("role deleted").isEqualTo(start + 10);
    }

    @Test
    @Order(6)
    void settingAUsersRolesRaisesTheVersionOnceForTheWholeSet() throws Exception {
        createRole("warden");
        createRole("proctor");
        Reply user = call("POST", "/v1/iam/users", "{\"username\":\"warden1\",\"userType\":\"staff\"}");
        assertThat(user.status()).as(user.text()).isEqualTo(201);
        String userId = user.body().path("id").asString();
        long before = version();

        Reply set = call("POST", "/v1/iam/users/" + userId + ":setRoles",
                "{\"grants\":[{\"roleCode\":\"warden\",\"scopeType\":\"tenant\"},"
                        + "{\"roleCode\":\"proctor\",\"scopeType\":\"tenant\"}]}");
        assertThat(set.status()).as(set.text()).isEqualTo(200);
        assertThat(version()).as("two grants, one change").isEqualTo(before + 1);

        Reply again = call("POST", "/v1/iam/users/" + userId + ":setRoles",
                "{\"grants\":[{\"roleCode\":\"warden\",\"scopeType\":\"tenant\"},"
                        + "{\"roleCode\":\"proctor\",\"scopeType\":\"tenant\"}]}");
        assertThat(again.status()).as(again.text()).isEqualTo(200);
        assertThat(version()).as("the same set changes nothing").isEqualTo(before + 1);
    }

    @Test
    @Order(7)
    void moreRolesThanAnybodyCanHoldAreRefused() {
        List<UUID> tooMany = Stream.generate(UUID::randomUUID).limit(AuthzService.MAX_ROLES + 1L).toList();

        assertThatThrownBy(() -> as(TENANT, () -> authz.rolePermissions(TENANT, tooMany)))
                .isInstanceOf(InvalidArgumentException.class);
    }

    /** SEC-0 (4): a role that still holds a retired permission code says so on every read, derived from the data. */
    @Test
    @Order(8)
    void aRoleHoldingARetiredCodeIsFlaggedOnRead() throws Exception {
        String roleId = createRole("archivist");
        grantToRole(roleId, ALSO_GRANTED);
        assertThat(call("GET", "/v1/iam/roles/" + roleId, null).body().path("holdsRetiredPermissions").asBoolean())
                .isFalse();

        setRetired(ALSO_GRANTED, true);
        try {
            assertThat(call("GET", "/v1/iam/roles/" + roleId, null).body().path("holdsRetiredPermissions").asBoolean())
                    .isTrue();
            JsonNode listed = call("GET", "/v1/iam/roles?size=100", null).body().path("data");
            listed.forEach(role -> {
                if (role.path("id").asString().equals(roleId)) {
                    assertThat(role.path("holdsRetiredPermissions").asBoolean()).as("on lists too").isTrue();
                }
            });
        } finally {
            setRetired(ALSO_GRANTED, false);
        }
        assertThat(call("GET", "/v1/iam/roles/" + roleId, null).body().path("holdsRetiredPermissions").asBoolean())
                .as("published again").isFalse();
    }

    private void setRetired(String code, boolean retired) {
        inTenant(TENANT, () -> database.jdbc().sql(
                "UPDATE iam.permission SET retired_at = CASE WHEN :retired THEN now() END WHERE code = :code")
                .param("retired", retired).param("code", code).update());
    }

    private String roleIdOf(String code) {
        return inTenant(TENANT, () -> database.jdbc()
                .sql("SELECT id FROM iam.role WHERE tenant_id = :tenantId AND code = :code AND deleted_at IS NULL")
                .param("code", code).query(UUID.class).single()).toString();
    }

    private static List<String> codesOf(AuthzService.RoleAuthz answer, String roleId) {
        return answer.roles().stream().filter(role -> role.roleId().toString().equals(roleId))
                .findFirst().orElseThrow().permissionCodes();
    }
}
