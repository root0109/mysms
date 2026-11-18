package com.mysms.core.payment;

import com.mysms.core.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * No connection to the gateway could be made, so the request never reached it (503). Unlike a timeout, the caller
 * knows the gateway did nothing.
 */
public final class PaymentGatewayUnreachableException extends ApiException {

    private static final long serialVersionUID = 1L;

    public PaymentGatewayUnreachableException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "UNAVAILABLE", "The payment gateway is not reachable.");
    }
}
