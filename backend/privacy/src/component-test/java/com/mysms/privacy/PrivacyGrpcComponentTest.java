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
import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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
 * ConsentService, PurposeService's UpdatePurpose, each RPC of ProcessingActivityService, ConsentDecisionService (as
 * another module's service calls it), BreachService, ConsentCoverageService, LegalHoldService (its holds check as a
 * module calls it) and RetentionService, and RightsRequestService's requester RPCs, staff steps, fulfilment steps and
 * access summaries serve the same business service as REST, path ids, etags, update masks and filters included.
 */
@SpringBootTest(classes = PrivacyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=PrivacyGrpcComponentTest")
class PrivacyGrpcComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String NOTICES = "NoticeService";
    private static final String CONSENTS = "ConsentService";
    private static final String DECISIONS = "ConsentDecisionService";
    private static final String RIGHTS = "RightsRequestService";
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
        return call(service, method, request, token, null);
    }

    /** {@link #call} as another module's service would, in the tenant sent as x-tenant-id (null: none sent). */
    private static JsonNode call(String service, String method, Map<String, ?> request, String token, UUID tenant)
            throws Exception {
        if (channel == null) {
            channel = GrpcParity.channel("PrivacyGrpcComponentTest");
        }
        Channel target = channel;
        if (tenant != null) {
            Metadata headers = new Metadata();
            headers.put(Metadata.Key.of("x-tenant-id", Metadata.ASCII_STRING_MARSHALLER), tenant.toString());
            target = ClientInterceptors.intercept(channel, MetadataUtils.newAttachHeadersInterceptor(headers));
        }
        MethodDescriptor rpc = PrivacyProto.getDescriptor().findServiceByName(service).findMethodByName(method);
        DynamicMessage.Builder message = DynamicMessage.newBuilder(rpc.getInputType());
        JsonFormat.parser().merge(JSON.writeValueAsString(request), message);
        GrpcParity.Answer answer = GrpcParity.call(target, rpc, message.build(), token);
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
        JsonNode mine = call(CONSENTS, "RecordMyConsent", decision, person).path("myConsent");
        assertThat(mine.path("myStatus").asString()).isEqualTo("MY_STATUS_GRANTED");
        assertThat(mine.path("capacity").asString()).isEqualTo("CAPACITY_SELF");
        assertThat(mine.path("noticeVersionId").asString()).isEqualTo(version);
        assertThat(mine.path("allowedScopes").get(0).asString()).isEqualTo("SCOPE_CLASS");
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

    @Test
    void everyRightsRequestRpcServesLikeRest() throws Exception {
        PrivacyCalls calls = new PrivacyCalls(port, TOKENS);
        ConsentFixtures fixtures = new ConsentFixtures(calls, database, subjects, links);
        UUID tenant = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        fixtures.adult(tenant, self);
        String person = calls.person(tenant, self, ConsentFixtures.REQUESTER);
        String staff = calls.administrator(tenant, ConsentFixtures.RIGHTS_STAFF);

        JsonNode mine = call(RIGHTS, "SubmitMyRequest", Map.of("type", "TYPE_ACCESS", "subjectPersonId",
                self.toString(), "description", "What do you hold about me?"), person).path("myRightsRequest");
        assertThat(mine.path("status").asString()).isEqualTo("STATUS_VERIFIED");
        assertThat(mine.path("requesterCapacity").asString()).isEqualTo("REQUESTER_CAPACITY_SELF");
        assertThat(mine.path("number").asString()).matches("PRV-\\d{4}-\\d{6}");
        String id = mine.path("id").asString();
        assertThat(call(RIGHTS, "ListMyRequests", Map.of(), person).path("items")).hasSize(1);
        assertThat(call(RIGHTS, "GetMyRequest", Map.of("id", id), person).path("myRightsRequest").path("type")
                .asString()).isEqualTo("TYPE_ACCESS");

        assertThat(call(RIGHTS, "ListRightsRequests", Map.of("filter", "status = \"verified\""), staff)
                .path("items")).hasSize(1);
        JsonNode found = call(RIGHTS, "GetRightsRequest", Map.of("id", id), staff).path("rightsRequest");
        assertThat(found.path("identityCheck").asString()).isEqualTo("IDENTITY_CHECK_SIGNED_IN_SESSION");
        JsonNode assigned = call(RIGHTS, "AssignRightsRequest", Map.of("rightsRequestId", id, "assigneeUserId",
                UUID.randomUUID().toString(), "etag", found.path("etag").asString()), staff).path("rightsRequest");
        assertThat(assigned.path("status").asString()).isEqualTo("STATUS_IN_PROGRESS");
        String later = OffsetDateTime.parse(assigned.path("dueAt").asString()).plusDays(30).toString();
        assertThat(call(RIGHTS, "ExtendRightsRequest", Map.of("rightsRequestId", id, "dueAt", later, "reason",
                "Records are archived off site."), staff).path("rightsRequest").path("extendedReason").asString())
                .isEqualTo("Records are archived off site.");
        // an access request closes only as declined: it is fulfilled with its summary (FulfilAccessRequest)
        assertThat(call(RIGHTS, "CloseRightsRequest", Map.of("rightsRequestId", id, "outcomeCode",
                "OUTCOME_CODE_DECLINED"), staff).path("rightsRequest").path("status").asString())
                .isEqualTo("STATUS_CLOSED");

        String second = call(RIGHTS, "SubmitMyRequest", Map.of("type", "TYPE_GRIEVANCE", "subjectPersonId",
                self.toString(), "description", "A complaint.", "grievanceCategory", "GRIEVANCE_CATEGORY_OTHER"),
                person).path("myRightsRequest").path("id").asString();
        assertThat(call(RIGHTS, "RejectRightsRequest", Map.of("rightsRequestId", second, "reasonCode",
                "REASON_CODE_DUPLICATE"), staff).path("rightsRequest").path("outcomeCode").asString())
                .isEqualTo("OUTCOME_CODE_DUPLICATE");
    }

    @Test
    void theFulfilmentRpcsServeLikeRest() throws Exception {
        PrivacyCalls calls = new PrivacyCalls(port, TOKENS);
        ConsentFixtures fixtures = new ConsentFixtures(calls, database, subjects, links);
        UUID tenant = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        fixtures.adult(tenant, self);
        String person = calls.person(tenant, self, ConsentFixtures.REQUESTER);
        String staff = calls.administrator(tenant, "privacy.rights_request.assign", "privacy.rights_request.fulfil",
                "privacy.rights_request.retry_task");

        String export = call(RIGHTS, "SubmitMyRequest", Map.of("type", "TYPE_EXPORT", "subjectPersonId",
                self.toString(), "description", "A copy of my data."), person).path("myRightsRequest").path("id")
                .asString();
        call(RIGHTS, "AssignRightsRequest", Map.of("rightsRequestId", export, "assigneeUserId",
                UUID.randomUUID().toString()), staff);
        JsonNode started = call(RIGHTS, "StartRightsRequestFulfilment", Map.of("rightsRequestId", export, "etag", "2"),
                staff).path("rightsRequest");
        assertThat(started.path("tasks").findValuesAsString("status")).isNotEmpty().containsOnly("STATUS_DISPATCHED");
        String task = started.path("tasks").get(0).path("id").asString();
        JsonNode retried = call(RIGHTS, "RetryRightsRequestTask", Map.of("rightsRequestId", export, "taskId", task),
                staff).path("rightsRequest");
        assertThat(retried.path("tasks").get(0).path("attempts").asInt()).isEqualTo(2);

        String withdrawAll = call(RIGHTS, "SubmitMyRequest", Map.of("type", "TYPE_WITHDRAW_ALL", "subjectPersonId",
                self.toString(), "description", "Withdraw everything."), person).path("myRightsRequest").path("id")
                .asString();
        call(RIGHTS, "AssignRightsRequest", Map.of("rightsRequestId", withdrawAll, "assigneeUserId",
                UUID.randomUUID().toString()), staff);
        assertThat(call(RIGHTS, "FulfilWithdrawAllRequest", Map.of("rightsRequestId", withdrawAll, "etag", "2"), staff)
                .path("rightsRequest").path("outcomeCode").asString()).isEqualTo("OUTCOME_CODE_FULFILLED");
    }

    @Test
    void everyLegalHoldRpcServesLikeRest() throws Exception {
        UUID tenant = UUID.randomUUID();
        String officer = new PrivacyCalls(port, TOKENS).administrator(tenant, "privacy.legal_hold.read",
                "privacy.legal_hold.place", "privacy.legal_hold.update", "privacy.legal_hold.release");
        String holds = "LegalHoldService";
        UUID subject = UUID.randomUUID();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        JsonNode placed = call(holds, "PlaceLegalHold", Map.of("legalHold", Map.of("reasonCode",
                "REASON_CODE_SAFEGUARDING", "scope", "SCOPE_SUBJECT", "subjectPersonId", subject.toString(),
                "reviewBy", today.plusMonths(3).toString())), officer).path("legalHold");
        assertThat(placed.path("status").asString()).isEqualTo("STATUS_ACTIVE");
        String id = placed.path("id").asString();
        assertThat(call(holds, "GetLegalHold", Map.of("id", id), officer).path("legalHold").path("reviewBy")
                .asString()).isEqualTo(today.plusMonths(3).toString());
        assertThat(call(holds, "ListLegalHolds", Map.of("filter", "active = true"), officer).path("items")).hasSize(1);
        assertThat(call(holds, "UpdateLegalHold", Map.of("legalHold", Map.of("id", id, "etag", "1", "reviewBy",
                today.plusMonths(6).toString())), officer).path("legalHold").path("reviewBy").asString())
                .isEqualTo(today.plusMonths(6).toString());
        assertThat(call(holds, "ListActiveHolds", Map.of("subjectPersonId", subject.toString(), "module", "sis"),
                TOKENS.service("sis", "privacy.legal_hold.check"), tenant).path("holds").findValuesAsString("id"))
                .containsExactly(id);
        assertThat(call(holds, "ReleaseLegalHold", Map.of("legalHoldId", id, "etag", "2"), officer)
                .path("legalHold").path("status").asString()).isEqualTo("STATUS_RELEASED");
    }

    @Test
    void everyRetentionRpcServesLikeRest() throws Exception {
        String admin = new PrivacyCalls(port, TOKENS).administrator(UUID.randomUUID(), "privacy.retention_policy.read",
                "privacy.retention_policy.update", "privacy.retention_run.read");
        String retention = "RetentionService";

        JsonNode attendance = call(retention, "ListRetentionPolicies", Map.of("filter",
                "code = \"attendance.after_leaving\""), admin).path("items").get(0);
        assertThat(attendance.path("trigger").asString()).isEqualTo("TRIGGER_AFTER_LEAVING");
        String id = attendance.path("id").asString();
        int longer = attendance.path("minimumPeriodDays").asInt() + 30;
        assertThat(call(retention, "UpdateRetentionPolicy", Map.of("retentionPolicy", Map.of("id", id, "etag", "0",
                "periodDays", longer)), admin).path("retentionPolicy").path("overridden").asBoolean()).isTrue();
        assertThat(call(retention, "GetRetentionPolicy", Map.of("id", id), admin).path("retentionPolicy")
                .path("periodDays").asInt()).isEqualTo(longer);
        assertThat(call(retention, "ListRetentionRuns", Map.of(), admin).path("items")).isEmpty();
    }

    @Test
    void everyNomineeRpcServesLikeRest() throws Exception {
        PrivacyCalls calls = new PrivacyCalls(port, TOKENS);
        ConsentFixtures fixtures = new ConsentFixtures(calls, database, subjects, links);
        UUID tenant = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        fixtures.adult(tenant, self);
        String person = calls.person(tenant, self, "privacy.my_nominee.read", "privacy.my_nominee.set",
                "privacy.my_nominee.revoke");

        JsonNode named = call("NomineeService", "SetMyNominee", Map.of("nomineeName", "Meera Krishnan",
                "nomineeContact", "+91 98450 12345"), person).path("myNominee");
        assertThat(named.path("nomineeContactMasked").asString()).isEqualTo("••••2345");
        JsonNode found = call("NomineeService", "GetMyNominee", Map.of(), person).path("myNominee");
        assertThat(found.path("id").asString()).isEqualTo(named.path("id").asString());
        call("NomineeService", "RevokeMyNominee", Map.of("etag", found.path("etag").asString()), person);
        assertThat(new PrivacyCalls(port, TOKENS).get("/v1/privacy/my/nominee", person).status()).isEqualTo(404);
    }

    @Test
    void theAccessSummaryRpcsServeLikeRest() throws Exception {
        PrivacyCalls calls = new PrivacyCalls(port, TOKENS);
        ConsentFixtures fixtures = new ConsentFixtures(calls, database, subjects, links);
        UUID tenant = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        fixtures.adult(tenant, self);
        String person = calls.person(tenant, self, ConsentFixtures.REQUESTER);
        String staff = calls.administrator(tenant, ConsentFixtures.RIGHTS_STAFF);
        String id = call(RIGHTS, "SubmitMyRequest", Map.of("type", "TYPE_ACCESS", "subjectPersonId", self.toString(),
                "description", "What do you hold about me?"), person).path("myRightsRequest").path("id").asString();

        JsonNode preview = call(RIGHTS, "GetAccessSummary", Map.of("rightsRequestId", id), staff)
                .path("accessSummary");
        assertThat(preview.path("subjectStatus").asString()).isEqualTo("SUBJECT_STATUS_ADULT");
        assertThat(preview.path("activities").findValuesAsString("activityCode")).contains("comms.operational_notice");
        JsonNode assigned = call(RIGHTS, "AssignRightsRequest", Map.of("rightsRequestId", id, "assigneeUserId",
                UUID.randomUUID().toString()), staff).path("rightsRequest");
        JsonNode fulfilled = call(RIGHTS, "FulfilAccessRequest", Map.of("rightsRequestId", id, "etag",
                assigned.path("etag").asString()), staff).path("rightsRequest");
        assertThat(fulfilled.path("status").asString()).isEqualTo("STATUS_CLOSED");
        assertThat(fulfilled.path("outcomeCode").asString()).isEqualTo("OUTCOME_CODE_FULFILLED");

        JsonNode mine = call(RIGHTS, "GetMyAccessSummary", Map.of("rightsRequestId", id), person)
                .path("accessSummary");
        assertThat(mine.path("subjectPersonId").asString()).isEqualTo(self.toString());
        assertThat(OffsetDateTime.parse(mine.path("generatedAt").asString()))
                .isAtSameInstantAs(OffsetDateTime.parse(fulfilled.path("closedAt").asString()));
        JsonNode notices = null;
        for (JsonNode activity : mine.path("activities")) {
            if ("comms.operational_notice".equals(activity.path("activityCode").asString())) {
                notices = activity;
            }
        }
        assertThat(notices).isNotNull();
        assertThat(notices.path("legalBasis").asString()).isEqualTo("LEGAL_BASIS_LEGITIMATE_USE");
        assertThat(calls.get("/v1/privacy/my/requests/" + id + "/access-summary", person).body()
                .path("activities")).hasSize(mine.path("activities").size());
    }

    @Test
    void theProcessingRegisterServesLikeRest() throws Exception {
        String reader = new PrivacyCalls(port, TOKENS).administrator(UUID.randomUUID(),
                "privacy.processing_activity.read");
        JsonNode first = call("ProcessingActivityService", "ListProcessingActivities",
                Map.of("filter", "module = \"comms\"", "pageable", Map.of("size", 4)), reader);
        assertThat(first.path("items")).hasSize(4);
        assertThat(first.path("items").get(0).path("code").asString()).isEqualTo("comms.community_naming");
        assertThat(first.path("items").get(0).path("legalBasis").asString()).isEqualTo("LEGAL_BASIS_CONSENT");
        assertThat(first.path("pagination").path("nextCursor").asString()).as("6 comms activities: a next page")
                .isNotEmpty();
    }

    @Test
    void theRegisterExportRpcServesLikeRest() throws Exception {
        String reader = new PrivacyCalls(port, TOKENS).administrator(UUID.randomUUID(),
                "privacy.processing_activity.read");
        JsonNode export = call("ProcessingActivityService", "GetProcessingRegisterExport", Map.of(), reader)
                .path("processingRegisterExport");
        assertThat(export.path("csv").asString()).startsWith("activity_code,module,purpose,");
        assertThat(export.path("activities").asInt()).isEqualTo(18);
    }

    @Test
    void everyBreachRpcServesLikeRest() throws Exception {
        String officer = new PrivacyCalls(port, TOKENS).administrator(UUID.randomUUID(), "privacy.breach.read",
                "privacy.breach.report", "privacy.breach.update", "privacy.breach.triage", "privacy.breach.contain",
                "privacy.breach.record_notification", "privacy.breach.close", "privacy.breach.mark_not_a_breach");
        Instant aware = Instant.now().minus(Duration.ofHours(3)).truncatedTo(ChronoUnit.SECONDS);
        String breaches = "BreachService";

        JsonNode reported = call(breaches, "ReportBreach", Map.of("breach", Map.of("detectedAt",
                aware.minus(Duration.ofHours(1)).toString(), "becameAwareAt", aware.toString(), "description",
                "412 rows of sis.student exposed.", "categories", List.of("contact"))), officer).path("breach");
        assertThat(reported.path("status").asString()).isEqualTo("STATUS_REPORTED");
        String id = reported.path("id").asString();
        assertThat(call(breaches, "GetBreach", Map.of("id", id), officer).path("breach").path("number").asString())
                .matches("PRB-\\d{4}-\\d{6}");
        assertThat(call(breaches, "ListBreaches", Map.of("filter", "status = \"reported\""), officer)
                .path("items")).hasSize(1);
        assertThat(call(breaches, "UpdateBreach", Map.of("breach", Map.of("id", id, "etag", "1",
                "estimatedPrincipals", 412)), officer).path("breach").path("estimatedPrincipals").asInt())
                .isEqualTo(412);
        assertThat(call(breaches, "TriageBreach", Map.of("breachId", id, "etag", "2"), officer).path("breach")
                .path("status").asString()).isEqualTo("STATUS_TRIAGED");
        call(breaches, "ContainBreach", Map.of("breachId", id), officer);
        for (String[] notice : new String[][] {{"AUDIENCE_BOARD", "KIND_INITIAL"}, {"AUDIENCE_BOARD", "KIND_DETAILED"},
            {"AUDIENCE_PRINCIPALS", "KIND_DETAILED"}}) {
            call(breaches, "RecordBreachNotification", Map.of("breachId", id, "audience", notice[0], "kind",
                    notice[1], "sentAt", aware.plus(Duration.ofHours(1)).toString()), officer);
        }
        JsonNode closed = call(breaches, "CloseBreach", Map.of("breachId", id), officer).path("breach");
        assertThat(closed.path("status").asString()).isEqualTo("STATUS_CLOSED");
        assertThat(closed.path("notifications")).hasSize(3);

        String other = call(breaches, "ReportBreach", Map.of("breach", Map.of("detectedAt", aware.toString(),
                "becameAwareAt", aware.toString(), "description", "A test export.")), officer)
                .path("breach").path("id").asString();
        call(breaches, "TriageBreach", Map.of("breachId", other), officer);
        assertThat(call(breaches, "MarkBreachNotABreach", Map.of("breachId", other, "reason", "A test export."),
                officer).path("breach").path("status").asString()).isEqualTo("STATUS_NOT_A_BREACH");
    }

    @Test
    void theCoverageRpcServesLikeRest() throws Exception {
        String admin = new PrivacyCalls(port, TOKENS).administrator(UUID.randomUUID(), "privacy.coverage.read");
        JsonNode coverage = call("ConsentCoverageService", "GetConsentCoverage", Map.of("purposeCode",
                "marketing_comms"), admin).path("consentCoverage");
        assertThat(coverage.path("scopes").get(0).path("scope").asString()).isEqualTo("SCOPE_TENANT");
        assertThat(coverage.path("scopes").get(0).path("denied")).hasSize(6);
    }

    @Test
    void everyConsentDecisionRpcServesAnotherModulesServiceLikeRest() throws Exception {
        PrivacyCalls calls = new PrivacyCalls(port, TOKENS);
        ConsentFixtures fixtures = new ConsentFixtures(calls, database, subjects, links);
        UUID tenant = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        fixtures.adult(tenant, self);
        String version = fixtures.enable(tenant, ConsentFixtures.MARKETING);
        assertThat(calls.post(ConsentFixtures.MY_CONSENTS, calls.person(tenant, self, ConsentFixtures.SELF_SERVICE),
                ConsentFixtures.decision(self, ConsentFixtures.MARKETING, "tenant", "granted", version, "en"))
                .status()).isEqualTo(201);
        String service = TOKENS.service("comms", "privacy.consent_decision.check",
                "privacy.consent_decision.batch_check", "privacy.consent_decision.get_purpose_status",
                "privacy.consent_decision.list_subjects");
        String activity = "comms.marketing_broadcast";

        JsonNode checked = call(DECISIONS, "CheckConsent", Map.of("activityCode", activity,
                "subjectPersonId", self.toString(), "scope", "tenant"), service, tenant).path("decision");
        assertThat(checked.path("allowed").asBoolean()).isTrue();
        assertThat(checked.path("reason").asString()).isEqualTo("REASON_CONSENT");
        assertThat(call(DECISIONS, "BatchCheckConsent", Map.of("activityCode", activity, "scope", "SCOPE_TENANT",
                "subjectPersonIds", List.of(UUID.randomUUID().toString(), self.toString())), service, tenant)
                .path("decisions").findValuesAsString("reason")).containsExactly("REASON_NOT_ANSWERED",
                "REASON_CONSENT");
        assertThat(call(DECISIONS, "GetPurposeStatus", Map.of("activityCode", activity), service, tenant)
                .path("purposeStatus").path("activeNoticeVersionId").asString()).isEqualTo(version);
        assertThat(call(DECISIONS, "ListSubjectsWithConsent", Map.of("activityCode", activity, "scope", "tenant"),
                service, tenant).path("items").findValuesAsString("subjectPersonId")).containsExactly(self.toString());
    }
}
