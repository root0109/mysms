package com.mysms.core.imports;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * What a module did with one {@link ImportBatch}: the {@code <module>.import.batch_applied} event it publishes in the
 * transaction that created the rows, which the platform turns into the import operation's result. Row errors name
 * the row, the field and a sentence the service wrote on purpose — never a cell value, so the event carries no
 * personal data.
 */
public record ImportBatchResult(UUID operationId, String entityType, int batchNumber, int createdCount,
                                List<RowError> errors) {

    /** The event type's suffix after the module: {@code sis.import.batch_applied}. */
    public static final String EVENT_SUFFIX = ".import.batch_applied";
    /** The aggregate suffix after the module: {@code sis.import}. */
    public static final String AGGREGATE_SUFFIX = ".import";
    /** The longest message kept per row error (the column of the platform's row errors). */
    public static final int MAX_MESSAGE = 500;

    /** One refused row: its record number in the file, the field at fault (empty: the row as a whole) and why. */
    public record RowError(int rowNumber, String field, String message) {

        public RowError {
            message = message == null || message.isBlank() ? "refused" : message.length() > MAX_MESSAGE
                    ? message.substring(0, MAX_MESSAGE) : message;
        }
    }

    public ImportBatchResult {
        errors = List.copyOf(errors);
    }

    /** The event type a module publishes its results under. */
    public static String eventType(String module) {
        return module + EVENT_SUFFIX;
    }

    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operationId", operationId.toString());
        payload.put("entityType", entityType);
        payload.put("batchNumber", batchNumber);
        payload.put("createdCount", createdCount);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (RowError error : errors) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rowNumber", error.rowNumber());
            if (error.field() != null) {
                row.put("field", error.field());
            }
            row.put("message", error.message());
            rows.add(row);
        }
        payload.put("errors", rows);
        return payload;
    }

    /**
     * Reads a result event's payload.
     *
     * @throws IllegalArgumentException for a payload that is not a batch result
     */
    public static ImportBatchResult fromPayload(String data, JsonMapper json) {
        try {
            JsonNode node = json.readTree(data);
            List<RowError> errors = new ArrayList<>();
            for (JsonNode error : node.path("errors")) {
                String field = error.path("field").asString("");
                errors.add(new RowError(error.path("rowNumber").asInt(0), field.isEmpty() ? null : field,
                        error.path("message").asString("")));
            }
            int created = node.path("createdCount").asInt(-1);
            int batch = node.path("batchNumber").asInt(0);
            if (created < 0 || batch < 1) {
                throw new IllegalArgumentException("a batch result needs its batch number and created count");
            }
            return new ImportBatchResult(UUID.fromString(node.path("operationId").asString()),
                    node.path("entityType").asString(), batch, created, errors);
        } catch (RuntimeException unreadable) {
            throw new IllegalArgumentException("not an import batch result", unreadable);
        }
    }
}
