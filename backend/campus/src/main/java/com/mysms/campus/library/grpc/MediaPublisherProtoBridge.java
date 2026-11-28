package com.mysms.campus.library.grpc;

import com.mysms.campus.library.api.model.MediaPublisherUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateMediaPublisher over gRPC (JAVA-CODING-RULES §7.2): the {@code library.v1.MediaPublisherUpdate} message as
 * REST's {@link MediaPublisherUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value
 * sets it.
 */
final class MediaPublisherProtoBridge {

    private MediaPublisherProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static MediaPublisherUpdate changes(ProtoBridge bridge, com.mysms.library.v1.MediaPublisherUpdate request) {
        MediaPublisherUpdate changes = new MediaPublisherUpdate();
        changes.setPublisherId(bridge.uuid(request.hasPublisherId(), request.getPublisherId(), "publisher_id"));
        return bridge.patch(changes);
    }
}
