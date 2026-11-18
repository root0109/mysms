package com.mysms.finance.errors;

import com.mysms.core.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Online payment is not set up where the money would go: no active gateway account for the institution (or the
 * tenant), or one without its key secret or webhook secret. 503 {@code PAYMENT_GATEWAY_NOT_CONFIGURED}: nothing was
 * changed, and the answer becomes a success once a tenant administrator configures the account.
 */
public final class PaymentGatewayNotConfiguredException extends ApiException {

    private static final long serialVersionUID = 1L;

    public PaymentGatewayNotConfiguredException(String detail) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_GATEWAY_NOT_CONFIGURED", detail);
    }
}
