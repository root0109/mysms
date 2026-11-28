package com.mysms.hrms.payroll.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.hrms.payroll.api.model.PaymentTypeUpdate;

/**
 * UpdatePaymentType over gRPC (JAVA-CODING-RULES §7.2): the {@code payroll.v1.PaymentTypeUpdate} message as REST's
 * {@link PaymentTypeUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class PaymentTypeProtoBridge {

    private PaymentTypeProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PaymentTypeUpdate changes(ProtoBridge bridge, com.mysms.payroll.v1.PaymentTypeUpdate request) {
        PaymentTypeUpdate changes = new PaymentTypeUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        return bridge.patch(changes);
    }
}
