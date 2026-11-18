package com.mysms.core.json;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The one JSON mapper of MySMS (HTTP, inter-module calls, events, page tokens), so the wire format is decided in one
 * place. Every setting is stated even where it equals Jackson's default, because it is part of the contract:
 * ISO-8601 dates held in UTC, unknown properties ignored, a missing primitive an error, and no single value accepted
 * for an array. Immutable and thread-safe.
 */
public final class JsonMappers {

    private JsonMappers() {
    }

    /** A new mapper with the MySMS settings. */
    public static JsonMapper create() {
        return JsonMapper.builder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .enable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .disable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .build();
    }
}
