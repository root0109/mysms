package com.mysms.core.trace;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * W3C Trace Context (`traceparent: 00-<trace-id>-<parent-id>-<flags>`), carried by every request, outbound call and
 * event so one trace id follows a request across modules.
 */
public record TraceContext(String traceId, String spanId, boolean sampled) {

    private static final Pattern TRACEPARENT = Pattern.compile("^00-([0-9a-f]{32})-([0-9a-f]{16})-([0-9a-f]{2})$");
    private static final String INVALID_TRACE = "0".repeat(32);
    private static final String INVALID_SPAN = "0".repeat(16);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    /** Parses a traceparent header; empty for absent, malformed or all-zero values (which must be ignored). */
    public static Optional<TraceContext> parse(String header) {
        if (header == null) {
            return Optional.empty();
        }
        var matcher = TRACEPARENT.matcher(header.trim());
        if (!matcher.matches() || INVALID_TRACE.equals(matcher.group(1)) || INVALID_SPAN.equals(matcher.group(2))) {
            return Optional.empty();
        }
        boolean sampled = (Integer.parseInt(matcher.group(3), 16) & 1) == 1;
        return Optional.of(new TraceContext(matcher.group(1), matcher.group(2), sampled));
    }

    /** A new root trace. */
    public static TraceContext newRoot() {
        return new TraceContext(randomHex(16), randomHex(8), true);
    }

    /** A child span in the same trace (for the server span of an inbound call, or an outbound call). */
    public TraceContext newChild() {
        return new TraceContext(traceId, randomHex(8), sampled);
    }

    public String toHeader() {
        return "00-" + traceId + "-" + spanId + (sampled ? "-01" : "-00");
    }

    private static String randomHex(int bytes) {
        byte[] buffer = new byte[bytes];
        String hex;
        do {
            RANDOM.nextBytes(buffer);
            hex = HEX.formatHex(buffer);
        } while (hex.chars().allMatch(c -> c == '0'));
        return hex;
    }
}
