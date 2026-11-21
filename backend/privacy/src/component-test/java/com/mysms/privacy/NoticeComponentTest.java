package com.mysms.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.testkit.TestDatabase;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Notices, their versions and translations against real PostgreSQL as the non-owner role: the publish workflow, the
 * content hash, frozen published versions, the tenant-wide write rule and tenant isolation.
 */
@SpringBootTest(classes = PrivacyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NoticeComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String NOTICES = "/v1/privacy/notices";
    private static final String[] NOTICE_PERMISSIONS = {"privacy.notice.read", "privacy.notice.create",
        "privacy.notice.update", "privacy.notice.delete", "privacy.notice_version.read", "privacy.notice_version.create",
        "privacy.notice_version.update", "privacy.notice_version.publish", "privacy.notice_translation.read",
        "privacy.notice_translation.create", "privacy.notice_translation.update", "privacy.notice_translation.delete"};
    private static final String BODY = "We publish photos of school events on the school website.";
    private static final String SUMMARY = "Your school may put photos of you on its website.";

    @LocalServerPort
    private int port;

    private PrivacyCalls calls;
    private String admin;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "privacy");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @BeforeEach
    void anAdministratorOfANewTenant() {
        calls = new PrivacyCalls(port, TOKENS);
        admin = calls.administrator(UUID.randomUUID(), NOTICE_PERMISSIONS);
    }

    private PrivacyCalls.Reply notice(String token, String code) throws Exception {
        return calls.post(NOTICES, token, Map.of("code", code, "title", "Photos and videos"));
    }

    private PrivacyCalls.Reply version(String noticeId, String... purposeCodes) throws Exception {
        return calls.post(NOTICES + "/" + noticeId + "/versions", admin, Map.of("purposeCodes", List.of(purposeCodes),
                "grievanceContact", Map.of("name", "Grievance Officer", "email", "grievance@school.example")));
    }

    private PrivacyCalls.Reply translation(String noticeId, String versionId, String language) throws Exception {
        return calls.post(translations(noticeId, versionId), admin,
                Map.of("language", language, "bodyMarkdown", BODY, "summaryForChildren", SUMMARY));
    }

    private PrivacyCalls.Reply publish(String noticeId, String versionId) throws Exception {
        return calls.post(versions(noticeId) + "/" + versionId + ":publish", admin, Map.of());
    }

    private static String versions(String noticeId) {
        return NOTICES + "/" + noticeId + "/versions";
    }

    private static String translations(String noticeId, String versionId) {
        return versions(noticeId) + "/" + versionId + "/translations";
    }

    @Test
    void aNoticeIsWrittenTranslatedAndPublished() throws Exception {
        PrivacyCalls.Reply notice = notice(admin, "community_photos");
        assertThat(notice.status()).as(notice.body().toString()).isEqualTo(201);
        assertThat(notice.etag()).isEqualTo("\"1\"");
        String noticeId = notice.text("id");

        PrivacyCalls.Reply draft = version(noticeId, "photo_video_publication");
        assertThat(draft.status()).as(draft.body().toString()).isEqualTo(201);
        assertThat(draft.text("status")).isEqualTo("draft");
        assertThat(draft.body().path("version").asInt()).isEqualTo(1);
        assertThat(draft.body().path("requiresReconsent").asBoolean()).isFalse();
        assertThat(draft.body().path("grievanceContact").path("email").asString()).isEqualTo("grievance@school.example");

        PrivacyCalls.Reply english = translation(noticeId, draft.text("id"), "en");
        assertThat(english.status()).as(english.body().toString()).isEqualTo(201);
        String shown = BODY + "\n\n" + SUMMARY;
        assertThat(english.text("contentSha256")).isEqualTo(HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(shown.getBytes(StandardCharsets.UTF_8))));

        PrivacyCalls.Reply published = publish(noticeId, draft.text("id"));
        assertThat(published.status()).as(published.body().toString()).isEqualTo(200);
        assertThat(published.text("status")).isEqualTo("active");
        assertThat(published.body().has("publishedAt")).isTrue();
        assertThat(published.etag()).isEqualTo("\"2\"");
        assertThat(calls.get(versions(noticeId), admin).body().path("data").findValuesAsString("status"))
                .containsExactly("active");
    }

    @Test
    void aPublishedVersionAndItsTranslationsNoLongerChange() throws Exception {
        String noticeId = notice(admin, "general_notice").text("id");
        String versionId = version(noticeId, "marketing_comms").text("id");
        PrivacyCalls.Reply english = translation(noticeId, versionId, "en");
        assertThat(publish(noticeId, versionId).status()).isEqualTo(200);

        String version = versions(noticeId) + "/" + versionId;
        assertThat(calls.patch(version, admin, "\"2\"", Map.of("requiresReconsent", true)).status()).isEqualTo(409);
        assertThat(translation(noticeId, versionId, "hi").status()).isEqualTo(409);
        String translation = translations(noticeId, versionId) + "/" + english.text("id");
        assertThat(calls.patch(translation, admin, english.etag(), Map.of("bodyMarkdown", "Changed.")).status())
                .isEqualTo(409);
        assertThat(calls.delete(translation, admin, english.etag()).status()).isEqualTo(409);
        assertThat(publish(noticeId, versionId).status()).as("an active version is no draft").isEqualTo(409);
        assertThat(calls.delete(NOTICES + "/" + noticeId, admin, "\"1\"").status())
                .as("a notice with an active version is kept").isEqualTo(409);
        assertThat(calls.get(translation, admin).text("bodyMarkdown")).isEqualTo(BODY);
    }

    @Test
    void publishingTheNextVersionRetiresThePreviousOne() throws Exception {
        String noticeId = notice(admin, "class_photos").text("id");
        String first = version(noticeId, "photo_video_publication").text("id");
        translation(noticeId, first, "en");
        assertThat(publish(noticeId, first).status()).isEqualTo(200);

        PrivacyCalls.Reply next = version(noticeId, "photo_video_publication", "community_naming");
        assertThat(next.body().path("version").asInt()).isEqualTo(2);
        String second = next.text("id");
        assertThat(publish(noticeId, second).status()).as("a version without a translation").isEqualTo(409);
        translation(noticeId, second, "en");
        assertThat(publish(noticeId, second).status()).isEqualTo(200);

        PrivacyCalls.Reply retired = calls.get(versions(noticeId) + "/" + first, admin);
        assertThat(retired.text("status")).isEqualTo("retired");
        assertThat(retired.text("supersededById")).isEqualTo(second);
        assertThat(calls.get(versions(noticeId) + "/" + second, admin).text("status")).isEqualTo("active");
    }

    @Test
    void aVersionNamesActivePurposesOfTheCatalogueOnly() throws Exception {
        String noticeId = notice(admin, "unknown_purposes").text("id");
        PrivacyCalls.Reply refused = version(noticeId, "photo_video_publication", "no_such_purpose");

        assertThat(refused.status()).isEqualTo(422);
        assertThat(PrivacyCalls.fieldErrors(refused)).containsExactly("purposeCodes[1]:PURPOSE_NOT_ACTIVE");
    }

    @Test
    void aCodeIsUniqueAmongTheTenantsLiveNotices() throws Exception {
        assertThat(notice(admin, "consent_form").status()).isEqualTo(201);
        assertThat(notice(admin, "consent_form").status()).isEqualTo(409);
        assertThat(notice(admin, "Consent Form").status()).isEqualTo(422);
        assertThat(notice(calls.administrator(UUID.randomUUID(), NOTICE_PERMISSIONS), "consent_form").status())
                .as("another tenant has its own codes").isEqualTo(201);
    }

    @Test
    void onlyATenantWideGrantChangesNotices() throws Exception {
        UUID tenant = UUID.randomUUID();
        String school = calls.schoolAdministrator(tenant, NOTICE_PERMISSIONS);

        assertThat(notice(school, "school_notice").status()).isEqualTo(403);
        String noticeId = notice(calls.administrator(tenant, NOTICE_PERMISSIONS), "tenant_notice").text("id");
        assertThat(calls.get(NOTICES + "/" + noticeId, school).status()).as("reading needs no tenant-wide grant")
                .isEqualTo(200);
        assertThat(calls.patch(NOTICES + "/" + noticeId, school, "\"1\"", Map.of("title", "Changed")).status())
                .isEqualTo(403);
    }

    @Test
    void anotherTenantNeverSeesTheNotice() throws Exception {
        String noticeId = notice(admin, "private_notice").text("id");
        String other = calls.administrator(UUID.randomUUID(), NOTICE_PERMISSIONS);

        assertThat(calls.get(NOTICES + "/" + noticeId, other).status()).isEqualTo(404);
        assertThat(calls.get(NOTICES, other).body().path("data")).isEmpty();
        assertThat(calls.patch(NOTICES + "/" + noticeId, other, "\"1\"", Map.of("title", "Taken")).status())
                .isEqualTo(404);
        assertThat(version(noticeId, "marketing_comms").status()).isEqualTo(201);
        assertThat(calls.get(versions(noticeId), other).status()).isEqualTo(404);
    }
}
