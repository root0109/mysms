package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.RubricCellUpdate;

/**
 * UpdateRubricCell over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.RubricCellUpdate} message as REST's {@link
 * RubricCellUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class RubricCellProtoBridge {

    private RubricCellProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static RubricCellUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.RubricCellUpdate request) {
        RubricCellUpdate changes = new RubricCellUpdate();
        changes.setRubricCriterionId(bridge.uuid(
                request.hasRubricCriterionId(), request.getRubricCriterionId(), "rubric_criterion_id"));
        changes.setRubricLevelId(bridge.uuid(
                request.hasRubricLevelId(), request.getRubricLevelId(), "rubric_level_id"));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
