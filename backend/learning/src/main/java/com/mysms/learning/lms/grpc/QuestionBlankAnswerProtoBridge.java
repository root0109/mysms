package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.QuestionBlankAnswerUpdate;

/**
 * UpdateQuestionBlankAnswer over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.QuestionBlankAnswerUpdate} message as
 * REST's {@link QuestionBlankAnswerUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class QuestionBlankAnswerProtoBridge {

    private QuestionBlankAnswerProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static QuestionBlankAnswerUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.QuestionBlankAnswerUpdate request) {
        QuestionBlankAnswerUpdate changes = new QuestionBlankAnswerUpdate();
        changes.setQuestionBlankId(bridge.uuid(
                request.hasQuestionBlankId(), request.getQuestionBlankId(), "question_blank_id"));
        changes.setAnswerText(ProtoBridge.text(request.hasAnswerText(), request.getAnswerText()));
        changes.setFeedback(ProtoBridge.text(request.hasFeedback(), request.getFeedback()));
        return bridge.patch(changes);
    }
}
