package com.mysms.core.error;

import java.util.List;
import org.springframework.http.HttpStatus;

/** Field validation failed (422 VALIDATION_FAILED with `errors[]`). */
public final class ValidationFailedException extends ApiException {

    private static final long serialVersionUID = 1L;

    public ValidationFailedException(List<FieldError> errors) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_FAILED", "The request has invalid fields.", errors);
    }
}
