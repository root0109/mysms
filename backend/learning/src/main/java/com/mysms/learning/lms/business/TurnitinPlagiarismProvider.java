package com.mysms.learning.lms.business;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turnitin Core API adapter: creates a submission, uploads the original file, asks for the similarity report and
 * signs viewer links, with the account's API key as the bearer credential. Callbacks are Turnitin's
 * {@code SUBMISSION_COMPLETE} and {@code SIMILARITY_COMPLETE} events.
 */
@Component
public class TurnitinPlagiarismProvider implements PlagiarismProvider {

    static final String NAME = "turnitin";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final List<String> REPOSITORIES = List.of("INTERNET", "SUBMITTED_WORK", "PUBLICATION");

    private final HttpClient http;
    private final JsonMapper json;
    private final String integrationName;
    private final String integrationVersion;

    public TurnitinPlagiarismProvider(JsonMapper json,
            @Value("${mysms.lms.plagiarism.integration-name}") String integrationName,
            @Value("${mysms.lms.plagiarism.integration-version}") String integrationVersion) {
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.json = json;
        this.integrationName = integrationName;
        this.integrationVersion = integrationVersion;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String submit(Account account, Upload upload) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("owner", upload.ownerRef());
        body.put("submitter", upload.submitterRef());
        body.put("title", upload.title());
        body.put("metadata", Map.of("custom", upload.metadata()));
        JsonNode created = send(request(account, "/api/v1/submissions")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                .header("Content-Type", "application/json"), 201);
        String id = created.path("id").asString("");
        if (id.isBlank()) {
            throw new Failure("Turnitin answered no submission id.");
        }
        send(request(account, "/api/v1/submissions/" + encode(id) + "/original")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(upload.content()))
                .header("Content-Type", "binary/octet-stream")
                .header("Content-Disposition", "inline; filename=\"" + upload.fileName().replace("\"", "") + "\""),
                202);
        return id;
    }

    @Override
    public void requestSimilarity(Account account, String submissionRef) {
        Map<String, Object> body = Map.of("generation_settings", Map.of("search_repositories", REPOSITORIES));
        send(request(account, "/api/v1/submissions/" + encode(submissionRef) + "/similarity")
                .PUT(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                .header("Content-Type", "application/json"), 202);
    }

    @Override
    public String viewerUrl(Account account, String submissionRef, String viewerRef, boolean teacher) {
        Map<String, Object> body = Map.of("viewer_user_id", viewerRef, "locale", "en-US",
                "viewer_default_permission_set", teacher ? "INSTRUCTOR" : "LEARNER");
        JsonNode answer = send(request(account, "/api/v1/submissions/" + encode(submissionRef)
                + "/viewer-url").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                .header("Content-Type", "application/json"), 200);
        String url = answer.path("viewer_url").asString("");
        if (!url.startsWith("https://")) {
            throw new Failure("Turnitin answered no viewer link.");
        }
        return url;
    }

    @Override
    public Optional<Event> event(Map<String, Object> body) {
        Object type = body.get("event_type") != null ? body.get("event_type") : body.get("type");
        JsonNode node = json.valueToTree(body);
        String id = node.path("id").asString(node.path("submission_id").asString(""));
        if (id.isBlank() || type == null) {
            return Optional.empty();
        }
        Map<String, String> metadata = new LinkedHashMap<>();
        node.path("metadata").path("custom").properties().forEach(e -> metadata.put(e.getKey(), e.getValue()
                .asString("")));
        String errorCode = node.path("error_code").isMissingNode() ? null : node.path("error_code").asString(null);
        return switch (type.toString()) {
            case "SUBMISSION_COMPLETE" -> Optional.of(new Event(id, errorCode == null, false, null, errorCode,
                    metadata));
            case "SIMILARITY_COMPLETE", "SIMILARITY_UPDATED" -> Optional.of(new Event(id, true,
                    "COMPLETE".equals(node.path("status").asString("COMPLETE")),
                    node.path("overall_match_percentage").isMissingNode() ? null
                            : node.path("overall_match_percentage").asString(), errorCode, metadata));
            default -> Optional.empty();
        };
    }

    private HttpRequest.Builder request(Account account, String path) {
        String base = account.apiUrl().endsWith("/") ? account.apiUrl().substring(0, account.apiUrl().length() - 1)
                : account.apiUrl();
        return HttpRequest.newBuilder(URI.create(base + path)).timeout(TIMEOUT)
                .header("Authorization", "Bearer " + account.secret())
                .header("X-Turnitin-Integration-Name", integrationName)
                .header("X-Turnitin-Integration-Version", integrationVersion);
    }

    private JsonNode send(HttpRequest.Builder request, int expected) {
        try {
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != expected && response.statusCode() / 100 != 2) {
                throw new Failure("Turnitin refused the request with status " + response.statusCode() + ".");
            }
            String text = response.body();
            return text == null || text.isBlank() ? json.createObjectNode() : json.readTree(text);
        } catch (IOException | JacksonException e) {
            throw new Failure("Turnitin could not be reached.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Failure("The call to Turnitin was interrupted.");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
