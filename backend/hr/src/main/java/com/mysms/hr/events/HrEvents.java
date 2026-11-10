package com.mysms.hr.events;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Domain events hr publishes through its outbox: {@code <schema>.<table>.created|updated|deleted} with the row id.
 * Dependent modules keep their copies and logical references fresh from these (database/logical-references.md).
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
     * The payload of a staff record (hr.staff)'s events: besides its id, whose record it is — login account (may be absent)
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
}
