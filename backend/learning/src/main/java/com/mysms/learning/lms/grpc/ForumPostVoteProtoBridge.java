package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ForumPostVoteUpdate;

/**
 * UpdateForumPostVote over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ForumPostVoteUpdate} message as REST's
 * {@link ForumPostVoteUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class ForumPostVoteProtoBridge {

    private ForumPostVoteProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ForumPostVoteUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ForumPostVoteUpdate request) {
        ForumPostVoteUpdate changes = new ForumPostVoteUpdate();
        changes.setForumPostId(bridge.uuid(request.hasForumPostId(), request.getForumPostId(), "forum_post_id"));
        changes.setIsLike(ProtoBridge.flag(request.hasIsLike(), request.getIsLike()));
        return bridge.patch(changes);
    }
}
