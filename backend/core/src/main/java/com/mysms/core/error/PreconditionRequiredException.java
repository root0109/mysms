package com.mysms.core.error;

import org.springframework.http.HttpStatus;

/** An If-Match header carrying the ETag of the last read is required (428). */
public final class PreconditionRequiredException extends ApiException {

    private static final long serialVersionUID = 1L;

    public PreconditionRequiredException(String detail) {
        super(HttpStatus.PRECONDITION_REQUIRED, "PRECONDITION_REQUIRED", detail);
    }
}
