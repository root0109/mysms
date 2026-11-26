package com.mysms.students.sis.events;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Domain events sis publishes through its outbox: {@code <schema>.<table>.created|updated|deleted} with the row id.
 * Dependent modules keep their copies and logical references fresh from these.
 */
public final class SisEvents {

    public static final String CREATED = ".created";
    public static final String UPDATED = ".updated";
    public static final String DELETED = ".deleted";

    private SisEvents() {
    }

    public static Map<String, Object> payload(UUID id) {
        return Map.of("id", id.toString());
    }

    /**
     * The payload of a student record (sis.student)'s events: besides its id, whose record it is — login account (may be absent)
     * and person — its institution and the row version the event reflects. iam keeps each account's and person's
     * records from these (iam.member_record) to answer iam.GetMe; the version lets it ignore a late event.
     */
    public static Map<String, Object> member(UUID id, UUID userId, UUID personId, UUID institutionId, int rowVersion) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", id.toString());
        payload.put("userId", userId == null ? null : userId.toString());
        payload.put("personId", personId == null ? null : personId.toString());
        payload.put("institutionId", institutionId == null ? null : institutionId.toString());
        payload.put("rowVersion", rowVersion);
        return payload;
    }

    /**
     * The payload of a guardian link (sis.student_guardian)'s events: besides its id, whom it joins — the student
     * record and its person, the guardian record, its person and login account (may be absent) — whether the guardian
     * is a legal guardian and has custody, its status and the row version the event reflects. privacy keeps who may
     * consent for a minor from these (privacy.guardian_link), comms whose feedback threads a guardian reaches
     * (comms.guardian_link); the version lets them ignore a late event.
     */
    public static Map<String, Object> guardianLink(UUID id, UUID studentId, UUID studentPersonId, UUID guardianId,
            UUID guardianPersonId, UUID guardianUserId, boolean isLegalGuardian, boolean hasCustody, String status,
            int rowVersion) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", id.toString());
        payload.put("studentId", studentId.toString());
        payload.put("guardianId", guardianId.toString());
        payload.put("studentPersonId", studentPersonId.toString());
        payload.put("guardianPersonId", guardianPersonId.toString());
        payload.put("guardianUserId", guardianUserId == null ? null : guardianUserId.toString());
        payload.put("isLegalGuardian", isLegalGuardian);
        payload.put("hasCustody", hasCustody);
        payload.put("status", status);
        payload.put("rowVersion", rowVersion);
        return payload;
    }
}
