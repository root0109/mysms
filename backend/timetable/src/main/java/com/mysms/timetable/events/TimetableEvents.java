package com.mysms.timetable.events;

import java.util.Map;
import java.util.UUID;

/**
 * Domain events timetable publishes through its outbox: {@code <schema>.<table>.created|updated|deleted} with the row
 * id; dependent modules keep their copies and references fresh from these.
 */
public final class TimetableEvents {

    public static final String CREATED = ".created";
    public static final String UPDATED = ".updated";
    public static final String DELETED = ".deleted";

    private TimetableEvents() {
    }

    public static Map<String, Object> payload(UUID id) {
        return Map.of("id", id.toString());
    }
}
