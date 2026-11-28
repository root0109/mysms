package com.mysms.identity.iam.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.identity.iam.api.model.UserDepartmentAccessUpdate;

/**
 * UpdateUserDepartmentAccess over gRPC (JAVA-CODING-RULES §7.2): the {@code iam.v1.UserDepartmentAccessUpdate} message
 * as REST's {@link UserDepartmentAccessUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class UserDepartmentAccessProtoBridge {

    private UserDepartmentAccessProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static UserDepartmentAccessUpdate changes(ProtoBridge bridge, com.mysms.iam.v1.UserDepartmentAccessUpdate request) {
        UserDepartmentAccessUpdate changes = new UserDepartmentAccessUpdate();
        changes.setDepartmentId(bridge.uuid(request.hasDepartmentId(), request.getDepartmentId(), "department_id"));
        changes.setIsPrimary(ProtoBridge.flag(request.hasIsPrimary(), request.getIsPrimary()));
        return bridge.patch(changes);
    }
}
