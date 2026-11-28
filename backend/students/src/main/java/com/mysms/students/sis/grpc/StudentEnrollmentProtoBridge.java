package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.sis.api.model.StudentEnrollmentUpdate;

/**
 * UpdateStudentEnrollment over gRPC (JAVA-CODING-RULES §7.2): the {@code sis.v1.StudentEnrollmentUpdate} message as
 * REST's {@link StudentEnrollmentUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class StudentEnrollmentProtoBridge {

    private StudentEnrollmentProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static StudentEnrollmentUpdate changes(ProtoBridge bridge, com.mysms.sis.v1.StudentEnrollmentUpdate request) {
        StudentEnrollmentUpdate changes = new StudentEnrollmentUpdate();
        changes.setStudentId(bridge.uuid(request.hasStudentId(), request.getStudentId(), "student_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setClassSectionId(bridge.uuid(
                request.hasClassSectionId(), request.getClassSectionId(), "class_section_id"));
        changes.setRollNumber(ProtoBridge.text(request.hasRollNumber(), request.getRollNumber()));
        changes.setIdCardNumber(ProtoBridge.text(request.hasIdCardNumber(), request.getIdCardNumber()));
        changes.setBoardRegistrationNumber(ProtoBridge.text(
                request.hasBoardRegistrationNumber(), request.getBoardRegistrationNumber()));
        changes.setEnrollmentStatus(bridge.choice(
                request.hasEnrollmentStatus(), request.getEnrollmentStatus(),
                StudentEnrollmentUpdate.EnrollmentStatusEnum::fromValue));
        changes.setResultStatus(bridge.choice(
                request.hasResultStatus(), request.getResultStatus(),
                StudentEnrollmentUpdate.ResultStatusEnum::fromValue));
        changes.setEnrolledOn(bridge.date(request.hasEnrolledOn(), request.getEnrolledOn(), "enrolled_on"));
        changes.setLeftOn(bridge.date(request.hasLeftOn(), request.getLeftOn(), "left_on"));
        return bridge.patch(changes);
    }
}
