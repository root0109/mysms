package com.mysms.hrms.timeattendance.events;

import java.util.Map;
import java.util.UUID;

/**
 * Domain events time_attendance publishes through its outbox: {@code <schema>.<table>.created|updated|deleted} with
 * the row id; dependent modules keep their copies and references fresh from these.
 */
public final class TimeAttendanceEvents {

    public static final String CREATED = ".created";
    public static final String UPDATED = ".updated";
    public static final String DELETED = ".deleted";

    private TimeAttendanceEvents() {
    }

    public static Map<String, Object> payload(UUID id) {
        return Map.of("id", id.toString());
    }
}
