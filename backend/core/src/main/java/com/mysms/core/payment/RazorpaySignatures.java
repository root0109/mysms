package com.mysms.core.payment;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Razorpay's two proofs, both HMAC-SHA256 in lower-case hex and compared in constant time: a webhook's
 * {@code X-Razorpay-Signature} over the raw request body with the webhook secret, and a checkout's
 * {@code razorpay_signature} over {@code order_id|payment_id} with the key secret. A redirect or a client message is
 * never proof of payment without one of them.
 */
public final class RazorpaySignatures {

    private static final String HMAC = "HmacSHA256";

    private RazorpaySignatures() {
    }

    /** Whether {@code signature} is the webhook secret's signature of exactly these body bytes. */
    public static boolean webhookValid(byte[] body, String signature, String webhookSecret) {
        return matches(hmac(webhookSecret, body), signature);
    }

    /** Whether {@code signature} proves the gateway paid {@code orderId} with {@code paymentId}. */
    public static boolean checkoutValid(String orderId, String paymentId, String signature, String keySecret) {
        return matches(hmac(keySecret, (orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8)), signature);
    }

    static String hmac(String secret, byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC));
            return HexFormat.of().formatHex(mac.doFinal(data));
        } catch (GeneralSecurityException missingAlgorithm) {
            throw new IllegalStateException("HmacSHA256 is not available in this JVM.", missingAlgorithm);
        }
    }

    private static boolean matches(String expected, String given) {
        return given != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                given.trim().toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }
}
