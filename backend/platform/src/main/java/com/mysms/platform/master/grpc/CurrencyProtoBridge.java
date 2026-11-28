package com.mysms.platform.master.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.platform.master.api.model.CurrencyUpdate;

/**
 * UpdateCurrency over gRPC (JAVA-CODING-RULES §7.2): the {@code master.v1.CurrencyUpdate} message
 * as REST's {@link CurrencyUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class CurrencyProtoBridge {

    private CurrencyProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CurrencyUpdate changes(ProtoBridge bridge, com.mysms.master.v1.CurrencyUpdate request) {
        CurrencyUpdate changes = new CurrencyUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setSymbol(ProtoBridge.text(request.hasSymbol(), request.getSymbol()));
        changes.setMinorUnits(ProtoBridge.number(request.hasMinorUnits(), request.getMinorUnits()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
