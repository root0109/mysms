package com.mysms.platform.org;

import com.mysms.platform.PlatformApplication;
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
import com.mysms.platform.PlatformTenants;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.org.config.OrgDatabaseConfig;
import com.mysms.platform.org.events.OrgImportHandler;
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
 * org creates the holidays of the platform's imports on real PostgreSQL: each row through its own service as the
 * importing user — an institution's holidays in the batch's institution only, tenant-wide holidays only under a
 * tenant-wide grant; refused rows (its rules, the database's checks and unique keys) are answered as row errors while
 * the others are kept; a batch is applied once; another tenant sees nothing.
 */
@SpringBootTest(classes = PlatformApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrgImportsComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.fromString("0190b0a0-0000-7000-8000-0000000d411");
    private static final UUID OTHER = UUID.fromString("0190b0a0-0000-7000-8000-0000000d412");
    private static final UUID SCHOOL = UUID.fromString("0190b0a0-0000-7000-8000-0000000d413");
    private static final UUID OTHER_SCHOOL = UUID.fromString("0190b0a0-0000-7000-8000-0000000d414");

    @Autowired
    @Qualifier(OrgDatabaseConfig.DATABASE)
    private ModuleDatabase database;
    @Autowired
    @Qualifier(PlatformDatabaseConfig.DATABASE)
    private ModuleDatabase platform;

    @Autowired
    private OrgImportHandler imports;

    @Autowired
    private FieldCipher cipher;

    @Autowired
    private JsonMapper json;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "org", "platform", "master");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @Test
    void holidaysAreCreatedForTheInstitutionOrTheWholeTenant() {
        UUID user = UUID.randomUUID();
        write(TENANT, "INSERT INTO org.institution (id, tenant_id, code, name, row_version) VALUES (:id, :tenantId, "
                + "'S1', 'School one', 1)", Map.of("id", SCHOOL));
        UUID operation = UUID.randomUUID();
        ImportBatch batch = new ImportBatch(operation, "holidays", Optional.of(SCHOOL), 1, user, Optional.empty(),
                false, List.of(new ImportBatch.Row(2, Map.of("title", "Diwali", "startDate", "2026-11-08",
                                "endDate", "2026-11-10", "institutionId", OTHER_SCHOOL.toString())),
                        new ImportBatch.Row(3, Map.of("title", "Diwali", "startDate", "2026-11-08",
                                "endDate", "2026-11-09")),
                        new ImportBatch.Row(4, Map.of("title", "Backwards", "startDate", "2026-12-02",
                                "endDate", "2026-12-01")),
                        new ImportBatch.Row(5, Map.of("title", "Christmas", "startDate", "2026-12-25",
                                "endDate", "2026-12-25"))));
        EventEnvelope event = event(TENANT, batch, TENANT);
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        assertThat(inbox.deliver(event, imports::handle)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
        assertThat(inbox.deliver(event, imports::handle)).isEqualTo(InboxProcessor.Outcome.DUPLICATE);
        ImportBatchResult result = onlyResult(TENANT, operation);
        assertThat(result.createdCount()).isEqualTo(2);
        assertThat(result.errors()).extracting(ImportBatchResult.RowError::rowNumber).containsExactly(3, 4);
        assertThat(result.errors().get(1).message()).isEqualTo("The request violates a data rule.");

        // a tenant-wide holiday needs the tenant-wide grant
        ImportBatch tenantWide = new ImportBatch(UUID.randomUUID(), "holidays", Optional.empty(), 1, user,
                Optional.empty(), false, List.of(new ImportBatch.Row(2, Map.of("title", "Republic Day",
                        "startDate", "2027-01-26", "endDate", "2027-01-26"))));
        assertThatThrownBy(() -> inbox.deliver(event(TENANT, tenantWide, TENANT), imports::handle))
                .isInstanceOf(RejectedEventException.class);
        UUID granted = UUID.randomUUID();
        inbox.deliver(event(TENANT, new ImportBatch(granted, "holidays", Optional.empty(), 1, user, Optional.empty(),
                true, tenantWide.rows()), TENANT), imports::handle);
        assertThat(onlyResult(TENANT, granted).createdCount()).isOne();

        List<Map<String, Object>> holidays = read(TENANT, () -> database.jdbc().sql("SELECT title, institution_id, "
                + "created_by FROM org.holiday WHERE tenant_id = :tenantId ORDER BY start_date").query().listOfRows());
        assertThat(holidays).extracting(row -> row.get("title")).containsExactly("Diwali", "Christmas",
                "Republic Day");
        assertThat(holidays).extracting(row -> row.get("institution_id")).containsExactly(SCHOOL, SCHOOL, null);
        assertThat(holidays).allSatisfy(row -> assertThat(row).containsEntry("created_by", user));
        assertThat(read(OTHER, () -> database.jdbc().sql("SELECT count(*) FROM org.holiday WHERE tenant_id = "
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
        List<String> results = read(tenant, () -> database.jdbc().sql("SELECT payload FROM org.outbox_event "
                + "WHERE tenant_id = :tenantId AND event_type = 'org.import.batch_applied' AND aggregate_id = :op")
                .param("op", operation).query(String.class).list());
        assertThat(results).as("one answer per batch").hasSize(1);
        return ImportBatchResult.fromPayload(results.get(0), json);
    }

    private <T> T read(UUID tenant, Supplier<T> work) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).read(work::get));
    }

    private void write(UUID tenant, String statement, Map<String, Object> params) {
        PlatformTenants.register(platform, tenant);
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).write(() -> database.jdbc().sql(statement).params(params)
                        .update()));
    }
}
