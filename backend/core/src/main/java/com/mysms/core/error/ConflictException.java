package com.mysms.core.error;

import org.springframework.http.HttpStatus;

/** A conflict with the current state that has its own stable code (409), e.g. SETTING_LOCKED. */
public final class ConflictException extends ApiException {

    private static final long serialVersionUID = 1L;

    public ConflictException(String code, String detail) {
        super(HttpStatus.CONFLICT, code, detail);
    }
}
