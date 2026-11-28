package com.mysms.comms.grpc;

import com.mysms.comms.api.model.ClassPageAttachmentUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateClassPageAttachment over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.ClassPageAttachmentUpdate} message
 * as REST's {@link ClassPageAttachmentUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class ClassPageAttachmentProtoBridge {

    private ClassPageAttachmentProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ClassPageAttachmentUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.ClassPageAttachmentUpdate request) {
        ClassPageAttachmentUpdate changes = new ClassPageAttachmentUpdate();
        changes.setFileId(bridge.uuid(request.hasFileId(), request.getFileId(), "file_id"));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
