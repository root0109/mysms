package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ExternalToolRoleUpdate;

/**
 * UpdateExternalToolRole over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ExternalToolRoleUpdate} message as
 * REST's {@link ExternalToolRoleUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class ExternalToolRoleProtoBridge {

    private ExternalToolRoleProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ExternalToolRoleUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ExternalToolRoleUpdate request) {
        ExternalToolRoleUpdate changes = new ExternalToolRoleUpdate();
        changes.setRoleId(bridge.uuid(request.hasRoleId(), request.getRoleId(), "role_id"));
        return bridge.patch(changes);
    }
}
