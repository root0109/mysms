package com.mysms.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.platform.PlatformCalls.Reply;
import com.mysms.platform.config.PlatformDatabaseConfig;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import tools.jackson.databind.node.ObjectNode;

/**
 * The file store end to end over HTTP, on real PostgreSQL and a real Google Cloud Storage emulator
 * (fsouza/fake-gcs-server in Testcontainers): the client's own PUT to the signed upload URL, completion against the
 * stored object, the download rules (uploader, owner permission, attachments, themes, other users and tenants,
 * guests), quota and plan limits (with two completions racing for the last bytes), copies, deletion, branding with the
 * logo URL, report output and global search over files. Providers this context leaves unconfigured answer 503.
 */
@SpringBootTest(classes = PlatformApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlatformFilesComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final GcsEmulator GCS = GcsEmulator.withBucket("mysms-files");
    private static final UUID TENANT = UUID.fromString("0190b0a0-0000-7000-8000-0000000f1001");
    private static final UUID OTHER_TENANT = UUID.fromString("0190b0a0-0000-7000-8000-0000000f1002");
    private static final UUID SCHOOL = UUID.fromString("0190b0a0-0000-7000-8000-0000000f1003");
    private static final String UPLOAD = "platform.file_object.create_upload_url";
    private static final String COMPLETE = "platform.file_object.complete_upload";
    private static final String DOWNLOAD = "platform.file_object.get_download_url";
    private static final String READ = "platform.file_object.read";
    private static final String DELETE = "platform.file_object.delete";
    private static final String COPY = "platform.file_object.copy_file";
    private static final String[] FILE_USER = {UPLOAD, COMPLETE, DOWNLOAD, READ, DELETE, COPY};

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(PlatformDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    private PlatformCalls http;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "platform", "master", "org");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        GCS.register(registry);
        registry.add("mysms.platform.files.max-size-bytes", () -> "1000");
    }

    @AfterAll
    static void stopStore() {
        GCS.close();
    }

    @BeforeEach
    void tenants() {
        http = new PlatformCalls(TOKENS, () -> port, database);
        http.tenant(TENANT, "files-a");
        http.tenant(OTHER_TENANT, "files-b");
    }

    @Test
    void aFileIsUploadedCompletedDownloadedAndDeleted() throws Exception {
        UUID uploader = UUID.randomUUID();
        String token = http.token(TENANT, uploader, List.of(SCHOOL.toString()), FILE_USER);
        byte[] bytes = "Term 1 marks".getBytes(StandardCharsets.UTF_8);
        Reply target = http.call("POST", "/v1/platform/files:createUploadUrl", token, uploadRequest("folder/Report card.pdf",
                "application/pdf", bytes.length, sha256(bytes), null, null));
        assertThat(target.status()).as(target.text()).isEqualTo(200);
        assertThat(target.header("Cache-Control")).contains("no-store");
        String fileId = target.body().path("fileId").asString();
        assertThat(target.body().path("uploadHeaders").path("Content-Type").asString()).isEqualTo("application/pdf");
        assertThat(target.body().path("uploadHeaders").path("x-goog-content-length-range").asString())
                .as("the declared size is signed").isEqualTo(bytes.length + "," + bytes.length);
        assertThat(target.body().path("uploadHeaders").path("x-goog-if-generation-match").asString())
                .as("the URL creates the object once, never overwrites it").isEqualTo("0");
        assertThat(target.body().path("uploadUrl").asString()).contains("x-goog-content-length-range")
                .contains("x-goog-if-generation-match");
        assertThat(target.body().path("uploadUrl").asString()).contains("X-Goog-Signature")
                .doesNotContain("Report");

        Reply early = http.call("POST", "/v1/platform/files/" + fileId + ":complete", token, "{}");
        assertThat(early.status()).as(early.text()).isEqualTo(409);
        assertThat(early.body().path("code").asString()).isEqualTo("UPLOAD_MISSING");

        assertThat(GCS.put(target.body(), bytes)).isEqualTo(200);
        String stranger = http.token(TENANT, UUID.randomUUID(), List.of(SCHOOL.toString()), FILE_USER);
        assertThat(http.call("POST", "/v1/platform/files/" + fileId + ":complete", stranger, "{}").status())
                .as("only the uploader completes").isEqualTo(404);
        Reply done = http.call("POST", "/v1/platform/files/" + fileId + ":complete", token, "{}");
        assertThat(done.status()).as(done.text()).isEqualTo(200);
        assertThat(done.body().path("status").asString()).isEqualTo("available");
        assertThat(done.body().path("fileName").asString()).isEqualTo("Report card.pdf");
        assertThat(done.body().path("fileKind").asString()).isEqualTo("pdf");
        assertThat(done.body().path("sizeBytes").asString()).isEqualTo(String.valueOf(bytes.length));
        assertThat(done.body().has("storageKey")).isFalse();
        assertThat(done.etag()).isEqualTo("\"2\"");
        Reply again = http.call("POST", "/v1/platform/files/" + fileId + ":complete", token, "{}");
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.body().path("code").asString()).isEqualTo("INVALID_STATE_TRANSITION");

        Reply link = http.call("GET", "/v1/platform/files/" + fileId + ":downloadUrl", token, null);
        assertThat(link.status()).as(link.text()).isEqualTo(200);
        assertThat(link.body().path("url").asString()).contains("generation=");
        assertThat(GCS.get(link.body().path("url").asString())).isEqualTo(bytes);
        assertThat(http.call("GET", "/v1/platform/files/" + fileId + ":downloadUrl", stranger, null).status())
                .as("another user without a right to the file").isEqualTo(404);
        assertThat(http.call("GET", "/v1/platform/files/" + fileId, stranger, null).status()).isEqualTo(404);
        assertThat(http.call("GET", "/v1/platform/files/" + fileId + ":downloadUrl", null, null).status())
                .as("guests resolve no tenant file").isEqualTo(404);
        String otherTenant = http.admin(OTHER_TENANT, UUID.randomUUID(), FILE_USER);
        assertThat(http.call("GET", "/v1/platform/files/" + fileId, otherTenant, null).status()).isEqualTo(404);
        assertThat(http.call("GET", "/v1/platform/files/" + fileId + ":downloadUrl", otherTenant, null).status())
                .isEqualTo(404);

        assertThat(ids(http.call("GET", "/v1/platform/files?size=500", token, null))).contains(fileId);
        assertThat(ids(http.call("GET", "/v1/platform/files?size=500", stranger, null))).doesNotContain(fileId);
        String admin = http.admin(TENANT, UUID.randomUUID(), FILE_USER);
        assertThat(ids(http.call("GET", "/v1/platform/files?size=500&filter="
                + PlatformCalls.encode("fileKind = \"pdf\""), admin, null))).contains(fileId);
        assertThat(http.call("GET", "/v1/platform/files/" + fileId, admin, null).status()).isEqualTo(200);

        assertThat(http.call("DELETE", "/v1/platform/files/" + fileId, stranger, null, "If-Match", "\"2\"").status())
                .isEqualTo(404);
        assertThat(http.call("DELETE", "/v1/platform/files/" + fileId, token, null).status()).isEqualTo(428);
        assertThat(http.call("DELETE", "/v1/platform/files/" + fileId, token, null, "If-Match", "\"9\"").status())
                .isEqualTo(412);
        assertThat(http.call("DELETE", "/v1/platform/files/" + fileId, token, null, "If-Match", "\"2\"").status())
                .isEqualTo(204);
        assertThat(http.call("GET", "/v1/platform/files/" + fileId, token, null).status()).isEqualTo(404);
        assertThat(GCS.get(link.body().path("url").asString())).as("the bytes left the store").isNull();
    }

    @Test
    void bytesWrittenToTheKeyAfterCompletionAreNeverServed() throws Exception {
        String token = http.token(TENANT, UUID.randomUUID(), List.of(SCHOOL.toString()), FILE_USER);
        byte[] bytes = "checked".getBytes(StandardCharsets.UTF_8);
        Reply target = http.call("POST", "/v1/platform/files:createUploadUrl", token,
                uploadRequest("notes.txt", "text/plain", bytes.length, sha256(bytes), null, null));
        String fileId = target.body().path("fileId").asString();
        assertThat(GCS.put(target.body(), bytes)).isEqualTo(200);
        assertThat(http.call("POST", "/v1/platform/files/" + fileId + ":complete", token, "{}").status())
                .isEqualTo(200);
        String generation = http.query(TENANT, "SELECT CAST(storage_generation AS text) FROM platform.file_object "
                + "WHERE id = :id", Map.of("id", UUID.fromString(fileId)), String.class);
        assertThat(generation).as("the generation checked is kept").isNotBlank();

        // the emulator does not enforce the signed "create only" header, so the URL can still overwrite here
        byte[] swapped = "swapped and much longer".getBytes(StandardCharsets.UTF_8);
        assertThat(GCS.put(target.body(), swapped)).isEqualTo(200);
        String url = http.call("GET", "/v1/platform/files/" + fileId + ":downloadUrl", token, null).body()
                .path("url").asString();
        assertThat(url).contains("generation=" + generation);
        assertThat(GCS.get(url)).as("the generation completed, never the bytes written later").isNotEqualTo(swapped);
    }

    @Test
    void completionChecksTheStoredBytesAgainstWhatWasDeclared() throws Exception {
        String token = http.token(TENANT, UUID.randomUUID(), List.of(SCHOOL.toString()), FILE_USER);
        byte[] bytes = "abcde".getBytes(StandardCharsets.UTF_8);
        Reply target = http.call("POST", "/v1/platform/files:createUploadUrl", token,
                uploadRequest("notes.txt", "text/plain", bytes.length, sha256(bytes), null, null));
        String fileId = target.body().path("fileId").asString();
        assertThat(GCS.put(target.body(), "abcdef".getBytes(StandardCharsets.UTF_8))).isEqualTo(200);
        Reply wrongSize = http.call("POST", "/v1/platform/files/" + fileId + ":complete", token, "{}");
        assertThat(wrongSize.status()).as(wrongSize.text()).isEqualTo(422);
        assertThat(wrongSize.text()).contains("sizeBytes");
        assertThat(http.call("POST", "/v1/platform/files/" + fileId + ":complete", token, "{}").body().path("code")
                .asString()).as("the mismatching object was removed").isEqualTo("UPLOAD_MISSING");
        assertThat(GCS.put(target.body(), "edcba".getBytes(StandardCharsets.UTF_8))).isEqualTo(200);
        Reply wrongBytes = http.call("POST", "/v1/platform/files/" + fileId + ":complete", token, "{}");
        assertThat(wrongBytes.status()).isEqualTo(422);
        assertThat(wrongBytes.text()).contains("checksumSha256");
        assertThat(GCS.put(target.body(), bytes)).isEqualTo(200);
        assertThat(http.call("POST", "/v1/platform/files/" + fileId + ":complete", token, "{}").status())
                .isEqualTo(200);

        Reply refused = http.call("POST", "/v1/platform/files:createUploadUrl", token,
                uploadRequest("run.exe", "application/octet-stream", 10, null, null, null));
        assertThat(refused.status()).isEqualTo(422);
        assertThat(refused.text()).contains("fileName");
        Reply tooBig = http.call("POST", "/v1/platform/files:createUploadUrl", token,
                uploadRequest("big.pdf", "application/pdf", 1001, null, null, null));
        assertThat(tooBig.status()).as("the deployment's default limit").isEqualTo(422);
        Reply lying = http.call("POST", "/v1/platform/files:createUploadUrl", token,
                uploadRequest("photo.jpg", "application/pdf", 10, null, null, null));
        assertThat(lying.status()).isEqualTo(422);
        assertThat(lying.text()).contains("mimeType");
        assertThat(http.call("POST", "/v1/platform/files:createUploadUrl", token,
                uploadRequest("a.pdf", "application/pdf", 0, null, null, null)).status()).isEqualTo(422);
        assertThat(http.call("POST", "/v1/platform/files:createUploadUrl",
                http.token(TENANT, UUID.randomUUID(), List.of(SCHOOL.toString()), COMPLETE),
                uploadRequest("a.pdf", "application/pdf", 3, null, null, null)).status()).isEqualTo(403);
    }

    @Test
    void theOwningModulesPermissionDecidesWhoAttachesAndReads() throws Exception {
        UUID course = UUID.randomUUID();
        String noRight = http.token(TENANT, UUID.randomUUID(), List.of(SCHOOL.toString()), FILE_USER);
        Reply refused = http.call("POST", "/v1/platform/files:createUploadUrl", noRight,
                uploadRequest("cover.png", "image/png", 4, null, "lms.course", course));
        assertThat(refused.status()).as(refused.text()).isEqualTo(403);
        assertThat(http.call("POST", "/v1/platform/files:createUploadUrl", noRight,
                uploadRequest("cover.png", "image/png", 4, null, "lms.course", null)).status()).isEqualTo(422);
        assertThat(http.call("POST", "/v1/platform/files:createUploadUrl", noRight,
                uploadRequest("cover.png", "image/png", 4, null, "Not a table", course)).status()).isEqualTo(422);

        String teacher = http.token(TENANT, UUID.randomUUID(), List.of(SCHOOL.toString()),
                append(FILE_USER, "lms.course.update"));
        String fileId = uploaded(teacher, "cover.png", "image/png", new byte[] {1, 2, 3, 4}, "lms.course", course);
        String reader = http.token(TENANT, UUID.randomUUID(), List.of(SCHOOL.toString()), DOWNLOAD, READ,
                "lms.course.read");
        assertThat(http.call("GET", "/v1/platform/files/" + fileId + ":downloadUrl", reader, null).status())
                .as("the owner's read permission").isEqualTo(200);
        String attachedReader = http.token(TENANT, UUID.randomUUID(), List.of(SCHOOL.toString()), DOWNLOAD,
                "assessment.homework_submission.read");
        assertThat(http.call("GET", "/v1/platform/files/" + fileId + ":downloadUrl", attachedReader, null).status())
                .isEqualTo(404);
        http.sql(TENANT, "INSERT INTO platform.entity_attachment (id, tenant_id, entity_type, entity_id, file_id, "
                + "row_version) VALUES (:id, :tenant, 'assessment.homework_submission', :entity, :file, 1)",
                Map.of("id", UUID.randomUUID(), "tenant", TENANT, "entity", UUID.randomUUID(),
                        "file", UUID.fromString(fileId)));
        assertThat(http.call("GET", "/v1/platform/files/" + fileId + ":downloadUrl", attachedReader, null).status())
                .as("the read permission of a record type the file is attached to").isEqualTo(200);
        Reply blocked = http.call("DELETE", "/v1/platform/files/" + fileId, teacher, null, "If-Match", "\"2\"");
        assertThat(blocked.status()).as("still attached").isEqualTo(409);
    }

    @Test
    void quotaAndPlanLimitsHoldEvenWhenTwoCompletionsRace() throws Exception {
        UUID user = UUID.randomUUID();
        String token = http.token(TENANT, user, List.of(SCHOOL.toString()), append(FILE_USER,
                "platform.storage_quota.get_my_storage_usage"));
        assertThat(http.call("GET", "/v1/platform/me/storage", token, null).status()).isEqualTo(404);
        UUID quota = UUID.randomUUID();
        http.sql(TENANT, "INSERT INTO platform.storage_quota (id, tenant_id, user_id, quota_bytes, row_version) "
                + "VALUES (:id, :tenant, :user, 15, 1)", Map.of("id", quota, "tenant", TENANT, "user", user));
        List<JsonNode> targets = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Reply target = http.call("POST", "/v1/platform/files:createUploadUrl", token,
                    uploadRequest("part" + i + ".txt", "text/plain", 10, null, null, null));
            assertThat(target.status()).as(target.text()).isEqualTo(200);
            assertThat(GCS.put(target.body(), "0123456789".getBytes(StandardCharsets.UTF_8))).isEqualTo(200);
            targets.add(target.body());
        }
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Reply>> completions = new ArrayList<>();
            for (JsonNode target : targets) {
                Callable<Reply> complete = () -> http.call("POST", "/v1/platform/files/"
                        + target.path("fileId").asString() + ":complete", token, "{}");
                completions.add(pool.submit(complete));
            }
            List<Integer> statuses = new ArrayList<>();
            for (Future<Reply> completion : completions) {
                statuses.add(completion.get().status());
            }
            assertThat(statuses).as("15 bytes of quota hold one 10-byte file").containsExactlyInAnyOrder(200, 409);
        } finally {
            pool.shutdown();
        }
        Reply mine = http.call("GET", "/v1/platform/me/storage", token, null);
        assertThat(mine.status()).as(mine.text()).isEqualTo(200);
        assertThat(mine.body().path("usedBytes").asString()).isEqualTo("10");
        Reply full = http.call("POST", "/v1/platform/files:createUploadUrl", token,
                uploadRequest("more.txt", "text/plain", 6, null, null, null));
        assertThat(full.status()).isEqualTo(409);
        assertThat(full.body().path("code").asString()).isEqualTo("QUOTA_EXCEEDED");

        long live = http.query(TENANT, "SELECT count(*) FROM platform.file_object WHERE tenant_id = :tenant "
                + "AND deleted_at IS NULL", Map.of("tenant", TENANT), Long.class);
        http.sql(TENANT, "INSERT INTO platform.tenant_subscription (id, tenant_id, requested_by_user_id, status, "
                + "max_files, end_date, row_version) VALUES (:id, :tenant, :user, 'active', :max, current_date + 30, 1)",
                Map.of("id", UUID.randomUUID(), "tenant", TENANT, "user", user, "max", (int) live));
        String someoneElse = http.token(TENANT, UUID.randomUUID(), List.of(SCHOOL.toString()), FILE_USER);
        Reply limit = http.call("POST", "/v1/platform/files:createUploadUrl", someoneElse,
                uploadRequest("one-more.txt", "text/plain", 3, null, null, null));
        assertThat(limit.status()).as(limit.text()).isEqualTo(409);
        assertThat(limit.body().path("code").asString()).isEqualTo("PLAN_LIMIT_REACHED");
        http.sql(TENANT, "UPDATE platform.tenant_subscription SET status = 'cancelled' WHERE tenant_id = :tenant",
                Map.of("tenant", TENANT));
    }

    @Test
    void aCopyIsANewFileOfTheCaller() throws Exception {
        String owner = http.token(TENANT, UUID.randomUUID(), List.of(SCHOOL.toString()), FILE_USER);
        byte[] bytes = "lesson plan".getBytes(StandardCharsets.UTF_8);
        String fileId = uploaded(owner, "plan.docx", "application/vnd.openxmlformats-officedocument"
                + ".wordprocessingml.document", bytes, null, null);
        String stranger = http.token(TENANT, UUID.randomUUID(), List.of(SCHOOL.toString()), FILE_USER);
        assertThat(http.call("POST", "/v1/platform/files/" + fileId + ":copy", stranger, "{}").status())
                .isEqualTo(404);
        Reply copy = http.call("POST", "/v1/platform/files/" + fileId + ":copy", owner, "{}");
        assertThat(copy.status()).as(copy.text()).isEqualTo(200);
        String copyId = copy.body().path("id").asString();
        assertThat(copyId).isNotEqualTo(fileId);
        assertThat(copy.body().path("status").asString()).isEqualTo("available");
        Reply link = http.call("GET", "/v1/platform/files/" + copyId + ":downloadUrl", owner, null);
        assertThat(GCS.get(link.body().path("url").asString())).isEqualTo(bytes);
        assertThat(http.call("POST", "/v1/platform/files/" + fileId + ":copy", owner,
                "{\"ownerEntityType\":\"lms.course\",\"ownerEntityId\":\"" + UUID.randomUUID() + "\"}").status())
                .as("attaching the copy needs the owner's write permission").isEqualTo(403);
        Reply pending = http.call("POST", "/v1/platform/files:createUploadUrl", owner,
                uploadRequest("draft.txt", "text/plain", 3, null, null, null));
        assertThat(http.call("POST", "/v1/platform/files/" + pending.body().path("fileId").asString() + ":copy",
                owner, "{}").status()).isEqualTo(409);
    }

    @Test
    void theDefaultThemeBrandsTheTenantsPublicPages() throws Exception {
        UUID adminUser = UUID.randomUUID();
        String admin = http.admin(TENANT, adminUser, append(FILE_USER, "platform.theme.create",
                "platform.theme.update", "platform.theme.delete"));
        byte[] logo = {(byte) 0x89, 'P', 'N', 'G'};
        String logoId = uploaded(admin, "logo.png", "image/png", logo, null, null);
        Reply first = http.call("POST", "/v1/platform/themes", admin, "{\"name\":\"Classic\",\"isDefault\":true,"
                + "\"logoFileId\":\"" + logoId + "\",\"tokens\":{\"color.primary\":\"#0f766e\"}}");
        assertThat(first.status()).as(first.text()).isEqualTo(201);
        assertThat(first.body().path("tokens").path("color.primary").asString()).isEqualTo("#0F766E");

        Reply branding = http.call("GET", "/v1/platform/branding?tenantCode=files-a", null, null);
        assertThat(branding.status()).as(branding.text()).isEqualTo(200);
        assertThat(branding.body().path("tenantName").asString()).isEqualTo("School group files-a");
        assertThat(branding.body().path("primaryColor").asString()).isEqualTo("#0F766E");
        assertThat(branding.body().path("locale").asString()).isEqualTo("hi");
        assertThat(branding.body().path("timezone").asString()).isEqualTo("Asia/Kolkata");
        assertThat(GCS.get(branding.body().path("logoFileUrl").asString())).isEqualTo(logo);
        assertThat(branding.body().propertyNames()).containsExactlyInAnyOrder("tenantName", "primaryColor",
                "logoFileUrl", "locale", "timezone");

        String anyone = http.token(TENANT, UUID.randomUUID(), List.of(SCHOOL.toString()), DOWNLOAD);
        assertThat(http.call("GET", "/v1/platform/files/" + logoId + ":downloadUrl", anyone, null).status())
                .as("a file a theme shows is the tenant's to see").isEqualTo(200);
        assertThat(http.call("DELETE", "/v1/platform/files/" + logoId, admin, null, "If-Match", "\"2\"").status())
                .as("still shown by a theme").isEqualTo(409);

        Reply second = http.call("POST", "/v1/platform/themes", admin, "{\"name\":\"Night\",\"isDefault\":true}");
        assertThat(second.status()).isEqualTo(201);
        Reply reread = http.call("GET", "/v1/platform/themes/" + first.body().path("id").asString(), anyone, null);
        assertThat(reread.body().path("isDefault").asBoolean()).as("one default per tenant").isFalse();
        Reply now = http.call("GET", "/v1/platform/branding?tenantCode=files-a", null, null);
        assertThat(now.body().has("logoFileUrl")).isFalse();
        assertThat(now.body().has("primaryColor")).isFalse();

        Reply pending = http.call("POST", "/v1/platform/files:createUploadUrl", admin,
                uploadRequest("thumb.png", "image/png", 3, null, null, null));
        Reply notUploaded = http.call("POST", "/v1/platform/themes", admin, "{\"name\":\"Broken\","
                + "\"thumbnailFileId\":\"" + pending.body().path("fileId").asString() + "\"}");
        assertThat(notUploaded.status()).as(notUploaded.text()).isEqualTo(422);
        assertThat(notUploaded.text()).contains("thumbnailFileId");
    }

    @Test
    void aReportIsAFileTheCallerDownloads() throws Exception {
        UUID adminUser = UUID.randomUUID();
        String admin = http.admin(TENANT, adminUser, DOWNLOAD, "platform.tenant.run_report",
                "platform.tenant.list_report_definitions", "platform.audit_log.read", READ,
                "platform.operation.read");
        UUID subject = UUID.randomUUID();
        http.sql(TENANT, "INSERT INTO platform.audit_log (id, tenant_id, log_name, description, event, subject_type, "
                + "subject_id, causer_type, causer_id, occurred_at) VALUES (:id, :tenant, 'sis', 'update sis.student', "
                + "'update', 'sis.student', :subject, 'user', :causer, TIMESTAMPTZ '2026-10-05 08:00:00+00')",
                Map.of("id", UUID.randomUUID(), "tenant", TENANT, "subject", subject, "causer", adminUser));
        Reply definitions = http.call("GET", "/v1/platform/reports", admin, null);
        assertThat(definitions.status()).as(definitions.text()).isEqualTo(200);
        assertThat(definitions.body().path("data").findValuesAsString("reportCode"))
                .containsExactly("platform.audit_log", "platform.file_object");

        Reply run = http.call("POST", "/v1/platform/reports:run", admin, "{\"reportCode\":\"platform.audit_log\","
                + "\"format\":\"csv\",\"parameters\":{\"from\":\"2026-10-01\",\"to\":\"2026-10-31\"}}");
        assertThat(run.status()).as(run.text()).isEqualTo(202);
        assertThat(run.body().path("status").asString()).isEqualTo("succeeded");
        assertThat(run.body().path("module").asString()).isEqualTo("platform");
        String output = run.body().path("resultFileId").asString();
        Reply operation = http.call("GET", "/v1/platform/operations/" + run.body().path("operationId").asString(),
                admin, null);
        assertThat(operation.body().path("resultFileId").asString()).isEqualTo(output);
        Reply link = http.call("GET", "/v1/platform/files/" + output + ":downloadUrl", admin, null);
        String csv = new String(GCS.get(link.body().path("url").asString()), StandardCharsets.UTF_8);
        assertThat(csv).startsWith("﻿occurredAt,event,subjectType,subjectId,causerType,causerId,description\r\n")
                .contains("update,sis.student," + subject + ",user," + adminUser + ",update sis.student");

        Reply files = http.call("POST", "/v1/platform/reports:run", admin,
                "{\"reportCode\":\"platform.file_object\",\"format\":\"csv\"}");
        assertThat(files.status()).as(files.text()).isEqualTo(202);
        Reply xlsx = http.call("POST", "/v1/platform/reports:run", admin,
                "{\"reportCode\":\"platform.file_object\",\"format\":\"xlsx\"}");
        assertThat(xlsx.status()).isEqualTo(422);
        Reply range = http.call("POST", "/v1/platform/reports:run", admin, "{\"reportCode\":\"platform.audit_log\","
                + "\"format\":\"csv\",\"parameters\":{\"from\":\"2026-10-31\",\"to\":\"2026-10-01\"}}");
        assertThat(range.status()).isEqualTo(422);
    }

    @Test
    void searchFindsTheFilesTheCallerMaySee() throws Exception {
        String owner = http.token(TENANT, UUID.randomUUID(), List.of(SCHOOL.toString()), append(FILE_USER,
                "platform.tenant.global_search"));
        String fileId = uploaded(owner, "Annual_day 100% photos.zip", "application/zip", new byte[] {7}, null, null);
        Reply found = http.call("GET", "/v1/platform/search?query=" + PlatformCalls.encode("day 100%"), owner, null);
        assertThat(found.status()).as(found.text()).isEqualTo(200);
        assertThat(found.body().path("data").findValuesAsString("entityId")).contains(fileId);
        assertThat(found.body().path("data").get(0).path("entityType").asString()).isEqualTo("platform.file_object");
        Reply wildcard = http.call("GET", "/v1/platform/search?query=" + PlatformCalls.encode("y_1"), owner, null);
        assertThat(wildcard.body().path("data").findValuesAsString("entityId")).as("_ is a character, not a wildcard")
                .doesNotContain(fileId);
        String other = http.token(TENANT, UUID.randomUUID(), List.of(SCHOOL.toString()),
                "platform.tenant.global_search");
        assertThat(http.call("GET", "/v1/platform/search?query=Annual", other, null).body().path("data")
                .findValuesAsString("entityId")).doesNotContain(fileId);
    }

    @Test
    void providersThisDeploymentLeavesOutAnswerNotConfigured() {
        String admin = http.admin(TENANT, UUID.randomUUID(), "platform.tenant_subscription.start_subscription_checkout",
                "platform.tenant.search_stock_images");
        Reply checkout = http.call("POST", "/v1/platform/tenant-subscriptions:checkout", admin,
                "{\"planId\":\"" + UUID.randomUUID() + "\"}", "Idempotency-Key", UUID.randomUUID().toString());
        assertThat(checkout.status()).as(checkout.text()).isEqualTo(503);
        assertThat(checkout.body().path("code").asString()).isEqualTo("NOT_CONFIGURED");
        Reply photos = http.call("GET", "/v1/platform/stock-images?query=library", admin, null);
        assertThat(photos.status()).isEqualTo(503);
        assertThat(photos.body().path("code").asString()).isEqualTo("NOT_CONFIGURED");
        Reply webhook = http.call("POST", "/v1/platform/tenant-subscriptions:webhook", null, "{}",
                "X-Razorpay-Signature", "00");
        assertThat(webhook.status()).isEqualTo(503);
        assertThat(webhook.body().path("code").asString()).isEqualTo("NOT_CONFIGURED");
    }

    // ------------------------------------------------------------------------------------------------- helpers

    private String uploaded(String token, String name, String type, byte[] bytes, String ownerType, UUID ownerId)
            throws Exception {
        Reply target = http.call("POST", "/v1/platform/files:createUploadUrl", token,
                uploadRequest(name, type, bytes.length, null, ownerType, ownerId));
        assertThat(target.status()).as(target.text()).isEqualTo(200);
        assertThat(GCS.put(target.body(), bytes)).isEqualTo(200);
        String fileId = target.body().path("fileId").asString();
        Reply done = http.call("POST", "/v1/platform/files/" + fileId + ":complete", token, "{}");
        assertThat(done.status()).as(done.text()).isEqualTo(200);
        return fileId;
    }

    private static String uploadRequest(String name, String type, long size, String sha256, String ownerType,
            UUID ownerId) {
        ObjectNode body = PlatformCalls.JSON.createObjectNode().put("fileName", name).put("mimeType", type)
                .put("sizeBytes", String.valueOf(size));
        if (sha256 != null) {
            body.put("checksumSha256", sha256);
        }
        if (ownerType != null) {
            body.put("ownerEntityType", ownerType);
        }
        if (ownerId != null) {
            body.put("ownerEntityId", ownerId.toString());
        }
        return body.toString();
    }

    private static List<String> ids(Reply list) {
        assertThat(list.status()).as(list.text()).isEqualTo(200);
        return list.body().path("data").findValuesAsString("id");
    }

    private static String[] append(String[] permissions, String... more) {
        List<String> all = new ArrayList<>(List.of(permissions));
        all.addAll(List.of(more));
        return all.toArray(String[]::new);
    }

    private static String sha256(byte[] bytes) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
