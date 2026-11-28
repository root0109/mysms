package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ExternalToolUpdate;

/**
 * UpdateExternalTool over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ExternalToolUpdate} message as REST's {@link
 * ExternalToolUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class ExternalToolProtoBridge {

    private ExternalToolProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ExternalToolUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ExternalToolUpdate request) {
        ExternalToolUpdate changes = new ExternalToolUpdate();
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setIsIncluded(ProtoBridge.flag(request.hasIsIncluded(), request.getIsIncluded()));
        changes.setIsPaid(ProtoBridge.flag(request.hasIsPaid(), request.getIsPaid()));
        return bridge.patch(changes);
    }
}
