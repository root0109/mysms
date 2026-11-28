package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.QuestionAcceptedAnswerUpdate;

/**
 * UpdateQuestionAcceptedAnswer over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.QuestionAcceptedAnswerUpdate}
 * message as REST's {@link QuestionAcceptedAnswerUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class QuestionAcceptedAnswerProtoBridge {

    private QuestionAcceptedAnswerProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static QuestionAcceptedAnswerUpdate changes(
            ProtoBridge bridge, com.mysms.lms.v1.QuestionAcceptedAnswerUpdate request) {
        QuestionAcceptedAnswerUpdate changes = new QuestionAcceptedAnswerUpdate();
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setAnswerText(ProtoBridge.text(request.hasAnswerText(), request.getAnswerText()));
        return bridge.patch(changes);
    }
}
