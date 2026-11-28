package com.mysms.comms.grpc;

import com.mysms.comms.api.model.CommentReactionUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateCommentReaction over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.CommentReactionUpdate} message
 * as REST's {@link CommentReactionUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class CommentReactionProtoBridge {

    private CommentReactionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CommentReactionUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.CommentReactionUpdate request) {
        CommentReactionUpdate changes = new CommentReactionUpdate();
        changes.setReaction(bridge.choice(
                request.hasReaction(), request.getReaction(), CommentReactionUpdate.ReactionEnum::fromValue));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
