package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.QuestionMatchPairUpdate;

/**
 * UpdateQuestionMatchPair over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.QuestionMatchPairUpdate} message as
 * REST's {@link QuestionMatchPairUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class QuestionMatchPairProtoBridge {

    private QuestionMatchPairProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static QuestionMatchPairUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.QuestionMatchPairUpdate request) {
        QuestionMatchPairUpdate changes = new QuestionMatchPairUpdate();
        changes.setLeftText(ProtoBridge.text(request.hasLeftText(), request.getLeftText()));
        changes.setRightText(ProtoBridge.text(request.hasRightText(), request.getRightText()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
