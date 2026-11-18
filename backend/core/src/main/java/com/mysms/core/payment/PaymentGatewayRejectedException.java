package com.mysms.core.payment;

import com.mysms.core.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * The gateway refused a request (an amount it does not accept, a payment that cannot be refunded…): 422
 * {@code PAYMENT_GATEWAY_REJECTED}. The detail carries the gateway's own error code only, never its description, which
 * may echo request values.
 */
public final class PaymentGatewayRejectedException extends ApiException {

    private static final long serialVersionUID = 1L;

    public PaymentGatewayRejectedException(String gatewayCode) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "PAYMENT_GATEWAY_REJECTED",
                "The payment gateway refused the request (" + gatewayCode + ").");
    }
}
