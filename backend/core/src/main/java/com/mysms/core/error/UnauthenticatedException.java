package com.mysms.core.error;

import org.springframework.http.HttpStatus;

/**
 * The caller could not be authenticated (401). Sign-in and token refresh use one code and one message for every
 * failure — unknown account, wrong password, locked, inactive — so the answer never reveals which accounts exist.
 */
public final class UnauthenticatedException extends ApiException {

    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    private static final long serialVersionUID = 1L;

    public UnauthenticatedException(String code, String detail) {
        super(HttpStatus.UNAUTHORIZED, code, detail);
    }
}
