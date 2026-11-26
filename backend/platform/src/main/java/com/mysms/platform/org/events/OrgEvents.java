package com.mysms.platform.org.events;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Domain events org publishes through its outbox: {@code <schema>.<table>.created|updated|deleted} with the row id.
 * Dependent modules keep their copies and logical references fresh from these.
 */
public final class OrgEvents {

    public static final String CREATED = ".created";
    public static final String UPDATED = ".updated";
    public static final String DELETED = ".deleted";

    private OrgEvents() {
    }

    public static Map<String, Object> payload(UUID id) {
        return Map.of("id", id.toString());
    }

    /**
     * The payload of an institution's events: besides its id, its own time zone (absent when it keeps the tenant's)
     * and the row version the event reflects. timetable keeps each school's zone from these (timetable never calls
     * org); the version lets it ignore a late event.
     */
    public static Map<String, Object> institution(UUID id, String timezone, int rowVersion) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", id.toString());
        if (timezone != null && !timezone.isBlank()) {
            payload.put("timezone", timezone);
        }
        payload.put("rowVersion", rowVersion);
        return payload;
    }
}
