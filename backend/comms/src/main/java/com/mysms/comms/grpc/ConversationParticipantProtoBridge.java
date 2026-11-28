package com.mysms.comms.grpc;

import com.mysms.comms.api.model.ConversationParticipantUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateConversationParticipant over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.ConversationParticipantUpdate} message
 * as REST's {@link ConversationParticipantUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class ConversationParticipantProtoBridge {

    private ConversationParticipantProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ConversationParticipantUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.ConversationParticipantUpdate request) {
        ConversationParticipantUpdate changes = new ConversationParticipantUpdate();
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        changes.setLastReadAt(ProtoBridge.time(request.hasLastReadAt(), request.getLastReadAt()));
        return bridge.patch(changes);
    }
}
