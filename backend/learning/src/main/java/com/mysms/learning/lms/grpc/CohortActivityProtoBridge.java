package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.CohortActivityUpdate;

/**
 * UpdateCohortActivity over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.CohortActivityUpdate} message as REST's
 * {@link CohortActivityUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class CohortActivityProtoBridge {

    private CohortActivityProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CohortActivityUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.CohortActivityUpdate request) {
        CohortActivityUpdate changes = new CohortActivityUpdate();
        changes.setQuizId(bridge.uuid(request.hasQuizId(), request.getQuizId(), "quiz_id"));
        changes.setAssignmentId(bridge.uuid(request.hasAssignmentId(), request.getAssignmentId(), "assignment_id"));
        changes.setExerciseId(bridge.uuid(request.hasExerciseId(), request.getExerciseId(), "exercise_id"));
        return bridge.patch(changes);
    }
}
