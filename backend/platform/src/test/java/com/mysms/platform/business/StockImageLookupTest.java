package com.mysms.platform.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.error.ApiException;
import com.mysms.core.json.JsonMappers;
import com.mysms.core.paging.Page;
import com.mysms.platform.api.model.StockImage;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The Pexels adapter's request and response mapping, against a JDK HTTP server standing in for the provider. */
class StockImageLookupTest {

    private HttpServer server;
    private final AtomicReference<String> query = new AtomicReference<>();
    private final AtomicReference<String> key = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String answer = "{}";

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/search", exchange -> {
            query.set(exchange.getRequestURI().getRawQuery());
            key.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = answer.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private StockImageLookup search(String apiKey) {
        return new StockImageLookup(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1"), apiKey,
                Duration.ofSeconds(5), JsonMappers.create());
    }

    @Test
    void photosBecomeStockImagesWithTheirPhotographer() {
        answer = """
                {"page":2,"per_page":80,"photos":[{"id":1,"photographer":"A. Sen",
                 "src":{"large":"https://images.example/1-large.jpg","medium":"https://images.example/1-medium.jpg"}}],
                 "next_page":"https://api.example/v1/search?page=3"}""";
        Page<StockImage> page = search("key-1").search("  science & lab ", 2, 200);
        assertThat(query.get()).isEqualTo("query=science+%26+lab&page=2&per_page=80");
        assertThat(key.get()).isEqualTo("key-1");
        assertThat(page.items()).singleElement().satisfies(image -> {
            assertThat(image.getUrl()).isEqualTo("https://images.example/1-large.jpg");
            assertThat(image.getThumbUrl()).isEqualTo("https://images.example/1-medium.jpg");
            assertThat(image.getAuthor()).isEqualTo("A. Sen");
        });
        assertThat(page.hasNext()).isTrue();
        assertThat(page.size()).isEqualTo(80);
        answer = "{\"photos\":[]}";
        assertThat(search("key-1").search("x", null, null).hasNext()).isFalse();
    }

    @Test
    void failuresAreTyped() {
        assertThatThrownBy(() -> search(null).search("x", 1, 10)).isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("NOT_CONFIGURED"));
        assertThatThrownBy(() -> search("k").search(" ", 1, 10)).isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("INVALID_ARGUMENT"));
        status = 429;
        assertThatThrownBy(() -> search("k").search("x", 1, 10)).isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("UNAVAILABLE"));
        status = 200;
        answer = "not json";
        assertThatThrownBy(() -> search("k").search("x", 1, 10)).isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("UNAVAILABLE"));
    }
}
