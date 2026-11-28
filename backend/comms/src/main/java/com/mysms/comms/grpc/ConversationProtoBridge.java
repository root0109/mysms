package com.mysms.comms.grpc;

import com.mysms.comms.api.model.ConversationUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateConversation over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.ConversationUpdate} message
 * as REST's {@link ConversationUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class ConversationProtoBridge {

    private ConversationProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ConversationUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.ConversationUpdate request) {
        ConversationUpdate changes = new ConversationUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setConversationType(bridge.choice(
                request.hasConversationType(), request.getConversationType(),
                ConversationUpdate.ConversationTypeEnum::fromValue));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setLastMessageAt(ProtoBridge.time(request.hasLastMessageAt(), request.getLastMessageAt()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
