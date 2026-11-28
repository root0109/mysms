package com.mysms.campus.library.grpc;

import com.mysms.campus.library.api.model.PublisherUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdatePublisher over gRPC (JAVA-CODING-RULES §7.2): the {@code library.v1.PublisherUpdate} message as REST's {@link
 * PublisherUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class PublisherProtoBridge {

    private PublisherProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PublisherUpdate changes(ProtoBridge bridge, com.mysms.library.v1.PublisherUpdate request) {
        PublisherUpdate changes = new PublisherUpdate();
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        return bridge.patch(changes);
    }
}
