package com.mysms.hrms.hr.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.hrms.hr.api.model.LeaveApplicationUpdate;

/**
 * UpdateLeaveApplication over gRPC (JAVA-CODING-RULES §7.2): the {@code hr.v1.LeaveApplicationUpdate} message as REST's
 * {@link LeaveApplicationUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class LeaveApplicationProtoBridge {

    private LeaveApplicationProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static LeaveApplicationUpdate changes(ProtoBridge bridge, com.mysms.hr.v1.LeaveApplicationUpdate request) {
        LeaveApplicationUpdate changes = new LeaveApplicationUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setStaffId(bridge.uuid(request.hasStaffId(), request.getStaffId(), "staff_id"));
        changes.setLeaveTypeId(bridge.uuid(request.hasLeaveTypeId(), request.getLeaveTypeId(), "leave_type_id"));
        changes.setAbsenceReasonId(bridge.uuid(
                request.hasAbsenceReasonId(), request.getAbsenceReasonId(), "absence_reason_id"));
        changes.setClassSectionId(bridge.uuid(
                request.hasClassSectionId(), request.getClassSectionId(), "class_section_id"));
        changes.setFromAt(ProtoBridge.time(request.hasFromAt(), request.getFromAt()));
        changes.setToAt(ProtoBridge.time(request.hasToAt(), request.getToAt()));
        changes.setDayPart(bridge.choice(
                request.hasDayPart(), request.getDayPart(), LeaveApplicationUpdate.DayPartEnum::fromValue));
        changes.setRemarks(ProtoBridge.text(request.hasRemarks(), request.getRemarks()));
        return bridge.patch(changes);
    }
}
