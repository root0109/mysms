package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.QuizQuestionUpdate;

/**
 * UpdateQuizQuestion over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.QuizQuestionUpdate} message as REST's {@link
 * QuizQuestionUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class QuizQuestionProtoBridge {

    private QuizQuestionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static QuizQuestionUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.QuizQuestionUpdate request) {
        QuizQuestionUpdate changes = new QuizQuestionUpdate();
        changes.setQuestionId(bridge.uuid(request.hasQuestionId(), request.getQuestionId(), "question_id"));
        changes.setMarks(ProtoBridge.text(request.hasMarks(), request.getMarks()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
