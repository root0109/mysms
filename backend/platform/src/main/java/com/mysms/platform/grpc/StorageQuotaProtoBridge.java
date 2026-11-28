package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.platform.api.model.StorageQuotaUpdate;

/**
 * UpdateStorageQuota over gRPC (JAVA-CODING-RULES §7.2): the {@code platform.v1.StorageQuotaUpdate} message
 * as REST's {@link StorageQuotaUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class StorageQuotaProtoBridge {

    private StorageQuotaProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static StorageQuotaUpdate changes(ProtoBridge bridge, com.mysms.platform.v1.StorageQuotaUpdate request) {
        StorageQuotaUpdate changes = new StorageQuotaUpdate();
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setQuotaBytes(ProtoBridge.int64(request.hasQuotaBytes(), request.getQuotaBytes()));
        changes.setUsedBytes(ProtoBridge.int64(request.hasUsedBytes(), request.getUsedBytes()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
