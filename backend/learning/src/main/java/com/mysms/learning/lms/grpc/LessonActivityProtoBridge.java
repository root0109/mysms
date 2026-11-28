package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.LessonActivityUpdate;

/**
 * UpdateLessonActivity over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.LessonActivityUpdate} message as REST's
 * {@link LessonActivityUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class LessonActivityProtoBridge {

    private LessonActivityProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static LessonActivityUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.LessonActivityUpdate request) {
        LessonActivityUpdate changes = new LessonActivityUpdate();
        changes.setQuizId(bridge.uuid(request.hasQuizId(), request.getQuizId(), "quiz_id"));
        changes.setAssignmentId(bridge.uuid(request.hasAssignmentId(), request.getAssignmentId(), "assignment_id"));
        changes.setExerciseId(bridge.uuid(request.hasExerciseId(), request.getExerciseId(), "exercise_id"));
        changes.setTeacherOnly(ProtoBridge.flag(request.hasTeacherOnly(), request.getTeacherOnly()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
