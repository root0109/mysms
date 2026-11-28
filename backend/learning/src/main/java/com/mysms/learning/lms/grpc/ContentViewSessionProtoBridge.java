package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ContentViewSessionUpdate;

/**
 * UpdateContentViewSession over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ContentViewSessionUpdate} message as
 * REST's {@link ContentViewSessionUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class ContentViewSessionProtoBridge {

    private ContentViewSessionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ContentViewSessionUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ContentViewSessionUpdate request) {
        ContentViewSessionUpdate changes = new ContentViewSessionUpdate();
        changes.setContentItemId(bridge.uuid(
                request.hasContentItemId(), request.getContentItemId(), "content_item_id"));
        changes.setFileId(bridge.uuid(request.hasFileId(), request.getFileId(), "file_id"));
        changes.setCohortId(bridge.uuid(request.hasCohortId(), request.getCohortId(), "cohort_id"));
        changes.setStartedAt(ProtoBridge.time(request.hasStartedAt(), request.getStartedAt()));
        changes.setEndedAt(ProtoBridge.time(request.hasEndedAt(), request.getEndedAt()));
        changes.setLocation(ProtoBridge.text(request.hasLocation(), request.getLocation()));
        changes.setSessionRef(ProtoBridge.text(request.hasSessionRef(), request.getSessionRef()));
        return bridge.patch(changes);
    }
}
