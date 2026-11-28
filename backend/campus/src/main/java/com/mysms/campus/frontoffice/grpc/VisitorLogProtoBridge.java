package com.mysms.campus.frontoffice.grpc;

import com.mysms.campus.frontoffice.api.model.VisitorLogUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateVisitorLog over gRPC (JAVA-CODING-RULES §7.2): the {@code frontoffice.v1.VisitorLogUpdate} message as REST's
 * {@link VisitorLogUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class VisitorLogProtoBridge {

    private VisitorLogProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static VisitorLogUpdate changes(ProtoBridge bridge, com.mysms.frontoffice.v1.VisitorLogUpdate request) {
        VisitorLogUpdate changes = new VisitorLogUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setVisitorName(ProtoBridge.text(request.hasVisitorName(), request.getVisitorName()));
        changes.setVisitorType(ProtoBridge.text(request.hasVisitorType(), request.getVisitorType()));
        changes.setEmail(ProtoBridge.text(request.hasEmail(), request.getEmail()));
        changes.setPhone(ProtoBridge.text(request.hasPhone(), request.getPhone()));
        changes.setCompanyName(ProtoBridge.text(request.hasCompanyName(), request.getCompanyName()));
        changes.setAddress(ProtoBridge.text(request.hasAddress(), request.getAddress()));
        changes.setStudentId(bridge.uuid(request.hasStudentId(), request.getStudentId(), "student_id"));
        changes.setRelationToStudent(ProtoBridge.text(request.hasRelationToStudent(), request.getRelationToStudent()));
        changes.setRelatedPersonName(ProtoBridge.text(request.hasRelatedPersonName(), request.getRelatedPersonName()));
        changes.setVisitorCount(ProtoBridge.number(request.hasVisitorCount(), request.getVisitorCount()));
        changes.setPurpose(ProtoBridge.text(request.hasPurpose(), request.getPurpose()));
        changes.setHostStaffId(bridge.uuid(request.hasHostStaffId(), request.getHostStaffId(), "host_staff_id"));
        changes.setVisitDate(bridge.date(request.hasVisitDate(), request.getVisitDate(), "visit_date"));
        changes.setEntryTime(ProtoBridge.text(request.hasEntryTime(), request.getEntryTime()));
        changes.setExitTime(ProtoBridge.text(request.hasExitTime(), request.getExitTime()));
        changes.setRemarks(ProtoBridge.text(request.hasRemarks(), request.getRemarks()));
        return bridge.patch(changes);
    }
}
