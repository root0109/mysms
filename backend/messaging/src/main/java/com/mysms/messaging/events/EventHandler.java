package com.mysms.messaging.events;

/** Handles one delivered event inside the consuming module's tenant transaction. */
@FunctionalInterface
public interface EventHandler {

    void handle(EventEnvelope event);
}
