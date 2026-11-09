package com.mysms.core.error;

import org.springframework.http.HttpStatus;

/** A unique business key is already taken (409 DUPLICATE). */
public final class DuplicateResourceException extends ApiException {

    private static final long serialVersionUID = 1L;

    public DuplicateResourceException(String detail) {
        super(HttpStatus.CONFLICT, "DUPLICATE", detail);
    }
}
