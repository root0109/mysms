package com.mysms.comms.grpc;

import com.mysms.comms.api.model.TaskAssigneeUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateTaskAssignee over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.TaskAssigneeUpdate} message
 * as REST's {@link TaskAssigneeUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class TaskAssigneeProtoBridge {

    private TaskAssigneeProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static TaskAssigneeUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.TaskAssigneeUpdate request) {
        TaskAssigneeUpdate changes = new TaskAssigneeUpdate();
        changes.setAssigneeType(bridge.choice(
                request.hasAssigneeType(), request.getAssigneeType(), TaskAssigneeUpdate.AssigneeTypeEnum::fromValue));
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        changes.setClassSectionId(bridge.uuid(
                request.hasClassSectionId(), request.getClassSectionId(), "class_section_id"));
        changes.setGroupId(bridge.uuid(request.hasGroupId(), request.getGroupId(), "group_id"));
        changes.setClaimedByUserId(bridge.uuid(
                request.hasClaimedByUserId(), request.getClaimedByUserId(), "claimed_by_user_id"));
        changes.setStatus(bridge.choice(
                request.hasStatus(), request.getStatus(), TaskAssigneeUpdate.StatusEnum::fromValue));
        return bridge.patch(changes);
    }
}
