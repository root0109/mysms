package com.mysms.core.error;

import org.springframework.http.HttpStatus;

/**
 * The operation is part of the contract but this release does not serve it yet (501 NOT_IMPLEMENTED). Explicit,
 * so a client never mistakes a missing feature for a missing resource.
 */
public final class NotImplementedException extends ApiException {

    public static final String CODE = "NOT_IMPLEMENTED";
    public static final String DETAIL = "This operation is part of the contract but is not available in this release.";
    private static final long serialVersionUID = 1L;

    public NotImplementedException() {
        this(DETAIL);
    }

    public NotImplementedException(String detail) {
        super(HttpStatus.NOT_IMPLEMENTED, CODE, detail);
    }
}
