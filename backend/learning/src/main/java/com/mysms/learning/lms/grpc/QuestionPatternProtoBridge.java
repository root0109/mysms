package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.QuestionPatternUpdate;

/**
 * UpdateQuestionPattern over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.QuestionPatternUpdate} message as REST's
 * {@link QuestionPatternUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class QuestionPatternProtoBridge {

    private QuestionPatternProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static QuestionPatternUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.QuestionPatternUpdate request) {
        QuestionPatternUpdate changes = new QuestionPatternUpdate();
        changes.setModelAnswer(ProtoBridge.text(request.hasModelAnswer(), request.getModelAnswer()));
        changes.setPattern(ProtoBridge.text(request.hasPattern(), request.getPattern()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
