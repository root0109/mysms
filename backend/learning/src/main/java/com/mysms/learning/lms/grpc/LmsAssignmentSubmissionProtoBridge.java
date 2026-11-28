package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.LmsAssignmentSubmissionUpdate;

/**
 * UpdateLmsAssignmentSubmission over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.LmsAssignmentSubmissionUpdate}
 * message as REST's {@link LmsAssignmentSubmissionUpdate} body, one explicit line per field: unset keeps the value,
 * null clears it, a value sets it.
 */
final class LmsAssignmentSubmissionProtoBridge {

    private LmsAssignmentSubmissionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static LmsAssignmentSubmissionUpdate changes(
            ProtoBridge bridge, com.mysms.lms.v1.LmsAssignmentSubmissionUpdate request) {
        LmsAssignmentSubmissionUpdate changes = new LmsAssignmentSubmissionUpdate();
        changes.setAssignmentId(bridge.uuid(request.hasAssignmentId(), request.getAssignmentId(), "assignment_id"));
        changes.setLessonId(bridge.uuid(request.hasLessonId(), request.getLessonId(), "lesson_id"));
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        changes.setSubmissionType(bridge.choice(
                request.hasSubmissionType(), request.getSubmissionType(),
                LmsAssignmentSubmissionUpdate.SubmissionTypeEnum::fromValue));
        changes.setQuestionSnapshot(ProtoBridge.text(request.hasQuestionSnapshot(), request.getQuestionSnapshot()));
        changes.setAnswer(ProtoBridge.text(request.hasAnswer(), request.getAnswer()));
        changes.setAttachmentFileId(bridge.uuid(
                request.hasAttachmentFileId(), request.getAttachmentFileId(), "attachment_file_id"));
        changes.setContentItemId(bridge.uuid(
                request.hasContentItemId(), request.getContentItemId(), "content_item_id"));
        changes.setGroupId(bridge.uuid(request.hasGroupId(), request.getGroupId(), "group_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        return bridge.patch(changes);
    }
}
