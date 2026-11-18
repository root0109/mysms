package com.mysms.lms.business;

import com.mysms.core.error.UnauthenticatedException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Checks a provider callback signed the MySMS way: {@code X-Signature} is the lower-case hex HMAC-SHA256 of
 * {@code <X-Timestamp>.<raw body>} with the account's shared secret, and {@code X-Timestamp} (Unix seconds) lies within
 * five minutes of now, so a captured callback cannot be replayed later. Any mismatch is 401 {@code INVALID_SIGNATURE}.
 */
final class WebhookSignature {

    static final Duration TOLERANCE = Duration.ofMinutes(5);
    static final String INVALID = "INVALID_SIGNATURE";

    private WebhookSignature() {
    }

    static void verify(String secret, String timestamp, String signature, byte[] body, Instant now) {
        long seconds;
        try {
            seconds = Long.parseLong(timestamp == null ? "" : timestamp.trim());
        } catch (NumberFormatException e) {
            throw refused();
        }
        if (Math.abs(now.getEpochSecond() - seconds) > TOLERANCE.getSeconds() || signature == null) {
            throw refused();
        }
        byte[] expected = HexFormat.of().formatHex(sign(secret, seconds + ".", body)).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8))) {
            throw refused();
        }
    }

    static byte[] sign(String secret, String prefix, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update(prefix.getBytes(StandardCharsets.UTF_8));
            return mac.doFinal(body);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is part of every Java runtime", e);
        }
    }

    static UnauthenticatedException refused() {
        return new UnauthenticatedException(INVALID, "The callback's signature does not verify.");
    }
}
