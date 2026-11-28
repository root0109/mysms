package com.mysms.comms.grpc;

import com.mysms.comms.api.model.PostReactionUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdatePostReaction over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.PostReactionUpdate} message
 * as REST's {@link PostReactionUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class PostReactionProtoBridge {

    private PostReactionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PostReactionUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.PostReactionUpdate request) {
        PostReactionUpdate changes = new PostReactionUpdate();
        changes.setReaction(bridge.choice(
                request.hasReaction(), request.getReaction(), PostReactionUpdate.ReactionEnum::fromValue));
        changes.setIsSaved(ProtoBridge.flag(request.hasIsSaved(), request.getIsSaved()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
