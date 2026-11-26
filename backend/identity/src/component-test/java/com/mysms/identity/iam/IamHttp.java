package com.mysms.identity.iam;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.identity.iam.business.PasswordHasher;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * What the account component tests share: real HTTP calls to the running service, direct SQL in a tenant transaction
 * for fixtures and for checking what is stored, accounts with roles, and a clock the tests can move forward to let a
 * secret expire (the service takes its time only from the injected {@link Clock}).
 */
final class IamHttp {

    static final HttpClient HTTP = HttpClient.newHttpClient();
    static final JsonMapper JSON = JsonMapper.builder().build();
    static final PasswordHasher HASHER = new PasswordHasher();
    static final String PASSWORD = "first-strong-pass-1";
    /** A mailed one-time secret: {@code <tenant>.<256 random bits>}. */
    static final Pattern SECRET = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
            + "\\.[A-Za-z0-9_-]{43}");

    private final int port;
    private final ModuleDatabase database;
    private final String tenantCode;

    IamHttp(int port, ModuleDatabase database, String tenantCode) {
        this.port = port;
        this.database = database;
        this.tenantCode = tenantCode;
    }

    record Reply(int status, JsonNode body, String text) {

        String code() {
            return body.path("code").asString();
        }

        String firstErrorCode() {
            return body.path("errors").path(0).path("code").asString();
        }
    }

    Reply call(String method, String path, String token, String body) throws Exception {
        return call(method, path, token, body, null);
    }

    Reply call(String method, String path, String token, String body, String ifMatch) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (ifMatch != null) {
            request.header("If-Match", ifMatch);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode json = response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body());
        return new Reply(response.statusCode(), json, method + " " + path + " -> " + response.statusCode() + " "
                + response.body());
    }

    Reply signIn(String identifier, String password) throws Exception {
        return call("POST", "/v1/iam/auth:login", null, "{\"tenantCode\":\"" + tenantCode + "\",\"identifier\":\""
                + identifier + "\",\"password\":\"" + password + "\"}");
    }

    String token(String identifier, String password) throws Exception {
        Reply login = signIn(identifier, password);
        if (login.status() != 200) {
            throw new AssertionError(login.text());
        }
        return access(login);
    }

    static String access(Reply login) {
        return login.body().path("tokens").path("accessToken").asString();
    }

    static JsonNode claims(String jwt) {
        return JSON.readTree(new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), StandardCharsets.UTF_8));
    }

    /** The one-time secret in a mail body. */
    static String secretIn(String body) {
        Matcher m = SECRET.matcher(body);
        if (!m.find()) {
            throw new AssertionError("no code in: " + body);
        }
        return m.group();
    }

    UUID tenant() {
        return as(null, () -> TestTransactions.of(database).readWithoutTenant(() -> database.jdbc()
                .sql("SELECT tenant_id FROM iam.tenant_directory WHERE tenant_code = :code").param("code", tenantCode)
                .query(UUID.class).single()));
    }

    <T> T sql(Supplier<T> work) {
        return as(tenant(), () -> TestTransactions.of(database).write(work::get));
    }

    org.springframework.jdbc.core.simple.JdbcClient jdbc() {
        return database.jdbc();
    }

    private static <T> T as(UUID tenant, Supplier<T> work) {
        CallContext context = CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736").withIdentity(tenant, null);
        return CallContextHolder.callAs(context, work);
    }

    /** A role of the tenant holding the permissions (applies to every role's self-service ones too). */
    UUID role(String code, String... permissions) {
        UUID tenant = tenant();
        UUID role = UUID.randomUUID();
        sql(() -> {
            database.jdbc().sql("INSERT INTO iam.role (id, tenant_id, code) VALUES (:id, :tenant, :code)")
                    .param("id", role).param("tenant", tenant).param("code", code).update();
            for (String permission : permissions) {
                database.jdbc().sql("""
                        INSERT INTO iam.role_permission (id, tenant_id, role_id, permission_id)
                        SELECT :rp, :tenant, :role, pm.id FROM iam.permission pm WHERE pm.code = :code
                        """).param("rp", UUID.randomUUID()).param("tenant", tenant).param("role", role)
                        .param("code", permission).update();
            }
            return null;
        });
        return role;
    }

    /** Grants the role to the user at a scope (tenant when {@code scopeId} is null). */
    void grant(UUID user, UUID role, String scopeType, UUID scopeId) {
        UUID tenant = tenant();
        sql(() -> database.jdbc().sql("""
                INSERT INTO iam.user_role (id, tenant_id, user_id, role_id, scope_type, scope_id)
                VALUES (:id, :tenant, :user, :role, :scopeType, :scopeId)
                """).param("id", UUID.randomUUID()).param("tenant", tenant).param("user", user).param("role", role)
                .param("scopeType", scopeType).param("scopeId", scopeId).update());
    }

    /** An active account with the test password, an optional e-mail and a tenant-wide role of the permissions. */
    UUID user(String username, String email, String... permissions) {
        UUID tenant = tenant();
        UUID id = UUID.randomUUID();
        sql(() -> database.jdbc().sql("""
                INSERT INTO iam.user_account (id, tenant_id, username, email, password_hash, status, user_type)
                VALUES (:id, :tenant, :username, :email, :hash, 'active', 'teacher')
                """).param("id", id).param("tenant", tenant).param("username", username).param("email", email)
                .param("hash", HASHER.hash(PASSWORD)).update());
        grant(id, role(username + "-role", permissions), "tenant", null);
        return id;
    }

    /** The bootstrap administrator's id. */
    UUID admin() {
        return sql(() -> database.jdbc().sql("SELECT id FROM iam.user_account WHERE username = 'admin'")
                .query(UUID.class).single());
    }

    /** A clock the tests move forward; starts at the real time. */
    static final class MovableClock extends Clock {

        private volatile Duration offset = Duration.ZERO;

        void advance(Duration by) {
            offset = offset.plus(by);
        }

        void reset() {
            offset = Duration.ZERO;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.now().plus(offset);
        }
    }
}
