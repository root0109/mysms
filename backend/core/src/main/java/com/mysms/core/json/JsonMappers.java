package com.mysms.core.json;

import java.util.UUID;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * The one JSON mapper of MySMS (HTTP, inter-module calls, events, page tokens), so the wire format is decided in one
 * place. Every MySMS setting is stated even where it equals Jackson's default, because it is part of the contract:
 * ISO-8601 dates held in UTC, unknown properties ignored, a primitive set to null an error (an absent one takes its
 * default since Jackson 3.2, databind#5734; contract DTOs carry no primitives), no single value accepted for an array,
 * enums by name only (any letter case, never by number), UUIDs only in canonical form and boxed booleans only as
 * true/false ({@link StrictUuidDeserializer}, {@link StrictBooleanDeserializer}). Configured the reference service's
 * way: members without a view are in every view, and the Jackson modules on the classpath are registered, so a module
 * added to the build joins the wire format. Immutable and thread-safe.
 */
public final class JsonMappers {

    private JsonMappers() {
    }

    /** A new mapper with the MySMS settings. */
    public static JsonMapper create() {
        return JsonMapper.builder()
                .findAndAddModules()
                .addModule(new SimpleModule("mysms")
                        .addDeserializer(UUID.class, new StrictUuidDeserializer())
                        .addDeserializer(Boolean.class, new StrictBooleanDeserializer()))
                .enable(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
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
