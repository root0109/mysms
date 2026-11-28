package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.sis.api.model.ActivityTypeUpdate;

/**
 * UpdateActivityType over gRPC (JAVA-CODING-RULES §7.2): the {@code sis.v1.ActivityTypeUpdate} message as REST's {@link
 * ActivityTypeUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class ActivityTypeProtoBridge {

    private ActivityTypeProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ActivityTypeUpdate changes(ProtoBridge bridge, com.mysms.sis.v1.ActivityTypeUpdate request) {
        ActivityTypeUpdate changes = new ActivityTypeUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
