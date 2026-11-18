package com.mysms.core.payment;

import java.util.Objects;

/**
 * The key pair a merchant account signs its gateway calls with. The secret never leaves this object in readable form:
 * {@link #toString()} masks it, so a credentials object written to a log or an error shows only the key id.
 */
public record GatewayCredentials(String keyId, String keySecret) {

    public GatewayCredentials {
        Objects.requireNonNull(keyId, "keyId");
        Objects.requireNonNull(keySecret, "keySecret");
    }

    @Override
    public String toString() {
        return "GatewayCredentials[keyId=" + keyId + ", keySecret=••••]";
    }
}
