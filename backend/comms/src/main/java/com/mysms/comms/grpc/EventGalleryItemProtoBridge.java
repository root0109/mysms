package com.mysms.comms.grpc;

import com.mysms.comms.api.model.EventGalleryItemUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateEventGalleryItem over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.EventGalleryItemUpdate} message
 * as REST's {@link EventGalleryItemUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class EventGalleryItemProtoBridge {

    private EventGalleryItemProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static EventGalleryItemUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.EventGalleryItemUpdate request) {
        EventGalleryItemUpdate changes = new EventGalleryItemUpdate();
        changes.setFileId(bridge.uuid(request.hasFileId(), request.getFileId(), "file_id"));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
