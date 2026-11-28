package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.platform.api.model.UploadPolicyUpdate;

/**
 * UpdateUploadPolicy over gRPC (JAVA-CODING-RULES §7.2): the {@code platform.v1.UploadPolicyUpdate} message
 * as REST's {@link UploadPolicyUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class UploadPolicyProtoBridge {

    private UploadPolicyProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static UploadPolicyUpdate changes(ProtoBridge bridge, com.mysms.platform.v1.UploadPolicyUpdate request) {
        UploadPolicyUpdate changes = new UploadPolicyUpdate();
        changes.setRoleId(bridge.uuid(request.hasRoleId(), request.getRoleId(), "role_id"));
        changes.setModuleCode(ProtoBridge.text(request.hasModuleCode(), request.getModuleCode()));
        changes.setOperationCode(ProtoBridge.text(request.hasOperationCode(), request.getOperationCode()));
        changes.setMaxSizeBytes(ProtoBridge.int64(request.hasMaxSizeBytes(), request.getMaxSizeBytes()));
        changes.setAllowedExtensions(ProtoBridge.text(request.hasAllowedExtensions(), request.getAllowedExtensions()));
        return bridge.patch(changes);
    }
}
