package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.QuestionPoolItemUpdate;

/**
 * UpdateQuestionPoolItem over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.QuestionPoolItemUpdate} message as
 * REST's {@link QuestionPoolItemUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class QuestionPoolItemProtoBridge {

    private QuestionPoolItemProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static QuestionPoolItemUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.QuestionPoolItemUpdate request) {
        QuestionPoolItemUpdate changes = new QuestionPoolItemUpdate();
        changes.setQuestionId(bridge.uuid(request.hasQuestionId(), request.getQuestionId(), "question_id"));
        return bridge.patch(changes);
    }
}
