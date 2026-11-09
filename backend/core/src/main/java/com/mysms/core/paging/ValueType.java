package com.mysms.core.paging;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;

/** The type of a filterable or sortable field: how a literal from a request is converted to a bind value. */
public enum ValueType {
    STRING,
    INTEGER,
    BOOLEAN,
    UUID,
    TIMESTAMP,
    DATE,
    DECIMAL;

    /** Converts a literal to the bind value, or throws IllegalArgumentException when it does not fit the type. */
    public Object parse(String literal) {
        try {
            return switch (this) {
                case STRING -> literal;
                case INTEGER -> Long.parseLong(literal);
                case BOOLEAN -> parseBoolean(literal);
                case UUID -> java.util.UUID.fromString(literal);
                case TIMESTAMP -> OffsetDateTime.parse(literal);
                case DATE -> LocalDate.parse(literal);
                case DECIMAL -> new BigDecimal(literal);
            };
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new IllegalArgumentException("not a " + name().toLowerCase(Locale.ROOT) + ": " + literal, e);
        }
    }

    /** The canonical text of a bind value (inverse of {@link #parse}), used inside page tokens. */
    public String format(Object value) {
        return switch (this) {
            case TIMESTAMP -> ((OffsetDateTime) value).toString();
            case UUID -> ((UUID) value).toString();
            default -> String.valueOf(value);
        };
    }

    private static Boolean parseBoolean(String literal) {
        if ("true".equals(literal)) {
            return Boolean.TRUE;
        }
        if ("false".equals(literal)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("not a boolean: " + literal);
    }
}
