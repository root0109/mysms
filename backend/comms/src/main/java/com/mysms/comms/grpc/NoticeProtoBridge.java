package com.mysms.comms.grpc;

import com.mysms.comms.api.model.NoticeUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateNotice over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.NoticeUpdate} message
 * as REST's {@link NoticeUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class NoticeProtoBridge {

    private NoticeProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static NoticeUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.NoticeUpdate request) {
        NoticeUpdate changes = new NoticeUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setAudience(bridge.choice(
                request.hasAudience(), request.getAudience(), NoticeUpdate.AudienceEnum::fromValue));
        changes.setClassSectionId(bridge.uuid(
                request.hasClassSectionId(), request.getClassSectionId(), "class_section_id"));
        changes.setBackgroundId(bridge.uuid(request.hasBackgroundId(), request.getBackgroundId(), "background_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setBody(ProtoBridge.text(request.hasBody(), request.getBody()));
        changes.setAttachmentFileId(bridge.uuid(
                request.hasAttachmentFileId(), request.getAttachmentFileId(), "attachment_file_id"));
        changes.setPublishAt(ProtoBridge.time(request.hasPublishAt(), request.getPublishAt()));
        changes.setExpireAt(ProtoBridge.time(request.hasExpireAt(), request.getExpireAt()));
        changes.setCohortId(bridge.uuid(request.hasCohortId(), request.getCohortId(), "cohort_id"));
        changes.setShowInCalendar(ProtoBridge.flag(request.hasShowInCalendar(), request.getShowInCalendar()));
        changes.setAllowReplies(ProtoBridge.flag(request.hasAllowReplies(), request.getAllowReplies()));
        changes.setDiscussionTopicId(bridge.uuid(
                request.hasDiscussionTopicId(), request.getDiscussionTopicId(), "discussion_topic_id"));
        changes.setIsClosed(ProtoBridge.flag(request.hasIsClosed(), request.getIsClosed()));
        changes.setIsDeactivated(ProtoBridge.flag(request.hasIsDeactivated(), request.getIsDeactivated()));
        changes.setAuthorComment(ProtoBridge.text(request.hasAuthorComment(), request.getAuthorComment()));
        return bridge.patch(changes);
    }
}
