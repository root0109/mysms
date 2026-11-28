package com.mysms.hrms.payroll.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.hrms.payroll.api.model.PayCategoryUpdate;

/**
 * UpdatePayCategory over gRPC (JAVA-CODING-RULES §7.2): the {@code payroll.v1.PayCategoryUpdate} message as REST's
 * {@link PayCategoryUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class PayCategoryProtoBridge {

    private PayCategoryProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PayCategoryUpdate changes(ProtoBridge bridge, com.mysms.payroll.v1.PayCategoryUpdate request) {
        PayCategoryUpdate changes = new PayCategoryUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        return bridge.patch(changes);
    }
}
