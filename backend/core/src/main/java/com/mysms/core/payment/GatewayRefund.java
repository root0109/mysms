package com.mysms.core.payment;

/** A refund the gateway accepted: its id, the payment it returns money from, the amount in minor units and status. */
public record GatewayRefund(String id, String paymentId, long amountMinor, String status) {
}
