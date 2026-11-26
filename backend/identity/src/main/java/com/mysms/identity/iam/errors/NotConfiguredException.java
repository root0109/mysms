package com.mysms.identity.iam.errors;

import com.mysms.core.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * A provider this operation needs is not configured for this deployment (503 NOT_CONFIGURED): outgoing e-mail without
 * SMTP settings, mobile messages without an SMS provider. Nothing was changed; the detail names the provider, never
 * its address or settings.
 */
public final class NotConfiguredException extends ApiException {

    private static final long serialVersionUID = 1L;

    public NotConfiguredException(String detail) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "NOT_CONFIGURED", detail);
    }
}
