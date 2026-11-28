package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.DiscussionPromptUpdate;

/**
 * UpdateDiscussionPrompt over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.DiscussionPromptUpdate} message as
 * REST's {@link DiscussionPromptUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class DiscussionPromptProtoBridge {

    private DiscussionPromptProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static DiscussionPromptUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.DiscussionPromptUpdate request) {
        DiscussionPromptUpdate changes = new DiscussionPromptUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setIsInRepository(ProtoBridge.flag(request.hasIsInRepository(), request.getIsInRepository()));
        changes.setUsesInlineAuthoring(ProtoBridge.flag(
                request.hasUsesInlineAuthoring(), request.getUsesInlineAuthoring()));
        return bridge.patch(changes);
    }
}
