package com.mysms.lms.business;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * A video-conferencing service's callbacks (the port; {@link ZoomConferencingProvider} is the adapter): the adapter
 * maps the provider's payload to one neutral event, or to none when the payload is not an event lms acts on.
 */
public interface ConferencingEventProvider {

    /** The provider name a conferencing account names ({@code lms.conferencing_account.provider}). */
    String name();

    /** The event the parsed callback body carries; empty when it is none lms acts on. */
    Optional<Event> event(Map<String, Object> body);

    enum Kind { MEETING_ENDED, PARTICIPANT_JOINED, PARTICIPANT_LEFT, RECORDING_READY }

    /**
     * One callback: the tenant the meeting names, the provider's meeting reference, and — per kind — the MySMS user
     * the participant joined as, when it happened, or the recording's link.
     */
    record Event(Kind kind, String tenant, String meetingRef, String participantRef, OffsetDateTime occurredAt,
                 String recordingUrl) {
    }
}
