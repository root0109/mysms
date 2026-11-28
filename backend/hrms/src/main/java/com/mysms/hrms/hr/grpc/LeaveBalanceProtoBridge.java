package com.mysms.hrms.hr.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.hrms.hr.api.model.LeaveBalanceUpdate;

/**
 * UpdateLeaveBalance over gRPC (JAVA-CODING-RULES §7.2): the {@code hr.v1.LeaveBalanceUpdate} message as REST's {@link
 * LeaveBalanceUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class LeaveBalanceProtoBridge {

    private LeaveBalanceProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static LeaveBalanceUpdate changes(ProtoBridge bridge, com.mysms.hr.v1.LeaveBalanceUpdate request) {
        LeaveBalanceUpdate changes = new LeaveBalanceUpdate();
        changes.setStaffId(bridge.uuid(request.hasStaffId(), request.getStaffId(), "staff_id"));
        changes.setLeaveTypeId(bridge.uuid(request.hasLeaveTypeId(), request.getLeaveTypeId(), "leave_type_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setEntitledDays(ProtoBridge.text(request.hasEntitledDays(), request.getEntitledDays()));
        changes.setCarriedForwardDays(ProtoBridge.text(
                request.hasCarriedForwardDays(), request.getCarriedForwardDays()));
        return bridge.patch(changes);
    }
}
