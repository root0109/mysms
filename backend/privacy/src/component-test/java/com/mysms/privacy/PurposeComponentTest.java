package com.mysms.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
 * The purpose catalogue against real PostgreSQL as the non-owner role: every purpose is listed switched off until the
 * tenant sets it, a tenant sees its own setting only, the catalogue's rules (forbidden for minors) come through, and a
 * tenant administrator switches a purpose on only with a notice whose active version lists it.
 */
@SpringBootTest(classes = PrivacyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PurposeComponentTest {

    /** The purposes privacy's baseline seeds. */
    private static final int CATALOGUE = 20;
    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final String READ = "privacy.tenant_purpose.read";
    private static final String UPDATE = "privacy.tenant_purpose.update";
    private static final String PURPOSES = "/v1/privacy/purposes";
    private static final String PHOTOS = "photo_video_publication";
    private static final String[] NOTICE_PERMISSIONS = {"privacy.notice.create", "privacy.notice_version.create",
        "privacy.notice_translation.create", "privacy.notice_version.publish"};

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(PrivacyDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    private PrivacyCalls calls;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "privacy");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @BeforeEach
    void connect() {
        calls = new PrivacyCalls(port, TOKENS);
    }

    private void sql(UUID tenant, String statement) {
        CallContext context = CallContext.anonymous(TRACE).withIdentity(tenant, null);
        CallContextHolder.callAs(context, () -> TestTransactions.of(database).write(() -> {
            database.jdbc().sql(statement).update();
            return null;
        }));
    }

    private static JsonNode byCode(JsonNode list, String code) {
        for (JsonNode item : list.path("data")) {
            if (code.equals(item.path("code").asString())) {
                return item;
            }
        }
        throw new AssertionError(code + " is not listed in " + list);
    }

    /** The id of a catalogue purpose, the same for every tenant. */
    private String purposeId(String code) throws Exception {
        return byCode(calls.get(PURPOSES + "?size=500", calls.administrator(UUID.randomUUID(), READ)).body(), code)
                .path("id").asString();
    }

    /** A notice of the tenant whose published, active version lists the purpose. */
    private String activeNoticeFor(UUID tenant, String purposeCode) throws Exception {
        String admin = calls.administrator(tenant, NOTICE_PERMISSIONS);
        String noticeId = calls.post("/v1/privacy/notices", admin, Map.of("code", "notice_" + purposeCode,
                "title", "Consent notice")).text("id");
        String versions = "/v1/privacy/notices/" + noticeId + "/versions";
        String versionId = calls.post(versions, admin, Map.of("purposeCodes", List.of(purposeCode),
                "grievanceContact", Map.of("name", "Grievance Officer", "email", "grievance@school.example")))
                .text("id");
        calls.post(versions + "/" + versionId + "/translations", admin,
                Map.of("language", "en", "bodyMarkdown", "What we do and why."));
        assertThat(calls.post(versions + "/" + versionId + ":publish", admin, Map.of()).status()).isEqualTo(200);
        return noticeId;
    }

    @Test
    void aNewTenantSeesTheWholeCatalogueSwitchedOff() throws Exception {
        PrivacyCalls.Reply list = calls.get(PURPOSES + "?size=500", calls.administrator(UUID.randomUUID(), READ));

        assertThat(list.status()).as(list.body().toString()).isEqualTo(200);
        assertThat(list.body().path("data")).hasSize(CATALOGUE);
        assertThat(list.body().path("data").findValuesAsString("enabled")).containsOnly("false");
        assertThat(list.body().path("data").findValuesAsString("etag")).containsOnly("\"0\"");
        JsonNode photos = byCode(list.body(), PHOTOS);
        assertThat(photos.path("scopeKind").asString()).isEqualTo("width");
        assertThat(photos.path("majorityGrace").asString()).isEqualTo("keep_published");
        assertThat(photos.has("maxScope")).isFalse();
        assertThat(byCode(list.body(), "targeted_advertising").path("forbiddenForMinors").asBoolean()).isTrue();
        assertThat(byCode(list.body(), "product_analytics").path("fiduciary").asString()).isEqualTo("platform");
    }

    @Test
    void aTenantSeesItsOwnSettingAndNoOtherTenantsSetting() throws Exception {
        sql(TENANT, "INSERT INTO privacy.tenant_purpose (tenant_id, purpose_id, enabled, max_scope, row_version)"
                + " SELECT current_setting('app.tenant_id')::uuid, id, true, 'institution', 3"
                + " FROM privacy.purpose WHERE code = '" + PHOTOS + "'");

        PrivacyCalls.Reply own = calls.get(PURPOSES + "?filter=enabled%20%3D%20true", calls.administrator(TENANT, READ));
        assertThat(own.status()).as(own.body().toString()).isEqualTo(200);
        assertThat(own.body().path("data").findValuesAsString("code")).containsExactly(PHOTOS);
        JsonNode photos = own.body().path("data").get(0);
        assertThat(photos.path("maxScope").asString()).isEqualTo("institution");
        assertThat(photos.path("etag").asString()).isEqualTo("\"3\"");

        String other = calls.administrator(OTHER_TENANT, READ);
        PrivacyCalls.Reply others = calls.get(PURPOSES + "?filter=enabled%20%3D%20true", other);
        assertThat(others.status()).isEqualTo(200);
        assertThat(others.body().path("data")).isEmpty();
        PrivacyCalls.Reply single = calls.get(PURPOSES + "/" + photos.path("id").asString(), other);
        assertThat(single.status()).isEqualTo(200);
        assertThat(single.body().path("enabled").asBoolean()).isFalse();
    }

    @Test
    void anUnknownPurposeIsNotFoundAndAnUngrantedCallerIsRefused() throws Exception {
        assertThat(calls.get(PURPOSES + "/" + UUID.randomUUID(), calls.administrator(TENANT, READ)).status())
                .isEqualTo(404);
        assertThat(calls.get(PURPOSES, calls.administrator(TENANT, "privacy.other.read")).status()).isEqualTo(403);
    }

    @Test
    void aPurposeIsSwitchedOnOnlyWithANoticeWhoseActiveVersionListsIt() throws Exception {
        UUID tenant = UUID.randomUUID();
        String admin = calls.administrator(tenant, READ, UPDATE);
        String photos = PURPOSES + "/" + purposeId(PHOTOS);

        PrivacyCalls.Reply withoutNotice = calls.patch(photos, admin, "\"0\"", Map.of("enabled", true));
        assertThat(withoutNotice.status()).isEqualTo(422);
        assertThat(PrivacyCalls.fieldErrors(withoutNotice)).containsExactly("noticeId:CONSENT_NOTICE_NOT_ACTIVE");

        String noticeId = activeNoticeFor(tenant, PHOTOS);
        PrivacyCalls.Reply enabled = calls.patch(photos, admin, "\"0\"",
                Map.of("enabled", true, "maxScope", "institution", "noticeId", noticeId));
        assertThat(enabled.status()).as(enabled.body().toString()).isEqualTo(200);
        assertThat(enabled.etag()).isEqualTo("\"1\"");

        JsonNode listed = byCode(calls.get(PURPOSES + "?filter=enabled%20%3D%20true", admin).body(), PHOTOS);
        assertThat(listed.path("enabled").asBoolean()).isTrue();
        assertThat(listed.path("maxScope").asString()).isEqualTo("institution");
        assertThat(listed.path("noticeId").asString()).isEqualTo(noticeId);
        assertThat(listed.path("etag").asString()).isEqualTo("\"1\"");

        assertThat(calls.patch(photos, admin, "\"0\"", Map.of("enabled", false)).status()).isEqualTo(412);
        String otherNotice = activeNoticeFor(tenant, "marketing_comms");
        PrivacyCalls.Reply unlisted = calls.patch(photos, admin, "\"1\"", Map.of("noticeId", otherNotice));
        assertThat(PrivacyCalls.fieldErrors(unlisted)).containsExactly("noticeId:CONSENT_NOTICE_NOT_ACTIVE");
        PrivacyCalls.Reply foreign = calls.patch(photos, calls.administrator(UUID.randomUUID(), READ, UPDATE), "\"0\"",
                Map.of("enabled", true, "noticeId", noticeId));
        assertThat(PrivacyCalls.fieldErrors(foreign)).as("another tenant's notice does not exist for the caller")
                .containsExactly("noticeId:NOT_FOUND");
    }

    @Test
    void aWidthCeilingFitsWidthPurposesOnly() throws Exception {
        String admin = calls.administrator(UUID.randomUUID(), READ, UPDATE);
        PrivacyCalls.Reply refused = calls.patch(PURPOSES + "/" + purposeId("learning_analytics"), admin, "\"0\"",
                Map.of("maxScope", "class"));

        assertThat(refused.status()).isEqualTo(422);
        assertThat(PrivacyCalls.fieldErrors(refused)).containsExactly("maxScope:MAX_SCOPE_NOT_APPLICABLE");
    }

    @Test
    void aPlatformPurposeIsNeverSwitchedOnByATenant() throws Exception {
        UUID tenant = UUID.randomUUID();
        String noticeId = activeNoticeFor(tenant, "product_analytics");
        PrivacyCalls.Reply refused = calls.patch(PURPOSES + "/" + purposeId("product_analytics"),
                calls.administrator(tenant, READ, UPDATE), "\"0\"", Map.of("enabled", true, "noticeId", noticeId));

        assertThat(refused.status()).isEqualTo(422);
        assertThat(PrivacyCalls.fieldErrors(refused)).containsExactly("enabled:PURPOSE_PLATFORM_FIDUCIARY");
    }

    @Test
    void onlyATenantWideGrantChangesASetting() throws Exception {
        UUID tenant = UUID.randomUUID();
        PrivacyCalls.Reply refused = calls.patch(PURPOSES + "/" + purposeId(PHOTOS),
                calls.schoolAdministrator(tenant, READ, UPDATE), "\"0\"", Map.of("maxScope", "class"));

        assertThat(refused.status()).isEqualTo(403);
    }
}
