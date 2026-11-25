package com.mysms.authclient;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.error.UnavailableException;
import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * A module's service token for calls no person makes, from iam's client-credentials endpoint, cached until shortly
 * before expiry. The edge admits it only on {@code internal} operations. The secret goes only to the token endpoint
 * and is never logged; an unconfigured or failing endpoint is 503. gRPC calls carry the caller's own token unless made
 * with {@link #callCredentials}.
 */
public final class ServiceTokenProvider {

    /** A token is replaced this long before it expires (half its lifetime for very short-lived tokens). */
    static final Duration REFRESH_MARGIN = Duration.ofSeconds(60);
    /** Client secrets are random strings at least this long. */
    static final int MIN_SECRET_LENGTH = 32;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Logger LOG = LoggerFactory.getLogger(ServiceTokenProvider.class);
    private static final Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private record Cached(String token, Instant refreshAt) {
    }

    private final String tokenUri;
    private final Function<String, String> secrets;
    private final HttpClient http;
    private final JsonMapper json;
    private final Clock clock;
    private final ConcurrentMap<String, Cached> tokens = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> locks = new ConcurrentHashMap<>();

    /**
     * @param tokenUri iam's token endpoint ({@code mysms.auth.token-uri}); blank = service calls are not configured
     * @param secrets  a module's client secret by module name ({@code mysms.<module>.service-client.secret})
     */
    public ServiceTokenProvider(String tokenUri, Function<String, String> secrets, JsonMapper json, Clock clock) {
        this.tokenUri = tokenUri == null ? "" : tokenUri.trim();
        this.secrets = secrets;
        this.http = HttpClient.newBuilder().connectTimeout(TIMEOUT).followRedirects(HttpClient.Redirect.NEVER).build();
        this.json = json;
        this.clock = clock;
    }

    /** A valid service token of the module (cached, refreshed shortly before it expires). */
    public String token(String module) {
        Cached cached = tokens.get(module);
        if (cached != null && clock.instant().isBefore(cached.refreshAt())) {
            return cached.token();
        }
        synchronized (locks.computeIfAbsent(module, m -> new Object())) {
            cached = tokens.get(module);
            if (cached == null || !clock.instant().isBefore(cached.refreshAt())) {
                cached = fetch(module);
                tokens.put(module, cached);
            }
            return cached.token();
        }
    }

    /**
     * gRPC call credentials that call as the module, in the tenant of the calling thread's call context (if any). The
     * tenant is read here, on the thread that makes the call: gRPC may apply the credentials later on another thread
     * (on a channel's first connection, for one), where the call context is not set.
     */
    public CallCredentials callCredentials(String module) {
        Optional<UUID> tenant = tenant();
        return new CallCredentials() {
            @Override
            public void applyRequestMetadata(RequestInfo request, Executor executor, MetadataApplier applier) {
                executor.execute(() -> {
                    try {
                        Metadata metadata = new Metadata();
                        metadata.put(AUTHORIZATION, EdgeAuthorizer.BEARER_PREFIX + token(module));
                        tenant.ifPresent(t -> metadata.put(GrpcAuthInterceptor.TENANT, t.toString()));
                        applier.apply(metadata);
                    } catch (UnavailableException e) {
                        applier.fail(Status.UNAVAILABLE.withDescription(e.getMessage()));
                    }
                });
            }
        };
    }

    private static Optional<UUID> tenant() {
        return CallContextHolder.current().flatMap(CallContext::tenantId);
    }

    private Cached fetch(String module) {
        if (tokenUri.isEmpty()) {
            throw new UnavailableException("Service calls are not configured: no token endpoint.");
        }
        String secret = secrets.apply(module);
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new UnavailableException("The service credentials of " + module + " are not configured.");
        }
        String form = "grant_type=client_credentials&client_id=" + encode(module) + "&client_secret=" + encode(secret);
        HttpResponse<String> response;
        try {
            response = http.send(HttpRequest.newBuilder(URI.create(tokenUri)).timeout(TIMEOUT)
                    .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            LOG.warn("service token of {} not issued: the token endpoint did not answer", module);
            throw new UnavailableException("The identity service did not issue a service token.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UnavailableException("The identity service did not issue a service token.");
        }
        if (response.statusCode() != 200) {
            LOG.warn("service token of {} refused with status {}", module, response.statusCode());
            throw new UnavailableException("The identity service did not issue a service token.");
        }
        try {
            JsonNode body = json.readTree(response.body());
            String token = body.path("access_token").asString("");
            long lifetime = body.path("expires_in").asLong(0);
            if (token.isEmpty() || lifetime <= 0) {
                throw new UnavailableException("The identity service did not issue a service token.");
            }
            Duration ttl = Duration.ofSeconds(lifetime);
            Duration margin = ttl.compareTo(REFRESH_MARGIN.multipliedBy(2)) < 0 ? ttl.dividedBy(2) : REFRESH_MARGIN;
            return new Cached(token, clock.instant().plus(ttl).minus(margin));
        } catch (JacksonException e) {
            throw new UnavailableException("The identity service did not issue a service token.");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
