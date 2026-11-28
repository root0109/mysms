package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.EnrollmentRequestUpdate;

/**
 * UpdateEnrollmentRequest over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.EnrollmentRequestUpdate} message as
 * REST's {@link EnrollmentRequestUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class EnrollmentRequestProtoBridge {

    private EnrollmentRequestProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static EnrollmentRequestUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.EnrollmentRequestUpdate request) {
        EnrollmentRequestUpdate changes = new EnrollmentRequestUpdate();
        changes.setCohortId(bridge.uuid(request.hasCohortId(), request.getCohortId(), "cohort_id"));
        changes.setRequestedAt(ProtoBridge.time(request.hasRequestedAt(), request.getRequestedAt()));
        return bridge.patch(changes);
    }
}
