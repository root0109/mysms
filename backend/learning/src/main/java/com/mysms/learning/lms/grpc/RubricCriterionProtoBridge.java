package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.RubricCriterionUpdate;

/**
 * UpdateRubricCriterion over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.RubricCriterionUpdate} message as REST's
 * {@link RubricCriterionUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class RubricCriterionProtoBridge {

    private RubricCriterionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static RubricCriterionUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.RubricCriterionUpdate request) {
        RubricCriterionUpdate changes = new RubricCriterionUpdate();
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setGroupName(ProtoBridge.text(request.hasGroupName(), request.getGroupName()));
        changes.setWeightPercent(ProtoBridge.text(request.hasWeightPercent(), request.getWeightPercent()));
        changes.setGroupWeightPercent(ProtoBridge.text(
                request.hasGroupWeightPercent(), request.getGroupWeightPercent()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
