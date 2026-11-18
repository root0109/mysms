package com.mysms.lms.business;

import com.mysms.core.error.ApiException;
import org.springframework.http.HttpStatus;

/** The calendar provider is not set up in this deployment: 503 CALENDAR_NOT_CONFIGURED. */
public final class CalendarNotConfiguredException extends ApiException {

    private static final long serialVersionUID = 1L;

    public CalendarNotConfiguredException(String provider) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "CALENDAR_NOT_CONFIGURED",
                "The " + provider + " calendar provider is not configured.");
    }
}
