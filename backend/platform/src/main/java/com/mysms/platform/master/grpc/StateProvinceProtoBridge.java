package com.mysms.platform.master.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.platform.master.api.model.StateProvinceUpdate;

/**
 * UpdateStateProvince over gRPC (JAVA-CODING-RULES §7.2): the {@code master.v1.StateProvinceUpdate} message
 * as REST's {@link StateProvinceUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class StateProvinceProtoBridge {

    private StateProvinceProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static StateProvinceUpdate changes(ProtoBridge bridge, com.mysms.master.v1.StateProvinceUpdate request) {
        StateProvinceUpdate changes = new StateProvinceUpdate();
        changes.setCountryId(bridge.uuid(request.hasCountryId(), request.getCountryId(), "country_id"));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
