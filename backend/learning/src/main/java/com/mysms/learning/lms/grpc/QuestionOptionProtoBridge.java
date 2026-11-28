package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.QuestionOptionUpdate;

/**
 * UpdateQuestionOption over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.QuestionOptionUpdate} message as REST's
 * {@link QuestionOptionUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class QuestionOptionProtoBridge {

    private QuestionOptionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static QuestionOptionUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.QuestionOptionUpdate request) {
        QuestionOptionUpdate changes = new QuestionOptionUpdate();
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setOptionText(ProtoBridge.text(request.hasOptionText(), request.getOptionText()));
        changes.setIsCorrect(ProtoBridge.flag(request.hasIsCorrect(), request.getIsCorrect()));
        changes.setExplanation(ProtoBridge.text(request.hasExplanation(), request.getExplanation()));
        changes.setLabel(ProtoBridge.text(request.hasLabel(), request.getLabel()));
        return bridge.patch(changes);
    }
}
