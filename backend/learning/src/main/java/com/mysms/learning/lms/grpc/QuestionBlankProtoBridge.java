package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.QuestionBlankUpdate;

/**
 * UpdateQuestionBlank over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.QuestionBlankUpdate} message as REST's
 * {@link QuestionBlankUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class QuestionBlankProtoBridge {

    private QuestionBlankProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static QuestionBlankUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.QuestionBlankUpdate request) {
        QuestionBlankUpdate changes = new QuestionBlankUpdate();
        changes.setBlankText(ProtoBridge.text(request.hasBlankText(), request.getBlankText()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
