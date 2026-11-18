package com.mysms.core.imports;

import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ApiException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.IntegrityViolations;
import com.mysms.core.outbox.OutboxWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * How a module creates an import's rows (Template Method; the module supplies {@link #targets()}): each row runs as
 * the importing user through the business service in its own savepoint, so a refused row is reported and the others
 * kept, and the {@link ImportBatchResult} is published in the same transaction. The batch's institution replaces
 * anything a row says about it.
 */
public abstract class ImportBatchApplier extends AppTransactionManager {

    /**
     * One entity type a module imports.
     *
     * @param permission  the create permission the importing user was checked for
     * @param contract    the create contract (DTO) a row is read into
     * @param institution sets the batch's institution on the DTO (null: the records are tenant-level)
     * @param create      the business service's create, which refuses a row by throwing
     */
    public record ImportTarget<T>(String permission, Class<T> contract, BiConsumer<T, UUID> institution,
                                     Consumer<T> create) {
    }

    private final String module;
    private final OutboxWriter outbox;
    private final ImportIdentity identity;
    private final FieldCipher cipher;
    private final JsonMapper json;

    protected ImportBatchApplier(String module, ModuleDatabase database, PlatformTransactionManager transactions,
            OutboxWriter outbox, ImportIdentity identity, FieldCipher cipher, JsonMapper json) {
        super(database, transactions);
        this.module = module;
        this.outbox = outbox;
        this.identity = identity;
        this.cipher = cipher;
        this.json = json;
    }

    /** The entity types this module imports, by the platform's entity type name. */
    protected abstract Map<String, ImportTarget<?>> targets();

    /**
     * Applies the payload of one {@value ImportBatch#EVENT_TYPE} event of the tenant, inside the event's transaction;
     * empty when the batch is of an entity type another module owns (nothing is opened or written).
     *
     * @throws IllegalArgumentException for a payload that is not an import batch of this tenant (never retried)
     */
    public final Optional<ImportBatchResult> apply(UUID tenant, String data) {
        JsonNode node;
        try {
            node = json.readTree(data);
        } catch (JacksonException unreadable) {
            throw new IllegalArgumentException("not an import batch", unreadable);
        }
        ImportTarget<?> target = targets().get(node.path("entityType").asString(""));
        if (target == null) {
            return Optional.empty();
        }
        ImportBatch batch = ImportBatch.fromPayload(data, cipher, json, tenant);
        if (batch.batchNumber() < 1) {
            throw new IllegalArgumentException("an import batch needs its batch number");
        }
        return Optional.of(identity.callAs(batch, target.permission(),
                () -> doInWriteTransaction(() -> applyRows(batch, target))));
    }

    private <T> ImportBatchResult applyRows(ImportBatch batch, ImportTarget<T> target) {
        int created = 0;
        List<ImportBatchResult.RowError> errors = new ArrayList<>();
        for (ImportBatch.Row row : batch.rows()) {
            T record;
            try {
                record = json.convertValue(row.values(), target.contract());
            } catch (JacksonException | IllegalArgumentException unreadable) {
                errors.add(new ImportBatchResult.RowError(row.rowNumber(), null,
                        "a value does not have the format its column needs"));
                continue;
            }
            if (target.institution() != null) {
                batch.institutionId().ifPresent(institution -> target.institution().accept(record, institution));
            }
            try {
                doInNestedWriteTransaction(() -> {
                    target.create().accept(record);
                    return null;
                });
                created++;
            } catch (ApiException refused) {
                errors.addAll(rowErrors(row.rowNumber(), refused.getMessage(), refused.errors()));
            } catch (DataIntegrityViolationException refused) {
                errors.add(new ImportBatchResult.RowError(row.rowNumber(), null,
                        IntegrityViolations.problem(refused).detail()));
            }
        }
        ImportBatchResult result = new ImportBatchResult(batch.operationId(), batch.entityType(), batch.batchNumber(),
                created, errors);
        outbox.publish(ImportBatchResult.eventType(module), module + ImportBatchResult.AGGREGATE_SUFFIX,
                batch.operationId(), result.toPayload());
        return result;
    }

    /** A refusal as row errors: one per field it names, or the refusal's own sentence for the row as a whole. */
    private static List<ImportBatchResult.RowError> rowErrors(int rowNumber, String detail, List<FieldError> fields) {
        if (fields.isEmpty()) {
            return List.of(new ImportBatchResult.RowError(rowNumber, null, detail));
        }
        return fields.stream().map(f -> new ImportBatchResult.RowError(rowNumber, f.field(), f.message())).toList();
    }
}
