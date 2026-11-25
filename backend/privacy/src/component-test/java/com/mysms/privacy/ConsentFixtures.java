package com.mysms.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.privacy.events.GuardianLinkSync;
import com.mysms.privacy.events.StaffSync;
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
    static final String MY_REQUESTS = "/v1/privacy/my/requests";
    static final String RIGHTS_REQUESTS = "/v1/privacy/rights-requests";
    static final String[] REQUESTER = {"privacy.my_request.create", "privacy.my_request.read"};
    static final String[] RIGHTS_STAFF = {"privacy.rights_request.read", "privacy.rights_request.verify_identity",
        "privacy.rights_request.assign", "privacy.rights_request.extend", "privacy.rights_request.reject",
        "privacy.rights_request.close", "privacy.rights_request.fulfil"};
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
        publish(admin, notice.text("id"), languages, false, purposeCodes);
        return notice.text("id");
    }

    /**
     * Publishes a new version of the tenant's notice (the one {@link #enable} wrote) in English that asks everyone
     * again; answers the version's id.
     */
    String reconsent(UUID tenant, String... purposeCodes) throws Exception {
        String noticeId = calls.get("/v1/privacy/notices", calls.administrator(tenant, "privacy.notice.read")).body()
                .path("data").get(0).path("id").asString();
        return publish(calls.administrator(tenant, ADMINISTRATION), noticeId, List.of("en"), true, purposeCodes);
    }

    /** A new version of the notice, translated and published; answers the version's id. */
    private String publish(String admin, String noticeId, List<String> languages, boolean requiresReconsent,
            String... purposeCodes) throws Exception {
        String versions = "/v1/privacy/notices/" + noticeId + "/versions";
        PrivacyCalls.Reply version = calls.post(versions, admin, Map.of("purposeCodes", List.of(purposeCodes),
                "requiresReconsent", requiresReconsent,
                "grievanceContact", Map.of("name", "Grievance Officer", "email", "grievance@school.example")));
        assertThat(version.status()).as(version.body().toString()).isEqualTo(201);
        for (String language : languages) {
            PrivacyCalls.Reply translated = calls.post(versions + "/" + version.text("id") + "/translations", admin,
                    Map.of("language", language, "bodyMarkdown", "en".equals(language) ? ENGLISH : HINDI));
            assertThat(translated.status()).as(translated.body().toString()).isEqualTo(201);
        }
        assertThat(calls.post(versions + "/" + version.text("id") + ":publish", admin, Map.of()).status())
                .isEqualTo(200);
        return version.text("id");
    }

    /** RecordMyConsent's body. */
    static Map<String, Object> decision(UUID subject, String purposeCode, String scope, String decision,
            String noticeVersionId, String language) {
        return Map.of("subjectPersonId", subject.toString(), "purposeCode", purposeCode, "scope", scope,
                "decision", decision, "noticeVersionId", noticeVersionId, "noticeLanguage", language);
    }

    /**
     * SubmitMyRequest's body, with the details its type requires: a correction names a record and one field, a
     * grievance its category.
     */
    static Map<String, Object> rightsRequest(UUID subject, String type, String description) {
        Map<String, Object> body = new HashMap<>(Map.of("type", type, "subjectPersonId", subject.toString(),
                "description", description));
        if ("correction".equals(type)) {
            body.put("correction", Map.of("module", "people", "recordType", "person",
                    "fields", List.of(Map.of("field", "surname", "requestedValue", "Sharma"))));
        }
        if ("grievance".equals(type)) {
            body.put("grievanceCategory", "other");
        }
        return body;
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

    /** The person with no birth date known: neither a minor nor an adult can be told. */
    void ageUnknown(UUID tenant, UUID person) {
        deliver(tenant, () -> subjects.apply(event(tenant, "people.person.created", person,
                "{\"id\":\"" + person + "\",\"rowVersion\":1}")));
    }

    /** The person's birth date corrected to 30 years ago: the person row's second version makes them an adult. */
    void grownUp(UUID tenant, UUID person) {
        deliver(tenant, () -> subjects.apply(event(tenant, "people.person.updated", person,
                person(person, TODAY.minusYears(30), 2))));
    }

    /** An active legal-guardian link, as sis.student_guardian's event tells privacy; answers the link's id. */
    UUID guardian(UUID tenant, UUID guardian, UUID student) {
        UUID link = UUID.randomUUID();
        deliver(tenant, () -> links.apply(event(tenant, "sis.student_guardian.created", link,
                link(link, student, guardian, 1))));
        return link;
    }

    /** The link deleted in sis (its second version), as sis.student_guardian's event tells privacy. */
    void unlink(UUID tenant, UUID link, UUID guardian, UUID student) {
        deliver(tenant, () -> links.apply(event(tenant, "sis.student_guardian.deleted", link,
                link(link, student, guardian, 2))));
    }

    /** A live student record of the person, as sis.student's event tells privacy; answers the record's id. */
    UUID student(UUID tenant, UUID person) {
        return student(tenant, person, UUID.randomUUID());
    }

    /** A live student record of the person at the institution; answers the record's id. */
    UUID student(UUID tenant, UUID person, UUID institution) {
        UUID record = UUID.randomUUID();
        deliver(tenant, () -> subjects.apply(event(tenant, "sis.student.created", record, "{\"id\":\"" + record
                + "\",\"userId\":\"" + UUID.randomUUID() + "\",\"personId\":\"" + person
                + "\",\"institutionId\":\"" + institution + "\",\"rowVersion\":1}")));
        return record;
    }

    /**
     * A live staff record of the person with a login account, as hr.staff's event tells privacy; answers the
     * record's id.
     */
    UUID staff(UUID tenant, StaffSync staffSync, UUID person, UUID userId) {
        UUID record = UUID.randomUUID();
        deliver(tenant, () -> staffSync.apply(event(tenant, "hr.staff.created", record, "{\"id\":\"" + record
                + "\",\"userId\":\"" + userId + "\",\"personId\":\"" + person
                + "\",\"institutionId\":\"" + UUID.randomUUID() + "\",\"rowVersion\":1}")));
        return record;
    }

    /** Runs the work in a read transaction of the tenant (row-level security applies). */
    <T> T read(UUID tenant, Supplier<T> work) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).read(work::get));
    }

    /** Runs the work in a write transaction of the tenant (row-level security applies). */
    <T> T write(UUID tenant, Supplier<T> work) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).write(work::get));
    }

    private void born(UUID tenant, UUID person, LocalDate birthDate) {
        deliver(tenant, () -> subjects.apply(event(tenant, "people.person.created", person,
                person(person, birthDate, 1))));
    }

    private static String person(UUID person, LocalDate birthDate, int version) {
        return "{\"id\":\"" + person + "\",\"dateOfBirth\":\"" + birthDate + "\",\"rowVersion\":" + version + "}";
    }

    private static String link(UUID link, UUID student, UUID guardian, int version) {
        return "{\"id\":\"" + link + "\",\"studentPersonId\":\"" + student + "\",\"guardianPersonId\":\"" + guardian
                + "\",\"guardianUserId\":null,\"isLegalGuardian\":true,\"hasCustody\":true,\"status\":\"active\","
                + "\"rowVersion\":" + version + "}";
    }

    private void deliver(UUID tenant, Supplier<Boolean> handler) {
        boolean changed = CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).write(handler::get));
        assertThat(changed).isTrue();
    }

    /** An event of another module about one of its rows, as the inbox delivers it to privacy's handlers. */
    static EventEnvelope event(UUID tenant, String type, UUID id, String data) {
        String aggregate = type.substring(0, type.lastIndexOf('.'));
        return new EventEnvelope(UUID.randomUUID(), aggregate.substring(0, aggregate.indexOf('.')), type,
                Optional.of(tenant), aggregate, id, Instant.now(), data, Optional.of(TRACE));
    }
}
