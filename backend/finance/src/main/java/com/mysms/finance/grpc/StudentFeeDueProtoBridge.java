package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.finance.api.model.StudentFeeDueUpdate;

/**
 * UpdateStudentFeeDue over gRPC (JAVA-CODING-RULES §7.2): the {@code finance.v1.StudentFeeDueUpdate} message
 * as REST's {@link StudentFeeDueUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class StudentFeeDueProtoBridge {

    private StudentFeeDueProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static StudentFeeDueUpdate changes(ProtoBridge bridge, com.mysms.finance.v1.StudentFeeDueUpdate request) {
        StudentFeeDueUpdate changes = new StudentFeeDueUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setDueDate(bridge.date(request.hasDueDate(), request.getDueDate(), "due_date"));
        return bridge.patch(changes);
    }
}
