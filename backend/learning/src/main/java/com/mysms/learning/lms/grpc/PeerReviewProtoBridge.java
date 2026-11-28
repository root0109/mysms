package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.PeerReviewUpdate;

/**
 * UpdatePeerReview over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.PeerReviewUpdate} message as REST's {@link
 * PeerReviewUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class PeerReviewProtoBridge {

    private PeerReviewProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PeerReviewUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.PeerReviewUpdate request) {
        PeerReviewUpdate changes = new PeerReviewUpdate();
        changes.setAssignmentGroupId(bridge.uuid(
                request.hasAssignmentGroupId(), request.getAssignmentGroupId(), "assignment_group_id"));
        changes.setReviewerUserId(bridge.uuid(
                request.hasReviewerUserId(), request.getReviewerUserId(), "reviewer_user_id"));
        changes.setRevieweeUserId(bridge.uuid(
                request.hasRevieweeUserId(), request.getRevieweeUserId(), "reviewee_user_id"));
        changes.setPoints(ProtoBridge.text(request.hasPoints(), request.getPoints()));
        return bridge.patch(changes);
    }
}
