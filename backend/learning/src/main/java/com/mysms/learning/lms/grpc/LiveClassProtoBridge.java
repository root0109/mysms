package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.LiveClassUpdate;

/**
 * UpdateLiveClass over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.LiveClassUpdate} message as REST's {@link
 * LiveClassUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class LiveClassProtoBridge {

    private LiveClassProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static LiveClassUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.LiveClassUpdate request) {
        LiveClassUpdate changes = new LiveClassUpdate();
        changes.setCohortId(bridge.uuid(request.hasCohortId(), request.getCohortId(), "cohort_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setClassDate(bridge.date(request.hasClassDate(), request.getClassDate(), "class_date"));
        changes.setStartTime(ProtoBridge.text(request.hasStartTime(), request.getStartTime()));
        changes.setDurationMinutes(ProtoBridge.number(request.hasDurationMinutes(), request.getDurationMinutes()));
        changes.setTimezone(ProtoBridge.text(request.hasTimezone(), request.getTimezone()));
        changes.setHostUserId(bridge.uuid(request.hasHostUserId(), request.getHostUserId(), "host_user_id"));
        changes.setConferencingProvider(bridge.choice(
                request.hasConferencingProvider(), request.getConferencingProvider(),
                LiveClassUpdate.ConferencingProviderEnum::fromValue));
        changes.setConferencingAccountId(bridge.uuid(
                request.hasConferencingAccountId(), request.getConferencingAccountId(), "conferencing_account_id"));
        changes.setMeetingId(ProtoBridge.text(request.hasMeetingId(), request.getMeetingId()));
        changes.setMeetingUuid(ProtoBridge.text(request.hasMeetingUuid(), request.getMeetingUuid()));
        changes.setJoinUrl(ProtoBridge.text(request.hasJoinUrl(), request.getJoinUrl()));
        changes.setMeetingPassword(ProtoBridge.text(request.hasMeetingPassword(), request.getMeetingPassword()));
        changes.setAutoRecording(bridge.choice(
                request.hasAutoRecording(), request.getAutoRecording(), LiveClassUpdate.AutoRecordingEnum::fromValue));
        changes.setCalendarEventId(bridge.uuid(
                request.hasCalendarEventId(), request.getCalendarEventId(), "calendar_event_id"));
        changes.setAttendeeCount(ProtoBridge.number(request.hasAttendeeCount(), request.getAttendeeCount()));
        changes.setInviteStatus(bridge.choice(
                request.hasInviteStatus(), request.getInviteStatus(), LiveClassUpdate.InviteStatusEnum::fromValue));
        changes.setInviteAttempts(ProtoBridge.number(request.hasInviteAttempts(), request.getInviteAttempts()));
        changes.setModeratorPassword(ProtoBridge.text(request.hasModeratorPassword(), request.getModeratorPassword()));
        changes.setWelcomeMessage(ProtoBridge.text(request.hasWelcomeMessage(), request.getWelcomeMessage()));
        changes.setProviderCreatedRef(ProtoBridge.text(
                request.hasProviderCreatedRef(), request.getProviderCreatedRef()));
        changes.setRecordingUrl(ProtoBridge.text(request.hasRecordingUrl(), request.getRecordingUrl()));
        changes.setInviteeCount(ProtoBridge.number(request.hasInviteeCount(), request.getInviteeCount()));
        changes.setPrepMinutes(ProtoBridge.number(request.hasPrepMinutes(), request.getPrepMinutes()));
        changes.setExtensionMinutes(ProtoBridge.number(request.hasExtensionMinutes(), request.getExtensionMinutes()));
        changes.setShowInCalendar(ProtoBridge.flag(request.hasShowInCalendar(), request.getShowInCalendar()));
        changes.setHostJoined(ProtoBridge.flag(request.hasHostJoined(), request.getHostJoined()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setEndedAt(ProtoBridge.time(request.hasEndedAt(), request.getEndedAt()));
        return bridge.patch(changes);
    }
}
