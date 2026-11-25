package com.mysms.authclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.error.UnavailableException;
import com.sun.net.httpserver.HttpServer;
import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * The service-token provider against a stand-in token endpoint: the request it sends, caching and refresh, a wrong
 * secret, and the credentials it gives gRPC clients.
 */
class ServiceTokenProviderTest {

    private static final String SECRET = "s3cr3t-0123456789-abcdefghijklmnopqrstuvwxyz";
    private final List<Map<String, String>> requests = new CopyOnWriteArrayList<>();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-11T10:00:00Z"));
    private HttpServer server;
    private String tokenUri;
    private volatile int issued;
    private volatile long lifetime = 300;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/oauth2/token", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> form = new java.util.HashMap<>();
            for (String pair : body.split("&")) {
                String[] kv = pair.split("=", 2);
                form.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
            form.put("content-type", exchange.getRequestHeaders().getFirst("Content-Type"));
            requests.add(form);
            boolean valid = SECRET.equals(form.get("client_secret")) && "client_credentials".equals(form.get("grant_type"));
            String answer = valid
                    ? "{\"access_token\":\"token-" + (++issued) + "\",\"token_type\":\"Bearer\",\"expires_in\":" + lifetime
                            + "}"
                    : "{\"code\":\"INVALID_CLIENT\",\"error\":\"invalid_client\"}";
            byte[] bytes = answer.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(valid ? 200 : 401, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
        tokenUri = "http://127.0.0.1:" + server.getAddress().getPort() + "/oauth2/token";
    }

    @AfterEach
    void stop() {
        server.stop(0);
        CallContextHolder.clear();
    }

    private ServiceTokenProvider provider(String uri, String secret) {
        return new ServiceTokenProvider(uri, module -> "finance".equals(module) ? secret : null,
                JsonMapper.builder().build(), clock);
    }

    @Test
    void sendsTheClientCredentialsAndKeepsTheTokenUntilShortlyBeforeItExpires() {
        ServiceTokenProvider provider = provider(tokenUri, SECRET);
        assertThat(provider.token("finance")).isEqualTo("token-1");
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0)).containsEntry("grant_type", "client_credentials").containsEntry("client_id", "finance")
                .containsEntry("content-type", "application/x-www-form-urlencoded");
        clock.advance(Duration.ofSeconds(239));
        assertThat(provider.token("finance")).as("still cached").isEqualTo("token-1");
        clock.advance(Duration.ofSeconds(1));
        assertThat(provider.token("finance")).as("refreshed 60 s before expiry").isEqualTo("token-2");
        assertThat(requests).hasSize(2);
    }

    @Test
    void aShortLivedTokenIsRefreshedAtHalfItsLifetime() {
        lifetime = 60;
        ServiceTokenProvider provider = provider(tokenUri, SECRET);
        assertThat(provider.token("finance")).isEqualTo("token-1");
        clock.advance(Duration.ofSeconds(29));
        assertThat(provider.token("finance")).isEqualTo("token-1");
        clock.advance(Duration.ofSeconds(1));
        assertThat(provider.token("finance")).isEqualTo("token-2");
    }

    @Test
    void aRefusedOrUnconfiguredClientIsUnavailableAndNeverRevealsTheSecret() {
        String wrong = "w".repeat(40);
        assertThatThrownBy(() -> provider(tokenUri, wrong).token("finance")).isInstanceOf(UnavailableException.class)
                .hasMessageNotContaining(wrong);
        assertThatThrownBy(() -> provider(tokenUri, SECRET).token("platform")).isInstanceOf(UnavailableException.class)
                .hasMessageContaining("platform");
        assertThatThrownBy(() -> provider(tokenUri, "too-short").token("finance"))
                .isInstanceOf(UnavailableException.class);
        assertThatThrownBy(() -> provider("", SECRET).token("finance")).isInstanceOf(UnavailableException.class);
        assertThatThrownBy(() -> provider("http://127.0.0.1:1/oauth2/token", SECRET).token("finance"))
                .isInstanceOf(UnavailableException.class);
        assertThat(requests).hasSize(1).allSatisfy(r -> assertThat(r).containsEntry("client_secret", wrong));
    }

    @Test
    void grpcCallCredentialsCarryTheTokenOrFailUnavailable() {
        UUID tenant = UUID.randomUUID();
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736").withIdentity(tenant, null));
        AtomicReference<Metadata> applied = new AtomicReference<>();
        AtomicReference<Status> failed = new AtomicReference<>();
        CallCredentials.MetadataApplier applier = new CallCredentials.MetadataApplier() {
            @Override
            public void apply(Metadata headers) {
                applied.set(headers);
            }

            @Override
            public void fail(Status status) {
                failed.set(status);
            }
        };
        provider(tokenUri, SECRET).callCredentials("finance").applyRequestMetadata(null, Runnable::run, applier);
        assertThat(applied.get().get(GrpcAuthInterceptor.AUTHORIZATION)).isEqualTo("Bearer token-1");
        assertThat(applied.get().get(GrpcAuthInterceptor.TENANT)).isEqualTo(tenant.toString());
        provider("", SECRET).callCredentials("finance").applyRequestMetadata(null, Runnable::run, applier);
        assertThat(failed.get().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
    }

    @Test
    void theTenantIsTheCallersEvenWhenGrpcAppliesTheCredentialsOnAnotherThread() throws Exception {
        UUID tenant = UUID.randomUUID();
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736").withIdentity(tenant, null));
        CallCredentials credentials = provider(tokenUri, SECRET).callCredentials("finance");
        CallContextHolder.clear();
        AtomicReference<Metadata> applied = new AtomicReference<>();
        CallCredentials.MetadataApplier applier = new CallCredentials.MetadataApplier() {
            @Override
            public void apply(Metadata headers) {
                applied.set(headers);
            }

            @Override
            public void fail(Status status) {
                throw new AssertionError(status);
            }
        };
        // a thread without the caller's call context, as gRPC's channel executor is
        Thread channelThread = new Thread(() -> credentials.applyRequestMetadata(null, Runnable::run, applier));
        channelThread.start();
        channelThread.join();
        assertThat(applied.get()).as("the credentials were applied").isNotNull();
        assertThat(applied.get().get(GrpcAuthInterceptor.TENANT)).isEqualTo(tenant.toString());
    }

    /** A clock the test moves forward. */
    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
