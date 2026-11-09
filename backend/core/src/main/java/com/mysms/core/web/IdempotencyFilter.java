package com.mysms.core.web;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.IdempotencyStore;
import com.mysms.core.error.ProblemResponseWriter;
import com.mysms.core.error.Problems;
import com.mysms.core.trace.TraceFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * The Idempotency-Key machinery (api/README "Idempotency"): for a POST of a contract operation that declares the
 * header, sent with a key by a tenant caller, the first request runs and its response is kept for 24 hours in the
 * owning module's own {@code idempotency_key} table; a retry with the same key and the same request gets that
 * response again (marked {@code Idempotent-Replayed: true}), a key reused for a different request is 422
 * {@code IDEMPOTENCY_KEY_REUSED}, and a retry while the first is still running is 409 {@code REQUEST_IN_PROGRESS}.
 * A server failure (5xx) is not kept, so the retry runs again. Stored responses are encrypted with
 * {@link FieldCipher} for the tenant: they can carry personal data, and a few carry tokens or one-time secrets.
 * Runs after the edge filter, which marks the matched operation and binds the caller.
 */
public final class IdempotencyFilter extends OncePerRequestFilter implements Ordered {

    /** Right after auth-client's edge filter (TraceFilter.ORDER + 10). */
    public static final int ORDER = TraceFilter.ORDER + 20;
    /** The attribute value of operations that must carry the header (money and bulk operations). */
    public static final String REQUIRED = "required";
    /** Request attribute the edge sets to "optional" or "required" when the operation declares the header. */
    public static final String ATTRIBUTE = "mysms.idempotency";
    public static final String HEADER = "Idempotency-Key";
    /** Kept responses are at most this large; a larger body is not stored (see {@link #seal}). */
    static final int MAX_STORED_BODY = 1024 * 1024;
    /** Answers that say "try again" are not kept: a retry with the same key runs again. */
    private static final java.util.Set<Integer> TRANSIENT = java.util.Set.of(408, 425, 429);
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(IdempotencyFilter.class);
    static final Duration RETENTION = Duration.ofHours(24);
    static final Duration STALE_LOCK = Duration.ofMinutes(2);
    static final int MAX_BODY = 10 * 1024 * 1024;

    private final Map<String, IdempotencyStore> stores;
    private final FieldCipher cipher;
    private final ProblemResponseWriter problems;
    private final JsonMapper json;
    private final Clock clock;

    public IdempotencyFilter(List<IdempotencyStore> stores, FieldCipher cipher, ProblemResponseWriter problems,
                             JsonMapper json, Clock clock) {
        Map<String, IdempotencyStore> byModule = new LinkedHashMap<>();
        stores.forEach(s -> byModule.put(s.module(), s));
        this.stores = Map.copyOf(byModule);
        this.cipher = cipher;
        this.problems = problems;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if ("POST".equals(request.getMethod()) && REQUIRED.equals(request.getAttribute(ATTRIBUTE))
                && (request.getHeader(HEADER) == null || request.getHeader(HEADER).isBlank())) {
            problems.write(response, HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT",
                    "This operation needs an Idempotency-Key header.");
            return;
        }
        Optional<Target> target = target(request);
        if (target.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        byte[] body = request.getInputStream().readNBytes(MAX_BODY + 1);
        if (body.length > MAX_BODY) {
            problems.write(response, HttpStatus.PAYLOAD_TOO_LARGE, "INVALID_ARGUMENT", "The request body is too large.");
            return;
        }
        Target t = target.get();
        OffsetDateTime now = now();
        String hash = hash(request, t.user(), body);
        IdempotencyStore.Claim claim = t.store().claim(t.key(), t.user(), t.operation(), hash, now,
                now.plus(RETENTION), now.minus(STALE_LOCK));
        switch (claim.outcome()) {
            case MISMATCH -> problems.write(response, HttpStatus.UNPROCESSABLE_CONTENT, "IDEMPOTENCY_KEY_REUSED",
                    "This Idempotency-Key was used for a different request.");
            case IN_PROGRESS -> problems.write(response, HttpStatus.CONFLICT, "REQUEST_IN_PROGRESS",
                    "A request with this Idempotency-Key is still running; retry shortly.");
            case REPLAY -> replay(response, t, claim);
            case RUN -> run(request, response, chain, t, claim, body);
            default -> throw new IllegalStateException("unknown outcome");
        }
    }

    private void run(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Target t,
                     IdempotencyStore.Claim claim, byte[] body) throws ServletException, IOException {
        ContentCachingResponseWrapper captured = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(new CachedBodyRequest(request, body), captured);
        } catch (ServletException | IOException | RuntimeException e) {
            release(t, claim); // the request failed on the server: a retry may run it again
            throw e;
        }
        int status = captured.getStatus();
        try {
            if (status >= 500 || TRANSIENT.contains(status)) {
                release(t, claim);
            } else {
                complete(t, claim, status, captured);
            }
        } finally {
            captured.copyBodyToResponse(); // the client gets its answer whatever happens to the stored copy
        }
    }

    /**
     * Keeps the response. The request has run (its own transaction may have committed), so a failure here never frees
     * the key: it is tried once more, then the lock is left in place and the key answers 409 until it goes stale.
     */
    private void complete(Target t, IdempotencyStore.Claim claim, int status, ContentCachingResponseWrapper captured) {
        String sealed = seal(t, captured);
        for (int attempt = 1; ; attempt++) {
            try {
                t.store().complete(claim.id(), status, sealed, now());
                return;
            } catch (RuntimeException e) {
                if (attempt == 2) {
                    LOG.error("Idempotency-Key response of {} not kept; the key stays locked", t.operation(), e);
                    return;
                }
            }
        }
    }

    private void release(Target t, IdempotencyStore.Claim claim) {
        try {
            t.store().release(claim.id(), now());
        } catch (RuntimeException e) {
            LOG.error("Idempotency-Key of {} not released; it frees itself when the lock goes stale", t.operation(), e);
        }
    }

    private void replay(HttpServletResponse response, Target t, IdempotencyStore.Claim claim) throws IOException {
        Map<String, Object> stored = json.readValue(cipher.decrypt(claim.storedBody(), t.tenant(), column(t))
                .orElse("{}"), Map.class);
        response.setStatus(claim.status());
        Object headers = stored.get("headers");
        if (headers instanceof Map<?, ?> map) {
            map.forEach((name, value) -> response.setHeader(String.valueOf(name), String.valueOf(value)));
        }
        response.setHeader("Idempotent-Replayed", "true");
        Object text = stored.get("body");
        if (text != null) {
            byte[] bytes = String.valueOf(text).getBytes(StandardCharsets.UTF_8);
            response.setContentLength(bytes.length);
            response.getOutputStream().write(bytes);
        }
    }

    /**
     * The response as kept: status, the headers a client relies on, and the body, encrypted for the tenant. A body
     * larger than {@link #MAX_STORED_BODY} is not kept (a replay then answers the status and headers only, and the
     * client reads the resource by its Location).
     */
    private String seal(Target t, ContentCachingResponseWrapper captured) {
        Map<String, Object> headers = new LinkedHashMap<>();
        for (String name : List.of("Content-Type", "Location", "ETag", "Cache-Control")) {
            String value = captured.getHeader(name);
            if (value != null) {
                headers.put(name, value);
            }
        }
        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put("headers", headers);
        byte[] bytes = captured.getContentAsByteArray();
        if (bytes.length > 0 && bytes.length <= MAX_STORED_BODY) {
            stored.put("body", new String(bytes, StandardCharsets.UTF_8));
        }
        return cipher.encrypt(json.writeValueAsString(stored), t.tenant(), column(t));
    }

    private Optional<Target> target(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod()) || request.getAttribute(ATTRIBUTE) == null) {
            return Optional.empty();
        }
        Object operation = request.getAttribute(Problems.OPERATION_ATTRIBUTE);
        String header = request.getHeader(HEADER);
        Optional<CallContext> context = CallContextHolder.current();
        if (operation == null || header == null || context.isEmpty() || context.get().tenantId().isEmpty()) {
            return Optional.empty(); // no key, or no tenant (public and platform-staff calls): runs as usual
        }
        UUID key;
        try {
            key = UUID.fromString(header.trim());
        } catch (IllegalArgumentException e) {
            return Optional.empty(); // the contract's parameter binding answers 400 for a malformed key
        }
        int dot = operation.toString().indexOf('.');
        if (dot <= 0) {
            return Optional.empty();
        }
        String module = operation.toString().substring(0, dot);
        IdempotencyStore store = stores.get(module);
        if (store == null) {
            return Optional.empty();
        }
        return Optional.of(new Target(store, operation.toString(), key, context.get().tenantId().get(),
                context.get().userId().orElse(null)));
    }

    private static String hash(HttpServletRequest request, UUID user, byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String query = request.getQueryString();
            digest.update((request.getMethod() + "\n" + request.getRequestURI() + "\n" + (query == null ? "" : query)
                    + "\n" + user + "\n").getBytes(StandardCharsets.UTF_8));
            digest.update(body);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String column(Target t) {
        return t.store().module() + ".idempotency_key.response_body";
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    private record Target(IdempotencyStore store, String operation, UUID key, UUID tenant, UUID user) {
    }

    /** The request with its body already read (for the hash), readable again by the controller. */
    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body.clone();
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream in = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return in.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException("synchronous only");
                }

                @Override
                public int read() {
                    return in.read();
                }

                @Override
                public int read(byte[] b, int off, int len) {
                    return in.read(b, off, len);
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }
}
