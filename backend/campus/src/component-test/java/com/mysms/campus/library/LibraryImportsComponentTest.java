package com.mysms.campus.library;

import com.mysms.campus.CampusApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.imports.ImportBatch;
import com.mysms.core.imports.ImportBatchResult;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.InboxProcessor;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.campus.library.config.LibraryDatabaseConfig;
import com.mysms.campus.library.events.LibraryImportHandler;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Imports of catalogue titles on real PostgreSQL: each row through its own service as the importing user, in the
 * batch's institution only; refused rows (its references) are answered as row errors while the others are kept; a batch
 * is applied once; another tenant sees nothing.
 */
@SpringBootTest(classes = CampusApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LibraryImportsComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.fromString("0190b0a0-0000-7000-8000-0000000d311");
    private static final UUID OTHER = UUID.fromString("0190b0a0-0000-7000-8000-0000000d312");
    private static final UUID SCHOOL = UUID.fromString("0190b0a0-0000-7000-8000-0000000d313");
    private static final UUID OTHER_SCHOOL = UUID.fromString("0190b0a0-0000-7000-8000-0000000d314");

    @Autowired
    @Qualifier(LibraryDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private LibraryImportHandler imports;

    @Autowired
    private FieldCipher cipher;

    @Autowired
    private JsonMapper json;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "library", "transport", "frontoffice", "facility");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @Test
    void titlesAreCreatedAsTheImporterInTheBatchesInstitutionOnly() {
        UUID user = UUID.randomUUID();
        UUID operation = UUID.randomUUID();
        ImportBatch batch = new ImportBatch(operation, "library_media", Optional.of(SCHOOL), 1, user, Optional.empty(),
                false, List.of(new ImportBatch.Row(2, Map.of("title", "Atlas of India", "isbn", "9780000000001",
                                "institutionId", OTHER_SCHOOL.toString())),
                        new ImportBatch.Row(3, Map.of("title", "Globe", "mediaTypeId", UUID.randomUUID().toString())),
                        new ImportBatch.Row(4, Map.of("title", "Old Maps", "isActive", false))));
        EventEnvelope event = event(TENANT, batch, TENANT);
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        assertThat(inbox.deliver(event, imports::handle)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
        assertThat(inbox.deliver(event, imports::handle)).isEqualTo(InboxProcessor.Outcome.DUPLICATE);

        List<Map<String, Object>> titles = read(TENANT, () -> database.jdbc().sql("SELECT title, institution_id, "
                + "created_by, is_active FROM library.media WHERE tenant_id = :tenantId ORDER BY title").query()
                .listOfRows());
        assertThat(titles).extracting(row -> row.get("title")).containsExactly("Atlas of India", "Old Maps");
        assertThat(titles).allSatisfy(row -> assertThat(row).containsEntry("institution_id", SCHOOL)
                .containsEntry("created_by", user));
        assertThat(titles).extracting(row -> row.get("is_active")).containsExactly(true, false);
        ImportBatchResult result = onlyResult(TENANT, operation);
        assertThat(result.createdCount()).isEqualTo(2);
        assertThat(result.errors()).containsExactly(
                new ImportBatchResult.RowError(3, "mediaTypeId", "media type does not exist"));
        assertThat(read(OTHER, () -> database.jdbc().sql("SELECT count(*) FROM library.media WHERE tenant_id = "
                + ":tenantId").query(Integer.class).single())).isZero();
        assertThatThrownBy(() -> inbox.deliver(event(OTHER, batch, TENANT), imports::handle))
                .isInstanceOf(RejectedEventException.class);
    }

    /** The platform's batch event, delivered in {@code tenant}, its rows sealed for {@code sealedFor}. */
    private EventEnvelope event(UUID tenant, ImportBatch batch, UUID sealedFor) {
        return new EventEnvelope(UUID.randomUUID(), "platform", ImportBatch.EVENT_TYPE, Optional.of(tenant),
                ImportBatch.AGGREGATE, batch.operationId(), Instant.now(),
                json.writeValueAsString(batch.toPayload(cipher, json, sealedFor)), Optional.of(TRACE));
    }

    private ImportBatchResult onlyResult(UUID tenant, UUID operation) {
        List<String> results = read(tenant, () -> database.jdbc().sql("SELECT payload FROM library.outbox_event "
                + "WHERE tenant_id = :tenantId AND event_type = 'library.import.batch_applied' AND aggregate_id = :op")
                .param("op", operation).query(String.class).list());
        assertThat(results).as("one answer per batch").hasSize(1);
        return ImportBatchResult.fromPayload(results.get(0), json);
    }

    private <T> T read(UUID tenant, Supplier<T> work) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).read(work::get));
    }
}
