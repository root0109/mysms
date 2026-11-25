package com.mysms.comms.errors;

import com.mysms.core.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * A provider this operation needs is not configured for this deployment (503 NOT_CONFIGURED): e-mail without SMTP
 * settings, SMS or push without a provider. Nothing was changed; the detail names the channel, never its settings.
 */
public final class NotConfiguredException extends ApiException {

    private static final long serialVersionUID = 1L;

    public NotConfiguredException(String detail) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "NOT_CONFIGURED", detail);
    }
}
