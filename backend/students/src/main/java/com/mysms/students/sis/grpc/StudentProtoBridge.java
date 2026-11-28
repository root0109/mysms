package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.sis.api.model.StudentUpdate;

/**
 * UpdateStudent over gRPC (JAVA-CODING-RULES §7.2): the {@code sis.v1.StudentUpdate} message as REST's {@link
 * StudentUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class StudentProtoBridge {

    private StudentProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static StudentUpdate changes(ProtoBridge bridge, com.mysms.sis.v1.StudentUpdate request) {
        StudentUpdate changes = new StudentUpdate();
        changes.setPersonId(bridge.uuid(request.hasPersonId(), request.getPersonId(), "person_id"));
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setAdmissionNumber(ProtoBridge.text(request.hasAdmissionNumber(), request.getAdmissionNumber()));
        changes.setAdmissionDate(bridge.date(request.hasAdmissionDate(), request.getAdmissionDate(), "admission_date"));
        changes.setJoiningDate(bridge.date(request.hasJoiningDate(), request.getJoiningDate(), "joining_date"));
        changes.setStudentCategoryId(bridge.uuid(
                request.hasStudentCategoryId(), request.getStudentCategoryId(), "student_category_id"));
        changes.setGovernmentStudentId(ProtoBridge.text(
                request.hasGovernmentStudentId(), request.getGovernmentStudentId()));
        changes.setBonafideCertificateNumber(ProtoBridge.text(
                request.hasBonafideCertificateNumber(), request.getBonafideCertificateNumber()));
        changes.setExitDate(bridge.date(request.hasExitDate(), request.getExitDate(), "exit_date"));
        changes.setExitReason(ProtoBridge.text(request.hasExitReason(), request.getExitReason()));
        return bridge.patch(changes);
    }
}
