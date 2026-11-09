package com.mysms.core.paging;

import com.mysms.core.error.InvalidArgumentException;
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
 * The opaque `pageToken` (api/README "Pagination"): the sort keys of the last row returned, bound to the order and
 * filter it was issued for and valid for 24 hours. A token presented with a different order or filter, or after
 * expiry, is rejected with 400.
 */
public final class PageToken {

    public static final Duration LIFETIME = Duration.ofHours(24);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    /** The decoded position after which the next page starts. */
    public record Cursor(List<Object> sortValues, UUID lastId) {
        public Cursor {
            sortValues = List.copyOf(sortValues);
        }
    }

    private PageToken() {
    }

    public static String encode(SortSpec sort, String filter, List<Object> sortValues, UUID lastId, Clock clock) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < sortValues.size(); i++) {
            values.add(sort.orders().get(i).field().type().format(sortValues.get(i)));
        }
        Map<String, Object> token = new LinkedHashMap<>();
        token.put("v", 1);
        token.put("s", sort.canonical());
        token.put("f", fingerprint(filter));
        token.put("t", clock.instant().getEpochSecond());
        token.put("k", values);
        token.put("i", lastId.toString());
        return ENCODER.encodeToString(JSON.writeValueAsBytes(token));
    }

    public static Cursor decode(String token, SortSpec sort, String filter, Clock clock) {
        Map<?, ?> map;
        try {
            map = JSON.readValue(DECODER.decode(token), Map.class);
        } catch (JacksonException | IllegalArgumentException e) {
            throw invalid();
        }
        if (!Integer.valueOf(1).equals(map.get("v")) || !(map.get("t") instanceof Number issued)
                || !(map.get("k") instanceof List<?> raw) || !(map.get("i") instanceof String lastId)) {
            throw invalid();
        }
        if (!sort.canonical().equals(map.get("s")) || !fingerprint(filter).equals(map.get("f"))) {
            throw new InvalidArgumentException("pageToken was issued for a different orderBy or filter.");
        }
        if (clock.instant().getEpochSecond() - issued.longValue() > LIFETIME.toSeconds()) {
            throw new InvalidArgumentException("pageToken has expired.");
        }
        if (raw.size() != sort.orders().size()) {
            throw invalid();
        }
        try {
            List<Object> values = new ArrayList<>();
            for (int i = 0; i < raw.size(); i++) {
                if (!(raw.get(i) instanceof String text)) {
                    throw invalid();
                }
                values.add(sort.orders().get(i).field().type().parse(text));
            }
            return new Cursor(values, UUID.fromString(lastId));
        } catch (IllegalArgumentException e) {
            throw invalid();
        }
    }

    private static InvalidArgumentException invalid() {
        return new InvalidArgumentException("pageToken is not valid.");
    }

    private static String fingerprint(String filter) {
        String text = filter == null ? "" : filter.trim();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
