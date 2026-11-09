package com.mysms.core.events;

/**
 * An event a consumer must never handle, however often it is delivered: unreadable, from a publisher that may not send
 * it, or of a type its source does not own. Buses set it aside at once (RabbitMQ: the module's dead-letter queue)
 * instead of redelivering it.
 */
public final class RejectedEventException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RejectedEventException(String message) {
        super(message);
    }
}
