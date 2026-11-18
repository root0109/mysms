package com.mysms.core.web;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.IdempotencyLedger;
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
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Idempotency-Key for a tenant caller's POST of an operation that declares it: a retry is replayed, a reused key is
 * 422 {@code IDEMPOTENCY_KEY_REUSED}, a retry while the first runs is 409 {@code REQUEST_IN_PROGRESS}, and a 5xx is
 * not kept. Responses are stored encrypted ({@link FieldCipher}): they can carry personal data and one-time secrets.
 */
public final class IdempotencyFilter extends OncePerRequestFilter implements Ordered {

    /** Right after auth-client's edge filter (TraceFilter.ORDER + 10). */
    public static final int ORDER = TraceFilter.ORDER + 20;
    /** The attribute value of operations that must carry the header (money and bulk operations). */
    public static final String REQUIRED = "required";
    /** Request attribute the edge sets to "optional" or "required" when the operation declares the header. */
    public static final String ATTRIBUTE = "mysms.idempotency";
    public static final String HEADER = "Idempotency-Key";
    /** Kept responses are at most this large; a larger body is not stored (see {@link #kept}). */
    static final int MAX_STORED_BODY = 1024 * 1024;
    /** Answers that say "try again" are not kept: a retry with the same key runs again. */
    private static final Set<Integer> TRANSIENT = Set.of(408, 425, 429);
    static final int MAX_BODY = 10 * 1024 * 1024;

    private final IdempotencyLedger ledger;
    private final ProblemResponseWriter problems;

    public IdempotencyFilter(List<IdempotencyStore> stores, FieldCipher cipher, ProblemResponseWriter problems,
                             JsonMapper json, Clock clock) {
        this(new IdempotencyLedger(stores, cipher, json, clock), problems);
    }

    public IdempotencyFilter(IdempotencyLedger ledger, ProblemResponseWriter problems) {
        this.ledger = ledger;
        this.problems = problems;
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
        IdempotencyLedger.Ticket ticket = ledger.claim(t.store(), t.operation(), t.key(), t.tenant(), t.user(),
                hash(request, t.user(), body));
        switch (ticket.outcome()) {
            case MISMATCH -> problems.write(response, HttpStatus.UNPROCESSABLE_CONTENT, "IDEMPOTENCY_KEY_REUSED",
                    "This Idempotency-Key was used for a different request.");
            case IN_PROGRESS -> problems.write(response, HttpStatus.CONFLICT, "REQUEST_IN_PROGRESS",
                    "A request with this Idempotency-Key is still running; retry shortly.");
            case REPLAY -> replay(response, ticket);
            case RUN -> run(request, response, chain, ticket, body);
            default -> throw new IllegalStateException("unknown outcome");
        }
    }

    private void run(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                     IdempotencyLedger.Ticket ticket, byte[] body) throws ServletException, IOException {
        ContentCachingResponseWrapper captured = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(new CachedBodyRequest(request, body), captured);
        } catch (ServletException | IOException | RuntimeException e) {
            ledger.release(ticket); // the request failed on the server: a retry may run it again
            throw e;
        }
        int status = captured.getStatus();
        try {
            if (status >= 500 || TRANSIENT.contains(status)) {
                ledger.release(ticket);
            } else {
                ledger.complete(ticket, status, kept(captured));
            }
        } finally {
            captured.copyBodyToResponse(); // the client gets its answer whatever happens to the stored copy
        }
    }

    private void replay(HttpServletResponse response, IdempotencyLedger.Ticket ticket) throws IOException {
        Map<String, Object> stored = ledger.kept(ticket);
        response.setStatus(ticket.claim().status());
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
     * The response as kept: status, the headers a client relies on, and the body. A body larger than
     * {@link #MAX_STORED_BODY} is not kept (a replay then answers the status and headers only, and the client reads
     * the resource by its Location).
     */
    private static Map<String, Object> kept(ContentCachingResponseWrapper captured) {
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
        return stored;
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
        return ledger.storeFor(operation.toString()).map(store -> new Target(store, operation.toString(), key,
                context.get().tenantId().get(), context.get().userId().orElse(null)));
    }

    private static String hash(HttpServletRequest request, UUID user, byte[] body) {
        String query = request.getQueryString();
        return IdempotencyLedger.hash(request.getMethod() + "\n" + request.getRequestURI() + "\n"
                + (query == null ? "" : query) + "\n" + user + "\n", body);
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
