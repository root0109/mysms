package com.mysms.messaging.events;

import java.util.Map;

/**
 * The events one module consumes and its handler for each (a bean per module; generated for the reference cleanup
 * of database/logical-references.md, hand-written for workflows). Handlers run through the module's
 * {@link InboxProcessor}, inside the event's tenant. A module may have several beans: their handlers are merged into
 * the module's one subscription, and two handlers for one event type stop the application at startup.
 */
public interface EventSubscriptions {

    /** The consuming module. */
    String module();

    /** Event type → handler. */
    Map<String, EventHandler> handlers();
}
