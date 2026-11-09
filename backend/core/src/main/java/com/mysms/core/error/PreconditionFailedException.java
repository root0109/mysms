package com.mysms.core.error;

import org.springframework.http.HttpStatus;

/** The If-Match ETag is stale: the row changed since it was read (412 ETAG_MISMATCH). */
public final class PreconditionFailedException extends ApiException {

    private static final long serialVersionUID = 1L;

    public PreconditionFailedException(String detail) {
        super(HttpStatus.PRECONDITION_FAILED, "ETAG_MISMATCH", detail);
    }
}
