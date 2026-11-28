package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.RubricLevelUpdate;

/**
 * UpdateRubricLevel over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.RubricLevelUpdate} message as REST's {@link
 * RubricLevelUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class RubricLevelProtoBridge {

    private RubricLevelProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static RubricLevelUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.RubricLevelUpdate request) {
        RubricLevelUpdate changes = new RubricLevelUpdate();
        changes.setLabel(ProtoBridge.text(request.hasLabel(), request.getLabel()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setScorePercent(ProtoBridge.text(request.hasScorePercent(), request.getScorePercent()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
