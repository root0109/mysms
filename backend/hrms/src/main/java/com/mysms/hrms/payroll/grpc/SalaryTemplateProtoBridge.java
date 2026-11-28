package com.mysms.hrms.payroll.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.hrms.payroll.api.model.SalaryTemplateUpdate;

/**
 * UpdateSalaryTemplate over gRPC (JAVA-CODING-RULES §7.2): the {@code payroll.v1.SalaryTemplateUpdate} message as
 * REST's {@link SalaryTemplateUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value
 * sets it.
 */
final class SalaryTemplateProtoBridge {

    private SalaryTemplateProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static SalaryTemplateUpdate changes(ProtoBridge bridge, com.mysms.payroll.v1.SalaryTemplateUpdate request) {
        SalaryTemplateUpdate changes = new SalaryTemplateUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
