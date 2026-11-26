package com.mysms.identity.iam.business;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Opaque refresh tokens: {@code <tenant id>.<256 random bits, base64url>}. The tenant prefix lets the anonymous
 * refresh operation bind the right tenant for row-level security before it looks the token up; it grants nothing by
 * itself. Only the SHA-256 of a token is stored (iam.auth_token.token_hash), never the token.
 */
public final class RefreshTokens {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int RANDOM_BYTES = 32;
    private static final int ENCODED_RANDOM_LENGTH = 43;
    private static final int UUID_LENGTH = 36;

    private RefreshTokens() {
    }

    public static String generate(UUID tenant) {
        byte[] random = new byte[RANDOM_BYTES];
        RANDOM.nextBytes(random);
        return tenant + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    /** The tenant a well-formed token belongs to; empty for anything malformed. */
    public static Optional<UUID> tenantOf(String token) {
        if (token == null || token.length() != UUID_LENGTH + 1 + ENCODED_RANDOM_LENGTH
                || token.charAt(UUID_LENGTH) != '.') {
            return Optional.empty();
        }
        String secret = token.substring(UUID_LENGTH + 1);
        if (!secret.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '-' || c == '_')) {
            return Optional.empty();
        }
        try {
            UUID tenant = UUID.fromString(token.substring(0, UUID_LENGTH));
            return tenant.toString().equals(token.substring(0, UUID_LENGTH)) ? Optional.of(tenant) : Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Lower-case hex SHA-256, the stored form. */
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
