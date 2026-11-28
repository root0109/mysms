package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.AnnotationShareUpdate;

/**
 * UpdateAnnotationShare over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.AnnotationShareUpdate} message as REST's
 * {@link AnnotationShareUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class AnnotationShareProtoBridge {

    private AnnotationShareProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static AnnotationShareUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.AnnotationShareUpdate request) {
        AnnotationShareUpdate changes = new AnnotationShareUpdate();
        changes.setSharedWithUserId(bridge.uuid(
                request.hasSharedWithUserId(), request.getSharedWithUserId(), "shared_with_user_id"));
        changes.setSharedAt(ProtoBridge.time(request.hasSharedAt(), request.getSharedAt()));
        return bridge.patch(changes);
    }
}
