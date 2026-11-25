package com.mysms.hr.events;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Domain events hr publishes through its outbox: {@code <schema>.<table>.created|updated|deleted} with the row id;
 * dependent modules keep their copies and references fresh from these.
 */
public final class HrEvents {

    public static final String CREATED = ".created";
    public static final String UPDATED = ".updated";
    public static final String DELETED = ".deleted";

    private HrEvents() {
    }

    public static Map<String, Object> payload(UUID id) {
        return Map.of("id", id.toString());
    }

    /**
     * The payload of an hr.staff event: the record's login account (may be absent), person, institution, joining date
     * and row version. iam keeps its member records from these for iam.GetMe, people its member links for birthdays
     * and work anniversaries; the version lets them ignore a late event.
     */
    public static Map<String, Object> member(UUID id, UUID userId, UUID personId, UUID institutionId,
            LocalDate joiningDate, int rowVersion) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", id.toString());
        payload.put("userId", userId == null ? null : userId.toString());
        payload.put("personId", personId == null ? null : personId.toString());
        payload.put("institutionId", institutionId == null ? null : institutionId.toString());
        payload.put("joiningDate", joiningDate == null ? null : joiningDate.toString());
        payload.put("rowVersion", rowVersion);
        return payload;
    }
}
