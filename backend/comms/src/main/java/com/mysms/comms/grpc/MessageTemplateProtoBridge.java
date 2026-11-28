package com.mysms.comms.grpc;

import com.mysms.comms.api.model.MessageTemplateUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateMessageTemplate over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.MessageTemplateUpdate} message
 * as REST's {@link MessageTemplateUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class MessageTemplateProtoBridge {

    private MessageTemplateProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static MessageTemplateUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.MessageTemplateUpdate request) {
        MessageTemplateUpdate changes = new MessageTemplateUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setChannel(bridge.choice(
                request.hasChannel(), request.getChannel(), MessageTemplateUpdate.ChannelEnum::fromValue));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setSubject(ProtoBridge.text(request.hasSubject(), request.getSubject()));
        changes.setBody(ProtoBridge.text(request.hasBody(), request.getBody()));
        changes.setExternalTemplateId(ProtoBridge.text(
                request.hasExternalTemplateId(), request.getExternalTemplateId()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
