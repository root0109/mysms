package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.finance.api.model.AcquisitionSourceUpdate;

/**
 * UpdateAcquisitionSource over gRPC (JAVA-CODING-RULES §7.2): the {@code finance.v1.AcquisitionSourceUpdate} message
 * as REST's {@link AcquisitionSourceUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class AcquisitionSourceProtoBridge {

    private AcquisitionSourceProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static AcquisitionSourceUpdate changes(ProtoBridge bridge, com.mysms.finance.v1.AcquisitionSourceUpdate request) {
        AcquisitionSourceUpdate changes = new AcquisitionSourceUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        return bridge.patch(changes);
    }
}
