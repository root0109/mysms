package com.mysms.core.payment;

/** An order the gateway opened for a checkout: its id, the amount in minor units (paise), the currency and status. */
public record GatewayOrder(String id, long amountMinor, String currency, String status) {
}
