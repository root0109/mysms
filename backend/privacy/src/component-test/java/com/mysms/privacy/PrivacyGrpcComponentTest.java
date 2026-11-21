package com.mysms.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.GrpcParity;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.events.GuardianLinkSync;
import com.mysms.privacy.events.SubjectSync;
import com.mysms.privacy.v1.PrivacyProto;
import io.grpc.ManagedChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * privacy's gRPC services against real PostgreSQL through every edge interceptor: each RPC of NoticeService and
 * ConsentService, and PurposeService's UpdatePurpose, serves the same business service as REST, path ids, etags and
 * update masks included.
 */
@SpringBootTest(classes = PrivacyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=PrivacyGrpcComponentTest")
class PrivacyGrpcComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String NOTICES = "NoticeService";
    private static final String CONSENTS = "ConsentService";
    private static final String[] STAFF = {"privacy.consent_record.read", "privacy.consent_record.record_on_behalf",
        "privacy.consent_record.verify"};
    private static final String[] PERMISSIONS = {"privacy.notice.read", "privacy.notice.create",
        "privacy.notice.update", "privacy.notice.delete", "privacy.notice_version.read", "privacy.notice_version.create",
        "privacy.notice_version.update", "privacy.notice_version.publish", "privacy.notice_translation.read",
        "privacy.notice_translation.create", "privacy.notice_translation.update", "privacy.notice_translation.delete",
        "privacy.tenant_purpose.read", "privacy.tenant_purpose.update"};
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(PrivacyDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private SubjectSync subjects;

    @Autowired
    private GuardianLinkSync links;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "privacy");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @AfterAll
    static void closeChannel() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    /** Calls the RPC with the request given as proto JSON and answers the response, which must be a success. */
    private static JsonNode call(String service, String method, Map<String, ?> request, String token)
            throws Exception {
        if (channel == null) {
            channel = GrpcParity.channel("PrivacyGrpcComponentTest");
        }
        MethodDescriptor rpc = PrivacyProto.getDescriptor().findServiceByName(service).findMethodByName(method);
        DynamicMessage.Builder message = DynamicMessage.newBuilder(rpc.getInputType());
        JsonFormat.parser().merge(JSON.writeValueAsString(request), message);
        GrpcParity.Answer answer = GrpcParity.call(channel, rpc, message.build(), token);
        assertThat(answer.status()).as("%s/%s answered %s", service, method, answer.code()).isEqualTo(200);
        return answer.body();
    }

    @Test
    void everyNoticeRpcAndTheSwitchServeLikeRest() throws Exception {
        PrivacyCalls calls = new PrivacyCalls(port, TOKENS);
        String admin = calls.administrator(UUID.randomUUID(), PERMISSIONS);

        String noticeId = call(NOTICES, "CreateNotice", Map.of("notice", Map.of("code", "grpc_notice",
                "title", "Over gRPC")), admin).path("notice").path("id").asString();
        assertThat(call(NOTICES, "GetNotice", Map.of("id", noticeId), admin).path("notice").path("code").asString())
                .isEqualTo("grpc_notice");
        assertThat(call(NOTICES, "ListNotices", Map.of(), admin).path("items")).hasSize(1);
        assertThat(call(NOTICES, "UpdateNotice", Map.of("notice", Map.of("id", noticeId, "etag", "1",
                "title", "Renamed")), admin).path("notice").path("title").asString()).isEqualTo("Renamed");

        Map<String, ?> contact = Map.of("name", "Grievance Officer", "email", "grievance@school.example");
        String versionId = call(NOTICES, "CreateNoticeVersion", Map.of("noticeId", noticeId, "noticeVersion",
                Map.of("purposeCodes", List.of("marketing_comms"), "grievanceContact", contact)), admin)
                .path("noticeVersion").path("id").asString();
        Map<String, String> version = Map.of("noticeId", noticeId, "id", versionId);
        assertThat(call(NOTICES, "GetNoticeVersion", version, admin).path("noticeVersion").path("status").asString())
                .isEqualTo("STATUS_DRAFT");
        assertThat(call(NOTICES, "ListNoticeVersions", Map.of("noticeId", noticeId), admin).path("items")).hasSize(1);
        assertThat(call(NOTICES, "UpdateNoticeVersion", Map.of("noticeId", noticeId, "noticeVersion",
                Map.of("id", versionId, "etag", "1", "requiresReconsent", true), "updateMask", "requiresReconsent"),
                admin).path("noticeVersion").path("requiresReconsent").asBoolean()).isTrue();

        Map<String, String> parent = Map.of("noticeId", noticeId, "noticeVersionId", versionId);
        String translationId = call(NOTICES, "CreateNoticeTranslation", Map.of("noticeId", noticeId,
                "noticeVersionId", versionId, "noticeTranslation", Map.of("language", "en", "bodyMarkdown",
                "What we send and why.")), admin).path("noticeTranslation").path("id").asString();
        assertThat(call(NOTICES, "GetNoticeTranslation", Map.of("noticeId", noticeId, "noticeVersionId", versionId,
                "id", translationId), admin).path("noticeTranslation").path("contentSha256").asString()).hasSize(64);
        assertThat(call(NOTICES, "ListNoticeTranslations", parent, admin).path("items")).hasSize(1);
        assertThat(call(NOTICES, "UpdateNoticeTranslation", Map.of("noticeId", noticeId, "noticeVersionId",
                versionId, "noticeTranslation", Map.of("id", translationId, "etag", "1", "language", "hi"),
                "updateMask", "language"), admin).path("noticeTranslation").path("language").asString())
                .isEqualTo("hi");
        String extra = call(NOTICES, "CreateNoticeTranslation", Map.of("noticeId", noticeId, "noticeVersionId",
                versionId, "noticeTranslation", Map.of("language", "en", "bodyMarkdown", "Draft.")), admin)
                .path("noticeTranslation").path("id").asString();
        call(NOTICES, "DeleteNoticeTranslation", Map.of("noticeId", noticeId, "noticeVersionId", versionId,
                "id", extra, "etag", "1"), admin);
        assertThat(call(NOTICES, "PublishNoticeVersion", parent, admin).path("noticeVersion").path("status")
                .asString()).isEqualTo("STATUS_ACTIVE");

        String purposeId = null;
        for (JsonNode purpose : calls.get("/v1/privacy/purposes?size=500", admin).body().path("data")) {
            if ("marketing_comms".equals(purpose.path("code").asString())) {
                purposeId = purpose.path("id").asString();
            }
        }
        JsonNode switched = call("PurposeService", "UpdatePurpose", Map.of("purpose", Map.of("id", purposeId,
                "etag", "0", "enabled", true, "noticeId", noticeId), "updateMask", "enabled,noticeId"), admin)
                .path("purpose");
        assertThat(switched.path("enabled").asBoolean()).isTrue();
        assertThat(switched.path("etag").asString()).isEqualTo("\"1\"");

        String spare = call(NOTICES, "CreateNotice", Map.of("notice", Map.of("code", "spare_notice",
                "title", "Unused")), admin).path("notice").path("id").asString();
        call(NOTICES, "DeleteNotice", Map.of("id", spare, "etag", "1"), admin);
        assertThat(calls.get("/v1/privacy/notices/" + spare, admin).status()).isEqualTo(404);
    }

    @Test
    void everyConsentRpcServesLikeRest() throws Exception {
        PrivacyCalls calls = new PrivacyCalls(port, TOKENS);
        ConsentFixtures fixtures = new ConsentFixtures(calls, database, subjects, links);
        UUID tenant = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        fixtures.adult(tenant, self);
        String version = fixtures.enable(tenant, ConsentFixtures.PHOTOS);
        String person = calls.person(tenant, self, ConsentFixtures.SELF_SERVICE);
        String recorder = calls.administrator(tenant, STAFF);
        String verifier = calls.administrator(tenant, STAFF);

        assertThat(call(CONSENTS, "GetMyNotice", Map.of("noticeCode", ConsentFixtures.NOTICE, "language", "hi"),
                person).path("myNotice").path("noticeVersionId").asString()).isEqualTo(version);
        Map<String, String> decision = Map.of("subjectPersonId", self.toString(), "purposeCode",
                ConsentFixtures.PHOTOS, "scope", "SCOPE_CLASS", "decision", "DECISION_GRANTED", "noticeVersionId",
                version, "noticeLanguage", "en");
        assertThat(call(CONSENTS, "RecordMyConsent", decision, person).path("myConsent").path("myStatus").asString())
                .isEqualTo("MY_STATUS_GRANTED");
        assertThat(call(CONSENTS, "ListMyConsents", Map.of(), person).path("items")).isNotEmpty();

        Map<String, String> paper = new HashMap<>(decision);
        paper.put("scope", "SCOPE_PUBLIC");
        paper.put("giverPersonId", self.toString());
        paper.put("giverCapacity", "GIVER_CAPACITY_SELF");
        paper.put("evidenceFileId", UUID.randomUUID().toString());
        JsonNode recorded = call(CONSENTS, "RecordConsentOnBehalf", paper, recorder).path("consentRecord");
        assertThat(recorded.path("verificationStatus").asString()).isEqualTo("VERIFICATION_STATUS_UNVERIFIED");
        String id = recorded.path("id").asString();
        assertThat(call(CONSENTS, "VerifyConsentRecord", Map.of("consentRecordId", id), verifier)
                .path("consentRecord").path("verificationStatus").asString()).isEqualTo("VERIFICATION_STATUS_VERIFIED");
        assertThat(call(CONSENTS, "GetConsentRecord", Map.of("id", id), recorder).path("consentRecord")
                .path("channel").asString()).isEqualTo("CHANNEL_PAPER_FORM");
        assertThat(call(CONSENTS, "ListConsentRecords", Map.of(), recorder).path("items")).hasSize(2);
        assertThat(call(CONSENTS, "WithdrawMyConsent", Map.of("consentRecordId", id), person).path("myConsent")
                .path("myStatus").asString()).isEqualTo("MY_STATUS_NOT_ANSWERED");
    }
}
