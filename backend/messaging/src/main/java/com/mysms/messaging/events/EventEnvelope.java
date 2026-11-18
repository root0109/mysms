package com.mysms.messaging.events;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * One domain event on the bus as a CloudEvents 1.0 structured JSON message, with the MySMS extensions {@code tenantid},
 * {@code aggregatetype} and {@code traceparent}. {@code data} carries ids and codes, never personal data.
 *
 * @param id globally unique and stable across redeliveries (derived from the module and outbox row)
 * @param tenantId empty for global master data
 * @param traceId W3C trace id of the request that made the change, when known
 */
public record EventEnvelope(UUID id, String module, String type, Optional<UUID> tenantId, String aggregateType,
                            UUID aggregateId, Instant time, String data, Optional<String> traceId) {

    public static final String SPEC_VERSION = "1.0";
    public static final String CONTENT_TYPE = "application/cloudevents+json";
    /** The one event type every module may publish: its audit records, kept by the platform. */
    public static final String AUDIT_TYPE = "audit.recorded";
    private static final String JSON_DATA = "application/json";
    private static final String TEXT_DATA = "text/plain";

    public EventEnvelope {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(module, "module");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(aggregateType, "aggregateType");
        Objects.requireNonNull(aggregateId, "aggregateId");
        Objects.requireNonNull(time, "time");
        tenantId = tenantId == null ? Optional.empty() : tenantId;
        traceId = traceId == null ? Optional.empty() : traceId;
        data = data == null || data.isBlank() ? "{}" : data;
    }

    /** The stable event id of a module's outbox row: the same row always becomes the same event. */
    public static UUID idOf(String module, long outboxId) {
        return UUID.nameUUIDFromBytes(("mysms:" + module + ":outbox:" + outboxId).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Whether the source module owns this event: its type and aggregate are the module's own (`hr.staff.deleted` from
     * hr), or it is the module's audit record. A consumer handles nothing else, so one module cannot speak for another.
     */
    public boolean ownedBySource() {
        String prefix = module + ".";
        return (type.startsWith(prefix) || AUDIT_TYPE.equals(type)) && aggregateType.startsWith(prefix);
    }

    /** The CloudEvents JSON form; a payload that is not JSON travels as text, never fails. */
    public String toJson(JsonMapper json) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("specversion", SPEC_VERSION);
        event.put("id", id.toString());
        event.put("source", "/mysms/" + module);
        event.put("type", type);
        event.put("time", time.toString());
        event.put("subject", aggregateId.toString());
        tenantId.ifPresent(t -> event.put("tenantid", t.toString()));
        event.put("aggregatetype", aggregateType);
        traceId.ifPresent(t -> event.put("traceparent", "00-" + t + "-0000000000000001-01"));
        ObjectNode node = json.valueToTree(event);
        try {
            node.set("data", json.readTree(data));
            node.put("datacontenttype", JSON_DATA);
        } catch (JacksonException e) {
            node.put("data", data);
            node.put("datacontenttype", TEXT_DATA);
        }
        return json.writeValueAsString(node);
    }

    /** Reads the CloudEvents JSON form; refuses anything that is not a MySMS event. */
    public static EventEnvelope fromJson(JsonMapper json, String text) {
        JsonNode node = json.readTree(text);
        if (!SPEC_VERSION.equals(node.path("specversion").asString())) {
            throw new IllegalArgumentException("not a CloudEvents 1.0 event");
        }
        String source = node.path("source").asString();
        if (!source.startsWith("/mysms/")) {
            throw new IllegalArgumentException("not a MySMS event source: " + source);
        }
        String tenant = node.path("tenantid").asString(null);
        String traceparent = node.path("traceparent").asString(null);
        Optional<String> trace = Optional.ofNullable(traceparent).filter(t -> t.length() >= 35)
                .map(t -> t.substring(3, 35));
        return new EventEnvelope(UUID.fromString(node.path("id").asString()), source.substring("/mysms/".length()),
                node.path("type").asString(), Optional.ofNullable(tenant).map(UUID::fromString),
                node.path("aggregatetype").asString(), UUID.fromString(node.path("subject").asString()),
                Instant.parse(node.path("time").asString()),
                !node.has("data") ? "{}" : TEXT_DATA.equals(node.path("datacontenttype").asString())
                        ? node.get("data").asString() : json.writeValueAsString(node.get("data")), trace);
    }
}
