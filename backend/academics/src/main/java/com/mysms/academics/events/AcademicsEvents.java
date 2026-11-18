package com.mysms.academics.events;

import java.util.Map;
import java.util.UUID;

/**
 * Domain events academics publishes through its outbox: {@code <schema>.<table>.created|updated|deleted} with the row
 * id. Dependent modules keep their copies and logical references fresh from these.
 */
public final class AcademicsEvents {

    public static final String CREATED = ".created";
    public static final String UPDATED = ".updated";
    public static final String DELETED = ".deleted";

    private AcademicsEvents() {
    }

    public static Map<String, Object> payload(UUID id) {
        return Map.of("id", id.toString());
    }
}
