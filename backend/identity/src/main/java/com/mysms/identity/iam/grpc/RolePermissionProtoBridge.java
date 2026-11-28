package com.mysms.identity.iam.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.identity.iam.api.model.RolePermissionUpdate;

/**
 * UpdateRolePermission over gRPC (JAVA-CODING-RULES §7.2): the {@code iam.v1.RolePermissionUpdate} message
 * as REST's {@link RolePermissionUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class RolePermissionProtoBridge {

    private RolePermissionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static RolePermissionUpdate changes(ProtoBridge bridge, com.mysms.iam.v1.RolePermissionUpdate request) {
        RolePermissionUpdate changes = new RolePermissionUpdate();
        changes.setPermissionId(bridge.uuid(request.hasPermissionId(), request.getPermissionId(), "permission_id"));
        return bridge.patch(changes);
    }
}
