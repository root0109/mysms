package com.mysms.identity.iam.errors;

import com.mysms.core.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Too many requests of one kind for one account in a short time (429 RATE_LIMITED): a verification code was sent
 * several times already. Nothing was changed; the caller may try again later.
 */
public final class RateLimitedException extends ApiException {

    private static final long serialVersionUID = 1L;

    public RateLimitedException(String detail) {
        super(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", detail);
    }
}
