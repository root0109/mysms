package com.mysms.authclient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;

/**
 * One module's API-permission catalogue as it is published to iam (JAVA-CODING-RULES §16.2): every operation of its
 * operation table with the permission that guards it. The SHA-256 covers the entries only, in operation-id order, so
 * the same contract always has the same hash whatever the release or the table's order.
 *
 * @param module  the publishing module
 * @param release the deployed release ({@code mysms.auth.release})
 * @param sha256  hex SHA-256 of the canonical entries
 * @param entries the operations, sorted by operation id
 */
public record ApiPermissionCatalogue(String module, String release, String sha256, List<Entry> entries) {

    /** The event the catalogue travels as: {@code <module>.api_permissions.published}. */
    public static final String EVENT_SUFFIX = ".api_permissions.published";
    /** Its aggregate: {@code <module>.api_permissions}, one per module. */
    public static final String AGGREGATE_SUFFIX = ".api_permissions";

    /**
     * One operation of the catalogue, as the module's operation table states it.
     *
     * @param operationId the contract operation id, {@code <module>.<Method>}
     * @param method      the HTTP method
     * @param path        the path template
     * @param permission  the {@code x-permission} code guarding it (every operation has one, anonymous ones too)
     * @param schemes     the security schemes whose tokens may call it, sorted; empty for a provider's signed callback
     * @param anonymous   true when the operation may also be called without a token
     * @param audience    public, internal or platform ({@link OperationRule})
     */
    public record Entry(String operationId, String method, String path, String permission, List<String> schemes,
                        boolean anonymous, String audience) {

        public Entry {
            schemes = List.copyOf(schemes);
        }

        static Entry of(OperationRule rule) {
            return new Entry(rule.operationId(), rule.method(), rule.path(), rule.permission(),
                    rule.schemes().stream().sorted().toList(), rule.anonymous(), rule.audience());
        }
    }

    public ApiPermissionCatalogue {
        entries = List.copyOf(entries);
    }

    /** The catalogue of one module of the table. */
    public static ApiPermissionCatalogue of(OperationTable table, String module, String release, JsonMapper json) {
        List<Entry> entries = table.rulesOf(module).stream()
                .map(Entry::of)
                .sorted(Comparator.comparing(Entry::operationId))
                .toList();
        return new ApiPermissionCatalogue(module, release, sha256(json.writeValueAsString(entries)), entries);
    }

    /** The event type the module publishes it under. */
    public String eventType() {
        return module + EVENT_SUFFIX;
    }

    /** The event's aggregate type. */
    public String aggregateType() {
        return module + AGGREGATE_SUFFIX;
    }

    /** The module's catalogue aggregate: one stable id per module. */
    public UUID aggregateId() {
        return UUID.nameUUIDFromBytes(("mysms:" + module + ":api_permissions").getBytes(StandardCharsets.UTF_8));
    }

    /** The event data: module, release, hash and entries. */
    public String toJson(JsonMapper json) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("module", module);
        data.put("release", release);
        data.put("sha256", sha256);
        data.put("entries", entries);
        return json.writeValueAsString(data);
    }

    private static String sha256(String canonical) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required of every Java runtime", e);
        }
    }
}
