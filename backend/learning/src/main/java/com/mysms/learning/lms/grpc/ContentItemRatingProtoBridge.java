package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ContentItemRatingUpdate;

/**
 * UpdateContentItemRating over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ContentItemRatingUpdate} message as
 * REST's {@link ContentItemRatingUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class ContentItemRatingProtoBridge {

    private ContentItemRatingProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ContentItemRatingUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ContentItemRatingUpdate request) {
        ContentItemRatingUpdate changes = new ContentItemRatingUpdate();
        changes.setCohortId(bridge.uuid(request.hasCohortId(), request.getCohortId(), "cohort_id"));
        changes.setIsLike(ProtoBridge.flag(request.hasIsLike(), request.getIsLike()));
        changes.setRating(ProtoBridge.number(request.hasRating(), request.getRating()));
        return bridge.patch(changes);
    }
}
