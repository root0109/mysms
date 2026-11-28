package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.AssignmentGroupUpdate;

/**
 * UpdateAssignmentGroup over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.AssignmentGroupUpdate} message as REST's
 * {@link AssignmentGroupUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class AssignmentGroupProtoBridge {

    private AssignmentGroupProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static AssignmentGroupUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.AssignmentGroupUpdate request) {
        AssignmentGroupUpdate changes = new AssignmentGroupUpdate();
        changes.setCohortId(bridge.uuid(request.hasCohortId(), request.getCohortId(), "cohort_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setPoints(ProtoBridge.text(request.hasPoints(), request.getPoints()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setDiscussionTopicId(bridge.uuid(
                request.hasDiscussionTopicId(), request.getDiscussionTopicId(), "discussion_topic_id"));
        changes.setIsReassigned(ProtoBridge.flag(request.hasIsReassigned(), request.getIsReassigned()));
        return bridge.patch(changes);
    }
}
