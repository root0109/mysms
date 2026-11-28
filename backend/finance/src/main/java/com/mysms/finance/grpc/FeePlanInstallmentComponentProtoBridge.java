package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.finance.api.model.FeePlanInstallmentComponentUpdate;

/**
 * UpdateFeePlanInstallmentComponent over gRPC (JAVA-CODING-RULES §7.2): the
 * {@code finance.v1.FeePlanInstallmentComponentUpdate} message as REST's {@link FeePlanInstallmentComponentUpdate} body,
 * one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class FeePlanInstallmentComponentProtoBridge {

    private FeePlanInstallmentComponentProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static FeePlanInstallmentComponentUpdate changes(ProtoBridge bridge,
            com.mysms.finance.v1.FeePlanInstallmentComponentUpdate request) {
        FeePlanInstallmentComponentUpdate changes = new FeePlanInstallmentComponentUpdate();
        changes.setInstallmentId(bridge.uuid(request.hasInstallmentId(), request.getInstallmentId(), "installment_id"));
        changes.setFeeHeadId(bridge.uuid(request.hasFeeHeadId(), request.getFeeHeadId(), "fee_head_id"));
        changes.setSequence(ProtoBridge.number(request.hasSequence(), request.getSequence()));
        changes.setSharePercent(ProtoBridge.text(request.hasSharePercent(), request.getSharePercent()));
        return bridge.patch(changes);
    }
}
