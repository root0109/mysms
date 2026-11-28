package com.mysms.identity.people.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.identity.people.api.model.HonorificUpdate;

/**
 * UpdateHonorific over gRPC (JAVA-CODING-RULES §7.2): the {@code people.v1.HonorificUpdate} message
 * as REST's {@link HonorificUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class HonorificProtoBridge {

    private HonorificProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static HonorificUpdate changes(ProtoBridge bridge, com.mysms.people.v1.HonorificUpdate request) {
        HonorificUpdate changes = new HonorificUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setShortcut(ProtoBridge.text(request.hasShortcut(), request.getShortcut()));
        return bridge.patch(changes);
    }
}
