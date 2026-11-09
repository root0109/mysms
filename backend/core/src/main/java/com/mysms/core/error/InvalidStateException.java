package com.mysms.core.error;

import org.springframework.http.HttpStatus;

/** The aggregate is not in a state that allows the operation (409 INVALID_STATE_TRANSITION). */
public final class InvalidStateException extends ApiException {

    private static final long serialVersionUID = 1L;

    public InvalidStateException(String detail) {
        super(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION", detail);
    }
}
