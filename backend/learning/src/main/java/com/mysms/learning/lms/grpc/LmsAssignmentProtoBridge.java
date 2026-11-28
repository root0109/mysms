package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.LmsAssignmentUpdate;

/**
 * UpdateLmsAssignment over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.LmsAssignmentUpdate} message as REST's
 * {@link LmsAssignmentUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class LmsAssignmentProtoBridge {

    private LmsAssignmentProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static LmsAssignmentUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.LmsAssignmentUpdate request) {
        LmsAssignmentUpdate changes = new LmsAssignmentUpdate();
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setQuestion(ProtoBridge.text(request.hasQuestion(), request.getQuestion()));
        changes.setSubmissionType(bridge.choice(
                request.hasSubmissionType(), request.getSubmissionType(),
                LmsAssignmentUpdate.SubmissionTypeEnum::fromValue));
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        changes.setIsGraded(ProtoBridge.flag(request.hasIsGraded(), request.getIsGraded()));
        changes.setShowModelAnswer(ProtoBridge.flag(request.hasShowModelAnswer(), request.getShowModelAnswer()));
        changes.setModelAnswer(ProtoBridge.text(request.hasModelAnswer(), request.getModelAnswer()));
        changes.setSchedulingEnabled(ProtoBridge.flag(request.hasSchedulingEnabled(), request.getSchedulingEnabled()));
        changes.setScheduleStart(ProtoBridge.time(request.hasScheduleStart(), request.getScheduleStart()));
        changes.setScheduleEnd(ProtoBridge.time(request.hasScheduleEnd(), request.getScheduleEnd()));
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setSubmissionMode(bridge.choice(
                request.hasSubmissionMode(), request.getSubmissionMode(),
                LmsAssignmentUpdate.SubmissionModeEnum::fromValue));
        changes.setMinGroupSize(ProtoBridge.number(request.hasMinGroupSize(), request.getMinGroupSize()));
        changes.setMaxGroupSize(ProtoBridge.number(request.hasMaxGroupSize(), request.getMaxGroupSize()));
        changes.setPeerReviewPoints(ProtoBridge.text(request.hasPeerReviewPoints(), request.getPeerReviewPoints()));
        changes.setUsesInlineAuthoring(ProtoBridge.flag(
                request.hasUsesInlineAuthoring(), request.getUsesInlineAuthoring()));
        changes.setIsInRepository(ProtoBridge.flag(request.hasIsInRepository(), request.getIsInRepository()));
        return bridge.patch(changes);
    }
}
