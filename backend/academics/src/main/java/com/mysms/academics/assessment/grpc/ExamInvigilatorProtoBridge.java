package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.ExamInvigilatorUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateExamInvigilator over gRPC (JAVA-CODING-RULES §7.2): the {@code assessment.v1.ExamInvigilatorUpdate} message as
 * REST's {@link ExamInvigilatorUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class ExamInvigilatorProtoBridge {

    private ExamInvigilatorProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ExamInvigilatorUpdate changes(ProtoBridge bridge, com.mysms.assessment.v1.ExamInvigilatorUpdate request) {
        ExamInvigilatorUpdate changes = new ExamInvigilatorUpdate();
        changes.setStaffId(bridge.uuid(request.hasStaffId(), request.getStaffId(), "staff_id"));
        return bridge.patch(changes);
    }
}
