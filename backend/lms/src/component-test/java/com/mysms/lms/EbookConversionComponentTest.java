package com.mysms.lms;

import static com.mysms.lms.LmsFixtures.expect;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.lms.LmsFixtures.Reply;
import com.mysms.lms.config.LmsDatabaseConfig;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * E-book conversion over HTTP against real PostgreSQL: ConvertEbook's refusals (no source document, unknown row,
 * no permission, a failed job surfaced and cleared) and the conversion service's signed callback recording a job's
 * outcome on the e-book that sent it. Neither CloudConvert nor the platform file store is running here, so a fresh
 * submit stops at the file store with 503 and nothing changes.
 */
@SpringBootTest(classes = LmsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EbookConversionComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String WEBHOOK = "/v1/lms/integrations/conversion/cloudconvert:webhook";
    private static final String CALLBACK_SECRET = "conversion-callback-secret";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(LmsDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    private LmsFixtures fx;
    private UUID tenant;
    private UUID institution;
    private String admin;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "lms");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.secrets.keys", () -> LmsFixtures.SECRETS);
        registry.add("mysms.secrets.active-key", () -> "test");
        registry.add("mysms.lms.conversion.provider", () -> "cloudconvert");
        registry.add("mysms.lms.conversion.api-url", () -> "https://api.cloudconvert.example.com");
        registry.add("mysms.lms.conversion.api-key", () -> "test-key");
        registry.add("mysms.lms.conversion.callback-secret", () -> CALLBACK_SECRET);
    }

    @BeforeEach
    void school() {
        fx = new LmsFixtures(TOKENS, database, port);
        tenant = UUID.randomUUID();
        institution = UUID.randomUUID();
        admin = fx.admin(tenant);
    }

    private UUID ebook(UUID sourceFileId, String jobRef) {
        UUID ebook = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.ebook (id, tenant_id, institution_id, title, source_file_id, "
                        + "conversion_job_ref) VALUES (:id, :tenantId, :institution, 'Algebra basics', :source, :job)",
                "id", ebook, "institution", institution, "source", sourceFileId, "job", jobRef);
        return ebook;
    }

    private Reply convert(UUID ebook, String token) throws Exception {
        return fx.call("POST", "/v1/lms/ebooks/" + ebook + ":convert", token, "{}");
    }

    private Reply callback(String body, String secret) throws Exception {
        long now = System.currentTimeMillis() / 1000;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of()
                .formatHex(mac.doFinal((now + "." + body).getBytes(StandardCharsets.UTF_8)));
        return fx.call("POST", WEBHOOK, null, body, "X-Signature", signature, "X-Timestamp", Long.toString(now));
    }

    private String finishedJob(String jobRef, UUID ebook) {
        return """
                {"event":"job.finished","job":{"id":"%s","status":"finished",
                 "tag":"mysms_tenant=%s;mysms_ebook=%s","tasks":[
                  {"operation":"export/url","status":"finished","result":{"files":[
                    {"url":"https://storage.cloudconvert.example.com/out/a.pdf"}]}}]}}
                """.formatted(jobRef, tenant, ebook);
    }

    @Test
    void convertRefusesWhatItMust() throws Exception {
        UUID withoutSource = ebook(null, null);
        expect(convert(withoutSource, admin), 409);
        expect(convert(UUID.randomUUID(), admin), 404);
        expect(convert(withoutSource, fx.member(tenant, institution, "lms.ebook.read")), 403);
        expect(convert(withoutSource, fx.admin(UUID.randomUUID())), 404);
        // a fresh submit reads the source from the platform file store, which is not running here
        expect(convert(ebook(UUID.randomUUID(), null), admin), 503);
    }

    @Test
    void aSignedCallbackRecordsTheOutcomeAndAFailureSurfacesOnTheNextConvert() throws Exception {
        UUID ebook = ebook(UUID.randomUUID(), "job-7");
        expect(callback(finishedJob("job-7", ebook), CALLBACK_SECRET), 204);
        assertThat(fx.value(tenant, "SELECT conversion_output_url FROM lms.ebook WHERE id = :id", String.class,
                "id", ebook)).isEqualTo("https://storage.cloudconvert.example.com/out/a.pdf");

        UUID failed = ebook(UUID.randomUUID(), "job-8");
        expect(callback("""
                {"event":"job.failed","job":{"id":"job-8","status":"error","tag":"mysms_tenant=%s;mysms_ebook=%s",
                 "tasks":[{"operation":"convert","status":"error","message":"The document is password protected."}]}}
                """.formatted(tenant, failed), CALLBACK_SECRET), 204);
        Reply refused = expect(convert(failed, admin), 409);
        assertThat(refused.body().path("code").asString()).isEqualTo("CONVERSION_FAILED");
        assertThat(refused.body().path("detail").asString()).contains("password protected");
        assertThat(fx.value(tenant, "SELECT conversion_error FROM lms.ebook WHERE id = :id", String.class,
                "id", failed)).as("surfacing the failure clears it, so the next call retries").isNull();
    }

    @Test
    void aCallbackWithoutAGoodSignatureJobOrProviderChangesNothing() throws Exception {
        UUID ebook = ebook(UUID.randomUUID(), "job-9");
        expect(callback(finishedJob("job-9", ebook), "wrong-secret"), 401);
        expect(callback(finishedJob("other-job", ebook), CALLBACK_SECRET), 404);
        expect(callback(finishedJob("job-9", UUID.randomUUID()), CALLBACK_SECRET), 404);
        expect(callback("{\"event\":\"job.finished\",\"job\":{\"id\":\"job-9\",\"status\":\"finished\","
                + "\"tag\":\"mysms_tenant=nope\",\"tasks\":[]}}", CALLBACK_SECRET), 401);
        expect(callback("{\"event\":\"user.updated\"}", CALLBACK_SECRET), 204);
        long now = System.currentTimeMillis() / 1000;
        expect(fx.call("POST", "/v1/lms/integrations/conversion/acme:webhook", null, "{}",
                "X-Signature", "x", "X-Timestamp", Long.toString(now)), 404);
        assertThat(fx.value(tenant, "SELECT conversion_output_url FROM lms.ebook WHERE id = :id", String.class,
                "id", ebook)).isNull();
    }
}
