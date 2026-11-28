package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.CohortGroupUpdate;

/**
 * UpdateCohortGroup over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.CohortGroupUpdate} message as REST's {@link
 * CohortGroupUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class CohortGroupProtoBridge {

    private CohortGroupProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CohortGroupUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.CohortGroupUpdate request) {
        CohortGroupUpdate changes = new CohortGroupUpdate();
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setIsDisabled(ProtoBridge.flag(request.hasIsDisabled(), request.getIsDisabled()));
        return bridge.patch(changes);
    }
}
