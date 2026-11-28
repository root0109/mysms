package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ItemOverrideUpdate;

/**
 * UpdateItemOverride over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ItemOverrideUpdate} message as REST's {@link
 * ItemOverrideUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class ItemOverrideProtoBridge {

    private ItemOverrideProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ItemOverrideUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ItemOverrideUpdate request) {
        ItemOverrideUpdate changes = new ItemOverrideUpdate();
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        changes.setGroupId(bridge.uuid(request.hasGroupId(), request.getGroupId(), "group_id"));
        changes.setAvailableFrom(ProtoBridge.time(request.hasAvailableFrom(), request.getAvailableFrom()));
        changes.setDueAt(ProtoBridge.time(request.hasDueAt(), request.getDueAt()));
        changes.setExtraAttempts(ProtoBridge.number(request.hasExtraAttempts(), request.getExtraAttempts()));
        changes.setExtraTimeMinutes(ProtoBridge.number(request.hasExtraTimeMinutes(), request.getExtraTimeMinutes()));
        changes.setReason(ProtoBridge.text(request.hasReason(), request.getReason()));
        return bridge.patch(changes);
    }
}
