package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ContentAuthorUpdate;

/**
 * UpdateContentAuthor over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ContentAuthorUpdate} message as REST's
 * {@link ContentAuthorUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class ContentAuthorProtoBridge {

    private ContentAuthorProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ContentAuthorUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ContentAuthorUpdate request) {
        ContentAuthorUpdate changes = new ContentAuthorUpdate();
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        changes.setQuizId(bridge.uuid(request.hasQuizId(), request.getQuizId(), "quiz_id"));
        changes.setQuestionId(bridge.uuid(request.hasQuestionId(), request.getQuestionId(), "question_id"));
        changes.setAssignmentId(bridge.uuid(request.hasAssignmentId(), request.getAssignmentId(), "assignment_id"));
        changes.setExerciseId(bridge.uuid(request.hasExerciseId(), request.getExerciseId(), "exercise_id"));
        changes.setLearningProgramId(bridge.uuid(
                request.hasLearningProgramId(), request.getLearningProgramId(), "learning_program_id"));
        return bridge.patch(changes);
    }
}
