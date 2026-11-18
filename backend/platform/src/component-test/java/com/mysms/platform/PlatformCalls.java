package com.mysms.platform;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestTransactions;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntSupplier;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * What the platform's workflow component tests share: real HTTP calls to the running service, real signed tokens
 * (tenant users with institution grants, tenant administrators, platform staff) and SQL set-up as the application
 * role in a tenant's transaction (row-level security on).
 */
final class PlatformCalls {

    static final JsonMapper JSON = JsonMapper.builder().build();
    static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    /** One answer: the status, the body as JSON (null node when empty), the raw text and the headers. */
    record Reply(int status, JsonNode body, String text, HttpResponse<String> response) {

        String header(String name) {
            return response.headers().firstValue(name).orElse(null);
        }

        String etag() {
            return header("ETag");
        }
    }

    private final TestTokens tokens;
    private final IntSupplier port;
    private final ModuleDatabase database;

    PlatformCalls(TestTokens tokens, IntSupplier port, ModuleDatabase database) {
        this.tokens = tokens;
        this.port = port;
        this.database = database;
    }

    Reply call(String method, String path, String token, String body, String... headers) {
        return send(method, path, token, body == null ? null : body.getBytes(StandardCharsets.UTF_8), headers);
    }

    Reply send(String method, String path, String token, byte[] body, String... headers) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port.getAsInt() + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofByteArray(body));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        try {
            HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
            return new Reply(response.statusCode(),
                    response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()), response.body(),
                    response);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /** A tenant administrator: every permission given, through a tenant-wide grant. */
    String admin(UUID tenant, UUID user, String... permissions) {
        return token(tenant, user, List.of("*"), permissions);
    }

    /** A tenant user whose grants are in the listed institutions ({@code *}: tenant-wide). */
    String token(UUID tenant, UUID user, List<String> institutions, String... permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("amr", List.of("pwd"));
        claims.put("perms", List.of(permissions));
        claims.put("tperms", institutions.contains("*") ? List.of(permissions) : List.of());
        claims.put("ins", institutions);
        return tokens.custom(claims, Instant.now().plusSeconds(900));
    }

    String staff(UUID user, String... permissions) {
        return tokens.platformOperator(user, permissions);
    }

    /** Runs set-up SQL as the application role in the tenant's transaction (row-level security on). */
    void sql(UUID tenant, String statement, Map<String, Object> params) {
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).write(() -> database.jdbc().sql(statement).params(params)
                        .update()));
    }

    /** Runs set-up SQL on global tables (no tenant bound), as platform staff tooling would. */
    void sqlGlobal(String statement, Map<String, Object> params) {
        TestTransactions.of(database).writeWithoutTenant(() -> database.jdbc().sql(statement).params(params)
                .update());
    }

    /** A value read in the tenant's transaction. */
    <T> T query(UUID tenant, String statement, Map<String, Object> params, Class<T> type) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).read(() -> database.jdbc().sql(statement).params(params)
                        .query(type).single()));
    }

    void tenant(UUID tenant, String code) {
        sql(tenant, "INSERT INTO platform.tenant (id, code, name, locale, timezone) VALUES (:tenant, :code, :name, "
                + "'hi', 'Asia/Kolkata') ON CONFLICT (id) DO NOTHING",
                Map.of("tenant", tenant, "code", code, "name", "School group " + code));
    }

    static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
