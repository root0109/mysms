package com.mysms.identity.iam.business;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * One-time MFA recovery codes: ten codes of ten base32 characters (50 random bits each), shown to the user once as
 * {@code xxxxx-xxxxx}. Only their SHA-256 hashes are kept (inside the encrypted credential); using one removes it.
 */
public final class RecoveryCodeSet {

    public static final int COUNT = 10;
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz234567";
    private static final int LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private RecoveryCodeSet() {
    }

    /** New codes for display (formatted); store {@link #hashes} of them. */
    public static List<String> generate() {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < COUNT; i++) {
            StringBuilder code = new StringBuilder();
            for (int c = 0; c < LENGTH; c++) {
                code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
            codes.add(code.substring(0, LENGTH / 2) + "-" + code.substring(LENGTH / 2));
        }
        return codes;
    }

    /** The stored form: comma-separated SHA-256 hex of each normalized code. */
    public static String hashes(List<String> codes) {
        return String.join(",", codes.stream().map(RecoveryCodeSet::hash).toList());
    }

    /**
     * The stored form after using {@code code}, when it is one of the remaining codes; empty when it is not (or the
     * input is not shaped like a recovery code).
     */
    public static Optional<String> consume(String stored, String code) {
        if (stored == null || code == null) {
            return Optional.empty();
        }
        String normalized = normalize(code);
        if (normalized.length() != LENGTH || !normalized.chars().allMatch(c -> ALPHABET.indexOf(c) >= 0)) {
            return Optional.empty();
        }
        String wanted = hash(normalized);
        List<String> remaining = new ArrayList<>(Arrays.asList(stored.split(",")));
        boolean found = false;
        for (int i = 0; i < remaining.size(); i++) {
            if (!found && MessageDigest.isEqual(remaining.get(i).getBytes(StandardCharsets.US_ASCII),
                    wanted.getBytes(StandardCharsets.US_ASCII))) {
                remaining.remove(i);
                found = true;
            }
        }
        return found ? Optional.of(String.join(",", remaining)) : Optional.empty();
    }

    private static String normalize(String code) {
        return code.replace("-", "").replace(" ", "").toLowerCase(Locale.ROOT);
    }

    private static String hash(String code) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(normalize(code).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
