package com.mysms.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.imports.ImportBatch;
import com.mysms.core.imports.ImportBatchResult;
import com.mysms.core.testkit.GrpcParity;
import com.mysms.core.testkit.GrpcParity.Answer;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.InboxProcessor;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.platform.PlatformCalls.Reply;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.events.PlatformImportHandler;
import com.mysms.platform.validation.CsvReader;
import com.mysms.platform.v1.PlatformProto;
import io.grpc.ManagedChannel;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Bulk imports end to end on real PostgreSQL, a real Google Cloud Storage emulator and the real gRPC server: the
 * template file, the template check of an uploaded CSV file (dry run), the sealed batches a run publishes to the
 * owning module, the owner's answers adding up to the operation's result (exactly once, misfits rejected), the
 * Idempotency-Key, the refusals (permission and scope, institution, file state, type and size) and tenant isolation;
 * gRPC answers like REST.
 */
@SpringBootTest(classes = PlatformApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=PlatformImportsComponentTest")
class PlatformImportsComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final GcsEmulator GCS = GcsEmulator.withBucket("mysms-imports");
    private static final UUID TENANT = UUID.fromString("0190b0a0-0000-7000-8000-0000000f5001");
    private static final UUID OTHER_TENANT = UUID.fromString("0190b0a0-0000-7000-8000-0000000f5002");
    private static final UUID SCHOOL = UUID.fromString("0190b0a0-0000-7000-8000-0000000f5003");
    private static final UUID OTHER_SCHOOL = UUID.fromString("0190b0a0-0000-7000-8000-0000000f5004");
    private static final String[] IMPORTER = {"platform.file_object.create_upload_url",
        "platform.file_object.complete_upload", "platform.file_object.get_download_url",
        "platform.tenant.get_import_template", "platform.tenant.start_import", "platform.operation.read",
        "platform.operation.cancel_operation", "sis.student.create", "sis.guardian.create"};
    private static final String STUDENTS_HEADER = "personId,admissionNumber,admissionDate";
    private static final String BATCH_REQUESTED = """
            SELECT payload FROM platform.outbox_event
             WHERE tenant_id = :tenantId AND event_type = 'platform.import.batch_requested' AND aggregate_id = :op
             ORDER BY id
            """;
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(PlatformDatabaseConfig.DATABASE)
    private ModuleDatabase database;


    @Autowired
    private PlatformImportHandler imports;

    @Autowired
    private FieldCipher cipher;

    @Autowired
    private JsonMapper json;

    private PlatformCalls http;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "platform", "master", "org");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        GCS.register(registry);
    }

    @AfterAll
    static void stop() {
        if (channel != null) {
            channel.shutdownNow();
        }
        GCS.close();
    }

    @BeforeEach
    void tenants() {
        http = new PlatformCalls(TOKENS, () -> port, database);
        http.tenant(TENANT, "imports-a");
        http.tenant(OTHER_TENANT, "imports-b");
    }

    @Test
    void theTemplateDescribesEveryColumnAndIsStoredOnce() throws Exception {
        String token = http.token(TENANT, UUID.randomUUID(), List.of(SCHOOL.toString()), IMPORTER);
        Reply link = http.call("GET", "/v1/platform/imports/templates/students", token, null);
        assertThat(link.status()).as(link.text()).isEqualTo(200);
        assertThat(link.header("Cache-Control")).contains("no-store");
        byte[] bytes = GCS.get(link.body().path("url").asString());
        List<List<String>> records = CsvReader.read(bytes);
        assertThat(records).contains(List.of("# personId: required; an id (UUID). The student's person record (people)."),
                List.of("# admissionNumber: optional; text of at most 30 characters. Admission number, unique in the "
                        + "institution."));
        assertThat(records.get(records.size() - 1)).containsExactly("personId", "admissionNumber", "admissionDate",
                "joiningDate", "studentCategoryId", "governmentStudentId", "bonafideCertificateNumber");

        Answer overGrpc = grpc("GetImportTemplate", JsonMapper.builder().build().createObjectNode()
                .put("entityType", "students"), token);
        assertThat(overGrpc.status()).as(overGrpc.code()).isEqualTo(200);
        assertThat(GCS.get(overGrpc.body().path("result").path("url").asString())).isEqualTo(bytes);
        assertThat(http.query(TENANT, "SELECT count(*) FROM platform.file_object WHERE tenant_id = :tenantId "
                + "AND file_name = 'import-template-students.csv'", Map.of(), Long.class))
                .as("the same template is kept once per user").isOne();

        // the template is a valid import file: its instruction lines are skipped, it holds no row
        Reply dry = start(token, "students", SCHOOL, upload(token, "template.csv", bytes), true, null);
        assertThat(dry.status()).as(dry.text()).isEqualTo(202);
        assertThat(dry.body().path("result").path("rowCount").asInt()).isZero();
        assertThat(dry.body().path("result").path("errorCount").asInt()).isZero();

        assertThat(http.call("GET", "/v1/platform/imports/templates/teachers", token, null).status()).isEqualTo(404);
        Reply unavailable = http.call("GET", "/v1/platform/imports/templates/lms_course_package", token, null);
        assertThat(unavailable.status()).isEqualTo(422);
        assertThat(unavailable.body().path("code").asString()).isEqualTo("IMPORT_TYPE_UNAVAILABLE");
    }

    @Test
    void aDryRunChecksEveryRowAndSendsNothing() throws Exception {
        String token = http.token(TENANT, UUID.randomUUID(), List.of(SCHOOL.toString()), IMPORTER);
        String csv = "﻿" + STUDENTS_HEADER + "\r\n"
                + UUID.randomUUID() + ",A-1,2026-04-01\r\n"
                + ",A-2,2026-04-01\r\n"
                + UUID.randomUUID() + ",A-3,01/04/2026\r\n"
                + ",,\r\n"
                + "# a note the importer left\r\n"
                + UUID.randomUUID() + ",\"" + "x".repeat(31) + "\",\r\n";
        String file = upload(token, "students.csv", csv.getBytes(StandardCharsets.UTF_8));
        Reply dry = start(token, "students", SCHOOL, file, true, null);
        assertThat(dry.status()).as(dry.text()).isEqualTo(202);
        JsonNode operation = dry.body();
        assertThat(operation.path("status").asString()).isEqualTo("succeeded");
        assertThat(operation.path("module").asString()).isEqualTo("platform");
        assertThat(operation.path("kind").asString()).isEqualTo("platform.StartImport");
        assertThat(operation.path("progressPercent").asInt()).isEqualTo(100);
        JsonNode result = operation.path("result");
        assertThat(result.path("entityType").asString()).isEqualTo("students");
        assertThat(result.path("institutionId").asString()).isEqualTo(SCHOOL.toString());
        assertThat(result.path("dryRun").asBoolean()).isTrue();
        assertThat(result.path("rowCount").asInt()).isEqualTo(4);
        assertThat(result.path("createdCount").asInt()).isZero();
        assertThat(result.path("errorCount").asInt()).isEqualTo(3);
        assertThat(errors(result)).containsExactly("3 personId required", "4 admissionDate a date as yyyy-mm-dd",
                "7 admissionNumber at most 30 characters");
        String op = operation.path("operationId").asString();
        assertThat(batchEvents(TENANT, op)).as("a dry run sends nothing").isEmpty();

        Reply read = http.call("GET", "/v1/platform/operations/" + op, token, null);
        assertThat(read.body().path("result")).isEqualTo(result);

        // the same dry run over gRPC answers the same result
        ObjectNode request = JsonMapper.builder().build().createObjectNode().put("entityType", "ENTITY_TYPE_STUDENTS")
                .put("fileId", file).put("institutionId", SCHOOL.toString()).put("dryRun", true);
        Answer overGrpc = grpc("StartImport", request, token);
        assertThat(overGrpc.status()).as(overGrpc.code()).isEqualTo(200);
        assertThat(overGrpc.body().path("result").path("status").asString()).isEqualTo("succeeded");
        JsonNode grpcResult = overGrpc.body().path("result").path("result");
        assertThat(errors(grpcResult)).isEqualTo(errors(result));
        assertThat(grpcResult.path("rowCount").asInt()).isEqualTo(4);
        assertThat(grpcResult.path("entityType").asString()).isEqualTo("students");

        // the header must fit the template; a file that is no UTF-8 text is one error
        String wrongHeader = upload(token, "wrong.csv", "admissionNumber,nickname\r\nA-9,Bee\r\n"
                .getBytes(StandardCharsets.UTF_8));
        assertThat(errors(start(token, "students", SCHOOL, wrongHeader, true, null).body().path("result")))
                .containsExactly("1 nickname not a column of the students template",
                        "1 personId a required column is missing");
        String latin1 = upload(token, "latin.csv", new byte[] {'p', ',', (byte) 0xE9, '\n'});
        assertThat(errors(start(token, "students", SCHOOL, latin1, false, null).body().path("result")))
                .containsExactly("1 - the file is not UTF-8 text; save it as CSV UTF-8");
    }

    @Test
    void aRunSendsSealedBatchesAndTheOwnersAnswersFinishIt() throws Exception {
        UUID user = UUID.randomUUID();
        String token = http.token(TENANT, user, List.of(SCHOOL.toString()), IMPORTER);
        StringBuilder csv = new StringBuilder(STUDENTS_HEADER + "\n");
        for (int i = 1; i <= 450; i++) {
            csv.append(UUID.randomUUID()).append(",ADM-").append(String.format("%04d", i)).append(",2026-04-01\n");
            if (i == 300) {
                csv.append("not-an-id,ADM-X,2026-04-01\n");
            }
        }
        String file = upload(token, "students.csv", csv.toString().getBytes(StandardCharsets.UTF_8));
        String key = UUID.randomUUID().toString();
        Reply started = start(token, "students", SCHOOL, file, false, key);
        assertThat(started.status()).as(started.text()).isEqualTo(202);
        assertThat(started.body().path("status").asString()).isEqualTo("running");
        assertThat(started.body().path("progressPercent").asInt()).isZero();
        assertThat(started.body().path("result").path("rowCount").asInt()).isEqualTo(451);
        assertThat(errors(started.body().path("result"))).containsExactly("302 personId an id (UUID)");
        UUID op = UUID.fromString(started.body().path("operationId").asString());

        Reply replayed = start(token, "students", SCHOOL, file, false, key);
        assertThat(replayed.header("Idempotent-Replayed")).isEqualTo("true");
        assertThat(replayed.body().path("operationId").asString()).isEqualTo(op.toString());

        List<String> payloads = batchEvents(TENANT, op.toString());
        assertThat(payloads).as("one event per batch of 200, once").hasSize(3);
        assertThat(payloads).allSatisfy(payload -> assertThat(payload).doesNotContain("ADM-"));
        ImportBatch first = ImportBatch.fromPayload(payloads.get(0), cipher, json, TENANT);
        assertThat(first.operationId()).isEqualTo(op);
        assertThat(first.entityType()).isEqualTo("students");
        assertThat(first.institutionId()).contains(SCHOOL);
        assertThat(first.userId()).isEqualTo(user);
        assertThat(first.tenantWide()).isFalse();
        assertThat(first.batchNumber()).isEqualTo(1);
        assertThat(first.rows()).hasSize(200);
        assertThat(first.rows().get(0).rowNumber()).isEqualTo(2);
        assertThat(first.rows().get(0).values()).containsEntry("admissionNumber", "ADM-0001")
                .containsEntry("admissionDate", "2026-04-01");
        assertThatThrownBy(() -> ImportBatch.fromPayload(payloads.get(0), cipher, json, OTHER_TENANT))
                .isInstanceOf(IllegalArgumentException.class);

        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        EventEnvelope one = answer("sis", op, 1, 200, List.of());
        assertThat(inbox.deliver(one, imports::apply)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
        assertThat(inbox.deliver(one, imports::apply)).isEqualTo(InboxProcessor.Outcome.DUPLICATE);
        assertThat(inbox.deliver(answer("sis", op, 1, 200, List.of()), event -> assertThat(imports.apply(event))
                .as("a second answer to a batch changes nothing").isFalse()))
                .isEqualTo(InboxProcessor.Outcome.PROCESSED);
        Reply running = http.call("GET", "/v1/platform/operations/" + op, token, null);
        assertThat(running.body().path("status").asString()).isEqualTo("running");
        assertThat(running.body().path("progressPercent").asInt()).isEqualTo(33);
        Reply cancel = http.call("POST", "/v1/platform/operations/" + op + ":cancel", token, "{}");
        assertThat(cancel.status()).as("an import's rows are with their owner").isEqualTo(409);

        for (EventEnvelope misfit : List.of(answer("hr", op, 2, 1, List.of()), answer("sis", op, 4, 1, List.of()),
                answer("sis", op, 2, 400, List.of()), answer("sis", UUID.randomUUID(), 2, 1, List.of()))) {
            assertThatThrownBy(() -> inbox.deliver(misfit, imports::apply))
                    .isInstanceOf(RejectedEventException.class);
        }

        inbox.deliver(answer("sis", op, 2, 199, List.of(new ImportBatchResult.RowError(150, "admissionNumber",
                "A student with the same admission number already exists."))), imports::apply);
        inbox.deliver(answer("sis", op, 3, 50, List.of()), imports::apply);
        Reply done = http.call("GET", "/v1/platform/operations/" + op, token, null);
        assertThat(done.body().path("status").asString()).isEqualTo("succeeded");
        assertThat(done.body().path("progressPercent").asInt()).isEqualTo(100);
        assertThat(done.body().path("doneTime").isMissingNode()).isFalse();
        JsonNode result = done.body().path("result");
        assertThat(result.path("createdCount").asInt()).isEqualTo(449);
        assertThat(result.path("errorCount").asInt()).isEqualTo(2);
        assertThat(errors(result)).containsExactly(
                "150 admissionNumber A student with the same admission number already exists.",
                "302 personId an id (UUID)");

        // over gRPC a run is started alike (its own operation, its own batches)
        ObjectNode request = JsonMapper.builder().build().createObjectNode().put("entityType", "ENTITY_TYPE_STUDENTS")
                .put("fileId", file).put("institutionId", SCHOOL.toString())
                .put("requestId", UUID.randomUUID().toString());
        Answer overGrpc = grpc("StartImport", request, token);
        assertThat(overGrpc.status()).as(overGrpc.code()).isEqualTo(200);
        JsonNode grpcOperation = overGrpc.body().path("result");
        assertThat(grpcOperation.path("status").asString()).isEqualTo("running");
        assertThat(batchEvents(TENANT, grpcOperation.path("operationId").asString())).hasSize(3);

        // another tenant sees neither the operation nor its import
        String stranger = http.token(OTHER_TENANT, user, List.of("*"), IMPORTER);
        assertThat(http.call("GET", "/v1/platform/operations/" + op, stranger, null).status()).isEqualTo(404);
        assertThat(http.query(OTHER_TENANT, "SELECT count(*) FROM platform.import_job WHERE tenant_id = :tenantId",
                Map.of(), Long.class)).isZero();
    }

    @Test
    void anImportIsRefusedOutsideTheCallersRightsAndForUnfitFiles() throws Exception {
        UUID user = UUID.randomUUID();
        String token = http.token(TENANT, user, List.of(SCHOOL.toString()), IMPORTER);
        byte[] good = (STUDENTS_HEADER + "\n" + UUID.randomUUID() + ",A-1,\n").getBytes(StandardCharsets.UTF_8);
        String file = upload(token, "students.csv", good);

        String noCreate = http.token(TENANT, user, List.of(SCHOOL.toString()), "platform.tenant.start_import");
        assertThat(start(noCreate, "students", SCHOOL, file, true, null).status()).isEqualTo(403);
        assertThat(start(token, "students", OTHER_SCHOOL, file, true, null).status()).isEqualTo(403);
        Reply noSchool = start(token, "students", null, file, true, null);
        assertThat(noSchool.status()).isEqualTo(422);
        assertThat(noSchool.body().path("errors").get(0).path("field").asString()).isEqualTo("institutionId");
        assertThat(start(token, "guardians", SCHOOL, file, true, null).status()).isEqualTo(422);
        assertThat(start(token, "guardians", null, file, true, null).status())
                .as("guardians belong to the tenant: a tenant-wide grant").isEqualTo(403);
        String tenantAdmin = http.token(TENANT, user, List.of("*"), IMPORTER);
        Reply guardians = start(tenantAdmin, "guardians", null, upload(tenantAdmin, "guardians.csv",
                ("personId,profession\n" + UUID.randomUUID() + ",Teacher\n,pilot\n").getBytes(StandardCharsets.UTF_8)),
                true, null);
        assertThat(guardians.status()).as(guardians.text()).isEqualTo(202);
        assertThat(errors(guardians.body().path("result"))).containsExactly("3 personId required",
                "3 profession one of admin, business, central_government, state_government, private_sector, "
                        + "home_maker, teacher, librarian, other");
        Reply course = start(token, "lms_course_package", SCHOOL, file, true, null);
        assertThat(course.status()).isEqualTo(422);
        assertThat(course.body().path("code").asString()).isEqualTo("IMPORT_TYPE_UNAVAILABLE");

        String colleague = http.token(TENANT, UUID.randomUUID(), List.of(SCHOOL.toString()), IMPORTER);
        assertThat(start(colleague, "students", SCHOOL, file, true, null).status())
                .as("another user's upload is not found").isEqualTo(404);
        String elsewhere = http.token(OTHER_TENANT, user, List.of(SCHOOL.toString()), IMPORTER);
        assertThat(start(elsewhere, "students", SCHOOL, file, true, null).status()).as("another tenant's file")
                .isEqualTo(404);

        Reply pending = http.call("POST", "/v1/platform/files:createUploadUrl", token,
                "{\"fileName\":\"later.csv\",\"mimeType\":\"text/csv\",\"sizeBytes\":\"5\"}");
        assertThat(start(token, "students", SCHOOL, pending.body().path("fileId").asString(), true, null).status())
                .as("an upload that is not complete").isEqualTo(409);
        Reply text = start(token, "students", SCHOOL, upload(token, "students.txt", good), true, null);
        assertThat(text.status()).isEqualTo(422);
        assertThat(text.body().path("errors").get(0).path("code").asString()).isEqualTo("FILE_TYPE");

        StringBuilder huge = new StringBuilder(STUDENTS_HEADER + "\n");
        String row = UUID.randomUUID() + ",A,\n";
        huge.append(row.repeat(10_001));
        Reply tooMany = start(token, "students", SCHOOL, upload(token, "huge.csv",
                huge.toString().getBytes(StandardCharsets.UTF_8)), true, null);
        assertThat(tooMany.status()).isEqualTo(422);
        assertThat(tooMany.body().path("errors").get(0).path("code").asString()).isEqualTo("TOO_MANY_ROWS");
        assertThat(http.query(TENANT, "SELECT count(*) FROM platform.operation WHERE tenant_id = :tenantId "
                + "AND started_by_user_id = :user", Map.of("user", user), Long.class))
                .as("only the guardians' dry run became an operation").isOne();
    }

    // ------------------------------------------------------------------ helpers

    private Reply start(String token, String entityType, UUID institution, String fileId, boolean dryRun,
            String idempotencyKey) {
        ObjectNode body = PlatformCalls.JSON.createObjectNode().put("entityType", entityType).put("fileId", fileId)
                .put("dryRun", dryRun);
        if (institution != null) {
            body.put("institutionId", institution.toString());
        }
        return idempotencyKey == null ? http.call("POST", "/v1/platform/imports", token, body.toString())
                : http.call("POST", "/v1/platform/imports", token, body.toString(), "Idempotency-Key", idempotencyKey);
    }

    /** The client's upload of a file: signed URL, the PUT to the store, completion. */
    private String upload(String token, String name, byte[] bytes) throws Exception {
        String type = name.endsWith(".csv") ? "text/csv" : "text/plain";
        Reply target = http.call("POST", "/v1/platform/files:createUploadUrl", token, PlatformCalls.JSON
                .createObjectNode().put("fileName", name).put("mimeType", type)
                .put("sizeBytes", String.valueOf(bytes.length)).toString());
        assertThat(target.status()).as(target.text()).isEqualTo(200);
        assertThat(GCS.put(target.body(), bytes)).isEqualTo(200);
        String fileId = target.body().path("fileId").asString();
        Reply done = http.call("POST", "/v1/platform/files/" + fileId + ":complete", token, "{}");
        assertThat(done.status()).as(done.text()).isEqualTo(200);
        return fileId;
    }

    /** A module's answer to one batch, as its outbox would publish it. */
    private static EventEnvelope answer(String module, UUID operation, int batch, int created,
            List<ImportBatchResult.RowError> errors) {
        ImportBatchResult result = new ImportBatchResult(operation, "students", batch, created, errors);
        return new EventEnvelope(UUID.randomUUID(), module, ImportBatchResult.eventType(module), Optional.of(TENANT),
                module + ImportBatchResult.AGGREGATE_SUFFIX, operation, Instant.now(),
                PlatformCalls.JSON.writeValueAsString(result.toPayload()), Optional.of(PlatformCalls.TRACE));
    }

    /** The batch events of an import in the tenant's outbox, oldest first. */
    private List<String> batchEvents(UUID tenant, String operation) {
        return CallContextHolder.callAs(CallContext.anonymous(PlatformCalls.TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).read(() -> database.jdbc().sql(BATCH_REQUESTED)
                        .param("op", UUID.fromString(operation)).query(String.class).list()));
    }

    /** The listed row errors as "row field message" ("-" for the row as a whole). */
    private static List<String> errors(JsonNode result) {
        List<String> lines = new ArrayList<>();
        result.path("errors").forEach(e -> lines.add(e.path("rowNumber").asInt() + " "
                + e.path("field").asString("-") + " " + e.path("message").asString()));
        return lines;
    }

    private Answer grpc(String method, ObjectNode request, String token) throws Exception {
        if (channel == null) {
            channel = GrpcParity.channel("PlatformImportsComponentTest");
        }
        MethodDescriptor rpc = PlatformProto.getDescriptor().findServiceByName("TenantService")
                .findMethodByName(method);
        DynamicMessage.Builder builder = DynamicMessage.newBuilder(rpc.getInputType());
        JsonFormat.parser().merge(request.toString(), builder);
        return GrpcParity.call(channel, rpc, builder.build(), token);
    }
}
