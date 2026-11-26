package com.mysms.hrms.hr;

import com.mysms.hrms.HrmsApplication;
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
import com.mysms.hrms.hr.config.HrDatabaseConfig;
import com.mysms.hrms.hr.events.HrImportHandler;
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
 * Imports of staff on real PostgreSQL: each row through its own service as the importing user, in the batch's
 * institution only; refused rows (its own rules, the database's unique keys) are answered as row errors while the
 * others are kept; a batch is applied once; another tenant sees nothing.
 */
@SpringBootTest(classes = HrmsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HrImportsComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.fromString("0190b0a0-0000-7000-8000-0000000d211");
    private static final UUID OTHER = UUID.fromString("0190b0a0-0000-7000-8000-0000000d212");
    private static final UUID SCHOOL = UUID.fromString("0190b0a0-0000-7000-8000-0000000d213");
    private static final UUID OTHER_SCHOOL = UUID.fromString("0190b0a0-0000-7000-8000-0000000d214");

    @Autowired
    @Qualifier(HrDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private HrImportHandler imports;

    @Autowired
    private FieldCipher cipher;

    @Autowired
    private JsonMapper json;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "hr", "payroll");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @Test
    void staffAreCreatedAsTheImporterInTheBatchesInstitutionOnly() {
        UUID user = UUID.randomUUID();
        UUID operation = UUID.randomUUID();
        ImportBatch batch = new ImportBatch(operation, "staff", Optional.of(SCHOOL), 3, user, Optional.empty(), false,
                List.of(new ImportBatch.Row(2, Map.of("personId", UUID.randomUUID().toString(), "employeeCode", "E-1",
                                "employmentType", "full_time", "maxPeriodsPerWeek", 30, "joiningDate", "2026-06-01",
                                "institutionId", OTHER_SCHOOL.toString())),
                        new ImportBatch.Row(3, Map.of("personId", UUID.randomUUID().toString(), "employeeCode",
                                "E-1")),
                        new ImportBatch.Row(4, Map.of("personId", UUID.randomUUID().toString(), "designationId",
                                UUID.randomUUID().toString())),
                        new ImportBatch.Row(5, Map.of("personId", UUID.randomUUID().toString(), "employeeCode",
                                "E-5"))));
        EventEnvelope event = event(TENANT, batch, TENANT);
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        assertThat(inbox.deliver(event, imports::handle)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
        assertThat(inbox.deliver(event, imports::handle)).isEqualTo(InboxProcessor.Outcome.DUPLICATE);

        List<Map<String, Object>> staff = read(TENANT, () -> database.jdbc().sql("SELECT employee_code, "
                + "institution_id, created_by, max_periods_per_week FROM hr.staff WHERE tenant_id = :tenantId "
                + "ORDER BY employee_code").query().listOfRows());
        assertThat(staff).extracting(row -> row.get("employee_code")).containsExactly("E-1", "E-5");
        assertThat(staff).allSatisfy(row -> assertThat(row).containsEntry("institution_id", SCHOOL)
                .containsEntry("created_by", user));
        assertThat(staff.get(0)).containsEntry("max_periods_per_week", 30);
        ImportBatchResult result = onlyResult(TENANT, operation);
        assertThat(result.batchNumber()).isEqualTo(3);
        assertThat(result.createdCount()).isEqualTo(2);
        assertThat(result.errors()).containsExactly(
                new ImportBatchResult.RowError(3, null, "A resource with the same unique key already exists."),
                new ImportBatchResult.RowError(4, "designationId", "designation does not exist"));
        assertThat(read(OTHER, () -> database.jdbc().sql("SELECT count(*) FROM hr.staff WHERE tenant_id = "
                + ":tenantId").query(Integer.class).single())).isZero();

        // other modules' batches are left alone; a batch sealed for another tenant does not open
        UUID mediaOp = UUID.randomUUID();
        assertThat(inbox.deliver(event(TENANT, new ImportBatch(mediaOp, "library_media", Optional.of(SCHOOL), 1, user,
                Optional.empty(), false, List.of()), TENANT), imports::handle))
                .isEqualTo(InboxProcessor.Outcome.PROCESSED);
        assertThat(read(TENANT, () -> database.jdbc().sql("SELECT count(*) FROM hr.outbox_event WHERE tenant_id = "
                + ":tenantId AND aggregate_id = :op").param("op", mediaOp).query(Integer.class).single())).isZero();
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
        List<String> results = read(tenant, () -> database.jdbc().sql("SELECT payload FROM hr.outbox_event "
                + "WHERE tenant_id = :tenantId AND event_type = 'hr.import.batch_applied' AND aggregate_id = :op")
                .param("op", operation).query(String.class).list());
        assertThat(results).as("one answer per batch").hasSize(1);
        return ImportBatchResult.fromPayload(results.get(0), json);
    }

    private <T> T read(UUID tenant, Supplier<T> work) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).read(work::get));
    }
}
