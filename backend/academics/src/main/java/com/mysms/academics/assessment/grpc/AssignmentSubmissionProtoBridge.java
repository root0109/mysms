package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.AssignmentSubmissionUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateAssignmentSubmission over gRPC (JAVA-CODING-RULES §7.2): the {@code assessment.v1.AssignmentSubmissionUpdate}
 * message as REST's {@link AssignmentSubmissionUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class AssignmentSubmissionProtoBridge {

    private AssignmentSubmissionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static AssignmentSubmissionUpdate changes(
            ProtoBridge bridge, com.mysms.assessment.v1.AssignmentSubmissionUpdate request) {
        AssignmentSubmissionUpdate changes = new AssignmentSubmissionUpdate();
        changes.setAssignmentId(bridge.uuid(request.hasAssignmentId(), request.getAssignmentId(), "assignment_id"));
        changes.setContent(ProtoBridge.text(request.hasContent(), request.getContent()));
        return bridge.patch(changes);
    }
}
