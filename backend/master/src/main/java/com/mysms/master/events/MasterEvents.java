package com.mysms.master.events;

import java.util.Map;
import java.util.UUID;

/**
 * Domain events master publishes through its outbox; dependent modules keep their copies and logical references fresh
 * from them. Payloads carry ids only.
 */
public final class MasterEvents {

    public static final String CREATED = ".created";
    public static final String UPDATED = ".updated";
    public static final String DELETED = ".deleted";

    private MasterEvents() {
    }

    public static Map<String, Object> payload(UUID id) {
        return Map.of("id", id.toString());
    }
}
