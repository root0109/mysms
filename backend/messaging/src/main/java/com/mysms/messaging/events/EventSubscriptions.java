package com.mysms.messaging.events;

import java.util.Map;

/**
 * The events one module consumes and its handler for each. Handlers run inside the event's tenant; a module's several
 * beans merge into one subscription, and two handlers for one event type stop the application at startup.
 */
public interface EventSubscriptions {

    String module();

    Map<String, EventHandler> handlers();
}
