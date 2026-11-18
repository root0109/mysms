package com.mysms.core.payment;

/**
 * A payment as the gateway reports it: its id, the order it paid, the amount in minor units, the currency, the
 * gateway's status ({@code captured} is money received) and the method used (card, upi, netbanking…).
 */
public record GatewayPayment(String id, String orderId, long amountMinor, String currency, String status,
        String method) {

    /** Whether the gateway holds the money for the merchant (only a captured payment settles a due). */
    public boolean captured() {
        return "captured".equals(status);
    }
}
