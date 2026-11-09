package com.mysms.core.events;

/** Handles one delivered event inside the consuming module's tenant transaction ({@link InboxProcessor}). */
@FunctionalInterface
public interface EventHandler {

    void handle(EventEnvelope event);
}
