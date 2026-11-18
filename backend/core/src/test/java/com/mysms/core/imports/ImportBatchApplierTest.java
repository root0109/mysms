package com.mysms.core.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.json.JsonMappers;
import com.mysms.core.outbox.OutboxWriter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import tools.jackson.databind.json.JsonMapper;

/** The applier against stand-ins: which rows become records, how refusals become row errors, what is published. */
class ImportBatchApplierTest {

    private static final FieldCipher CIPHER = FieldCipher.fromSettings(
            "k1:" + Base64.getEncoder().encodeToString(new byte[32]), "k1");
    private static final JsonMapper JSON = JsonMappers.create();
    private static final UUID TENANT = UUID.fromString("01960000-0000-7000-8000-0000000000b1");

    /** A create contract: a title and the institution the batch decides. */
    public static final class Thing {
        private String title;
        private Integer copies;
        private UUID institutionId;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Integer getCopies() {
            return copies;
        }

        public void setCopies(Integer copies) {
            this.copies = copies;
        }

        public UUID getInstitutionId() {
            return institutionId;
        }

        public void setInstitutionId(UUID institutionId) {
            this.institutionId = institutionId;
        }
    }

    private final List<Thing> created = new ArrayList<>();
    private final List<String> permissions = new ArrayList<>();
    private final OutboxWriter outbox = mock(OutboxWriter.class);

    private final class Things extends ImportBatchApplier {

        Things(ModuleDatabase database, PlatformTransactionManager transactions) {
            super("demo", database, transactions, outbox, new ImportIdentity() {
                @Override
                public <T> T callAs(ImportBatch batch, String permission, Supplier<T> work) {
                    permissions.add(permission);
                    return work.get();
                }
            }, CIPHER, JSON);
        }

        @Override
        protected Map<String, ImportTarget<?>> targets() {
            return Map.of("things", new ImportTarget<>("demo.thing.create", Thing.class, Thing::setInstitutionId,
                    this::create));
        }

        private void create(Thing thing) {
            switch (thing.getTitle()) {
                case "invalid" -> throw new ValidationFailedException(List.of(
                        new FieldError("title", "is reserved", "RESERVED"), new FieldError("copies", "too many", "Max")));
                case "taken" -> throw new DuplicateResourceException("A thing with the same title already exists.");
                case "clash" -> throw new DuplicateKeyException("uq_thing");
                default -> created.add(thing);
            }
        }
    }

    private Things things() {
        ModuleDatabase database = mock(ModuleDatabase.class);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        when(database.writeDefinition()).thenReturn(new DefaultTransactionDefinition());
        // the batch's transaction is new; every row joins it in a savepoint
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus(true),
                new SimpleTransactionStatus(false));
        return new Things(database, transactions);
    }

    private static String payload(String entityType, UUID institution, List<ImportBatch.Row> rows) {
        ImportBatch batch = new ImportBatch(UUID.randomUUID(), entityType, Optional.of(institution), 1,
                UUID.randomUUID(), Optional.empty(), false, rows);
        return JSON.writeValueAsString(batch.toPayload(CIPHER, JSON, TENANT));
    }

    @Test
    void validRowsBecomeRecordsAndRefusedRowsBecomeErrors() {
        UUID institution = UUID.randomUUID();
        String data = payload("things", institution, List.of(
                new ImportBatch.Row(2, Map.of("title", "Atlas", "copies", 3, "institutionId", UUID.randomUUID())),
                new ImportBatch.Row(3, Map.of("title", "invalid")),
                new ImportBatch.Row(4, Map.of("title", "taken")),
                new ImportBatch.Row(5, Map.of("title", "clash")),
                new ImportBatch.Row(6, Map.of("title", "Globe", "copies", "many"))));

        ImportBatchResult result = things().apply(TENANT, data).orElseThrow();

        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(created).singleElement().satisfies(thing -> {
            assertThat(thing.getTitle()).isEqualTo("Atlas");
            assertThat(thing.getInstitutionId()).as("the batch decides the institution").isEqualTo(institution);
        });
        assertThat(result.errors()).extracting(ImportBatchResult.RowError::rowNumber).containsExactly(3, 3, 4, 5, 6);
        assertThat(result.errors().get(0)).isEqualTo(new ImportBatchResult.RowError(3, "title", "is reserved"));
        assertThat(result.errors().get(2).message()).isEqualTo("A thing with the same title already exists.");
        assertThat(result.errors().get(3).message()).isEqualTo("A resource with the same unique key already exists.");
        assertThat(result.errors().get(4).message()).contains("format");
        assertThat(permissions).containsExactly("demo.thing.create");
        verify(outbox).publish(eq("demo.import.batch_applied"), eq("demo.import"), eq(result.operationId()), anyMap());
    }

    @Test
    void anotherModulesBatchIsLeftAloneAndGarbageIsRefused() {
        Things things = things();
        assertThat(things.apply(TENANT, payload("staff", UUID.randomUUID(), List.of()))).isEmpty();
        verify(outbox, never()).publish(any(), any(), any(), anyMap());
        assertThatThrownBy(() -> things.apply(TENANT, "not json")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> things.apply(TENANT, "{\"entityType\":\"things\",\"rows\":\"v1.x\"}"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
