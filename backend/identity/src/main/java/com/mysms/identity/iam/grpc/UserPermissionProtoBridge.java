package com.mysms.identity.iam.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.identity.iam.api.model.UserPermissionUpdate;

/**
 * UpdateUserPermission over gRPC (JAVA-CODING-RULES §7.2): the {@code iam.v1.UserPermissionUpdate} message
 * as REST's {@link UserPermissionUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class UserPermissionProtoBridge {

    private UserPermissionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static UserPermissionUpdate changes(ProtoBridge bridge, com.mysms.iam.v1.UserPermissionUpdate request) {
        UserPermissionUpdate changes = new UserPermissionUpdate();
        changes.setPermissionId(bridge.uuid(request.hasPermissionId(), request.getPermissionId(), "permission_id"));
        changes.setEffect(bridge.choice(
                request.hasEffect(), request.getEffect(), UserPermissionUpdate.EffectEnum::fromValue));
        return bridge.patch(changes);
    }
}
