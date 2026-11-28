package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.sis.api.model.ProgramEnrollmentUpdate;

/**
 * UpdateProgramEnrollment over gRPC (JAVA-CODING-RULES §7.2): the {@code sis.v1.ProgramEnrollmentUpdate} message as
 * REST's {@link ProgramEnrollmentUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class ProgramEnrollmentProtoBridge {

    private ProgramEnrollmentProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ProgramEnrollmentUpdate changes(ProtoBridge bridge, com.mysms.sis.v1.ProgramEnrollmentUpdate request) {
        ProgramEnrollmentUpdate changes = new ProgramEnrollmentUpdate();
        changes.setStudentId(bridge.uuid(request.hasStudentId(), request.getStudentId(), "student_id"));
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        changes.setBatchId(bridge.uuid(request.hasBatchId(), request.getBatchId(), "batch_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setAcademicTermId(bridge.uuid(
                request.hasAcademicTermId(), request.getAcademicTermId(), "academic_term_id"));
        changes.setRollNumber(ProtoBridge.text(request.hasRollNumber(), request.getRollNumber()));
        changes.setStatus(bridge.choice(
                request.hasStatus(), request.getStatus(), ProgramEnrollmentUpdate.StatusEnum::fromValue));
        return bridge.patch(changes);
    }
}
