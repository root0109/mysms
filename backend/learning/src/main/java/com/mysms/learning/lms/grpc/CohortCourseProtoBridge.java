package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.CohortCourseUpdate;

/**
 * UpdateCohortCourse over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.CohortCourseUpdate} message as REST's {@link
 * CohortCourseUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class CohortCourseProtoBridge {

    private CohortCourseProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CohortCourseUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.CohortCourseUpdate request) {
        CohortCourseUpdate changes = new CohortCourseUpdate();
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        changes.setEvaluatorId(bridge.uuid(request.hasEvaluatorId(), request.getEvaluatorId(), "evaluator_id"));
        return bridge.patch(changes);
    }
}
