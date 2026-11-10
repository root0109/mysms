package com.mysms.core.error;

/**
 * Gives a field error its sentence from the message catalogue by the error's code (modelled on the owner's
 * {@code common} module): {@code NotNull} becomes the catalogue's {@code constraint.NotNull} in the caller's
 * language. Every field error of a response passes through here in {@link GlobalExceptionHandler}, whether bean
 * validation or a service raised it, so a translated code is translated everywhere at once.
 *
 * <p>Only codes whose sentence needs nothing but the code are in the catalogue; an error with any other code keeps
 * the sentence it was raised with. So does an error that carries a known code but a sentence of its own (a service
 * saying more than "must not be null"): the catalogue replaces the standard sentence of a code, never a specific one.
 * Field errors Spring raises for request parameters (400) do not come this way; they keep the validator's sentence.
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
