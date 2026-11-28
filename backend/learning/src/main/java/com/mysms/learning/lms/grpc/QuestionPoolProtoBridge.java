package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.QuestionPoolUpdate;

/**
 * UpdateQuestionPool over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.QuestionPoolUpdate} message as REST's {@link
 * QuestionPoolUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class QuestionPoolProtoBridge {

    private QuestionPoolProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static QuestionPoolUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.QuestionPoolUpdate request) {
        QuestionPoolUpdate changes = new QuestionPoolUpdate();
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        return bridge.patch(changes);
    }
}
