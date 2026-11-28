package com.mysms.comms.grpc;

import com.mysms.comms.api.model.PostCommentUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdatePostComment over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.PostCommentUpdate} message
 * as REST's {@link PostCommentUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class PostCommentProtoBridge {

    private PostCommentProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PostCommentUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.PostCommentUpdate request) {
        PostCommentUpdate changes = new PostCommentUpdate();
        changes.setParentCommentId(bridge.uuid(
                request.hasParentCommentId(), request.getParentCommentId(), "parent_comment_id"));
        changes.setBody(ProtoBridge.text(request.hasBody(), request.getBody()));
        changes.setAttachmentFileId(bridge.uuid(
                request.hasAttachmentFileId(), request.getAttachmentFileId(), "attachment_file_id"));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
