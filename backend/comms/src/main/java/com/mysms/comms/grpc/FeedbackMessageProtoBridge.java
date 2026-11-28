package com.mysms.comms.grpc;

import com.mysms.comms.api.model.FeedbackMessageUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateFeedbackMessage over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.FeedbackMessageUpdate} message
 * as REST's {@link FeedbackMessageUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class FeedbackMessageProtoBridge {

    private FeedbackMessageProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static FeedbackMessageUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.FeedbackMessageUpdate request) {
        FeedbackMessageUpdate changes = new FeedbackMessageUpdate();
        changes.setCategory(bridge.choice(
                request.hasCategory(), request.getCategory(), FeedbackMessageUpdate.CategoryEnum::fromValue));
        changes.setMessage(ProtoBridge.text(request.hasMessage(), request.getMessage()));
        changes.setAttachmentFileId(bridge.uuid(
                request.hasAttachmentFileId(), request.getAttachmentFileId(), "attachment_file_id"));
        changes.setReviewStatus(bridge.choice(
                request.hasReviewStatus(), request.getReviewStatus(), FeedbackMessageUpdate.ReviewStatusEnum::fromValue));
        changes.setDeletedBySender(ProtoBridge.flag(request.hasDeletedBySender(), request.getDeletedBySender()));
        changes.setDeletedByReceiver(ProtoBridge.flag(request.hasDeletedByReceiver(), request.getDeletedByReceiver()));
        return bridge.patch(changes);
    }
}
