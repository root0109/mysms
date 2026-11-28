package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.AssignmentTypeUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateAssignmentType over gRPC (JAVA-CODING-RULES §7.2): the {@code assessment.v1.AssignmentTypeUpdate} message as
 * REST's {@link AssignmentTypeUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value
 * sets it.
 */
final class AssignmentTypeProtoBridge {

    private AssignmentTypeProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static AssignmentTypeUpdate changes(ProtoBridge bridge, com.mysms.assessment.v1.AssignmentTypeUpdate request) {
        AssignmentTypeUpdate changes = new AssignmentTypeUpdate();
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setGradingBasis(bridge.choice(
                request.hasGradingBasis(), request.getGradingBasis(),
                AssignmentTypeUpdate.GradingBasisEnum::fromValue));
        return bridge.patch(changes);
    }
}
