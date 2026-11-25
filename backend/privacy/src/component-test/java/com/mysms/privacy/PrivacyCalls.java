package com.mysms.privacy;

import com.mysms.authclient.testing.TestTokens;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** REST calls to a running privacy service with real signed tokens, shared by its component tests. */
final class PrivacyCalls {

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final int port;
    private final TestTokens tokens;

    PrivacyCalls(int port, TestTokens tokens) {
        this.port = port;
        this.tokens = tokens;
    }

    /** An answer: status, body (a null node when empty) and ETag header. */
    record Reply(int status, JsonNode body, String etag) {

        String text(String field) {
            return body.path(field).asString();
        }
    }

    /** A tenant administrator's token: the permissions granted tenant-wide. */
    String administrator(UUID tenant, String... permissions) {
        return token(tenant, List.of(permissions), List.of("*"), permissions);
    }

    /**
     * A tenant administrator's token whose account is linked to a person (the pid claim): a staff member who is also
     * a data principal of the tenant.
     */
    String administratorAs(UUID tenant, UUID person, String... permissions) {
        Map<String, Object> claims = claims(tenant, List.of(permissions), List.of("*"), permissions);
        claims.put("pid", person.toString());
        return tokens.custom(claims, Instant.now().plusSeconds(900));
    }

    /**
     * A tenant administrator's token for a given login account, with no person linked (no pid claim): the staff
     * member is known to privacy only through that account.
     */
    String administratorAccount(UUID tenant, UUID account, String... permissions) {
        Map<String, Object> claims = claims(tenant, List.of(permissions), List.of("*"), permissions);
        claims.put("sub", account.toString());
        return tokens.custom(claims, Instant.now().plusSeconds(900));
    }

    /** A school administrator's token: the permissions granted for one school only, none tenant-wide. */
    String schoolAdministrator(UUID tenant, String... permissions) {
        return token(tenant, List.of(), List.of(UUID.randomUUID().toString()), permissions);
    }

    /** A school administrator's token: the permissions granted for the given school only, none tenant-wide. */
    String schoolAdministratorOf(UUID tenant, UUID institution, String... permissions) {
        return token(tenant, List.of(), List.of(institution.toString()), permissions);
    }

    /**
     * A signed-in person's token: their account is linked to the person (the pid claim) and holds the permissions
     * through a school's role, none tenant-wide.
     */
    String person(UUID tenant, UUID person, String... permissions) {
        Map<String, Object> claims = claims(tenant, List.of(), List.of(UUID.randomUUID().toString()), permissions);
        claims.put("pid", person.toString());
        return tokens.custom(claims, Instant.now().plusSeconds(900));
    }

    /** The same person's token while an administrator acts as them (the act claim names the administrator). */
    String impersonated(UUID tenant, UUID person, UUID administrator, String... permissions) {
        Map<String, Object> claims = claims(tenant, List.of(), List.of(UUID.randomUUID().toString()), permissions);
        claims.put("pid", person.toString());
        claims.put("act", Map.of("sub", administrator.toString()));
        return tokens.custom(claims, Instant.now().plusSeconds(900));
    }

    /**
     * A tenant administrator's token while another administrator acts as them (the act claim names the one acting):
     * the permissions granted tenant-wide, as {@link #administrator}.
     */
    String impersonatedAdministrator(UUID tenant, UUID acting, String... permissions) {
        Map<String, Object> claims = claims(tenant, List.of(permissions), List.of("*"), permissions);
        claims.put("act", Map.of("sub", acting.toString()));
        return tokens.custom(claims, Instant.now().plusSeconds(900));
    }

    private String token(UUID tenant, List<String> tenantWide, List<String> institutions, String... permissions) {
        return tokens.custom(claims(tenant, tenantWide, institutions, permissions), Instant.now().plusSeconds(900));
    }

    private static Map<String, Object> claims(UUID tenant, List<String> tenantWide, List<String> institutions,
            String... permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", UUID.randomUUID().toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of(permissions));
        claims.put("tperms", tenantWide);
        claims.put("ins", institutions);
        return claims;
    }

    Reply get(String path, String token) throws Exception {
        return send("GET", path, token, null, null);
    }

    /** A call as another module's service (an internal operation), in the tenant named by X-Tenant-Id. */
    Reply asService(String method, String path, String serviceToken, UUID tenant, Object body) throws Exception {
        return send(method, path, serviceToken, null, body, tenant);
    }

    Reply post(String path, String token, Object body) throws Exception {
        return send("POST", path, token, null, body);
    }

    Reply patch(String path, String token, String ifMatch, Object body) throws Exception {
        return send("PATCH", path, token, ifMatch, body);
    }

    Reply delete(String path, String token, String ifMatch) throws Exception {
        return send("DELETE", path, token, ifMatch, null);
    }

    private Reply send(String method, String path, String token, String ifMatch, Object body) throws Exception {
        return send(method, path, token, ifMatch, body, null);
    }

    private Reply send(String method, String path, String token, String ifMatch, Object body, UUID tenant)
            throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        if (ifMatch != null) {
            request.header("If-Match", ifMatch);
        }
        if (tenant != null) {
            request.header("X-Tenant-Id", tenant.toString());
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()),
                response.headers().firstValue("ETag").orElse(null));
    }

    /** The codes of a 422's field errors, as {@code field:code}. */
    static List<String> fieldErrors(Reply reply) {
        List<String> errors = new ArrayList<>();
        for (JsonNode error : reply.body().path("errors")) {
            errors.add(error.path("field").asString() + ":" + error.path("code").asString());
        }
        return errors;
    }
}
