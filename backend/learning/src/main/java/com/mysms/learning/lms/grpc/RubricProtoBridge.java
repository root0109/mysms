package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.RubricUpdate;

/**
 * UpdateRubric over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.RubricUpdate} message as REST's {@link
 * RubricUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class RubricProtoBridge {

    private RubricProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static RubricUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.RubricUpdate request) {
        RubricUpdate changes = new RubricUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setIsDefault(ProtoBridge.flag(request.hasIsDefault(), request.getIsDefault()));
        return bridge.patch(changes);
    }
}
