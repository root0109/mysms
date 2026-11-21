package com.mysms.privacy.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Lowercase hex SHA-256 of UTF-8 text, the one digest privacy keeps: the wording of a notice translation, a client's
 * User-Agent and the canonical text of a consent evidence entry.
 */
public final class Sha256 {

    private Sha256() {
    }

    /** The 64 lowercase hex digits of the text's SHA-256. */
    public static String hex(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required of every Java runtime", e);
        }
    }
}
