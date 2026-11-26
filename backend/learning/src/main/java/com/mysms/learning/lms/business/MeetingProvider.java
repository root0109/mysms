package com.mysms.learning.lms.business;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * A video-meeting provider lms schedules live classes with (the port; one adapter per provider). Every call carries
 * the tenant's own provider account; a refusal or an unreachable provider is 503, and nothing is called inside a
 * database transaction.
 */
public interface MeetingProvider {

    /** The provider code this adapter serves (lms.conferencing_account.provider). */
    String provider();

    /** Whether the deployment configured this adapter; an unconfigured one is never called. */
    boolean configured();

    Meeting create(Account account, MeetingRequest request);

    /** A fresh host start link and when it stops working. */
    StartLink startLink(Account account, String meetingId);

    /** Ends the meeting for everyone. */
    void end(Account account, String meetingId);

    /** Who attended a finished meeting. */
    List<Attendance> attendance(Account account, String meetingUuid);

    /** The tenant's provider account, its secret decrypted. */
    record Account(String externalAccountId, String clientId, String clientSecret) {
    }

    record MeetingRequest(String topic, String agenda, OffsetDateTime startsAt, String timezone, int durationMinutes,
            String autoRecording) {
    }

    record Meeting(String meetingId, String meetingUuid, String joinUrl, String startUrl, String password) {
    }

    record StartLink(String url, Instant expiresAt) {
    }

    record Attendance(String email, OffsetDateTime joinedAt, OffsetDateTime leftAt, int durationMinutes) {
    }
}
