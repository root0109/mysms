package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.LearningProgramUpdate;

/**
 * UpdateLearningProgram over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.LearningProgramUpdate} message as REST's
 * {@link LearningProgramUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class LearningProgramProtoBridge {

    private LearningProgramProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static LearningProgramUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.LearningProgramUpdate request) {
        LearningProgramUpdate changes = new LearningProgramUpdate();
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setIsPublished(ProtoBridge.flag(request.hasIsPublished(), request.getIsPublished()));
        changes.setEnforceCourseOrder(ProtoBridge.flag(
                request.hasEnforceCourseOrder(), request.getEnforceCourseOrder()));
        changes.setCourseCount(ProtoBridge.number(request.hasCourseCount(), request.getCourseCount()));
        changes.setMemberCount(ProtoBridge.number(request.hasMemberCount(), request.getMemberCount()));
        return bridge.patch(changes);
    }
}
