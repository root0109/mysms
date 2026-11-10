package com.mysms.core.json;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The one JSON mapper of MySMS, built by hand (the {@code CommonConfig} of the owner's {@code common} module): every
 * HTTP request and response, every call one module makes to another, every event on the bus and every page token is
 * written and read by a mapper from here, so the wire format is decided in one place and does not change with a
 * library's defaults. Every setting below is stated even where it equals Jackson's default, because it is part of
 * the contract (api/README "Data formats").
 *
 * <ul>
 * <li>Dates and times are ISO-8601 strings, never numbers; an instant read with an offset is held in UTC.</li>
 * <li>Unknown properties are ignored when reading: a newer client or module may send fields this one does not know
 * (api/README: additive changes do not change the version).</li>
 * <li>A missing value for a primitive is an error rather than a silent zero.</li>
 * <li>A single value is not accepted where an array is expected: a list stays a list on the wire, which the
 * contracts and the validation rely on. (The {@code common} mapper accepts it; MySMS deliberately does not.)</li>
 * <li>Properties are written in declaration order, and empty optional values are left out by the contract classes
 * themselves ({@code @JsonInclude} per field), not by a second "slim" mapper.</li>
 * </ul>
 * The mapper is immutable and thread-safe; build it once per process and share it.
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
