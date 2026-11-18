package com.mysms.core.validation;

import com.mysms.core.error.FieldError;
import com.mysms.core.error.ValidationFailedException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The single trigger of DTO validation: on create and update bodies, and again on the merged model after a patch.
 * Violations become 422 VALIDATION_FAILED with one entry per field.
 */
public final class DtoValidator {

    private final Validator validator;

    public DtoValidator(Validator validator) {
        this.validator = validator;
    }

    public <T> void validate(T dto) {
        validateExcept(dto, Set.of());
    }

    /**
     * As {@link #validate}, ignoring the listed properties: the write-only secrets of a merged model, which a response
     * never carries (their plaintext was validated with the request, and the stored envelope is checked instead).
     */
    public <T> void validateExcept(T dto, Set<String> ignored) {
        Set<ConstraintViolation<T>> violations = validator.validate(dto);
        violations = violations.stream().filter(v -> !ignored.contains(v.getPropertyPath().toString()))
                .collect(Collectors.toSet());
        if (violations.isEmpty()) {
            return;
        }
        List<FieldError> errors = violations.stream()
                .map(v -> new FieldError(v.getPropertyPath().toString(), v.getMessage(), code(v)))
                .sorted(Comparator.comparing(FieldError::field).thenComparing(FieldError::message))
                .toList();
        throw new ValidationFailedException(errors);
    }

    private static String code(ConstraintViolation<?> violation) {
        return violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
    }
}
