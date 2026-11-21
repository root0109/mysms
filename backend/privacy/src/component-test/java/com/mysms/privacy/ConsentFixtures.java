package com.mysms.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.privacy.events.GuardianLinkSync;
import com.mysms.privacy.events.SubjectSync;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import tools.jackson.databind.JsonNode;

/**
 * The world a consent test needs, built the way production builds it: persons and guardian links through privacy's
 * real event handlers (people.person and sis.student_guardian events), notices and purpose settings through the REST
 * API as a tenant administrator.
 */
final class ConsentFixtures {

    static final String PHOTOS = "photo_video_publication";
    static final String MARKETING = "marketing_comms";
    static final String BEHAVIOURAL = "behavioural_analytics";
    static final String NOTICE = "consent_notice";
    static final String MY_CONSENTS = "/v1/privacy/my/consents";
    static final String CONSENT_RECORDS = "/v1/privacy/consent-records";
    static final String[] SELF_SERVICE = {"privacy.my_consent.read", "privacy.my_consent.create",
        "privacy.my_consent.withdraw"};
    static final String ENGLISH = "We publish photos of school events on the school website.";
    static final String HINDI = "Hum school ke karyakramon ki tasveerein school ki website par dalte hain.";
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);
    private static final String[] ADMINISTRATION = {"privacy.notice.create", "privacy.notice_version.create",
        "privacy.notice_translation.create", "privacy.notice_version.publish", "privacy.tenant_purpose.read",
        "privacy.tenant_purpose.update"};

    private final PrivacyCalls calls;
    private final ModuleDatabase database;
    private final SubjectSync subjects;
    private final GuardianLinkSync links;

    ConsentFixtures(PrivacyCalls calls, ModuleDatabase database, SubjectSync subjects, GuardianLinkSync links) {
        this.calls = calls;
        this.database = database;
        this.subjects = subjects;
        this.links = links;
    }

    /**
     * Switches the purposes on in the tenant under one published notice (code consent_notice, in English and Hindi)
     * and answers the id of its active version.
     */
    String enable(UUID tenant, String... purposeCodes) throws Exception {
        String admin = calls.administrator(tenant, ADMINISTRATION);
        String noticeId = notice(admin, NOTICE, List.of("en", "hi"), purposeCodes);
        String versionId = calls.get("/v1/privacy/notices/" + noticeId + "/versions", calls.administrator(tenant,
                "privacy.notice_version.read")).body().path("data").get(0).path("id").asString();
        JsonNode purposes = calls.get("/v1/privacy/purposes?size=500", admin).body().path("data");
        for (String code : purposeCodes) {
            for (JsonNode purpose : purposes) {
                if (code.equals(purpose.path("code").asString())) {
                    PrivacyCalls.Reply enabled = calls.patch("/v1/privacy/purposes/" + purpose.path("id").asString(),
                            admin, "\"0\"", Map.of("enabled", true, "noticeId", noticeId));
                    assertThat(enabled.status()).as(enabled.body().toString()).isEqualTo(200);
                }
            }
        }
        return versionId;
    }

    /** A published notice of the tenant with translations into the languages; answers the notice's id. */
    String notice(String admin, String code, List<String> languages, String... purposeCodes) throws Exception {
        PrivacyCalls.Reply notice = calls.post("/v1/privacy/notices", admin, Map.of("code", code,
                "title", "Consent notice"));
        assertThat(notice.status()).as(notice.body().toString()).isEqualTo(201);
        String versions = "/v1/privacy/notices/" + notice.text("id") + "/versions";
        String versionId = calls.post(versions, admin, Map.of("purposeCodes", List.of(purposeCodes),
                "grievanceContact", Map.of("name", "Grievance Officer", "email", "grievance@school.example")))
                .text("id");
        for (String language : languages) {
            PrivacyCalls.Reply translated = calls.post(versions + "/" + versionId + "/translations", admin,
                    Map.of("language", language, "bodyMarkdown", "en".equals(language) ? ENGLISH : HINDI));
            assertThat(translated.status()).as(translated.body().toString()).isEqualTo(201);
        }
        assertThat(calls.post(versions + "/" + versionId + ":publish", admin, Map.of()).status()).isEqualTo(200);
        return notice.text("id");
    }

    /** RecordMyConsent's body. */
    static Map<String, Object> decision(UUID subject, String purposeCode, String scope, String decision,
            String noticeVersionId, String language) {
        return Map.of("subjectPersonId", subject.toString(), "purposeCode", purposeCode, "scope", scope,
                "decision", decision, "noticeVersionId", noticeVersionId, "noticeLanguage", language);
    }

    /** RecordConsentOnBehalf's body: a paper form signed by the giver. */
    static Map<String, Object> paperForm(UUID subject, UUID giver, String capacity, String purposeCode, String scope,
            String decision, String noticeVersionId) {
        Map<String, Object> body = new HashMap<>(decision(subject, purposeCode, scope, decision, noticeVersionId, "en"));
        body.put("giverPersonId", giver.toString());
        body.put("giverCapacity", capacity);
        body.put("evidenceFileId", UUID.randomUUID().toString());
        return body;
    }

    /** The person, 30 years old, as people.person's event tells privacy. */
    void adult(UUID tenant, UUID person) {
        born(tenant, person, TODAY.minusYears(30));
    }

    /** The person, 10 years old. */
    void minor(UUID tenant, UUID person) {
        born(tenant, person, TODAY.minusYears(10));
    }

    /** An active legal-guardian link, as sis.student_guardian's event tells privacy. */
    void guardian(UUID tenant, UUID guardian, UUID student) {
        UUID link = UUID.randomUUID();
        deliver(tenant, () -> links.apply(event(tenant, "sis.student_guardian.created", link, "{\"id\":\"" + link
                + "\",\"studentPersonId\":\"" + student + "\",\"guardianPersonId\":\"" + guardian
                + "\",\"guardianUserId\":null,\"isLegalGuardian\":true,\"hasCustody\":true,\"status\":\"active\","
                + "\"rowVersion\":1}")));
    }

    /** Runs the work in a read transaction of the tenant (row-level security applies). */
    <T> T read(UUID tenant, Supplier<T> work) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).read(work::get));
    }

    private void born(UUID tenant, UUID person, LocalDate birthDate) {
        deliver(tenant, () -> subjects.apply(event(tenant, "people.person.created", person, "{\"id\":\"" + person
                + "\",\"dateOfBirth\":\"" + birthDate + "\",\"rowVersion\":1}")));
    }

    private void deliver(UUID tenant, Supplier<Boolean> handler) {
        boolean changed = CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).write(handler::get));
        assertThat(changed).isTrue();
    }

    private static EventEnvelope event(UUID tenant, String type, UUID id, String data) {
        String aggregate = type.substring(0, type.lastIndexOf('.'));
        return new EventEnvelope(UUID.randomUUID(), aggregate.substring(0, aggregate.indexOf('.')), type,
                Optional.of(tenant), aggregate, id, Instant.now(), data, Optional.of(TRACE));
    }
}
