package com.mysms.core.json;

import java.util.Locale;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * Reads a {@link Boolean} from {@code true} / {@code false}, as JSON literals or as the strings {@code "true"} /
 * {@code "false"} (any letter case, trimmed); a JSON {@code null} stays null. Numbers and other words ({@code 1},
 * {@code "yes"}) are refused rather than guessed.
 */
public final class StrictBooleanDeserializer extends ValueDeserializer<Boolean> {

    @Override
    public Boolean deserialize(JsonParser parser, DeserializationContext context) {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_TRUE) {
            return Boolean.TRUE;
        }
        if (token == JsonToken.VALUE_FALSE) {
            return Boolean.FALSE;
        }
        if (token != JsonToken.VALUE_STRING) {
            return (Boolean) context.handleUnexpectedToken(Boolean.class, parser);
        }
        String text = parser.getString().trim().toLowerCase(Locale.ROOT);
        return switch (text) {
            case "true" -> Boolean.TRUE;
            case "false" -> Boolean.FALSE;
            default -> throw InvalidFormatException.from(parser, "not a boolean", parser.getString(), Boolean.class);
        };
    }
}
