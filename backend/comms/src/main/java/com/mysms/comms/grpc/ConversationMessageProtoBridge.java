package com.mysms.comms.grpc;

import com.mysms.comms.api.model.ConversationMessageUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateConversationMessage over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.ConversationMessageUpdate} message
 * as REST's {@link ConversationMessageUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class ConversationMessageProtoBridge {

    private ConversationMessageProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ConversationMessageUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.ConversationMessageUpdate request) {
        ConversationMessageUpdate changes = new ConversationMessageUpdate();
        changes.setBody(ProtoBridge.text(request.hasBody(), request.getBody()));
        changes.setDeletedBySender(ProtoBridge.flag(request.hasDeletedBySender(), request.getDeletedBySender()));
        return bridge.patch(changes);
    }
}
