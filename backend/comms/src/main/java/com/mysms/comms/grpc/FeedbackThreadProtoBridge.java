package com.mysms.comms.grpc;

import com.mysms.comms.api.model.FeedbackThreadUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateFeedbackThread over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.FeedbackThreadUpdate} message
 * as REST's {@link FeedbackThreadUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class FeedbackThreadProtoBridge {

    private FeedbackThreadProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static FeedbackThreadUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.FeedbackThreadUpdate request) {
        FeedbackThreadUpdate changes = new FeedbackThreadUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setGuardianId(bridge.uuid(request.hasGuardianId(), request.getGuardianId(), "guardian_id"));
        changes.setStudentId(bridge.uuid(request.hasStudentId(), request.getStudentId(), "student_id"));
        changes.setAssignedToUserId(bridge.uuid(
                request.hasAssignedToUserId(), request.getAssignedToUserId(), "assigned_to_user_id"));
        return bridge.patch(changes);
    }
}
