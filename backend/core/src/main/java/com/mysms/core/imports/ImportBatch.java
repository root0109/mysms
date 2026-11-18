package com.mysms.core.imports;

import com.mysms.core.crypto.FieldCipher;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * One batch of an import, as the platform publishes it ({@value #EVENT_TYPE}) to the module that owns the rows. The
 * rows are personal data, so they travel sealed with the tenant's field key ({@link FieldCipher}) and the outbox, bus
 * and inbox never hold them in clear. The importing user and checked institution travel with them; the consumer
 * derives the permission itself and grants nothing beyond it ({@link ImportIdentity}).
 *
 * @param institutionId  empty for tenant-level records
 * @param batchNumber    1-based position of the batch in the import
 * @param tenantWide     the user held the entity type's create permission through a tenant-wide grant
 */
public record ImportBatch(UUID operationId, String entityType, Optional<UUID> institutionId, int batchNumber,
                          UUID userId, Optional<UUID> impersonatorId, boolean tenantWide, List<Row> rows) {

    /** The event the platform publishes for every batch of a running import. */
    public static final String EVENT_TYPE = "platform.import.batch_requested";
    /** The aggregate of the import events: the import's operation. */
    public static final String AGGREGATE = "platform.import";
    /** The column name the sealed rows are bound to (the associated data of the tenant's field key). */
    public static final String ROWS_COLUMN = "platform.import.rows";

    private static final TypeReference<List<Row>> ROWS = new TypeReference<>() {
    };

    /**
     * One row of the file: its record number in the file (the header is record 1, as a spreadsheet numbers rows) and
     * its non-empty cells by contract field name.
     */
    public record Row(int rowNumber, Map<String, Object> values) {

        public Row {
            values = Map.copyOf(values);
        }
    }

    public ImportBatch {
        institutionId = institutionId == null ? Optional.empty() : institutionId;
        impersonatorId = impersonatorId == null ? Optional.empty() : impersonatorId;
        rows = List.copyOf(rows);
    }

    /** The event payload: the batch with its rows sealed for the tenant. */
    public Map<String, Object> toPayload(FieldCipher cipher, JsonMapper json, UUID tenant) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operationId", operationId.toString());
        payload.put("entityType", entityType);
        institutionId.ifPresent(id -> payload.put("institutionId", id.toString()));
        payload.put("batchNumber", batchNumber);
        payload.put("userId", userId.toString());
        impersonatorId.ifPresent(id -> payload.put("impersonatorId", id.toString()));
        payload.put("tenantWide", tenantWide);
        payload.put("rows", cipher.encrypt(json.writeValueAsString(rows), tenant, ROWS_COLUMN));
        return payload;
    }

    /**
     * Reads an event payload of the tenant; the rows open only for the tenant they were sealed for.
     *
     * @throws IllegalArgumentException for a payload that is not an import batch, or rows that do not open
     */
    public static ImportBatch fromPayload(String data, FieldCipher cipher, JsonMapper json, UUID tenant) {
        try {
            JsonNode node = json.readTree(data);
            String sealed = node.path("rows").asString("");
            String rows = cipher.decrypt(sealed, tenant, ROWS_COLUMN)
                    .orElseThrow(() -> new IllegalArgumentException("the batch carries no rows"));
            return new ImportBatch(uuid(node, "operationId").orElseThrow(), node.path("entityType").asString(),
                    uuid(node, "institutionId"), node.path("batchNumber").asInt(0),
                    uuid(node, "userId").orElseThrow(), uuid(node, "impersonatorId"),
                    node.path("tenantWide").asBoolean(false), json.readValue(rows, ROWS));
        } catch (RuntimeException unreadable) {
            throw new IllegalArgumentException("not an import batch of this tenant", unreadable);
        }
    }

    private static Optional<UUID> uuid(JsonNode node, String field) {
        String value = node.path(field).asString("");
        return value.isEmpty() ? Optional.empty() : Optional.of(UUID.fromString(value));
    }
}
