package com.mysms.students.sis;

import com.mysms.students.StudentsApplication;
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
import com.mysms.students.sis.config.SisDatabaseConfig;
import com.mysms.students.sis.events.SisImportHandler;
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
 * sis creates the students, guardians and promotions of the platform's imports on real PostgreSQL: each row through
 * its own service as the importing user, in the batch's institution only; refused rows (its own rules, the database's
 * unique keys) are answered as row errors while the others are kept; a batch is applied once; a tenant-level batch
 * needs the tenant-wide grant; other modules' batches and other tenants are left alone.
 */
@SpringBootTest(classes = StudentsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SisImportsComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.fromString("0190b0a0-0000-7000-8000-0000000d1101");
    private static final UUID OTHER = UUID.fromString("0190b0a0-0000-7000-8000-0000000d1102");
    private static final UUID SCHOOL = UUID.fromString("0190b0a0-0000-7000-8000-0000000d1103");
    private static final UUID OTHER_SCHOOL = UUID.fromString("0190b0a0-0000-7000-8000-0000000d1104");

    @Autowired
    @Qualifier(SisDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private SisImportHandler imports;

    @Autowired
    private FieldCipher cipher;

    @Autowired
    private JsonMapper json;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "sis", "admission", "careers");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @Test
    void studentsAreCreatedAsTheImporterInTheBatchesInstitutionOnly() {
        UUID user = UUID.randomUUID();
        UUID operation = UUID.randomUUID();
        UUID person = UUID.randomUUID();
        ImportBatch batch = new ImportBatch(operation, "students", Optional.of(SCHOOL), 1, user, Optional.empty(),
                false, List.of(
                        new ImportBatch.Row(2, Map.of("personId", person.toString(), "admissionNumber", "A-1",
                                "admissionDate", "2026-04-01", "institutionId", OTHER_SCHOOL.toString())),
                        new ImportBatch.Row(3, Map.of("personId", UUID.randomUUID().toString(),
                                "admissionNumber", "A-1")),
                        new ImportBatch.Row(4, Map.of("personId", person.toString())),
                        new ImportBatch.Row(5, Map.of("personId", UUID.randomUUID().toString(),
                                "studentCategoryId", UUID.randomUUID().toString())),
                        new ImportBatch.Row(6, Map.of("personId", UUID.randomUUID().toString(),
                                "admissionDate", "1 April")),
                        new ImportBatch.Row(7, Map.of("personId", UUID.randomUUID().toString(),
                                "admissionNumber", "A-7"))));
        EventEnvelope event = event(TENANT, batch, TENANT);
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        assertThat(inbox.deliver(event, imports::handle)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
        assertThat(inbox.deliver(event, imports::handle)).as("a redelivery creates nothing again")
                .isEqualTo(InboxProcessor.Outcome.DUPLICATE);

        List<Map<String, Object>> students = read(TENANT, () -> database.jdbc().sql("""
                SELECT admission_number, institution_id, created_by FROM sis.student
                 WHERE tenant_id = :tenantId AND created_by = :user ORDER BY admission_number
                """).param("user", user).query().listOfRows());
        assertThat(students).hasSize(2).allSatisfy(row -> {
            assertThat(row).containsEntry("institution_id", SCHOOL).containsEntry("created_by", user);
        });
        assertThat(students).extracting(row -> row.get("admission_number")).containsExactly("A-1", "A-7");

        ImportBatchResult result = onlyResult(TENANT, operation);
        assertThat(result.createdCount()).isEqualTo(2);
        assertThat(result.batchNumber()).isEqualTo(1);
        assertThat(result.errors()).containsExactly(
                new ImportBatchResult.RowError(3, null, "A resource with the same unique key already exists."),
                new ImportBatchResult.RowError(4, null,
                        "A student with the same institution id and person id already exists."),
                new ImportBatchResult.RowError(5, "studentCategoryId", "student category does not exist"),
                new ImportBatchResult.RowError(6, null, "a value does not have the format its column needs"));
        assertThat(read(TENANT, () -> database.jdbc().sql("SELECT count(*) FROM sis.outbox_event "
                + "WHERE tenant_id = :tenantId AND event_type = 'audit.recorded' AND payload LIKE :actor")
                .param("actor", "%\"actorUserId\":\"" + user + "\"%").query(Integer.class).single()))
                .as("each record audited as the importer").isEqualTo(2);
        assertThat(read(OTHER, () -> database.jdbc().sql("SELECT count(*) FROM sis.student "
                + "WHERE tenant_id = :tenantId").query(Integer.class).single())).isZero();

        // sealed for one tenant, a batch does not open in another
        assertThatThrownBy(() -> inbox.deliver(event(OTHER, batch, TENANT), imports::handle))
                .isInstanceOf(RejectedEventException.class);
    }

    @Test
    void guardiansNeedTheTenantWideGrantAndPromotionsTheirStudent() {
        UUID user = UUID.randomUUID();
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        ImportBatch schoolOnly = new ImportBatch(UUID.randomUUID(), "guardians", Optional.empty(), 1, user,
                Optional.empty(), false, List.of(new ImportBatch.Row(2, Map.of("personId",
                        UUID.randomUUID().toString()))));
        assertThatThrownBy(() -> inbox.deliver(event(TENANT, schoolOnly, TENANT), imports::handle))
                .isInstanceOf(RejectedEventException.class);

        UUID guardianOp = UUID.randomUUID();
        ImportBatch guardians = new ImportBatch(guardianOp, "guardians", Optional.empty(), 1, user,
                Optional.of(UUID.randomUUID()), true, List.of(new ImportBatch.Row(2, Map.of("personId",
                        UUID.randomUUID().toString(), "profession", "teacher", "annualIncome", "480000.00"))));
        inbox.deliver(event(TENANT, guardians, TENANT), imports::handle);
        assertThat(onlyResult(TENANT, guardianOp).createdCount()).isOne();
        assertThat(read(TENANT, () -> database.jdbc().sql("SELECT profession FROM sis.guardian "
                + "WHERE tenant_id = :tenantId").query(String.class).single())).isEqualTo("teacher");

        UUID student = UUID.randomUUID();
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(TENANT, null),
                () -> TestTransactions.of(database).write(() -> database.jdbc().sql("""
                        INSERT INTO sis.student (id, tenant_id, person_id, institution_id, status, row_version)
                        VALUES (:id, :tenantId, :person, :school, 'active', 1)
                        """).param("id", student).param("person", UUID.randomUUID()).param("school", SCHOOL)
                        .update()));
        UUID promotionOp = UUID.randomUUID();
        UUID year = UUID.randomUUID();
        UUID grade = UUID.randomUUID();
        ImportBatch promotions = new ImportBatch(promotionOp, "promotions", Optional.of(SCHOOL), 1, user,
                Optional.empty(), false, List.of(
                        new ImportBatch.Row(2, Map.of("studentId", student.toString(), "fromAcademicYearId",
                                year.toString(), "fromGradeLevelId", grade.toString())),
                        new ImportBatch.Row(3, Map.of("studentId", UUID.randomUUID().toString(),
                                "fromAcademicYearId", year.toString(), "fromGradeLevelId", grade.toString()))));
        inbox.deliver(event(TENANT, promotions, TENANT), imports::handle);
        ImportBatchResult result = onlyResult(TENANT, promotionOp);
        assertThat(result.createdCount()).isOne();
        assertThat(result.errors()).containsExactly(
                new ImportBatchResult.RowError(3, "studentId", "student does not exist"));

        // a batch of another module's entity type is left to that module
        UUID staffOp = UUID.randomUUID();
        ImportBatch staff = new ImportBatch(staffOp, "staff", Optional.of(SCHOOL), 1, user, Optional.empty(),
                false, List.of(new ImportBatch.Row(2, Map.of("personId", UUID.randomUUID().toString()))));
        assertThat(inbox.deliver(event(TENANT, staff, TENANT), imports::handle))
                .isEqualTo(InboxProcessor.Outcome.PROCESSED);
        assertThat(results(TENANT, staffOp)).isEmpty();
    }

    /** The platform's batch event, delivered in {@code tenant}, its rows sealed for {@code sealedFor}. */
    private EventEnvelope event(UUID tenant, ImportBatch batch, UUID sealedFor) {
        return new EventEnvelope(UUID.randomUUID(), "platform", ImportBatch.EVENT_TYPE, Optional.of(tenant),
                ImportBatch.AGGREGATE, batch.operationId(), Instant.now(),
                json.writeValueAsString(batch.toPayload(cipher, json, sealedFor)), Optional.of(TRACE));
    }

    private ImportBatchResult onlyResult(UUID tenant, UUID operation) {
        List<String> results = results(tenant, operation);
        assertThat(results).as("one answer per batch").hasSize(1);
        return ImportBatchResult.fromPayload(results.get(0), json);
    }

    private List<String> results(UUID tenant, UUID operation) {
        return read(tenant, () -> database.jdbc().sql("SELECT payload FROM sis.outbox_event WHERE tenant_id = "
                + ":tenantId AND event_type = 'sis.import.batch_applied' AND aggregate_id = :op")
                .param("op", operation).query(String.class).list());
    }

    private <T> T read(UUID tenant, Supplier<T> work) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).read(work::get));
    }
}
