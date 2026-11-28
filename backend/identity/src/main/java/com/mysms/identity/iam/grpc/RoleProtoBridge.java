package com.mysms.identity.iam.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.identity.iam.api.model.RoleUpdate;

/**
 * UpdateRole over gRPC (JAVA-CODING-RULES §7.2): the {@code iam.v1.RoleUpdate} message
 * as REST's {@link RoleUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class RoleProtoBridge {

    private RoleProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static RoleUpdate changes(ProtoBridge bridge, com.mysms.iam.v1.RoleUpdate request) {
        RoleUpdate changes = new RoleUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setDisplayName(ProtoBridge.text(request.hasDisplayName(), request.getDisplayName()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setIsSystem(ProtoBridge.flag(request.hasIsSystem(), request.getIsSystem()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setAuthorityLevel(ProtoBridge.number(request.hasAuthorityLevel(), request.getAuthorityLevel()));
        changes.setParentRoleId(bridge.uuid(request.hasParentRoleId(), request.getParentRoleId(), "parent_role_id"));
        return bridge.patch(changes);
    }
}
