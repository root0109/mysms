package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.sis.api.model.SubjectRegistrationUpdate;

/**
 * UpdateSubjectRegistration over gRPC (JAVA-CODING-RULES §7.2): the {@code sis.v1.SubjectRegistrationUpdate} message as
 * REST's {@link SubjectRegistrationUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class SubjectRegistrationProtoBridge {

    private SubjectRegistrationProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static SubjectRegistrationUpdate changes(ProtoBridge bridge, com.mysms.sis.v1.SubjectRegistrationUpdate request) {
        SubjectRegistrationUpdate changes = new SubjectRegistrationUpdate();
        changes.setStudentId(bridge.uuid(request.hasStudentId(), request.getStudentId(), "student_id"));
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        changes.setBatchId(bridge.uuid(request.hasBatchId(), request.getBatchId(), "batch_id"));
        changes.setReferenceNo(ProtoBridge.text(request.hasReferenceNo(), request.getReferenceNo()));
        changes.setMinUnitLoad(ProtoBridge.text(request.hasMinUnitLoad(), request.getMinUnitLoad()));
        changes.setMaxUnitLoad(ProtoBridge.text(request.hasMaxUnitLoad(), request.getMaxUnitLoad()));
        changes.setReadByStudent(ProtoBridge.flag(request.hasReadByStudent(), request.getReadByStudent()));
        changes.setReadByGuardian(ProtoBridge.flag(request.hasReadByGuardian(), request.getReadByGuardian()));
        return bridge.patch(changes);
    }
}
