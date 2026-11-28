package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.finance.api.model.FeeHeadUpdate;

/**
 * UpdateFeeHead over gRPC (JAVA-CODING-RULES §7.2): the {@code finance.v1.FeeHeadUpdate} message
 * as REST's {@link FeeHeadUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class FeeHeadProtoBridge {

    private FeeHeadProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static FeeHeadUpdate changes(ProtoBridge bridge, com.mysms.finance.v1.FeeHeadUpdate request) {
        FeeHeadUpdate changes = new FeeHeadUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setDefaultAmount(ProtoBridge.text(request.hasDefaultAmount(), request.getDefaultAmount()));
        changes.setCurrencyId(bridge.uuid(request.hasCurrencyId(), request.getCurrencyId(), "currency_id"));
        changes.setLedgerAccountCode(ProtoBridge.text(request.hasLedgerAccountCode(), request.getLedgerAccountCode()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
