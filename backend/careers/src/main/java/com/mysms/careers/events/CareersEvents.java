package com.mysms.careers.events;

import java.util.Map;
import java.util.UUID;

/**
 * Domain events careers publishes through its outbox: {@code <schema>.<table>.created|updated|deleted} with the row id.
 * Dependent modules keep their copies and logical references fresh from these (database/logical-references.md).
 */
public final class CareersEvents {

    public static final String CREATED = ".created";
    public static final String UPDATED = ".updated";
    public static final String DELETED = ".deleted";

    private CareersEvents() {
    }

    public static Map<String, Object> payload(UUID id) {
        return Map.of("id", id.toString());
    }
}
