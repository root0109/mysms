package com.mysms.assessment.mapper;

import com.mysms.core.json.JsonMappers;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * A grading scheme's criteria between the contract (any JSON value) and the {@code criteria} jsonb column (its text).
 * Kept outside {@link GradingSchemeMapper} on purpose: MapStruct would otherwise take a {@code Object → String}
 * method of the mapper as the conversion for every {@code String} property and encode the name as JSON too.
 */
public final class GradingCriteriaJson {

    private static final JsonMapper JSON = JsonMappers.create();

    private GradingCriteriaJson() {
    }

    /** The stored text as a JSON value; no criteria is {@code null}. */
    public static Object read(String criteria) {
        if (criteria == null || criteria.isBlank()) {
            return null;
        }
        try {
            return JSON.readValue(criteria, Object.class);
        } catch (JacksonException invalidStoredJson) {
            throw new IllegalStateException("Stored grading scheme criteria is invalid JSON.", invalidStoredJson);
        }
    }

    /** The criteria as the text stored in the jsonb column; no criteria is {@code null}. */
    public static String write(Object criteria) {
        if (criteria == null) {
            return null;
        }
        try {
            return JSON.writeValueAsString(criteria);
        } catch (JacksonException invalidCriteria) {
            throw new IllegalArgumentException("Grading scheme criteria cannot be encoded.", invalidCriteria);
        }
    }
}
