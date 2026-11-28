package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ExternalToolInstitutionUpdate;

/**
 * UpdateExternalToolInstitution over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ExternalToolInstitutionUpdate}
 * message as REST's {@link ExternalToolInstitutionUpdate} body, one explicit line per field: unset keeps the value,
 * null clears it, a value sets it.
 */
final class ExternalToolInstitutionProtoBridge {

    private ExternalToolInstitutionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ExternalToolInstitutionUpdate changes(
            ProtoBridge bridge, com.mysms.lms.v1.ExternalToolInstitutionUpdate request) {
        ExternalToolInstitutionUpdate changes = new ExternalToolInstitutionUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        return bridge.patch(changes);
    }
}
