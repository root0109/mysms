package com.mysms.core.error;

import org.springframework.http.HttpStatus;

/** Authenticated, but this caller may not do this to this resource (403 PERMISSION_DENIED). */
public final class PermissionDeniedException extends ApiException {

    private static final long serialVersionUID = 1L;

    public PermissionDeniedException(String detail) {
        super(HttpStatus.FORBIDDEN, "PERMISSION_DENIED", detail);
    }
}
