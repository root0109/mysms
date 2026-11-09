package com.mysms.core.error;

import org.springframework.http.HttpStatus;

/** A malformed request: bad paging token, filter, sort or parameter (400 INVALID_ARGUMENT / INVALID_FILTER). */
public final class InvalidArgumentException extends ApiException {

    private static final long serialVersionUID = 1L;

    public static final String INVALID_ARGUMENT = "INVALID_ARGUMENT";
    public static final String INVALID_FILTER = "INVALID_FILTER";

    public InvalidArgumentException(String detail) {
        this(INVALID_ARGUMENT, detail);
    }

    public InvalidArgumentException(String code, String detail) {
        super(HttpStatus.BAD_REQUEST, code, detail);
    }
}
