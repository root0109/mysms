package com.mysms.core.web;

import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.PreconditionRequiredException;
import java.util.regex.Pattern;

/**
 * ETag ⇄ row_version: a row's ETag is its `row_version` as a strong tag ("7"), which updates and deletes send back
 * in If-Match.
 */
public final class Etags {

    private static final Pattern STRONG = Pattern.compile("^\"(\\d{1,9})\"$");

    private Etags() {
    }

    public static String of(int rowVersion) {
        return "\"" + rowVersion + "\"";
    }

    /**
     * The row version an If-Match header expects. Absent or `*` → 428 (an explicit ETag is required); a weak, malformed
     * or multi-valued tag can never match strongly → 412.
     */
    /**
     * The version of an ETag a workflow request carries in its body ({@code "etag"}, optional in the contract): the
     * value as the API returned it ({@code "\"3\""}) or bare ({@code 3}). Empty when the body has none.
     */
    public static java.util.OptionalInt bodyVersion(String etag) {
        if (etag == null || etag.isBlank()) {
            return java.util.OptionalInt.empty();
        }
        String trimmed = etag.trim();
        return java.util.OptionalInt.of(expectedVersion(trimmed.startsWith("\"") || trimmed.startsWith("W/") ? trimmed
                : "\"" + trimmed + "\""));
    }

    public static int expectedVersion(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank() || "*".equals(ifMatch.trim())) {
            throw new PreconditionRequiredException("If-Match with the ETag of the last read is required.");
        }
        var matcher = STRONG.matcher(ifMatch.trim());
        if (!matcher.matches()) {
            throw new PreconditionFailedException("If-Match does not match the current ETag.");
        }
        return Integer.parseInt(matcher.group(1));
    }
}
