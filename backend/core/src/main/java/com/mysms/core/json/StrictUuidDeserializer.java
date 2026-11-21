package com.mysms.core.json;

import java.util.UUID;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * Reads a UUID only in its canonical 36-character form (any letter case, no surrounding spaces).
 * {@link UUID#fromString} alone also accepts short forms such as {@code 1-2-3-4-5}, which would let one row be
 * addressed by many spellings.
 */
public final class StrictUuidDeserializer extends ValueDeserializer<UUID> {

    private static final int CANONICAL_LENGTH = 36;

    @Override
    public UUID deserialize(JsonParser parser, DeserializationContext context) {
        if (!parser.hasToken(JsonToken.VALUE_STRING)) {
            return (UUID) context.handleUnexpectedToken(UUID.class, parser);
        }
        String text = parser.getString();
        if (text.length() == CANONICAL_LENGTH) {
            try {
                UUID uuid = UUID.fromString(text);
                if (uuid.toString().equalsIgnoreCase(text)) {
                    return uuid;
                }
            } catch (IllegalArgumentException e) {
                // not a UUID: refused below
            }
        }
        throw InvalidFormatException.from(parser, "not a canonical UUID", text, UUID.class);
    }
}
