package com.mysms.core.events;

/**
 * An event a consumer must never handle, however often it is delivered: unreadable, from a publisher that may not send
 * it, or of a type its source does not own. Buses set it aside instead of spending the normal retry budget (RabbitMQ
 * dead-letters immediately; Google Cloud Pub/Sub publishes it to the consumer's dead-letter topic and acknowledges
 * the original only after the dead-letter publish is confirmed).
 */
public final class RejectedEventException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RejectedEventException(String message) {
        super(message);
    }
}
