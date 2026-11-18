package com.mysms.core.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.json.JsonMappers;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** The wire forms of an import batch and its result: sealed rows, tenant binding, refusal of anything else. */
class ImportBatchTest {

    private static final FieldCipher CIPHER = FieldCipher.fromSettings(
            "k1:" + Base64.getEncoder().encodeToString(new byte[32]), "k1");
    private static final JsonMapper JSON = JsonMappers.create();
    private static final UUID TENANT = UUID.fromString("01960000-0000-7000-8000-0000000000a1");
    private static final UUID OTHER = UUID.fromString("01960000-0000-7000-8000-0000000000a2");

    private static ImportBatch batch(Optional<UUID> institution) {
        return new ImportBatch(UUID.randomUUID(), "students", institution, 2, UUID.randomUUID(),
                Optional.of(UUID.randomUUID()), true, List.of(new ImportBatch.Row(3, Map.of("admissionNumber",
                        "ADM-7", "maxPeriodsPerWeek", 12, "isActive", true))));
    }

    @Test
    void theRowsTravelSealedForTheirTenantAndReadBackTyped() {
        ImportBatch sent = batch(Optional.of(UUID.randomUUID()));
        String data = JSON.writeValueAsString(sent.toPayload(CIPHER, JSON, TENANT));
        assertThat(data).doesNotContain("ADM-7").contains("\"entityType\":\"students\"");

        ImportBatch read = ImportBatch.fromPayload(data, CIPHER, JSON, TENANT);
        assertThat(read).isEqualTo(sent);
        assertThat(read.rows().get(0).values().get("maxPeriodsPerWeek")).isEqualTo(12);
        assertThat(read.rows().get(0).values().get("isActive")).isEqualTo(true);

        assertThatThrownBy(() -> ImportBatch.fromPayload(data, CIPHER, JSON, OTHER))
                .as("another tenant's key does not open the rows").isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ImportBatch.fromPayload("{\"entityType\":\"students\"}", CIPHER, JSON, TENANT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aTenantLevelBatchHasNoInstitution() {
        ImportBatch sent = batch(Optional.empty());
        String data = JSON.writeValueAsString(sent.toPayload(CIPHER, JSON, TENANT));
        assertThat(data).doesNotContain("institutionId");
        assertThat(ImportBatch.fromPayload(data, CIPHER, JSON, TENANT).institutionId()).isEmpty();
    }

    @Test
    void aResultCarriesCountsAndRowErrorsOnly() {
        UUID operation = UUID.randomUUID();
        ImportBatchResult result = new ImportBatchResult(operation, "staff", 4, 7, List.of(
                new ImportBatchResult.RowError(9, "employeeCode", "must not be blank"),
                new ImportBatchResult.RowError(11, null, "x".repeat(600))));
        String data = JSON.writeValueAsString(result.toPayload());
        ImportBatchResult read = ImportBatchResult.fromPayload(data, JSON);
        assertThat(read).isEqualTo(result);
        assertThat(read.errors().get(1).field()).isNull();
        assertThat(read.errors().get(1).message()).hasSize(ImportBatchResult.MAX_MESSAGE);
        assertThat(new ImportBatchResult.RowError(1, null, " ").message()).isEqualTo("refused");
        assertThat(ImportBatchResult.eventType("hr")).isEqualTo("hr.import.batch_applied");
        assertThatThrownBy(() -> ImportBatchResult.fromPayload("{\"operationId\":\"" + operation + "\"}", JSON))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ImportBatchResult.fromPayload("not json", JSON))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
