package com.mysms.demoseed.client;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * The seeder's typed edge to MySMS's REST API: JSON in and out, the caller's bearer token, list paging, and the
 * find-or-create step that makes a second run skip what the first one made. Expired tokens are renewed once and a
 * rate-limited call waits for its {@code Retry-After}; any other refusal is an {@link ApiException}.
 */
public final class ApiClient {

    private static final int PAGE_SIZE = 100;
    private static final int MAX_WAITS = 5;
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient http;
    private final URI base;
    private final JsonMapper json;
    private final Bearer bearer;

    private ApiClient(HttpClient http, URI base, JsonMapper json, Bearer bearer) {
        this.http = http;
        this.base = base;
        this.json = json;
        this.bearer = bearer;
    }

    /** A client without a token, for sign-in and the public reads. */
    public static ApiClient anonymous(URI base, JsonMapper json) {
        return new ApiClient(HttpClient.newBuilder().connectTimeout(TIMEOUT).build(), base, json, null);
    }

    /** The same server, calling as the given bearer. */
    public ApiClient as(Bearer caller) {
        return new ApiClient(http, base, json, caller);
    }

    public ObjectNode object() {
        return json.createObjectNode();
    }

    public JsonMapper json() {
        return json;
    }

    public JsonNode get(String path) {
        return send("GET", path, null, null, null);
    }

    public JsonNode post(String path, JsonNode body) {
        return send("POST", path, body, null, null);
    }

    /** A POST the contract requires an {@code Idempotency-Key} for; a deterministic key makes a retry a replay. */
    public JsonNode post(String path, JsonNode body, String idempotencyKey) {
        return send("POST", path, body, null, idempotencyKey);
    }

    public JsonNode patch(String path, JsonNode body, String etag) {
        return send("PATCH", path, body, etag, null);
    }

    public void delete(String path, String etag) {
        send("DELETE", path, null, etag, null);
    }

    /** Every row of a list, following its {@code next} links; {@code filter} in the API's filter grammar or null. */
    public List<JsonNode> list(String path, String filter) {
        List<JsonNode> rows = new ArrayList<>();
        String next = path + (path.contains("?") ? "&" : "?") + "size=" + PAGE_SIZE
                + (filter == null ? "" : "&filter=" + URLEncoder.encode(filter, StandardCharsets.UTF_8));
        while (next != null) {
            JsonNode page = get(next);
            page.path("data").forEach(rows::add);
            String link = page.path("paging").path("next").asString(null);
            next = link == null || link.isBlank() ? null : link;
        }
        return rows;
    }

    /** The first row the filter matches. */
    public Optional<JsonNode> find(String path, String filter) {
        List<JsonNode> rows = list(path, filter);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /** The row the filter names, created from {@code body} when there is none. */
    public Ensured ensure(String path, String filter, JsonNode body) {
        Optional<JsonNode> existing = find(path, filter);
        return existing.map(row -> new Ensured(row, false)).orElseGet(() -> new Ensured(post(path, body), true));
    }

    /** A row looked up or created by {@link #ensure}. */
    public record Ensured(JsonNode row, boolean created) {

        public String id() {
            return row.path("id").asString();
        }
    }

    private JsonNode send(String method, String path, JsonNode body, String etag, String idempotencyKey) {
        boolean renewed = false;
        for (int wait = 0; ; wait++) {
            HttpResponse<String> response = exchange(method, path, body, etag, idempotencyKey);
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return response.body().isEmpty() ? json.nullNode() : json.readTree(response.body());
            }
            if (status == 401 && bearer != null && !renewed && bearer.renew()) {
                renewed = true;
                continue;
            }
            if (status == 429 && wait < MAX_WAITS) {
                pause(response.headers().firstValue("Retry-After").map(Long::parseLong).orElse(1L));
                continue;
            }
            JsonNode problem = response.body().isEmpty() ? json.nullNode() : readProblem(response.body());
            throw new ApiException(method + " " + path, status, problem.path("code").asString(""),
                    problem.path("detail").asString("") + describeErrors(problem.path("errors")));
        }
    }

    private HttpResponse<String> exchange(String method, String path, JsonNode body, String etag,
                                          String idempotencyKey) {
        HttpRequest.Builder request = HttpRequest.newBuilder(base.resolve(path)).timeout(TIMEOUT)
                .header("Accept", "application/json, application/problem+json, */*;q=0.1")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer.token());
        }
        if (etag != null) {
            request.header("If-Match", etag);
        }
        if (idempotencyKey != null) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        try {
            return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IllegalStateException(method + " " + path + " could not reach " + base, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted during " + method + " " + path, e);
        }
    }

    private JsonNode readProblem(String body) {
        try {
            return json.readTree(body);
        } catch (tools.jackson.core.JacksonException e) {
            return json.nullNode();
        }
    }

    /** Field names and codes of a validation problem; the rejected values are left out. */
    private static String describeErrors(JsonNode errors) {
        StringBuilder text = new StringBuilder();
        errors.forEach(error -> text.append(" [").append(error.path("field").asString(""))
                .append(' ').append(error.path("code").asString("")).append(' ')
                .append(error.path("message").asString("")).append(']'));
        return text.toString();
    }

    private static void pause(long seconds) {
        try {
            Thread.sleep(Duration.ofSeconds(Math.max(1, seconds)).toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while rate limited", e);
        }
    }
}
