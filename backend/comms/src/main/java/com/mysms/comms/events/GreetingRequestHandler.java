package com.mysms.comms.events;

import com.mysms.comms.business.InAppNotificationService;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.messaging.events.RejectedEventException;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Delivers the greetings people asks for ({@code people.greeting.requested}: the greeting, person and template ids and
 * the channel): people may not call comms, so the request travels as an event. The greeting becomes an in-app
 * notification ({@link InAppNotificationService#notifyGreeting}) whatever channel was asked, since comms has no SMS
 * or push provider and no address of the person. An event without its ids is rejected and never retried; one naming a
 * template comms does not have (or no longer uses) is announced back to people as rejected, so the greeting does not
 * count against the sender.
 */
@Component
public class GreetingRequestHandler implements EventSubscriptions {

    static final String GREETING = "people.greeting";
    static final String REQUESTED = GREETING + ".requested";

    private final InAppNotificationService notifications;
    private final JsonMapper json;

    public GreetingRequestHandler(InAppNotificationService notifications, JsonMapper json) {
        this.notifications = notifications;
        this.json = json;
    }

    @Override
    public String module() {
        return "comms";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        return Map.of(REQUESTED, this::deliver);
    }

    /** Delivers one greeting; how many notifications it wrote (0 when comms knows no account of the person). */
    public int deliver(EventEnvelope event) {
        JsonNode data = json.readTree(event.data());
        if (!GREETING.equals(event.aggregateType())
                || !data.path("id").asString("").equals(event.aggregateId().toString())) {
            throw new RejectedEventException("greeting event " + event.id() + " is not about the greeting it names");
        }
        UUID personId;
        UUID templateId;
        try {
            personId = UUID.fromString(data.path("personId").asString(""));
            templateId = UUID.fromString(data.path("templateId").asString(""));
        } catch (IllegalArgumentException malformed) {
            throw new RejectedEventException("greeting event " + event.id() + " names no person or template");
        }
        return notifications.notifyGreeting(event.aggregateId(), personId, templateId);
    }
}
