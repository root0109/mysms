package com.mysms.core.error;

import org.springframework.http.HttpStatus;

/** The caller has used up an allowance of the operation for now: 429 {@code RATE_LIMITED}. */
public final class RateLimitedException extends ApiException {

    private static final long serialVersionUID = 1L;

    public RateLimitedException(String detail) {
        super(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", detail);
    }
}
