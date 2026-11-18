package com.mysms.lms.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.lms.business.EbookConversionProvider.Conversion;
import com.mysms.lms.business.EbookConversionProvider.Event;
import com.mysms.lms.business.EbookConversionProvider.Result;
import com.mysms.lms.business.EbookConversionProvider.Service;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The CloudConvert adapter's request and payload mapping, against a stub HTTP server. */
class CloudConvertConversionProviderTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final CloudConvertConversionProvider provider = new CloudConvertConversionProvider(JSON);
    private HttpServer server;
    private Service service;
    private final AtomicReference<String> jobAnswer = new AtomicReference<>();
    private final AtomicReference<JsonNode> jobRequest = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            if ("POST".equals(exchange.getRequestMethod())) {
                jobRequest.set(JSON.readTree(exchange.getRequestBody().readAllBytes()));
            }
            byte[] answer = jobAnswer.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(jobAnswer.get().isEmpty() ? 500 : 200, answer.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(answer);
            }
        });
        server.start();
        service = new Service("http://127.0.0.1:" + server.getAddress().getPort(), "api-key",
                Duration.ofSeconds(5));
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void submitBuildsTheThreeTaskJobAndAnswersItsId() {
        jobAnswer.set("{\"data\":{\"id\":\"job-1\",\"status\":\"waiting\"}}");
        Map<String, String> metadata = new java.util.LinkedHashMap<>();
        metadata.put("mysms_tenant", "t1");
        metadata.put("mysms_ebook", "e1");
        String ref = provider.submit(service, new Conversion("https://files/source?sig=1", "Algebra.docx", "pdf",
                metadata));
        assertThat(ref).isEqualTo("job-1");
        assertThat(authorization.get()).isEqualTo("Bearer api-key");
        JsonNode tasks = jobRequest.get().path("tasks");
        assertThat(tasks.path("import-source").path("operation").asString()).isEqualTo("import/url");
        assertThat(tasks.path("import-source").path("url").asString()).isEqualTo("https://files/source?sig=1");
        assertThat(tasks.path("import-source").path("filename").asString()).isEqualTo("Algebra.docx");
        assertThat(tasks.path("convert-source").path("input").asString()).isEqualTo("import-source");
        assertThat(tasks.path("convert-source").path("output_format").asString()).isEqualTo("pdf");
        assertThat(tasks.path("export-result").path("operation").asString()).isEqualTo("export/url");
        assertThat(jobRequest.get().path("tag").asString()).as("keys in order, whatever map the metadata came in")
                .isEqualTo("mysms_ebook=e1;mysms_tenant=t1");
    }

    @Test
    void submitWithoutAJobIdOrWithARefusalFails() {
        jobAnswer.set("{\"data\":{}}");
        Conversion conversion = new Conversion("https://s", "a.doc", "pdf", Map.of());
        assertThatThrownBy(() -> provider.submit(service, conversion))
                .isInstanceOf(EbookConversionProvider.Failure.class);
        jobAnswer.set("");
        assertThatThrownBy(() -> provider.submit(service, conversion))
                .isInstanceOf(EbookConversionProvider.Failure.class);
    }

    @Test
    void awaitReadsAFinishedJobsExportUrlAndAFailedJobsError() {
        jobAnswer.set("""
                {"data":{"id":"job-1","status":"finished","tasks":[
                  {"operation":"convert","status":"finished"},
                  {"operation":"export/url","status":"finished","result":{"files":[
                    {"filename":"a.pdf","url":"https://storage.cloudconvert.com/out/a.pdf"}]}}]}}
                """);
        Result finished = provider.await(service, "job-1").orElseThrow();
        assertThat(finished.succeeded()).isTrue();
        assertThat(finished.outputUrl()).isEqualTo("https://storage.cloudconvert.com/out/a.pdf");
        jobAnswer.set("""
                {"data":{"id":"job-1","status":"error","tasks":[
                  {"operation":"convert","status":"error","message":"The document is password protected."}]}}
                """);
        Result failed = provider.await(service, "job-1").orElseThrow();
        assertThat(failed.succeeded()).isFalse();
        assertThat(failed.error()).isEqualTo("The document is password protected.");
        jobAnswer.set("{\"data\":{\"id\":\"job-1\",\"status\":\"processing\"}}");
        assertThat(provider.await(service, "job-1")).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(String json) {
        return JSON.readValue(json, Map.class);
    }

    @Test
    void mapsJobFinishedAndJobFailedCallbacksWithTheirTag() {
        Event finished = provider.event(map("""
                {"event":"job.finished","job":{"id":"job-9","status":"finished",
                 "tag":"mysms_tenant=t1;mysms_ebook=e1","tasks":[
                  {"operation":"export/url","status":"finished","result":{"files":[
                    {"url":"https://storage.cloudconvert.com/out/b.pdf"}]}}]}}
                """)).orElseThrow();
        assertThat(finished.jobRef()).isEqualTo("job-9");
        assertThat(finished.result().succeeded()).isTrue();
        assertThat(finished.result().outputUrl()).isEqualTo("https://storage.cloudconvert.com/out/b.pdf");
        assertThat(finished.metadata()).containsEntry("mysms_tenant", "t1").containsEntry("mysms_ebook", "e1");
        Event failed = provider.event(map("""
                {"event":"job.failed","job":{"id":"job-9","status":"error","tag":"mysms_tenant=t1",
                 "tasks":[{"operation":"convert","status":"error","message":"Conversion crashed."}]}}
                """)).orElseThrow();
        assertThat(failed.result().succeeded()).isFalse();
        assertThat(failed.result().error()).isEqualTo("Conversion crashed.");
        assertThat(provider.event(Map.of("event", "task.finished"))).isEmpty();
        assertThat(provider.event(Map.of("event", "job.finished", "job", Map.of()))).isEmpty();
    }

    @Test
    void aFinishedJobWithoutAnExportUrlIsAFailure() {
        List<Map<String, Object>> tasks = List.of(Map.of("operation", "export/url", "status", "finished"));
        Event event = provider.event(Map.of("event", "job.finished",
                "job", Map.of("id", "j", "status", "finished", "tag", "", "tasks", tasks))).orElseThrow();
        assertThat(event.result().succeeded()).isFalse();
        assertThat(event.result().error()).contains("export URL");
    }
}
