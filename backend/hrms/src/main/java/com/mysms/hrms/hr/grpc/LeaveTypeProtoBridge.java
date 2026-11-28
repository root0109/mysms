package com.mysms.hrms.hr.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.hrms.hr.api.model.LeaveTypeUpdate;

/**
 * UpdateLeaveType over gRPC (JAVA-CODING-RULES §7.2): the {@code hr.v1.LeaveTypeUpdate} message as REST's {@link
 * LeaveTypeUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class LeaveTypeProtoBridge {

    private LeaveTypeProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static LeaveTypeUpdate changes(ProtoBridge bridge, com.mysms.hr.v1.LeaveTypeUpdate request) {
        LeaveTypeUpdate changes = new LeaveTypeUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setMaxDaysPerYear(ProtoBridge.text(request.hasMaxDaysPerYear(), request.getMaxDaysPerYear()));
        changes.setIsPaid(ProtoBridge.flag(request.hasIsPaid(), request.getIsPaid()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
