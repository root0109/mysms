package com.mysms.iam;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.RolePermissionResolver;
import com.mysms.authclient.TokenVerifier;
import com.mysms.authclient.VerifiedToken;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.iam.IamHttp.Reply;
import com.mysms.iam.business.AccessClaims;
import com.mysms.iam.business.ClaimsFactory;
import com.mysms.iam.business.RefreshTokens;
import com.mysms.iam.config.IamDatabaseConfig;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
 * The tokens iam issues besides sign-in, and the grants behind them, end to end over HTTP against real PostgreSQL:
 * personal API tokens (secret once, only its hash stored, owner-only lists, the exchange limited to the token's
 * permissions, revocation under If-Match, tenant isolation), impersonation (strictly fewer rights only, an `act`
 * claim, both identities in the audit, ended by StopImpersonation), the role switch (tokens of one role, kept across
 * refresh), replacing a user's grants without escalation, the login-name check and the inline user import.
 */
@SpringBootTest(classes = IamApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IamTokenGrantsComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String TENANT_CODE = "grants";
    private static final String ADMIN_PASSWORD = "bootstrap-admin-secret-5";
    private static final String READ_USERS = "iam.user_account.read";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(IamDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    private IamHttp iam;

    @Autowired
    private TokenVerifier verifier;

    @Autowired
    private RolePermissionResolver roles;

    @Autowired
    private ClaimsFactory claimsFactory;

    /** The permissions the filter resolves for a token, from its role claims and iam's role data. */
    private Set<String> permissionsOf(String token) {
        VerifiedToken verified = verifier.verify(token);
        return roles.resolve(verified.context().tenantId().orElseThrow(), verified.roleClaims().orElseThrow(), token)
                .permissions();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "iam");
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

    private String admin() throws Exception {
        return iam.token("admin", ADMIN_PASSWORD);
    }

    private static List<String> texts(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(v -> values.add(v.asString()));
        return values;
    }

    @Test
    @Order(1)
    void anApiTokenIsShownOnceStoredHashedAndExchangedWithinItsLimits() throws Exception {
        UUID coder = iam.user("coder", null, "iam.user_account.create_api_token", READ_USERS);
        UUID other = iam.user("other", null);
        String token = iam.token("coder", IamHttp.PASSWORD);
        String path = "/v1/iam/users/" + coder + ":createApiToken";

        assertThat(iam.call("POST", "/v1/iam/users/" + other + ":createApiToken", token, "{\"name\":\"x\"}").status())
                .as("only for oneself").isEqualTo(403);
        Reply refused = iam.call("POST", path, token,
                "{\"name\":\"CI\",\"abilities\":[\"iam.role.delete\",\"no.such.code\"],\"expiresAt\":\"2000-01-01T00:00:00Z\"}");
        assertThat(refused.status()).as(refused.text()).isEqualTo(422);
        assertThat(refused.text()).contains("NOT_HELD").contains("abilities[1]").contains("expiresAt");

        Reply created = iam.call("POST", path, token, "{\"name\":\"CI\",\"abilities\":[\"" + READ_USERS + "\"]}");
        assertThat(created.status()).as(created.text()).isEqualTo(200);
        String secret = created.body().path("token").asString();
        String id = created.body().path("authToken").path("id").asString();
        assertThat(secret).startsWith("mysms_pat_" + iam.tenant() + ".");
        assertThat(created.body().path("authToken").path("tokenType").asString()).isEqualTo("api");
        String stored = iam.sql(() -> iam.jdbc().sql("SELECT token_hash FROM iam.auth_token WHERE id = :id")
                .param("id", UUID.fromString(id)).query(String.class).single());
        assertThat(stored).as("the stored value is not the token").isEqualTo(RefreshTokens.hash(secret))
                .isNotEqualTo(secret);

        Reply listed = iam.call("GET", "/v1/iam/users/" + coder + "/api-tokens", token, null);
        assertThat(listed.status()).as(listed.text()).isEqualTo(200);
        assertThat(listed.body().path("data")).hasSize(1);
        assertThat(listed.text()).doesNotContain(secret).doesNotContain(stored);
        String plain = iam.token("other", IamHttp.PASSWORD);
        assertThat(iam.call("GET", "/v1/iam/users/" + coder + "/api-tokens", plain, null).status())
                .as("another user's tokens do not exist for them").isEqualTo(404);
        assertThat(iam.call("GET", "/v1/iam/users/" + coder + "/api-tokens", admin(), null).body().path("data"))
                .as("a tenant-wide administrator sees them").hasSize(1);
        String stranger = TOKENS.tenantUser(UUID.randomUUID(), UUID.randomUUID(), "iam.auth_token.read");
        assertThat(iam.call("GET", "/v1/iam/users/" + coder + "/api-tokens", stranger, null).status())
                .as("another tenant").isEqualTo(404);

        Reply exchanged = iam.call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + secret + "\"}");
        assertThat(exchanged.status()).as(exchanged.text()).isEqualTo(200);
        assertThat(exchanged.body().path("refreshToken").asString()).as("not rotated").isEqualTo(secret);
        String access = exchanged.body().path("accessToken").asString();
        JsonNode claims = IamHttp.claims(access);
        assertThat(claims.has("perms")).as("roles and abl, never a permission list").isFalse();
        assertThat(texts(claims.path("abl"))).as("the limit travels as abl").containsExactly(READ_USERS);
        assertThat(permissionsOf(access)).containsExactly(READ_USERS);
        assertThat(claims.path("sid").asString()).isEqualTo(id);
        assertThat(iam.call("GET", "/v1/iam/users", access, null).status()).isEqualTo(200);
        assertThat(iam.call("POST", path, access, "{\"name\":\"more\"}").status())
                .as("beyond the token's limits").isEqualTo(403);
        Map<String, Object> capped = new HashMap<>();
        IamHttp.claims(access).properties().forEach(c -> capped.put(c.getKey(), JSON.convertValue(c.getValue(),
                Object.class)));
        capped.put("abl", List.of());
        for (String own : List.of("exp", "iat", "nbf", "iss", "jti")) {
            capped.remove(own);
        }
        assertThat(iam.call("GET", "/v1/iam/users", TOKENS.custom(capped, Instant.now().plusSeconds(300)), null)
                .status()).as("an empty abl allows nothing, whatever the roles hold").isEqualTo(403);

        String tokenPath = "/v1/iam/users/" + coder + "/api-tokens/" + id;
        assertThat(iam.call("DELETE", tokenPath, token, null).status()).isEqualTo(428);
        assertThat(iam.call("DELETE", tokenPath, token, null, "\"7\"").status()).isEqualTo(412);
        assertThat(iam.call("DELETE", tokenPath, plain, null, "\"1\"").status()).isEqualTo(404);
        Reply deleted = iam.call("DELETE", tokenPath, token, null, listed.body().path("data").get(0).path("etag")
                .asString());
        assertThat(deleted.status()).as(deleted.text()).isEqualTo(204);
        assertThat(iam.call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + secret + "\"}").status())
                .as("revoked at once").isEqualTo(401);
        assertThat(iam.call("GET", "/v1/iam/users/" + coder + "/api-tokens", token, null).body().path("data"))
                .isEmpty();
        assertThat(iam.call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"mysms_pat_garbage\"}").status())
                .isEqualTo(401);
    }

    @Test
    @Order(2)
    void anAdministratorActsAsAnAccountWithFewerRightsUntilStopped() throws Exception {
        String admin = admin();
        UUID administrator = iam.admin();
        UUID pupil = iam.user("pupil", null, READ_USERS);
        String path = "/v1/iam/users/" + pupil + ":impersonate";

        assertThat(iam.call("POST", path, admin, "{\"reason\":\"  \"}").status()).isEqualTo(422);
        assertThat(iam.call("POST", "/v1/iam/users/" + administrator + ":impersonate", admin,
                "{\"reason\":\"test\"}").status()).as("never oneself").isEqualTo(403);
        assertThat(iam.call("POST", "/v1/iam/users/" + UUID.randomUUID() + ":impersonate", admin,
                "{\"reason\":\"test\"}").status()).isEqualTo(404);

        Reply started = iam.call("POST", path, admin, "{\"reason\":\"Ticket 42: cannot see the timetable\"}");
        assertThat(started.status()).as(started.text()).isEqualTo(200);
        String acting = started.body().path("accessToken").asString();
        assertThat(started.body().path("impersonatedUserId").asString()).isEqualTo(pupil.toString());
        assertThat(started.body().path("expiresInSeconds").asInt()).isBetween(1, 900);
        assertThat(started.body().has("refreshToken")).isFalse();
        JsonNode claims = IamHttp.claims(acting);
        assertThat(claims.path("sub").asString()).isEqualTo(pupil.toString());
        assertThat(claims.path("act").path("sub").asString()).isEqualTo(administrator.toString());
        assertThat(iam.call("GET", "/v1/iam/me", acting, null).body().path("user").path("id").asString())
                .isEqualTo(pupil.toString());
        Reply password = iam.call("POST", "/v1/iam/auth:changePassword", acting,
                "{\"currentPassword\":\"" + IamHttp.PASSWORD + "\",\"newPassword\":\"third-strong-pass-3\"}");
        assertThat(password.status()).as("only the holder changes their password: " + password.text())
                .isEqualTo(403);

        assertThat(iam.call("POST", "/v1/iam/auth:stopImpersonation", admin, "{}").status())
                .as("not acting as anyone").isEqualTo(409);
        assertThat(iam.call("POST", "/v1/iam/auth:stopImpersonation", acting, "{}").status()).isEqualTo(204);
        assertThat(iam.call("POST", "/v1/iam/auth:stopImpersonation", acting, "{}").status())
                .as("already ended").isEqualTo(409);
        String audit = iam.sql(() -> iam.jdbc().sql("SELECT payload FROM iam.outbox_event WHERE event_type = "
                + "'audit.recorded' AND payload LIKE '%stopImpersonation%'").query(String.class).single());
        assertThat(audit).as("both identities").contains("\"actorUserId\":\"" + pupil)
                .contains("\"impersonatorUserId\":\"" + administrator);
        String reason = iam.sql(() -> iam.jdbc().sql("SELECT payload FROM iam.outbox_event WHERE event_type = "
                + "'audit.recorded' AND payload LIKE '%startImpersonation%'").query(String.class).single());
        assertThat(reason).contains("Ticket 42");
        Boolean ended = iam.sql(() -> iam.jdbc().sql("SELECT ended_at IS NOT NULL FROM iam.user_session "
                + "WHERE user_id = :user AND impersonator_user_id = :admin").param("user", pupil)
                .param("admin", administrator).query(Boolean.class).single());
        assertThat(ended).isTrue();

        UUID deputy = iam.user("deputy", null);
        UUID administratorRole = iam.sql(() -> iam.jdbc().sql("SELECT id FROM iam.role WHERE code = 'administrator'")
                .query(UUID.class).single());
        iam.grant(deputy, administratorRole, "tenant", null);
        assertThat(iam.call("POST", "/v1/iam/users/" + deputy + ":impersonate", admin, "{\"reason\":\"x\"}")
                .status()).as("never an account with the same rights").isEqualTo(403);
        iam.user("helper", null, "iam.user_account.start_impersonation", READ_USERS);
        UUID wider = iam.user("wider", null, "iam.user_account.delete");
        assertThat(iam.call("POST", "/v1/iam/users/" + wider + ":impersonate", iam.token("helper", IamHttp.PASSWORD),
                "{\"reason\":\"x\"}").status()).as("never one with rights the caller lacks").isEqualTo(403);
        iam.sql(() -> iam.jdbc().sql("UPDATE iam.user_account SET status = 'inactive' WHERE id = :id")
                .param("id", pupil).update());
        assertThat(iam.call("POST", path, admin, "{\"reason\":\"x\"}").status()).as("inactive").isEqualTo(409);
    }

    /**
     * The permissions the filter resolves from the role claims and iam's role data are exactly those the database
     * grants (SEC-0 equivalence): every catalogue code, for a full sign-in with tenant and institution roles, a
     * direct grant and denies (one of a code no deny removes), and for a session switched into the tenant-wide role —
     * where only the codes every role holds are no longer tenant-wide (owner, 2026-11-21: accepted narrowing).
     */
    @Test
    @Order(3)
    void theRoleClaimsResolveToExactlyThePermissionsTheDatabaseGrants() throws Exception {
        UUID user = iam.user("equivalence", null, "iam.role.read", "iam.role.update", "iam.user_account.read");
        UUID reviewer = iam.role("equivalence-reviewer", "iam.user_device.read", "iam.role.read");
        iam.grant(user, reviewer, "institution", UUID.randomUUID());
        UUID tenant = iam.tenant();
        for (String[] direct : new String[][] {{"hr.staff.read", "grant"}, {"iam.role.update", "deny"},
            {"iam.user_account.accept_terms", "deny"}}) {
            iam.sql(() -> iam.jdbc().sql("""
                    INSERT INTO iam.user_permission (id, tenant_id, user_id, permission_id, effect)
                    SELECT :id, :tenant, :user, pm.id, :effect FROM iam.permission pm WHERE pm.code = :code
                    """).param("id", UUID.randomUUID()).param("tenant", tenant).param("user", user)
                    .param("effect", direct[1]).param("code", direct[0]).update());
        }
        List<String> catalogue = iam.sql(() -> iam.jdbc().sql("SELECT code FROM iam.permission WHERE deleted_at IS NULL")
                .query(String.class).list());
        List<String> common = iam.sql(() -> iam.jdbc().sql(
                "SELECT code FROM iam.permission WHERE applies_to_all_roles AND deleted_at IS NULL")
                .query(String.class).list());

        String full = IamHttp.access(iam.signIn("equivalence", IamHttp.PASSWORD));
        assertSameAnswers(full, iam.sql(() -> claimsFactory.full(user, Optional.empty(), tenant, UUID.randomUUID(),
                Optional.empty())), catalogue, List.of());

        UUID tenantRole = texts(IamHttp.claims(full).path("rgr")).stream().filter(g -> g.endsWith("@tenant"))
                .map(g -> UUID.fromString(g.split("@")[0])).findFirst().orElseThrow();
        Reply switched = iam.call("POST", "/v1/iam/me:switchRole", full, "{\"roleId\":\"" + tenantRole + "\"}");
        assertThat(switched.status()).as(switched.text()).isEqualTo(200);
        assertSameAnswers(switched.body().path("tokens").path("accessToken").asString(),
                iam.sql(() -> claimsFactory.forRole(user, Optional.empty(), tenant, UUID.randomUUID(), tenantRole,
                        Optional.empty())), catalogue, common);
    }

    /**
     * For every code: the permissions the database states (what the claims factory still computes, the former
     * {@code perms}/{@code tperms}) and those the filter resolves from the token's role claims agree, tenant-wide ones
     * except {@code exempt}.
     */
    private void assertSameAnswers(String token, AccessClaims expected, List<String> catalogue, List<String> exempt) {
        VerifiedToken verified = verifier.verify(token);
        AuthContext context = verified.context();
        AuthContext listed = context.withPermissions(expected.permissions(), expected.tenantWidePermissions());
        RolePermissionResolver.Resolved resolved = roles.resolve(context.tenantId().orElseThrow(),
                verified.roleClaims().orElseThrow(), token);
        AuthContext fromRoles = context.withPermissions(resolved.permissions(), resolved.tenantWidePermissions());
        for (String code : catalogue) {
            assertThat(fromRoles.hasPermission(code)).as(code).isEqualTo(listed.hasPermission(code));
            if (!exempt.contains(code)) {
                assertThat(fromRoles.hasTenantWidePermission(code)).as("tenant-wide " + code)
                        .isEqualTo(listed.hasTenantWidePermission(code));
            }
        }
    }

    /**
     * The role-based claims the permissions filter will resolve from (SEC-0): every counted role grant by id and
     * scope, the direct grants, the denies (never of a non-modifiable code) and the tenant's authorisation version.
     */
    @Test
    @Order(3)
    void aTokenCarriesItsRoleGrantsDirectGrantsDeniesAndTheAuthorisationVersion() throws Exception {
        UUID user = iam.user("authzclaims", null, "iam.role.read", "iam.role.update");
        UUID reviewer = iam.role("authz-reviewer", "iam.user_device.read");
        UUID institution = UUID.randomUUID();
        iam.grant(user, reviewer, "institution", institution);
        iam.grant(user, reviewer, "institution", UUID.randomUUID());
        UUID tenant = iam.tenant();
        for (String[] direct : new String[][] {{"hr.staff.read", "grant"}, {"iam.role.update", "deny"},
            {"iam.user_account.accept_terms", "deny"}}) {
            iam.sql(() -> iam.jdbc().sql("""
                    INSERT INTO iam.user_permission (id, tenant_id, user_id, permission_id, effect)
                    SELECT :id, :tenant, :user, pm.id, :effect FROM iam.permission pm WHERE pm.code = :code
                    """).param("id", UUID.randomUUID()).param("tenant", tenant).param("user", user)
                    .param("effect", direct[1]).param("code", direct[0]).update());
        }

        JsonNode claims = IamHttp.claims(IamHttp.access(iam.signIn("authzclaims", IamHttp.PASSWORD)));

        assertThat(texts(claims.path("rgr"))).as("each role once; a role held on units only has no scope")
                .hasSize(2).contains(reviewer.toString())
                .anyMatch(grant -> grant.endsWith("@tenant") && !grant.startsWith(reviewer.toString()));
        assertThat(texts(claims.path("dg"))).containsExactly("hr.staff.read");
        assertThat(texts(claims.path("dd"))).as("a self-service code is never denied").containsExactly("iam.role.update");
        assertThat(claims.path("azv").isIntegralNumber()).isTrue();
        assertThat(claims.has("abl")).as("only a limited API token carries abilities").isFalse();
        assertThat(claims.has("perms") || claims.has("tperms")).as("no permission lists (SEC-0)").isFalse();
        assertThat(permissionsOf(IamHttp.access(iam.signIn("authzclaims", IamHttp.PASSWORD))))
                .contains("iam.role.read", "hr.staff.read", "iam.user_device.read").doesNotContain("iam.role.update");
    }

    @Test
    @Order(3)
    void aSwitchedSessionCarriesOneRoleAcrossRefresh() throws Exception {
        UUID multi = iam.user("multi", null, "iam.role.read");
        UUID second = iam.role("reviewer", "iam.user_device.read");
        iam.grant(multi, second, "tenant", null);
        Reply login = iam.signIn("multi", IamHttp.PASSWORD);
        String token = IamHttp.access(login);
        String oldRefresh = login.body().path("tokens").path("refreshToken").asString();
        assertThat(permissionsOf(token)).contains("iam.role.read", "iam.user_device.read");
        Reply me = iam.call("GET", "/v1/iam/me", token, null);
        assertThat(me.body().path("roles")).hasSize(2);
        me.body().path("roles").forEach(r -> assertThat(r.hasNonNull("roleId")).isTrue());

        Reply notMine = iam.call("POST", "/v1/iam/me:switchRole", token, "{\"roleId\":\"" + UUID.randomUUID() + "\"}");
        assertThat(notMine.status()).isEqualTo(422);
        assertThat(notMine.firstErrorCode()).isEqualTo("NOT_GRANTED");
        Reply switched = iam.call("POST", "/v1/iam/me:switchRole", token, "{\"roleId\":\"" + second + "\"}");
        assertThat(switched.status()).as(switched.text()).isEqualTo(200);
        String switchedToken = switched.body().path("tokens").path("accessToken").asString();
        JsonNode claims = IamHttp.claims(switchedToken);
        assertThat(permissionsOf(switchedToken)).contains("iam.user_device.read").doesNotContain("iam.role.read");
        assertThat(texts(claims.path("roles"))).containsExactly("reviewer@tenant");
        assertThat(texts(claims.path("rgr"))).containsExactly(second + "@tenant");
        assertThat(texts(claims.path("dg"))).as("no direct grants while working in one role").isEmpty();
        assertThat(switched.body().path("me").path("activeRole").path("roleCode").asString()).isEqualTo("reviewer");
        assertThat(texts(switched.body().path("me").path("permissions"))).doesNotContain("iam.role.read");
        Integer remembered = iam.sql(() -> iam.jdbc().sql("SELECT count(*) FROM iam.user_account WHERE id = :id "
                + "AND last_login_role_id = :role").param("id", multi).param("role", second).query(Integer.class)
                .single());
        assertThat(remembered).isEqualTo(1);

        assertThat(iam.call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + oldRefresh + "\"}")
                .status()).as("the earlier pair is replaced").isEqualTo(401);
        Reply refreshed = iam.call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\""
                + switched.body().path("tokens").path("refreshToken").asString() + "\"}");
        assertThat(refreshed.status()).as(refreshed.text()).isEqualTo(200);
        assertThat(permissionsOf(refreshed.body().path("accessToken").asString()))
                .as("the role stays across refresh").doesNotContain("iam.role.read");
    }

    @Test
    @Order(4)
    void aUsersGrantsAreReplacedWithoutEscalation() throws Exception {
        String admin = admin();
        UUID target = iam.user("target", null);
        UUID clerkRole = iam.role("clerk", READ_USERS);
        iam.role("auditor", "iam.permission.read");
        String path = "/v1/iam/users/" + target + ":setRoles";

        Reply set = iam.call("POST", path, admin, "{\"grants\":[{\"roleCode\":\"clerk\",\"scopeType\":\"tenant\"}]}");
        assertThat(set.status()).as(set.text()).isEqualTo(200);
        assertThat(set.body().path("items")).hasSize(1);
        assertThat(set.body().path("items").get(0).path("roleId").asString()).isEqualTo(clerkRole.toString());
        Reply replaced = iam.call("POST", path, admin, "{\"grants\":[{\"roleCode\":\"auditor\",\"scopeType\":\"tenant\"},"
                + "{\"roleCode\":\"clerk\",\"scopeType\":\"tenant\"}]}");
        assertThat(replaced.body().path("items")).hasSize(2);
        assertThat(replaced.body().path("items").get(0).path("id").asString()).as("an unchanged grant is kept")
                .isEqualTo(set.body().path("items").get(0).path("id").asString());
        Reply narrowed = iam.call("POST", path, admin, "{\"grants\":[{\"roleCode\":\"auditor\",\"scopeType\":\"tenant\"}]}");
        assertThat(narrowed.body().path("items")).hasSize(1);
        Integer live = iam.sql(() -> iam.jdbc().sql("SELECT count(*) FROM iam.user_role WHERE user_id = :id "
                + "AND deleted_at IS NULL").param("id", target).query(Integer.class).single());
        assertThat(live).isEqualTo(1);

        Reply unknown = iam.call("POST", path, admin, "{\"grants\":[{\"roleCode\":\"nope\",\"scopeType\":\"tenant\"},"
                + "{\"roleCode\":\"clerk\",\"scopeType\":\"institution\"}]}");
        assertThat(unknown.status()).isEqualTo(422);
        assertThat(unknown.text()).contains("grants[0].roleCode").contains("grants[1].scopeId");
        assertThat(iam.call("POST", "/v1/iam/users/" + iam.admin() + ":setRoles", admin, "{\"grants\":[]}").status())
                .as("never one's own").isEqualTo(403);
        iam.user("granter", null, "iam.user_account.set_user_roles", READ_USERS);
        Reply escalation = iam.call("POST", path, iam.token("granter", IamHttp.PASSWORD),
                "{\"grants\":[{\"roleCode\":\"clerk\",\"scopeType\":\"tenant\"}]}");
        assertThat(escalation.status()).as("removing auditor needs its permissions too: " + escalation.text())
                .isEqualTo(403);
    }

    @Test
    @Order(5)
    void loginNamesAreCheckedOnlyForAccountAdministrators() throws Exception {
        assertThat(iam.call("GET", "/v1/iam/users:checkUsername?username=pupil", null, null).status())
                .as("anonymous").isEqualTo(401);
        iam.user("nobody", null);
        assertThat(iam.call("GET", "/v1/iam/users:checkUsername?username=pupil", iam.token("nobody", IamHttp.PASSWORD),
                null).status()).isEqualTo(403);
        Reply taken = iam.call("GET", "/v1/iam/users:checkUsername?username=PUPIL", admin(), null);
        assertThat(taken.status()).as(taken.text()).isEqualTo(200);
        assertThat(taken.body().path("available").asBoolean()).isFalse();
        assertThat(taken.body().path("suggestion").asString()).isEqualTo("PUPIL2");
        assertThat(iam.call("GET", "/v1/iam/users:checkUsername?username=fresh.name", admin(), null).body()
                .path("available").asBoolean()).isTrue();
    }

    @Test
    @Order(6)
    void usersAreImportedFromInlineRowsAllOrNothing() throws Exception {
        String admin = admin();
        String rows = "{\"rows\":[{\"username\":\"imp1\",\"email\":\"imp1@school.test\",\"userType\":\"teacher\","
                + "\"grants\":[{\"roleCode\":\"clerk\",\"scopeType\":\"tenant\"}]},{\"mobileNo\":\"9000000001\"}]";
        Reply dryRun = iam.call("POST", "/v1/iam/users:import", admin, rows + ",\"validateOnly\":true}");
        assertThat(dryRun.status()).as(dryRun.text()).isEqualTo(202);
        assertThat(dryRun.body().path("result").path("userAccountIds")).isEmpty();
        Reply imported = iam.call("POST", "/v1/iam/users:import", admin, rows + "}");
        assertThat(imported.status()).as(imported.text()).isEqualTo(202);
        assertThat(imported.body().path("status").asString()).isEqualTo("succeeded");
        assertThat(imported.body().path("result").path("userAccountIds")).hasSize(2);
        String operation = imported.body().path("operationId").asString();
        assertThat(iam.call("GET", "/v1/iam/operations/" + operation, admin, null).status()).isEqualTo(200);
        Integer grants = iam.sql(() -> iam.jdbc().sql("SELECT count(*) FROM iam.user_role ur JOIN iam.user_account ua "
                + "ON ua.tenant_id = ur.tenant_id AND ua.id = ur.user_id WHERE ua.username = 'imp1'")
                .query(Integer.class).single());
        assertThat(grants).isEqualTo(1);

        Reply again = iam.call("POST", "/v1/iam/users:import", admin, rows + "}");
        assertThat(again.status()).isEqualTo(422);
        assertThat(again.text()).contains("rows[0].username").contains("DUPLICATE");
        Reply file = iam.call("POST", "/v1/iam/users:import", admin, "{\"fileId\":\"" + UUID.randomUUID() + "\"}");
        assertThat(file.status()).isEqualTo(422);
        assertThat(file.firstErrorCode()).isEqualTo("FILE_IMPORT_UNAVAILABLE");
        Reply empty = iam.call("POST", "/v1/iam/users:import", admin, "{\"rows\":[{\"userType\":\"teacher\"}]}");
        assertThat(empty.firstErrorCode()).isEqualTo("REQUIRED");
        iam.user("importer", null, "iam.user_account.import_users");
        Reply escalation = iam.call("POST", "/v1/iam/users:import", iam.token("importer", IamHttp.PASSWORD),
                "{\"rows\":[{\"username\":\"imp9\",\"grants\":[{\"roleCode\":\"clerk\",\"scopeType\":\"tenant\"}]}]}");
        assertThat(escalation.status()).as(escalation.text()).isEqualTo(403);
        Integer created = iam.sql(() -> iam.jdbc().sql("SELECT count(*) FROM iam.user_account WHERE username = 'imp9'")
                .query(Integer.class).single());
        assertThat(created).as("all or nothing").isZero();
    }

    @Test
    @Order(7)
    void aCallMadeWithAnApiTokenCreatesNoTokenThatReachesFurtherOrLivesLonger() throws Exception {
        String create = "iam.user_account.create_api_token";
        UUID minter = iam.user("minter", null, create, READ_USERS, "iam.role.read");
        String session = iam.token("minter", IamHttp.PASSWORD);
        String path = "/v1/iam/users/" + minter + ":createApiToken";
        String ends = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).plusDays(10)
                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString();
        Reply limited = iam.call("POST", path, session, "{\"name\":\"CI\",\"abilities\":[\"" + create + "\",\""
                + READ_USERS + "\"],\"expiresAt\":\"" + ends + "\"}");
        assertThat(limited.status()).as(limited.text()).isEqualTo(200);
        String viaToken = exchange(limited.body().path("token").asString());
        assertThat(texts(IamHttp.claims(viaToken).path("amr"))).containsExactly("pat");

        Reply inherited = iam.call("POST", path, viaToken, "{\"name\":\"child\"}");
        assertThat(inherited.status()).as(inherited.text()).isEqualTo(200);
        UUID child = UUID.fromString(inherited.body().path("authToken").path("id").asString());
        assertThat(scopes(child)).as("no abilities named: the calling token's own, never unlimited")
                .isEqualTo("{" + create + "," + READ_USERS + "}");
        assertThat(java.time.OffsetDateTime.parse(inherited.body().path("authToken").path("expiresAt").asString())
                .toInstant()).as("it ends with the calling token").isEqualTo(java.time.Instant.parse(ends));
        Reply wider = iam.call("POST", path, viaToken, "{\"name\":\"wider\",\"abilities\":[\"iam.role.read\"]}");
        assertThat(wider.status()).as(wider.text()).isEqualTo(422);
        Reply longer = iam.call("POST", path, viaToken, "{\"name\":\"longer\",\"expiresAt\":\""
                + java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).plusDays(30) + "\"}");
        assertThat(longer.status()).as(longer.text()).isEqualTo(422);
        assertThat(longer.firstErrorCode()).isEqualTo("OUTLIVES_TOKEN");
        Reply narrower = iam.call("POST", path, viaToken, "{\"name\":\"narrow\",\"abilities\":[\"" + READ_USERS
                + "\"]}");
        assertThat(narrower.status()).as(narrower.text()).isEqualTo(200);

        Reply unlimited = iam.call("POST", path, session, "{\"name\":\"everything\"}");
        assertThat(unlimited.status()).as("a signed-in owner may create an unlimited token").isEqualTo(200);
        String viaUnlimited = exchange(unlimited.body().path("token").asString());
        Reply unnamed = iam.call("POST", path, viaUnlimited, "{\"name\":\"copy\"}");
        assertThat(unnamed.status()).as(unnamed.text()).isEqualTo(422);
        assertThat(unnamed.firstErrorCode()).isEqualTo("REQUIRED");
        assertThat(iam.call("POST", path, viaUnlimited, "{\"name\":\"named\",\"abilities\":[\"iam.role.read\"]}")
                .status()).isEqualTo(200);

        iam.sql(() -> iam.jdbc().sql("UPDATE iam.auth_token SET revoked_at = now() WHERE id = :id")
                .param("id", UUID.fromString(limited.body().path("authToken").path("id").asString())).update());
        assertThat(iam.call("POST", path, viaToken, "{\"name\":\"after\"}").status())
                .as("the calling token was revoked meanwhile").isEqualTo(403);
    }

    @Test
    @Order(8)
    void signingOutEverywhereEndsTheApiTokensToo() throws Exception {
        String admin = admin();
        String create = "iam.user_account.create_api_token";
        UUID quitter = iam.user("quitter", null, create, READ_USERS, "iam.user_account.logout");
        String secret = apiToken("quitter", quitter);
        assertThat(iam.call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + secret + "\"}")
                .status()).isEqualTo(200);
        assertThat(iam.call("POST", "/v1/iam/auth:logout", iam.token("quitter", IamHttp.PASSWORD),
                "{\"allDevices\":true}").status()).isEqualTo(204);
        assertThat(iam.call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + secret + "\"}")
                .status()).as("signed out of every device").isEqualTo(401);
        JsonNode listed = iam.call("GET", "/v1/iam/users/" + quitter + "/api-tokens", admin, null).body();
        assertThat(listed.path("data").get(0).hasNonNull("revokedAt")).as("kept in the list, revoked").isTrue();
        assertThat(listed.path("data").get(0).path("etag").asString()).as("a change of the token").isEqualTo("\"2\"");
        assertThat(iam.sql(() -> iam.jdbc().sql("SELECT count(*) FROM iam.outbox_event WHERE event_type = "
                + "'iam.user_account.api_token_revoked' AND aggregate_id = :id").param("id", quitter)
                .query(Integer.class).single())).isEqualTo(1);

        UUID leaver = iam.user("leaver", null, create, READ_USERS);
        String kept = apiToken("leaver", leaver);
        String etag = iam.call("GET", "/v1/iam/users/" + leaver, admin, null).body().path("etag").asString();
        Reply deactivated = iam.call("POST", "/v1/iam/users/" + leaver + ":deactivate", admin,
                "{\"etag\":" + IamHttp.JSON.writeValueAsString(etag) + "}");
        assertThat(deactivated.status()).as(deactivated.text()).isEqualTo(200);
        iam.sql(() -> iam.jdbc().sql("UPDATE iam.user_account SET status = 'active' WHERE id = :id")
                .param("id", leaver).update());
        assertThat(iam.call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + kept + "\"}")
                .status()).as("reactivating the account revives no token").isEqualTo(401);

        UUID handed = iam.user("handed", null, create, READ_USERS);
        String old = apiToken("handed", handed);
        Reply set = iam.call("POST", "/v1/iam/users/" + handed + ":setCredentials", admin,
                "{\"temporaryPassword\":\"temporary-pass-456\"}");
        assertThat(set.status()).as(set.text()).isEqualTo(200);
        assertThat(iam.call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + old + "\"}")
                .status()).as("new credentials end the old ones' tokens").isEqualTo(401);

        UUID changer = iam.user("changer", null, create, READ_USERS, "iam.user_account.change_password");
        String before = apiToken("changer", changer);
        Reply changed = iam.call("POST", "/v1/iam/auth:changePassword", iam.token("changer", IamHttp.PASSWORD),
                "{\"currentPassword\":\"" + IamHttp.PASSWORD + "\",\"newPassword\":\"changed-pass-789\"}");
        assertThat(changed.status()).as(changed.text()).isEqualTo(204);
        assertThat(iam.call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + before + "\"}")
                .status()).isEqualTo(401);
    }

    private String apiToken(String username, UUID user) throws Exception {
        Reply created = iam.call("POST", "/v1/iam/users/" + user + ":createApiToken",
                iam.token(username, IamHttp.PASSWORD), "{\"name\":\"CI\"}");
        assertThat(created.status()).as(created.text()).isEqualTo(200);
        return created.body().path("token").asString();
    }

    private String exchange(String secret) throws Exception {
        Reply exchanged = iam.call("POST", "/v1/iam/auth:refresh", null, "{\"refreshToken\":\"" + secret + "\"}");
        assertThat(exchanged.status()).as(exchanged.text()).isEqualTo(200);
        return exchanged.body().path("accessToken").asString();
    }

    private String scopes(UUID token) {
        return iam.sql(() -> iam.jdbc().sql("SELECT CAST(scopes AS text) FROM iam.auth_token WHERE id = :id")
                .param("id", token).query(String.class).single());
    }
}
