package com.mysms.privacy.events;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.RejectedEventException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the payload of another module's created, updated or deleted event for one of privacy's copies: the event must
 * name the row it is about, and its ids, dates and flags are read strictly — a malformed value rejects the event
 * (never retried) instead of being stored. Shared by the projections' handlers so each reads its source the same way.
 */
final class SourceEvent {

    private static final String DELETED = ".deleted";

    private final EventEnvelope event;
    private final JsonNode data;

    private SourceEvent(EventEnvelope event, JsonNode data) {
        this.event = event;
        this.data = data;
    }

    /** The event's payload; rejected when it does not name the row the event is about. */
    static SourceEvent read(EventEnvelope event, JsonMapper json) {
        JsonNode data = json.readTree(event.data());
        if (!data.path("id").asString("").equals(event.aggregateId().toString())) {
            throw rejected(event, "is not about the row it names");
        }
        return new SourceEvent(event, data);
    }

    /** The row the event is about. */
    UUID id() {
        return event.aggregateId();
    }

    /** Whether the event is the row's deletion. */
    boolean deleted() {
        return event.type().endsWith(DELETED);
    }

    /**
     * Whether the event carries the row's state; a reference cleanup's announcement names only the id of a row it
     * changed, and the copies keep what they have until the row's next event.
     */
    boolean carriesState() {
        return data.has("rowVersion");
    }

    /** The row version the event reflects (at least 1). */
    int rowVersion() {
        int version = data.path("rowVersion").asInt(0);
        if (version < 1) {
            throw rejected(event, "has no row version");
        }
        return version;
    }

    /** An id the payload may leave out or null. */
    UUID optionalId(String field) {
        JsonNode value = data.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (!value.isString()) {
            throw rejected(event, "carries a malformed " + field);
        }
        try {
            return UUID.fromString(value.asString());
        } catch (IllegalArgumentException malformed) {
            throw rejected(event, "carries a malformed " + field);
        }
    }

    /** An id the payload must carry. */
    UUID requiredId(String field) {
        UUID id = optionalId(field);
        if (id == null) {
            throw rejected(event, "has no " + field);
        }
        return id;
    }

    /** An ISO date the payload may leave out or null. */
    LocalDate optionalDate(String field) {
        JsonNode value = data.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (!value.isString()) {
            throw rejected(event, "carries a malformed " + field);
        }
        try {
            return LocalDate.parse(value.asString());
        } catch (DateTimeParseException malformed) {
            throw rejected(event, "carries a malformed " + field);
        }
    }

    /** A flag the payload must carry. */
    boolean requiredFlag(String field) {
        JsonNode value = data.path(field);
        if (!value.isBoolean()) {
            throw rejected(event, "has no " + field);
        }
        return value.booleanValue();
    }

    /** A text the payload must carry, one of the allowed values. */
    String requiredText(String field, Set<String> allowed) {
        JsonNode value = data.path(field);
        if (!value.isString() || !allowed.contains(value.asString())) {
            throw rejected(event, "has no valid " + field);
        }
        return value.asString();
    }

    private static RejectedEventException rejected(EventEnvelope event, String why) {
        return new RejectedEventException(event.aggregateType() + " event " + event.id() + " " + why);
    }
}
