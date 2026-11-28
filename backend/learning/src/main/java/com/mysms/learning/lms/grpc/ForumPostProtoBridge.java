package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ForumPostUpdate;

/**
 * UpdateForumPost over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ForumPostUpdate} message as REST's {@link
 * ForumPostUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class ForumPostProtoBridge {

    private ForumPostProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ForumPostUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ForumPostUpdate request) {
        ForumPostUpdate changes = new ForumPostUpdate();
        changes.setParentPostId(bridge.uuid(request.hasParentPostId(), request.getParentPostId(), "parent_post_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setBody(ProtoBridge.text(request.hasBody(), request.getBody()));
        changes.setIsDraft(ProtoBridge.flag(request.hasIsDraft(), request.getIsDraft()));
        changes.setShowSignature(ProtoBridge.flag(request.hasShowSignature(), request.getShowSignature()));
        changes.setUsesInlineAuthoring(ProtoBridge.flag(
                request.hasUsesInlineAuthoring(), request.getUsesInlineAuthoring()));
        return bridge.patch(changes);
    }
}
