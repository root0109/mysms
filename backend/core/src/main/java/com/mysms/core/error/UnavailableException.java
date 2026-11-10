package com.mysms.core.error;

import org.springframework.http.HttpStatus;

/**
 * A service this operation depends on could not answer, or is not configured for this deployment (503 UNAVAILABLE).
 * The caller may retry later; nothing was changed. The detail names the dependency, never its address or its error.
 */
public final class UnavailableException extends ApiException {

    private static final long serialVersionUID = 1L;

    public UnavailableException(String detail) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "UNAVAILABLE", detail);
    }
}
