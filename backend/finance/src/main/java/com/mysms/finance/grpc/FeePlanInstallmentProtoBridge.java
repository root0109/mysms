package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.finance.api.model.FeePlanInstallmentUpdate;

/**
 * UpdateFeePlanInstallment over gRPC (JAVA-CODING-RULES §7.2): the {@code finance.v1.FeePlanInstallmentUpdate} message
 * as REST's {@link FeePlanInstallmentUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class FeePlanInstallmentProtoBridge {

    private FeePlanInstallmentProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static FeePlanInstallmentUpdate changes(ProtoBridge bridge, com.mysms.finance.v1.FeePlanInstallmentUpdate request) {
        FeePlanInstallmentUpdate changes = new FeePlanInstallmentUpdate();
        changes.setSequence(ProtoBridge.number(request.hasSequence(), request.getSequence()));
        changes.setDueDays(ProtoBridge.number(request.hasDueDays(), request.getDueDays()));
        changes.setDueDate(bridge.date(request.hasDueDate(), request.getDueDate(), "due_date"));
        changes.setSharePercent(ProtoBridge.text(request.hasSharePercent(), request.getSharePercent()));
        return bridge.patch(changes);
    }
}
