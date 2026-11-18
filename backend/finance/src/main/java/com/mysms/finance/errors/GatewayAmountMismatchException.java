package com.mysms.finance.errors;

import com.mysms.core.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * A gateway notification reports an amount or a currency other than the order MySMS opened: 422
 * {@code GATEWAY_AMOUNT_MISMATCH}. No money is recorded; the event is kept with the reason as dispute evidence.
 */
public final class GatewayAmountMismatchException extends ApiException {

    private static final long serialVersionUID = 1L;

    public GatewayAmountMismatchException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "GATEWAY_AMOUNT_MISMATCH",
                "The notification's amount or currency does not match the order; nothing was recorded.");
    }
}
