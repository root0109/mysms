package com.mysms.campus.library.grpc;

import com.mysms.campus.library.api.model.MediaSubjectUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateMediaSubject over gRPC (JAVA-CODING-RULES §7.2): the {@code library.v1.MediaSubjectUpdate} message as REST's
 * {@link MediaSubjectUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class MediaSubjectProtoBridge {

    private MediaSubjectProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static MediaSubjectUpdate changes(ProtoBridge bridge, com.mysms.library.v1.MediaSubjectUpdate request) {
        MediaSubjectUpdate changes = new MediaSubjectUpdate();
        changes.setSubjectId(bridge.uuid(request.hasSubjectId(), request.getSubjectId(), "subject_id"));
        return bridge.patch(changes);
    }
}
