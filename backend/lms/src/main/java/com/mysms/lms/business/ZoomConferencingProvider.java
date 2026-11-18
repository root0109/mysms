package com.mysms.lms.business;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Zoom adapter: maps Zoom's {@code meeting.ended}, {@code meeting.participant_joined/left} and
 * {@code recording.completed} events. The tenant comes from the meeting's {@code mysms_tenant} tracking field (set
 * when the meeting is created) and the participant's MySMS user from Zoom's {@code customer_key} (set on the join
 * link); a participant without one is nobody lms tracks.
 */
@Component
public class ZoomConferencingProvider implements ConferencingEventProvider {

    static final String NAME = "zoom";
    static final String TENANT_FIELD = "mysms_tenant";

    private final JsonMapper json;

    public ZoomConferencingProvider(JsonMapper json) {
        this.json = json;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Optional<Event> event(Map<String, Object> body) {
        JsonNode root = json.valueToTree(body);
        JsonNode object = root.path("payload").path("object");
        String meetingRef = object.path("id").asString("");
        if (meetingRef.isBlank()) {
            return Optional.empty();
        }
        String tenant = tenant(root, object);
        JsonNode participant = object.path("participant");
        return switch (root.path("event").asString("")) {
            case "meeting.ended" -> Optional.of(new Event(Kind.MEETING_ENDED, tenant, meetingRef, null,
                    time(object.path("end_time")), null));
            case "meeting.participant_joined" -> Optional.of(new Event(Kind.PARTICIPANT_JOINED, tenant, meetingRef,
                    participant.path("customer_key").asString(null), time(participant.path("join_time")), null));
            case "meeting.participant_left" -> Optional.of(new Event(Kind.PARTICIPANT_LEFT, tenant, meetingRef,
                    participant.path("customer_key").asString(null), time(participant.path("leave_time")), null));
            case "recording.completed" -> Optional.of(new Event(Kind.RECORDING_READY, tenant, meetingRef, null,
                    null, object.path("share_url").asString(null)));
            default -> Optional.empty();
        };
    }

    private static String tenant(JsonNode root, JsonNode object) {
        for (JsonNode field : object.path("tracking_fields")) {
            if (TENANT_FIELD.equals(field.path("field").asString(""))) {
                return field.path("value").asString("");
            }
        }
        return root.path(TENANT_FIELD).asString("");
    }

    private static OffsetDateTime time(JsonNode value) {
        try {
            return value.isMissingNode() || value.asString("").isBlank() ? null
                    : OffsetDateTime.parse(value.asString(""));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
