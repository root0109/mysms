package com.mysms.hrms.payroll.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.hrms.payroll.api.model.StaffSalaryUpdate;

/**
 * UpdateStaffSalary over gRPC (JAVA-CODING-RULES §7.2): the {@code payroll.v1.StaffSalaryUpdate} message as REST's
 * {@link StaffSalaryUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class StaffSalaryProtoBridge {

    private StaffSalaryProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static StaffSalaryUpdate changes(ProtoBridge bridge, com.mysms.payroll.v1.StaffSalaryUpdate request) {
        StaffSalaryUpdate changes = new StaffSalaryUpdate();
        changes.setStaffId(bridge.uuid(request.hasStaffId(), request.getStaffId(), "staff_id"));
        changes.setSalaryTemplateId(bridge.uuid(
                request.hasSalaryTemplateId(), request.getSalaryTemplateId(), "salary_template_id"));
        changes.setGrossSalary(ProtoBridge.text(request.hasGrossSalary(), request.getGrossSalary()));
        changes.setEffectiveDate(bridge.date(request.hasEffectiveDate(), request.getEffectiveDate(), "effective_date"));
        changes.setRemarks(ProtoBridge.text(request.hasRemarks(), request.getRemarks()));
        return bridge.patch(changes);
    }
}
