package com.mysms.core.error;

import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * One failure as a client sees it, independent of the transport: REST writes it as problem+json ({@link Problems}),
 * gRPC as a {@code google.rpc.Status} with the same code, so both transports fail identically.
 */
public record Problem(HttpStatus status, String code, String detail, List<FieldError> errors) {

    public Problem {
        errors = List.copyOf(errors);
    }

    public static Problem of(ApiException exception) {
        return new Problem(exception.status(), exception.code(), exception.getMessage(), exception.errors());
    }

    /** The answer to anything unexpected: no detail, nothing of the failure leaks. */
    public static Problem internal() {
        return new Problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL", null, List.of());
    }
}
