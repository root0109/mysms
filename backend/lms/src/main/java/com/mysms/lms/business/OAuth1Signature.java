package com.mysms.lms.business;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * OAuth 1.0a HMAC-SHA1 signatures as LTI 1.1 uses them (RFC 5849): signing a launch's form fields, and reading and
 * checking the {@code Authorization: OAuth ...} header and body hash of an outcome request.
 */
final class OAuth1Signature {

    static final String METHOD = "HMAC-SHA1";

    private OAuth1Signature() {
    }

    /** The signature of a request: the method, its URL without query, and every parameter but the signature. */
    static String sign(String method, String url, Map<String, String> params, String consumerSecret) {
        TreeMap<String, List<String>> sorted = new TreeMap<>();
        params.forEach((k, v) -> {
            if (!"oauth_signature".equals(k)) {
                sorted.computeIfAbsent(encode(k), x -> new ArrayList<>()).add(encode(v));
            }
        });
        List<String> pairs = new ArrayList<>();
        sorted.forEach((k, values) -> values.stream().sorted().forEach(v -> pairs.add(k + "=" + v)));
        String base = method.toUpperCase(Locale.ROOT) + "&" + encode(normalUrl(url)) + "&"
                + encode(String.join("&", pairs));
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec((encode(consumerSecret) + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(base.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA1 is part of every Java runtime", e);
        }
    }

    /** Whether the signature sent with the parameters is the one the secret gives (compared in constant time). */
    static boolean verify(String method, String url, Map<String, String> params, String consumerSecret) {
        String sent = params.get("oauth_signature");
        return sent != null && MessageDigest.isEqual(sent.getBytes(StandardCharsets.UTF_8),
                sign(method, url, params, consumerSecret).getBytes(StandardCharsets.UTF_8));
    }

    /** The OAuth body hash of a request body: base64 of its SHA-1. */
    static String bodyHash(byte[] body) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest(body));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-1 is part of every Java runtime", e);
        }
    }

    /** The parameters of an {@code Authorization: OAuth k="v", ...} header; empty when it is not one. */
    static Map<String, String> header(String authorization) {
        Map<String, String> params = new LinkedHashMap<>();
        if (authorization == null || !authorization.regionMatches(true, 0, "OAuth ", 0, 6)) {
            return params;
        }
        for (String part : authorization.substring(6).split(",")) {
            int eq = part.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = part.substring(0, eq).trim();
            String value = part.substring(eq + 1).trim();
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            if (!"realm".equalsIgnoreCase(key)) {
                params.put(decode(key), decode(value));
            }
        }
        return params;
    }

    static String encode(String value) {
        StringBuilder out = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xff;
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '.'
                    || c == '_' || c == '~') {
                out.append((char) c);
            } else {
                out.append('%').append(String.format("%02X", c));
            }
        }
        return out.toString();
    }

    private static String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private static String normalUrl(String url) {
        URI uri = URI.create(url);
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        int port = uri.getPort();
        boolean defaultPort = port == -1 || ("http".equals(scheme) && port == 80) || ("https".equals(scheme)
                && port == 443);
        String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
        return scheme + "://" + host + (defaultPort ? "" : ":" + port) + path;
    }
}
