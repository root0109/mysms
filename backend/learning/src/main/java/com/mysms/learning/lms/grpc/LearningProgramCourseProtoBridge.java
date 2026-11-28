package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.LearningProgramCourseUpdate;

/**
 * UpdateLearningProgramCourse over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.LearningProgramCourseUpdate}
 * message as REST's {@link LearningProgramCourseUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class LearningProgramCourseProtoBridge {

    private LearningProgramCourseProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static LearningProgramCourseUpdate changes(
            ProtoBridge bridge, com.mysms.lms.v1.LearningProgramCourseUpdate request) {
        LearningProgramCourseUpdate changes = new LearningProgramCourseUpdate();
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
