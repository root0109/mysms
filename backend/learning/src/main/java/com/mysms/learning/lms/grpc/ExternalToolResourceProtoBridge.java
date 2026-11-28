package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ExternalToolResourceUpdate;

/**
 * UpdateExternalToolResource over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ExternalToolResourceUpdate} message
 * as REST's {@link ExternalToolResourceUpdate} body, one explicit line per field: unset keeps the value, null clears
 * it, a value sets it.
 */
final class ExternalToolResourceProtoBridge {

    private ExternalToolResourceProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ExternalToolResourceUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ExternalToolResourceUpdate request) {
        ExternalToolResourceUpdate changes = new ExternalToolResourceUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        return bridge.patch(changes);
    }
}
