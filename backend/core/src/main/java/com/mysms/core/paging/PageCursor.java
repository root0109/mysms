package com.mysms.core.paging;

import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.json.JsonMappers;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * The opaque {@code cursor} of a list's {@code next} link: the page it opens and the last row's sort keys, bound to the
 * sort and view it was issued for and valid for 24 hours; anything else is 400.
 *
 * <p>Not signed, and need not be: tenant, scope and parent are predicates of every query, never cursor values, so an
 * altered cursor only moves within rows the caller may read anyway.
 */
public final class PageCursor {

    public static final Duration LIFETIME = Duration.ofHours(24);
    private static final int VERSION = 2;
    private static final JsonMapper JSON = JsonMappers.create();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    /** The decoded position: the page it opens and the row after which that page starts. */
    public record Position(int page, List<Object> sortValues, UUID lastId) {
        public Position {
            sortValues = List.copyOf(sortValues);
        }
    }

    private PageCursor() {
    }

    public static String encode(int page, SortSpec sort, String view, List<Object> sortValues, UUID lastId,
            Clock clock) {
        List<String> values = new ArrayList<>(sortValues.size());
        for (int i = 0; i < sortValues.size(); i++) {
            values.add(sort.orders().get(i).field().type().format(sortValues.get(i)));
        }
        Map<String, Object> cursor = new LinkedHashMap<>();
        cursor.put("v", VERSION);
        cursor.put("p", page);
        cursor.put("s", sort.canonical());
        cursor.put("f", fingerprint(view));
        cursor.put("t", clock.instant().getEpochSecond());
        cursor.put("k", values);
        cursor.put("i", lastId.toString());
        return ENCODER.encodeToString(JSON.writeValueAsBytes(cursor));
    }

    /**
     * Decodes a cursor for the given page, sort and view.
     *
     * @throws InvalidArgumentException when it is malformed, expired, or issued for another page, sort or view
     */
    public static Position decode(String cursor, int page, SortSpec sort, String view, Clock clock) {
        Map<?, ?> map;
        try {
            map = JSON.readValue(DECODER.decode(cursor), Map.class);
        } catch (JacksonException | IllegalArgumentException e) {
            throw invalid();
        }
        if (!Integer.valueOf(VERSION).equals(map.get("v")) || !(map.get("p") instanceof Integer issuedFor)
                || !(map.get("t") instanceof Number issued) || !(map.get("k") instanceof List<?> raw)
                || !(map.get("i") instanceof String lastId)) {
            throw invalid();
        }
        if (!sort.canonical().equals(map.get("s")) || !fingerprint(view).equals(map.get("f"))) {
            throw new InvalidArgumentException("cursor was issued for a different sort or filter.");
        }
        if (issuedFor != page) {
            throw new InvalidArgumentException("cursor was issued for page " + issuedFor + ".");
        }
        if (clock.instant().getEpochSecond() - issued.longValue() > LIFETIME.toSeconds()) {
            throw new InvalidArgumentException("cursor has expired.");
        }
        if (raw.size() != sort.orders().size()) {
            throw invalid();
        }
        try {
            List<Object> values = new ArrayList<>(raw.size());
            for (int i = 0; i < raw.size(); i++) {
                if (!(raw.get(i) instanceof String text)) {
                    throw invalid();
                }
                values.add(sort.orders().get(i).field().type().parse(text));
            }
            return new Position(page, values, UUID.fromString(lastId));
        } catch (IllegalArgumentException e) {
            throw invalid();
        }
    }

    private static InvalidArgumentException invalid() {
        return new InvalidArgumentException("cursor is not valid.");
    }

    private static String fingerprint(String view) {
        String text = view == null ? "" : view.trim();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
