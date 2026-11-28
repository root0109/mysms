package com.mysms.campus.library.grpc;

import com.mysms.campus.library.api.model.MediaAuthorUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateMediaAuthor over gRPC (JAVA-CODING-RULES §7.2): the {@code library.v1.MediaAuthorUpdate} message as REST's
 * {@link MediaAuthorUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class MediaAuthorProtoBridge {

    private MediaAuthorProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static MediaAuthorUpdate changes(ProtoBridge bridge, com.mysms.library.v1.MediaAuthorUpdate request) {
        MediaAuthorUpdate changes = new MediaAuthorUpdate();
        changes.setAuthorId(bridge.uuid(request.hasAuthorId(), request.getAuthorId(), "author_id"));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
