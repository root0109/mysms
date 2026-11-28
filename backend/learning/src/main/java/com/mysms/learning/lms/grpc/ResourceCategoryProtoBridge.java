package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ResourceCategoryUpdate;

/**
 * UpdateResourceCategory over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ResourceCategoryUpdate} message as
 * REST's {@link ResourceCategoryUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class ResourceCategoryProtoBridge {

    private ResourceCategoryProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ResourceCategoryUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ResourceCategoryUpdate request) {
        ResourceCategoryUpdate changes = new ResourceCategoryUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        return bridge.patch(changes);
    }
}
