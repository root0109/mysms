package com.mysms.comms.events;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Domain events comms publishes through its outbox: {@code <schema>.<table>.created|updated|deleted} with the row id;
 * dependent modules keep their copies and references fresh from these.
 */
public final class CommsEvents {

    public static final String CREATED = ".created";
    public static final String UPDATED = ".updated";
    public static final String DELETED = ".deleted";

    private CommsEvents() {
    }

    public static Map<String, Object> payload(UUID id) {
        return Map.of("id", id.toString());
    }

    /**
     * The payload of a message template's events: besides its id, its institution (absent for a tenant-wide one),
     * whether it is usable and the row version the event reflects. people keeps which templates a greeting may name
     * from these (people.greeting_template); the version lets it ignore a late event.
     */
    public static Map<String, Object> template(UUID id, UUID institutionId, boolean active, int rowVersion) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", id.toString());
        if (institutionId != null) {
            payload.put("institutionId", institutionId.toString());
        }
        payload.put("isActive", active);
        payload.put("rowVersion", rowVersion);
        return payload;
    }
}
