package com.mysms.campus.library.grpc;

import com.mysms.campus.library.api.model.MediaTypeUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateMediaType over gRPC (JAVA-CODING-RULES §7.2): the {@code library.v1.MediaTypeUpdate} message as REST's {@link
 * MediaTypeUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class MediaTypeProtoBridge {

    private MediaTypeProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static MediaTypeUpdate changes(ProtoBridge bridge, com.mysms.library.v1.MediaTypeUpdate request) {
        MediaTypeUpdate changes = new MediaTypeUpdate();
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        return bridge.patch(changes);
    }
}
