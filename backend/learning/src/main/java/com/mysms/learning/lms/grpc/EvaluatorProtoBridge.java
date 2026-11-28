package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.EvaluatorUpdate;

/**
 * UpdateEvaluator over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.EvaluatorUpdate} message as REST's {@link
 * EvaluatorUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class EvaluatorProtoBridge {

    private EvaluatorProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static EvaluatorUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.EvaluatorUpdate request) {
        EvaluatorUpdate changes = new EvaluatorUpdate();
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        changes.setUnavailableFrom(bridge.date(
                request.hasUnavailableFrom(), request.getUnavailableFrom(), "unavailable_from"));
        changes.setUnavailableTo(bridge.date(request.hasUnavailableTo(), request.getUnavailableTo(), "unavailable_to"));
        return bridge.patch(changes);
    }
}
