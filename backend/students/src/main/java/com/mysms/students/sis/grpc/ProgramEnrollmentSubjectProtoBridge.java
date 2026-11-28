package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.sis.api.model.ProgramEnrollmentSubjectUpdate;

/**
 * UpdateProgramEnrollmentSubject over gRPC (JAVA-CODING-RULES §7.2): the {@code sis.v1.ProgramEnrollmentSubjectUpdate}
 * message as REST's {@link ProgramEnrollmentSubjectUpdate} body, one explicit line per field: unset keeps the value,
 * null clears it, a value sets it.
 */
final class ProgramEnrollmentSubjectProtoBridge {

    private ProgramEnrollmentSubjectProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ProgramEnrollmentSubjectUpdate changes(
            ProtoBridge bridge, com.mysms.sis.v1.ProgramEnrollmentSubjectUpdate request) {
        ProgramEnrollmentSubjectUpdate changes = new ProgramEnrollmentSubjectUpdate();
        changes.setSubjectId(bridge.uuid(request.hasSubjectId(), request.getSubjectId(), "subject_id"));
        return bridge.patch(changes);
    }
}
