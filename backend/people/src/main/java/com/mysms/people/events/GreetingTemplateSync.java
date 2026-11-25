package com.mysms.people.events;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.people.business.GreetingService;
import com.mysms.people.repository.GreetingTemplateRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The greetings' side of comms (people may not call comms): keeps people.greeting_template in step with
 * comms.message_template from its created, updated and deleted events, so a greeting names only a template comms
 * delivers, and takes back a greeting comms announced it could not deliver ({@code comms.greeting_delivery.rejected}),
 * so it does not count against the sender's day. A template entry changes only to a newer row version. Runs inside the
 * event's tenant (messaging's InboxProcessor: exactly once per event).
 */
@Component
public class GreetingTemplateSync implements EventSubscriptions {

    static final String TEMPLATE = "comms.message_template";
    static final String REJECTED = "comms.greeting_delivery.rejected";

    private final GreetingTemplateRepository templates;
    private final GreetingService greetings;
    private final JsonMapper json;
    private final Clock clock;

    public GreetingTemplateSync(GreetingTemplateRepository templates, GreetingService greetings, JsonMapper json,
            Clock clock) {
        this.templates = templates;
        this.greetings = greetings;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public String module() {
        return "people";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        return Map.of(TEMPLATE + ".created", this::apply, TEMPLATE + ".updated", this::apply,
                TEMPLATE + ".deleted", this::apply, REJECTED, this::rejected);
    }

    /** Applies one template event; whether the stored entry changed. */
    public boolean apply(EventEnvelope event) {
        JsonNode data = json.readTree(event.data());
        if (!TEMPLATE.equals(event.aggregateType())
                || !data.path("id").asString("").equals(event.aggregateId().toString())) {
            throw new RejectedEventException("template event " + event.id() + " is not about the template it names");
        }
        if (!data.has("rowVersion")) {
            return false;
        }
        int version = data.path("rowVersion").asInt(0);
        if (version < 1) {
            throw new RejectedEventException("template event " + event.id() + " has no row version");
        }
        UUID institution;
        try {
            JsonNode value = data.path("institutionId");
            institution = value.isString() && !value.asString().isEmpty() ? UUID.fromString(value.asString()) : null;
        } catch (IllegalArgumentException malformed) {
            throw new RejectedEventException("template event " + event.id() + " names a malformed institution");
        }
        return templates.apply(event.aggregateId(), institution, data.path("isActive").asBoolean(false),
                event.type().endsWith(".deleted"), version, OffsetDateTime.now(clock).withOffsetSameInstant(
                        ZoneOffset.UTC));
    }

    /** Takes back the greeting comms could not deliver; whether it was still counted. */
    public boolean rejected(EventEnvelope event) {
        if (!"comms.greeting_delivery".equals(event.aggregateType())) {
            throw new RejectedEventException("rejection " + event.id() + " is not about a greeting delivery");
        }
        return greetings.withdrawGreeting(event.aggregateId());
    }
}
