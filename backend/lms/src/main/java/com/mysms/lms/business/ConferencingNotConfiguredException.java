package com.mysms.lms.business;

import com.mysms.core.error.ApiException;
import org.springframework.http.HttpStatus;

/** The meeting provider of a live class is not set up in this deployment: 503 CONFERENCING_NOT_CONFIGURED. */
public final class ConferencingNotConfiguredException extends ApiException {

    private static final long serialVersionUID = 1L;

    public ConferencingNotConfiguredException(String provider) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "CONFERENCING_NOT_CONFIGURED",
                "The " + provider + " meeting provider is not configured.");
    }
}
