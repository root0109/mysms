package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.AssignmentGroupMemberUpdate;

/**
 * UpdateAssignmentGroupMember over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.AssignmentGroupMemberUpdate}
 * message as REST's {@link AssignmentGroupMemberUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class AssignmentGroupMemberProtoBridge {

    private AssignmentGroupMemberProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static AssignmentGroupMemberUpdate changes(
            ProtoBridge bridge, com.mysms.lms.v1.AssignmentGroupMemberUpdate request) {
        AssignmentGroupMemberUpdate changes = new AssignmentGroupMemberUpdate();
        changes.setAssignmentGroupId(bridge.uuid(
                request.hasAssignmentGroupId(), request.getAssignmentGroupId(), "assignment_group_id"));
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        changes.setContentItemId(bridge.uuid(
                request.hasContentItemId(), request.getContentItemId(), "content_item_id"));
        return bridge.patch(changes);
    }
}
