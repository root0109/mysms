package com.mysms.hrms.payroll.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.hrms.payroll.api.model.StaffSalaryComponentUpdate;

/**
 * UpdateStaffSalaryComponent over gRPC (JAVA-CODING-RULES §7.2): the {@code payroll.v1.StaffSalaryComponentUpdate}
 * message as REST's {@link StaffSalaryComponentUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class StaffSalaryComponentProtoBridge {

    private StaffSalaryComponentProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static StaffSalaryComponentUpdate changes(
            ProtoBridge bridge, com.mysms.payroll.v1.StaffSalaryComponentUpdate request) {
        StaffSalaryComponentUpdate changes = new StaffSalaryComponentUpdate();
        changes.setSalaryTemplateComponentId(bridge.uuid(
                request.hasSalaryTemplateComponentId(), request.getSalaryTemplateComponentId(),
                "salary_template_component_id"));
        changes.setAmount(ProtoBridge.text(request.hasAmount(), request.getAmount()));
        return bridge.patch(changes);
    }
}
