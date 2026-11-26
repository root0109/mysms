package com.mysms.learning.lms.errors;

import com.mysms.core.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * A provider this operation needs (the SCORM package host, the contact address) is not configured for this
 * deployment or tenant: 503 {@code NOT_CONFIGURED}. Nothing was changed; the detail names what is missing, never an
 * address or a key.
 */
public final class NotConfiguredException extends ApiException {

    private static final long serialVersionUID = 1L;

    public NotConfiguredException(String detail) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "NOT_CONFIGURED", detail);
    }
}
