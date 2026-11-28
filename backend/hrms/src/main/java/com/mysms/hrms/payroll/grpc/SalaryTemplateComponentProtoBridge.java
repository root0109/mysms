package com.mysms.hrms.payroll.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.hrms.payroll.api.model.SalaryTemplateComponentUpdate;

/**
 * UpdateSalaryTemplateComponent over gRPC (JAVA-CODING-RULES §7.2): the {@code
 * payroll.v1.SalaryTemplateComponentUpdate} message as REST's {@link SalaryTemplateComponentUpdate} body, one explicit
 * line per field: unset keeps the value, null clears it, a value sets it.
 */
final class SalaryTemplateComponentProtoBridge {

    private SalaryTemplateComponentProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static SalaryTemplateComponentUpdate changes(
            ProtoBridge bridge, com.mysms.payroll.v1.SalaryTemplateComponentUpdate request) {
        SalaryTemplateComponentUpdate changes = new SalaryTemplateComponentUpdate();
        changes.setPayComponentId(bridge.uuid(
                request.hasPayComponentId(), request.getPayComponentId(), "pay_component_id"));
        changes.setPayCategoryId(bridge.uuid(
                request.hasPayCategoryId(), request.getPayCategoryId(), "pay_category_id"));
        changes.setValue(ProtoBridge.text(request.hasValue(), request.getValue()));
        return bridge.patch(changes);
    }
}
