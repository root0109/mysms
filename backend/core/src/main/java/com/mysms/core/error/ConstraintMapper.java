package com.mysms.core.error;

/**
 * Gives a field error the catalogue sentence of its code in the caller's language ({@code NotNull} →
 * {@code constraint.NotNull}). Only a code's standard sentence is replaced; an error with an unknown code or a
 * sentence of its own keeps it.
 */
public final class ConstraintMapper {

    /** Prefix of the catalogue keys of field-error codes. */
    public static final String KEY_PREFIX = "constraint.";

    private final MessageUtil messages;

    public ConstraintMapper(MessageUtil messages) {
        this.messages = messages;
    }

    /** The error as the caller reads it. */
    public FieldError localized(FieldError error) {
        if (error.code() == null || error.message() == null) {
            return error;
        }
        String key = KEY_PREFIX + error.code();
        if (!error.message().equals(messages.getStandardMessage(key))) {
            return error;
        }
        String message = messages.getMessage(key, error.message());
        return message.equals(error.message()) ? error : new FieldError(error.field(), message, error.code());
    }
}
