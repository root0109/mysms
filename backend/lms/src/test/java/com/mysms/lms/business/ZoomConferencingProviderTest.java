package com.mysms.lms.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.lms.business.ConferencingEventProvider.Event;
import com.mysms.lms.business.ConferencingEventProvider.Kind;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Zoom's webhook payloads map to lms's neutral conferencing events. */
class ZoomConferencingProviderTest {

    private final ZoomConferencingProvider provider = new ZoomConferencingProvider(JsonMapper.builder().build());

    @SuppressWarnings("unchecked")
    private Event event(String payload) {
        Map<String, Object> body = (Map<String, Object>) JsonMapper.builder().build()
                .readValue(payload.getBytes(StandardCharsets.UTF_8), Map.class);
        return provider.event(body).orElse(null);
    }

    @Test
    void mapsMeetingEndedWithTheTenantTrackingField() {
        Event event = event("""
                {"event":"meeting.ended","payload":{"object":{"id":"912345678",
                 "end_time":"2026-11-12T10:30:00Z",
                 "tracking_fields":[{"field":"mysms_tenant","value":"0196f000-0000-7000-8000-00000000c001"}]}}}
                """);
        assertThat(event.kind()).isEqualTo(Kind.MEETING_ENDED);
        assertThat(event.tenant()).isEqualTo("0196f000-0000-7000-8000-00000000c001");
        assertThat(event.meetingRef()).isEqualTo("912345678");
        assertThat(event.occurredAt()).isEqualTo("2026-11-12T10:30:00Z");
    }

    @Test
    void mapsParticipantJoinedAndLeftWithTheCustomerKey() {
        Event joined = event("""
                {"event":"meeting.participant_joined","payload":{"object":{"id":"912345678",
                 "participant":{"customer_key":"0196f000-0000-7000-8000-00000000d001",
                 "join_time":"2026-11-12T09:00:05Z"}}},"mysms_tenant":"t"}
                """);
        assertThat(joined.kind()).isEqualTo(Kind.PARTICIPANT_JOINED);
        assertThat(joined.participantRef()).isEqualTo("0196f000-0000-7000-8000-00000000d001");
        assertThat(joined.occurredAt()).isEqualTo("2026-11-12T09:00:05Z");
        assertThat(joined.tenant()).isEqualTo("t");
        Event left = event("""
                {"event":"meeting.participant_left","payload":{"object":{"id":"912345678",
                 "participant":{"customer_key":"k","leave_time":"2026-11-12T09:45:00Z"}}}}
                """);
        assertThat(left.kind()).isEqualTo(Kind.PARTICIPANT_LEFT);
        assertThat(left.occurredAt()).isEqualTo("2026-11-12T09:45:00Z");
    }

    @Test
    void mapsRecordingCompletedToItsShareUrl() {
        Event event = event("""
                {"event":"recording.completed","payload":{"object":{"id":"912345678",
                 "share_url":"https://zoom.us/rec/share/abc"}}}
                """);
        assertThat(event.kind()).isEqualTo(Kind.RECORDING_READY);
        assertThat(event.recordingUrl()).isEqualTo("https://zoom.us/rec/share/abc");
    }

    @Test
    void otherEventsAndPayloadsWithoutAMeetingAreNone() {
        assertThat(event("{\"event\":\"meeting.created\",\"payload\":{\"object\":{\"id\":\"1\"}}}")).isNull();
        assertThat(event("{\"event\":\"meeting.ended\",\"payload\":{\"object\":{}}}")).isNull();
        Event unparseableTime = event("""
                {"event":"meeting.ended","payload":{"object":{"id":"1","end_time":"yesterday"}}}
                """);
        assertThat(unparseableTime.occurredAt()).isNull();
    }
}
