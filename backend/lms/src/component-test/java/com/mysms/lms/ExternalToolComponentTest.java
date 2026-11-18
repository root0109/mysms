package com.mysms.lms;

import static com.mysms.lms.LmsFixtures.expect;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.lms.LmsFixtures.Reply;
import com.mysms.lms.config.LmsDatabaseConfig;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import tools.jackson.databind.JsonNode;

/**
 * LTI 1.1 over HTTP against real PostgreSQL: activating a paid tool with its licence key, the signed launch of a
 * resource link (checked here with an independent HMAC-SHA1 implementation), and the tool's grade coming back
 * through the OAuth-signed outcome endpoint into the gradebook. The outcome endpoint carries its own OAuth
 * Authorization header through the edge; everything else still needs a bearer token.
 */
@SpringBootTest(classes = LmsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExternalToolComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String OUTCOME_URL = "https://lms.example.com/v1/lms/integrations/lti:outcome";
    private static final String OUTCOME_PATH = "/v1/lms/integrations/lti:outcome";
    private static final String LAUNCH_URL = "https://tool.example.com/launch";
    private static final String SECRET = "tool-shared-secret";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(LmsDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private FieldCipher cipher;

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
        registry.add("mysms.lms.lti.outcome-url", () -> OUTCOME_URL);
    }

    @BeforeEach
    void school() {
        fx = new LmsFixtures(TOKENS, database, port);
        tenant = UUID.randomUUID();
        institution = UUID.randomUUID();
        admin = fx.admin(tenant);
    }

    // ---- seeding

    private UUID tool(boolean paid, boolean included) {
        UUID tool = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.external_tool (id, tenant_id, title, is_paid, is_included) "
                + "VALUES (:id, :tenantId, 'Tool', :paid, :included)", "id", tool, "paid", paid, "included", included);
        return tool;
    }

    private void setting(UUID tool, String key, String value) {
        fx.sql(tenant, "INSERT INTO lms.external_tool_setting (id, tenant_id, external_tool_id, setting_key, "
                        + "setting_value) VALUES (:id, :tenantId, :tool, :key, :value)",
                "id", UUID.randomUUID(), "tool", tool, "key", key, "value", value);
    }

    private void secret(UUID tool) {
        fx.sql(tenant, "INSERT INTO lms.external_tool_setting (id, tenant_id, external_tool_id, setting_key, "
                        + "secret_value_enc) VALUES (:id, :tenantId, :tool, 'shared_secret', :enc)",
                "id", UUID.randomUUID(), "tool", tool,
                "enc", cipher.encrypt(SECRET, tenant, "lms.external_tool_setting.secret_value_enc"));
    }

    /** A configured tool with a resource and a link; answers {tool, resource, link}. */
    private UUID[] configured() {
        UUID tool = tool(false, false);
        setting(tool, "launch_url", LAUNCH_URL);
        setting(tool, "consumer_key", "mysms-key");
        secret(tool);
        UUID resource = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.external_tool_resource (id, tenant_id, institution_id, title) "
                + "VALUES (:id, :tenantId, :institution, 'Resource')", "id", resource, "institution", institution);
        UUID link = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.external_tool_link (id, tenant_id, external_tool_resource_id, "
                        + "external_tool_id, link_ref, link_title) VALUES (:id, :tenantId, :resource, :tool, 'ref-1', "
                        + "'Unit 1')", "id", link, "resource", resource, "tool", tool);
        return new UUID[] {tool, resource, link};
    }

    /** A student of the cohort holding the launch permission. */
    private String studentOf(UUID cohort, UUID user) {
        fx.sql(tenant, "INSERT INTO lms.cohort_enrollment (id, tenant_id, cohort_id, user_id) "
                + "VALUES (:id, :tenantId, :cohort, :user)", "id", UUID.randomUUID(), "cohort", cohort, "user", user);
        return fx.user(user, tenant, institution, "lms.external_tool_link.launch_external_tool");
    }

    /** A teacher of the cohort holding the launch permission. */
    private String teacherOf(UUID cohort) {
        UUID user = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.cohort_teacher (id, tenant_id, cohort_id, user_id) "
                + "VALUES (:id, :tenantId, :cohort, :user)", "id", UUID.randomUUID(), "cohort", cohort, "user", user);
        return fx.user(user, tenant, institution, "lms.external_tool_link.launch_external_tool");
    }

    private static String placed(UUID item) {
        return "{\"contentItemId\":\"" + item + "\"}";
    }

    private String launchPath(UUID resource, UUID link) {
        return "/v1/lms/external-tool-resources/" + resource + "/links/" + link + ":launch";
    }

    // ---- activation

    @Test
    void aPaidToolActivatesWithItsLicenceKeyOnly() throws Exception {
        UUID tool = tool(true, false);
        setting(tool, "activation_key_sha256", sha256("LICENCE-1"));
        String path = "/v1/lms/external-tools/" + tool + ":activate";
        expect(fx.call("POST", path, admin, "{\"activationKey\":\"WRONG\"}"), 422);
        expect(fx.call("POST", path, fx.member(tenant, institution, "lms.external_tool.activate_external_tool"),
                "{\"activationKey\":\"LICENCE-1\"}"), 403);
        Reply activated = expect(fx.call("POST", path, admin, "{\"activationKey\":\"LICENCE-1\"}"), 200);
        assertThat(activated.body().path("isIncluded").asBoolean()).isTrue();
        expect(fx.call("POST", path, admin, "{\"activationKey\":\"LICENCE-1\"}"), 409);
        assertThat(fx.value(tenant, "SELECT setting_value FROM lms.external_tool_setting WHERE external_tool_id = "
                + ":tool AND setting_key = 'activated_at'", String.class, "tool", tool)).isNotBlank();
    }

    @Test
    void aFreeToolNeedsNoLicenceAndAnUnknownToolIs404() throws Exception {
        UUID tool = tool(false, false);
        expect(fx.call("POST", "/v1/lms/external-tools/" + tool + ":activate", admin,
                "{\"activationKey\":\"ANY\"}"), 409);
        expect(fx.call("POST", "/v1/lms/external-tools/" + UUID.randomUUID() + ":activate", admin,
                "{\"activationKey\":\"ANY\"}"), 404);
    }

    // ---- launches

    @Test
    void aLaunchIsSignedSoTheToolCanVerifyIt() throws Exception {
        UUID[] ids = configured();
        UUID cohort = fx.cohort(tenant, institution);
        UUID item = fx.contentItem(tenant, cohort, "external_tool", "external_tool_resource_id", ids[1], null);
        String student = studentOf(cohort, UUID.randomUUID());
        Reply launch = expect(fx.call("POST", launchPath(ids[1], ids[2]), student, placed(item)), 200);
        assertThat(launch.body().path("launchUrl").asString()).isEqualTo(LAUNCH_URL);
        assertThat(launch.body().path("method").asString()).isEqualTo("POST");
        JsonNode form = launch.body().path("formFields");
        assertThat(form.path("lti_message_type").asString()).isEqualTo("basic-lti-launch-request");
        assertThat(form.path("lti_version").asString()).isEqualTo("LTI-1p0");
        assertThat(form.path("resource_link_id").asString()).isEqualTo("ref-1");
        assertThat(form.path("roles").asString()).isEqualTo("Learner");
        assertThat(form.path("oauth_consumer_key").asString()).isEqualTo("mysms-key");
        Map<String, String> fields = new TreeMap<>();
        form.properties().forEach(e -> fields.put(e.getKey(), e.getValue().asString()));
        assertThat(fields.remove("oauth_signature"))
                .isEqualTo(hmacSha1Signature("POST", LAUNCH_URL, fields, SECRET));
        Reply asTeacher = expect(fx.call("POST", launchPath(ids[1], ids[2]), teacherOf(cohort), placed(item)), 200);
        assertThat(asTeacher.body().path("formFields").path("roles").asString()).isEqualTo("Instructor");
    }

    @Test
    void onlyTheCohortsMembersLaunchAndOnlyItsTeachersAsInstructor() throws Exception {
        UUID[] ids = configured();
        UUID cohort = fx.cohort(tenant, institution);
        UUID item = fx.contentItem(tenant, cohort, "external_tool", "external_tool_resource_id", ids[1], null);
        String outsider = fx.member(tenant, institution, "lms.external_tool_link.launch_external_tool");
        expect(fx.call("POST", launchPath(ids[1], ids[2]), outsider, "{}"), 403);
        expect(fx.call("POST", launchPath(ids[1], ids[2]), outsider, placed(item)), 403);
        String grader = fx.member(tenant, institution, "lms.external_tool_link.launch_external_tool",
                "lms.gradebook_entry.grade_entry");
        expect(fx.call("POST", launchPath(ids[1], ids[2]), grader, placed(item)), 403);
        String student = studentOf(cohort, UUID.randomUUID());
        expect(fx.call("POST", launchPath(ids[1], ids[2]), student, "{}"), 403);
        String configurer = fx.member(tenant, institution, "lms.external_tool_link.launch_external_tool",
                "lms.external_tool_link.update");
        Reply tried = expect(fx.call("POST", launchPath(ids[1], ids[2]), configurer, "{}"), 200);
        assertThat(tried.body().path("formFields").path("roles").asString()).isEqualTo("Learner");
        assertThat(tried.body().path("formFields").has("context_id")).isFalse();
    }

    @Test
    void aLaunchRefusesWhatItMust() throws Exception {
        UUID[] ids = configured();
        UUID cohort = fx.cohort(tenant, institution);
        UUID item = fx.contentItem(tenant, cohort, "external_tool", "external_tool_resource_id", ids[1], null);
        String student = studentOf(cohort, UUID.randomUUID());
        expect(fx.call("POST", launchPath(ids[1], UUID.randomUUID()), student, placed(item)), 404);
        expect(fx.call("POST", launchPath(ids[1], ids[2]), fx.member(tenant, institution, "lms.ebook.read"), "{}"),
                403);
        expect(fx.call("POST", launchPath(ids[1], ids[2]), fx.admin(UUID.randomUUID()), "{}"), 404);
        // enabled for another institution only
        fx.sql(tenant, "INSERT INTO lms.external_tool_institution (id, tenant_id, external_tool_id, institution_id) "
                + "VALUES (:id, :tenantId, :tool, :institution)", "id", UUID.randomUUID(), "tool", ids[0],
                "institution", UUID.randomUUID());
        expect(fx.call("POST", launchPath(ids[1], ids[2]), student, placed(item)), 403);
    }

    @Test
    void anUnconfiguredToolIs503AndAPaidUnactivatedOneIs409() throws Exception {
        UUID tool = tool(false, false);
        UUID resource = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.external_tool_resource (id, tenant_id, institution_id, title) "
                + "VALUES (:id, :tenantId, :institution, 'Resource')", "id", resource, "institution", institution);
        UUID link = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.external_tool_link (id, tenant_id, external_tool_resource_id, "
                + "external_tool_id, link_ref) VALUES (:id, :tenantId, :resource, :tool, 'ref-2')",
                "id", link, "resource", resource, "tool", tool);
        Reply unconfigured = expect(fx.call("POST", launchPath(resource, link), admin, "{}"), 503);
        assertThat(unconfigured.body().path("code").asString()).isEqualTo("LTI_NOT_CONFIGURED");
        fx.sql(tenant, "UPDATE lms.external_tool SET is_paid = true WHERE id = :id", "id", tool);
        Reply unactivated = expect(fx.call("POST", launchPath(resource, link), admin, "{}"), 409);
        assertThat(unactivated.body().path("code").asString()).isEqualTo("TOOL_NOT_ACTIVATED");
    }

    @Test
    void aGradedPlacementGivesAStudentLaunchTheOutcomeService() throws Exception {
        UUID[] ids = configured();
        UUID cohort = fx.cohort(tenant, institution);
        UUID item = fx.contentItem(tenant, cohort, "external_tool", "external_tool_resource_id", ids[1], "20");
        String student = studentOf(cohort, UUID.randomUUID());
        Reply launch = expect(fx.call("POST", launchPath(ids[1], ids[2]), student,
                "{\"contentItemId\":\"" + item + "\"}"), 200);
        JsonNode form = launch.body().path("formFields");
        assertThat(form.path("context_id").asString()).isEqualTo(cohort.toString());
        assertThat(form.path("lis_outcome_service_url").asString()).isEqualTo(OUTCOME_URL);
        assertThat(form.path("lis_result_sourcedid").asString()).startsWith(tenant.toString() + ".");
        expect(fx.call("POST", launchPath(ids[1], ids[2]), student,
                "{\"contentItemId\":\"" + UUID.randomUUID() + "\"}"), 422);
        Reply asTeacher = expect(fx.call("POST", launchPath(ids[1], ids[2]), teacherOf(cohort),
                "{\"contentItemId\":\"" + item + "\"}"), 200);
        assertThat(asTeacher.body().path("formFields").has("lis_result_sourcedid")).isFalse();
    }

    // ---- outcomes

    @Test
    void aToolsSignedOutcomePostsTheGradeAsAutomaticPoints() throws Exception {
        UUID[] ids = configured();
        UUID cohort = fx.cohort(tenant, institution);
        UUID item = fx.contentItem(tenant, cohort, "external_tool", "external_tool_resource_id", ids[1], "20");
        UUID studentId = UUID.randomUUID();
        String student = studentOf(cohort, studentId);
        Reply launch = expect(fx.call("POST", launchPath(ids[1], ids[2]), student,
                "{\"contentItemId\":\"" + item + "\"}"), 200);
        String sourcedId = launch.body().path("formFields").path("lis_result_sourcedid").asString();

        byte[] body = outcomeXml(sourcedId, "0.85").getBytes(StandardCharsets.UTF_8);
        expect(fx.call("POST", OUTCOME_PATH, null, new String(body, StandardCharsets.UTF_8),
                "Authorization", oauthHeader(body, SECRET, "mysms-key"), "Content-Type", "application/xml"), 204);
        // 0.85 of 20 points, worked by hand: 17.00 automatic points on the student's graded entry
        assertThat(fx.value(tenant, "SELECT auto_points FROM lms.gradebook_entry WHERE content_item_id = :item "
                + "AND user_id = :user", BigDecimal.class, "item", item, "user", studentId))
                .isEqualByComparingTo("17.00");
        assertThat(fx.value(tenant, "SELECT status FROM lms.gradebook_entry WHERE content_item_id = :item",
                String.class, "item", item)).isEqualTo("graded");
    }

    @Test
    void anOutcomeWithoutAGoodSignatureBodyHashOrSourcedIdIsRefused() throws Exception {
        UUID[] ids = configured();
        UUID cohort = fx.cohort(tenant, institution);
        UUID item = fx.contentItem(tenant, cohort, "external_tool", "external_tool_resource_id", ids[1], "20");
        String student = studentOf(cohort, UUID.randomUUID());
        String sourcedId = expect(fx.call("POST", launchPath(ids[1], ids[2]), student,
                "{\"contentItemId\":\"" + item + "\"}"), 200)
                .body().path("formFields").path("lis_result_sourcedid").asString();
        byte[] body = outcomeXml(sourcedId, "0.85").getBytes(StandardCharsets.UTF_8);
        expect(fx.call("POST", OUTCOME_PATH, null, new String(body, StandardCharsets.UTF_8),
                "Authorization", oauthHeader(body, "wrong-secret", "mysms-key"), "Content-Type", "application/xml"),
                401);
        byte[] other = outcomeXml(sourcedId, "0.95").getBytes(StandardCharsets.UTF_8);
        String mismatchedHash = oauthHeader(body, SECRET, "mysms-key");
        expect(fx.call("POST", OUTCOME_PATH, null, new String(other, StandardCharsets.UTF_8),
                "Authorization", mismatchedHash, "Content-Type", "application/xml"), 401);
        byte[] forged = outcomeXml(tenant + ".forged-sourcedid", "0.85").getBytes(StandardCharsets.UTF_8);
        expect(fx.call("POST", OUTCOME_PATH, null, new String(forged, StandardCharsets.UTF_8),
                "Authorization", oauthHeader(forged, SECRET, "mysms-key"), "Content-Type", "application/xml"), 401);
        byte[] overOne = outcomeXml(sourcedId, "1.5").getBytes(StandardCharsets.UTF_8);
        expect(fx.call("POST", OUTCOME_PATH, null, new String(overOne, StandardCharsets.UTF_8),
                "Authorization", oauthHeader(overOne, SECRET, "mysms-key"), "Content-Type", "application/xml"), 422);
        assertThat(fx.value(tenant, "SELECT count(*) FROM lms.gradebook_entry WHERE content_item_id = :item",
                Integer.class, "item", item)).isZero();
    }

    // ---- an independent OAuth 1.0a signer

    private static String outcomeXml(String sourcedId, String score) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <imsx_POXEnvelopeRequest xmlns="http://www.imsglobal.org/services/ltiv1p1/xsd/imsoms_v1p0">
                  <imsx_POXBody><replaceResultRequest><resultRecord>
                    <sourcedGUID><sourcedId>%s</sourcedId></sourcedGUID>
                    <result><resultScore><textString>%s</textString></resultScore></result>
                  </resultRecord></replaceResultRequest></imsx_POXBody>
                </imsx_POXEnvelopeRequest>
                """.formatted(sourcedId, score);
    }

    private static String oauthHeader(byte[] body, String secret, String consumerKey) throws Exception {
        Map<String, String> params = new TreeMap<>();
        params.put("oauth_consumer_key", consumerKey);
        params.put("oauth_signature_method", "HMAC-SHA1");
        params.put("oauth_timestamp", Long.toString(System.currentTimeMillis() / 1000));
        params.put("oauth_nonce", UUID.randomUUID().toString().replace("-", ""));
        params.put("oauth_version", "1.0");
        params.put("oauth_body_hash", java.util.Base64.getEncoder()
                .encodeToString(MessageDigest.getInstance("SHA-1").digest(body)));
        String signature = hmacSha1Signature("POST", OUTCOME_URL, params, secret);
        StringBuilder header = new StringBuilder("OAuth ");
        params.forEach((k, v) -> header.append(k).append("=\"").append(percent(v)).append("\", "));
        return header.append("oauth_signature=\"").append(percent(signature)).append("\"").toString();
    }

    /** RFC 5849 HMAC-SHA1, written independently of the production class it checks. */
    private static String hmacSha1Signature(String method, String url, Map<String, String> params, String secret) {
        List<String> pairs = new ArrayList<>();
        new TreeMap<>(params).forEach((k, v) -> pairs.add(percent(k) + "=" + percent(v)));
        String base = method + "&" + percent(url) + "&" + percent(String.join("&", pairs));
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec((percent(secret) + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return java.util.Base64.getEncoder().encodeToString(mac.doFinal(base.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String percent(String value) {
        StringBuilder out = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xff;
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '.'
                    || c == '_' || c == '~') {
                out.append((char) c);
            } else {
                out.append('%').append(String.format("%02X", c));
            }
        }
        return out.toString();
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
