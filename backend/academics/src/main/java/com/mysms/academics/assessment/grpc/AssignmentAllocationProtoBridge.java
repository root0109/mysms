package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.AssignmentAllocationUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateAssignmentAllocation over gRPC (JAVA-CODING-RULES §7.2): the {@code assessment.v1.AssignmentAllocationUpdate}
 * message as REST's {@link AssignmentAllocationUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class AssignmentAllocationProtoBridge {

    private AssignmentAllocationProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static AssignmentAllocationUpdate changes(
            ProtoBridge bridge, com.mysms.assessment.v1.AssignmentAllocationUpdate request) {
        AssignmentAllocationUpdate changes = new AssignmentAllocationUpdate();
        changes.setStudentId(bridge.uuid(request.hasStudentId(), request.getStudentId(), "student_id"));
        return bridge.patch(changes);
    }
}
