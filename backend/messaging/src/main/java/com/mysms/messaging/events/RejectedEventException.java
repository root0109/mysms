package com.mysms.messaging.events;

/**
 * An event a consumer must never handle, however often delivered: unreadable, from a publisher that may not send it, or
 * of a type its source does not own. Buses park it at once instead of spending the retry budget.
 */
public final class RejectedEventException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RejectedEventException(String message) {
        super(message);
    }
}
