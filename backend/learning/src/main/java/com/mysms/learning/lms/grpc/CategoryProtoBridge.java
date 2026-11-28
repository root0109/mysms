package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.CategoryUpdate;

/**
 * UpdateCategory over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.CategoryUpdate} message as REST's {@link
 * CategoryUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class CategoryProtoBridge {

    private CategoryProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CategoryUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.CategoryUpdate request) {
        CategoryUpdate changes = new CategoryUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        return bridge.patch(changes);
    }
}
