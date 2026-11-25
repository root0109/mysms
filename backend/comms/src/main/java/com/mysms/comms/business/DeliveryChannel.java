package com.mysms.comms.business;

/**
 * One way a message reaches a person (the Strategy each channel of comms.outbound_message is sent through). Calls go
 * to an outside provider: never inside a database transaction.
 */
public interface DeliveryChannel {

    /** The channel's value in comms.outbound_message ({@code email}, {@code sms}, {@code push}, {@code web_push}). */
    String name();

    /** 503 NOT_CONFIGURED when this deployment has no provider for the channel. */
    void requireConfigured();

    /** Hands one message to the provider; 503 when the provider refuses it or cannot be reached. */
    void deliver(String to, String subject, String body);
}
