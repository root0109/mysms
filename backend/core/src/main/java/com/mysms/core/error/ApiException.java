package com.mysms.core.error;

import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Base of every expected, client-visible failure: HTTP status, stable problem+json `code`, and a `detail` with ids
 * only, never names or other personal data.
 */
public abstract class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final HttpStatus status;
    private final String code;
    private final List<FieldError> errors;

    protected ApiException(HttpStatus status, String code, String detail, List<FieldError> errors) {
        super(detail);
        this.status = status;
        this.code = code;
        this.errors = List.copyOf(errors);
    }

    protected ApiException(HttpStatus status, String code, String detail) {
        this(status, code, detail, List.of());
    }

    public final HttpStatus status() {
        return status;
    }

    public final String code() {
        return code;
    }

    public final List<FieldError> errors() {
        return errors;
    }
}
