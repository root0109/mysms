package com.mysms.platform.repository;

/**
 * What a subscription keeps of the payment that activated it: the gateway's status and method, the payer's contact as
 * the gateway reported it (kept for disputes, never returned by the contract) and the verified raw entity.
 */
public record GatewayPaymentRecord(String status, String method, String email, String phone, String raw) {
}
