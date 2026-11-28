package com.mysms.identity.iam.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.identity.iam.api.model.UserRoleUpdate;

/**
 * UpdateUserRole over gRPC (JAVA-CODING-RULES §7.2): the {@code iam.v1.UserRoleUpdate} message
 * as REST's {@link UserRoleUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class UserRoleProtoBridge {

    private UserRoleProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static UserRoleUpdate changes(ProtoBridge bridge, com.mysms.iam.v1.UserRoleUpdate request) {
        UserRoleUpdate changes = new UserRoleUpdate();
        changes.setRoleId(bridge.uuid(request.hasRoleId(), request.getRoleId(), "role_id"));
        changes.setScopeType(bridge.choice(
                request.hasScopeType(), request.getScopeType(), UserRoleUpdate.ScopeTypeEnum::fromValue));
        changes.setScopeId(bridge.uuid(request.hasScopeId(), request.getScopeId(), "scope_id"));
        changes.setValidFrom(bridge.date(request.hasValidFrom(), request.getValidFrom(), "valid_from"));
        changes.setValidTo(bridge.date(request.hasValidTo(), request.getValidTo(), "valid_to"));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
