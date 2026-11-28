package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.AssignmentSubjectUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateAssignmentSubject over gRPC (JAVA-CODING-RULES §7.2): the {@code assessment.v1.AssignmentSubjectUpdate} message
 * as REST's {@link AssignmentSubjectUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class AssignmentSubjectProtoBridge {

    private AssignmentSubjectProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static AssignmentSubjectUpdate changes(
            ProtoBridge bridge, com.mysms.assessment.v1.AssignmentSubjectUpdate request) {
        AssignmentSubjectUpdate changes = new AssignmentSubjectUpdate();
        changes.setSubjectId(bridge.uuid(request.hasSubjectId(), request.getSubjectId(), "subject_id"));
        return bridge.patch(changes);
    }
}
