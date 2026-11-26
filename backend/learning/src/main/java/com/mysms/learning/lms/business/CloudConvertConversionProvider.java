package com.mysms.learning.lms.business;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * CloudConvert v2 adapter: one job of three tasks (import the source by URL, convert it, export the result by URL),
 * the MySMS references in the job's tag, and the job waited on synchronously where it finishes quickly. Callbacks
 * are CloudConvert's {@code job.finished} and {@code job.failed} events.
 */
@Component
public class CloudConvertConversionProvider implements EbookConversionProvider {

    static final String NAME = "cloudconvert";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient http;
    private final JsonMapper json;

    public CloudConvertConversionProvider(JsonMapper json) {
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.json = json;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String submit(Service service, Conversion conversion) {
        Map<String, Object> tasks = new LinkedHashMap<>();
        tasks.put("import-source", Map.of("operation", "import/url", "url", conversion.sourceUrl(),
                "filename", conversion.sourceName()));
        tasks.put("convert-source", Map.of("operation", "convert", "input", "import-source",
                "output_format", conversion.targetFormat()));
        tasks.put("export-result", Map.of("operation", "export/url", "input", "convert-source"));
        Map<String, Object> body = Map.of("tasks", tasks, "tag", tag(conversion.metadata()));
        JsonNode created = send(request(service, "/v2/jobs", TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                .header("Content-Type", "application/json"), false).orElseThrow();
        String id = created.path("data").path("id").asString("");
        if (id.isBlank()) {
            throw new Failure("CloudConvert answered no job id.");
        }
        return id;
    }

    @Override
    public Optional<Result> await(Service service, String jobRef) {
        return send(request(service, "/v2/jobs/" + jobRef + "/wait", service.waitFor()).GET(), true)
                .flatMap(answer -> result(answer.path("data")));
    }

    @Override
    public Optional<Event> event(Map<String, Object> body) {
        Object type = body.get("event");
        if (!"job.finished".equals(type) && !"job.failed".equals(type)) {
            return Optional.empty();
        }
        JsonNode job = json.valueToTree(body).path("job");
        String id = job.path("id").asString("");
        if (id.isBlank()) {
            return Optional.empty();
        }
        return result(job).map(result -> new Event(id, result, metadata(job.path("tag").asString(""))));
    }

    /** How the job ended, from its JSON; empty while it is still running. */
    private Optional<Result> result(JsonNode job) {
        String status = job.path("status").asString("");
        if ("finished".equals(status)) {
            for (JsonNode task : job.path("tasks")) {
                JsonNode files = task.path("result").path("files");
                if ("export/url".equals(task.path("operation").asString("")) && files.size() > 0) {
                    String url = files.get(0).path("url").asString("");
                    if (url.startsWith("https://")) {
                        return Optional.of(new Result(true, url, null));
                    }
                }
            }
            return Optional.of(new Result(false, null, "The finished job carries no export URL."));
        }
        if ("error".equals(status)) {
            String error = "The conversion failed.";
            for (JsonNode task : job.path("tasks")) {
                if ("error".equals(task.path("status").asString("")) && !task.path("message").asString("").isBlank()) {
                    error = task.path("message").asString("");
                }
            }
            return Optional.of(new Result(false, null, error));
        }
        return Optional.empty();
    }

    static String tag(Map<String, String> metadata) {
        StringBuilder tag = new StringBuilder();
        new TreeMap<>(metadata).forEach((key, value) -> tag.append(tag.isEmpty() ? "" : ";").append(key).append('=')
                .append(value));
        return tag.toString();
    }

    static Map<String, String> metadata(String tag) {
        Map<String, String> metadata = new LinkedHashMap<>();
        for (String pair : tag.split(";")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                metadata.put(pair.substring(0, eq), pair.substring(eq + 1));
            }
        }
        return metadata;
    }

    private HttpRequest.Builder request(Service service, String path, Duration timeout) {
        String base = service.apiUrl().endsWith("/") ? service.apiUrl().substring(0, service.apiUrl().length() - 1)
                : service.apiUrl();
        return HttpRequest.newBuilder(URI.create(base + path)).timeout(timeout)
                .header("Authorization", "Bearer " + service.apiKey());
    }

    /** Sends the request; empty only when {@code emptyOnTimeout} and the wait ran out. */
    private Optional<JsonNode> send(HttpRequest.Builder request, boolean emptyOnTimeout) {
        try {
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new Failure("CloudConvert refused the request with status " + response.statusCode() + ".");
            }
            String text = response.body();
            return Optional.of(text == null || text.isBlank() ? json.createObjectNode() : json.readTree(text));
        } catch (HttpTimeoutException e) {
            if (emptyOnTimeout) {
                return Optional.empty();
            }
            throw new Failure("CloudConvert could not be reached.");
        } catch (IOException | JacksonException e) {
            throw new Failure("CloudConvert could not be reached.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Failure("The call to CloudConvert was interrupted.");
        }
    }
}
