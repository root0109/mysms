package com.mysms.platform.errors;

import com.mysms.core.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * An external provider this operation needs (the file store, the payment gateway, the stock-photo service) is not
 * configured for this deployment: 503 {@code NOT_CONFIGURED}. Nothing was changed; the detail names the provider, never
 * an address or a key.
 */
public final class NotConfiguredException extends ApiException {

    private static final long serialVersionUID = 1L;

    public NotConfiguredException(String detail) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "NOT_CONFIGURED", detail);
    }
}
