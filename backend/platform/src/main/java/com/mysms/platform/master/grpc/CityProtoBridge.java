package com.mysms.platform.master.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.platform.master.api.model.CityUpdate;

/**
 * UpdateCity over gRPC (JAVA-CODING-RULES §7.2): the {@code master.v1.CityUpdate} message
 * as REST's {@link CityUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class CityProtoBridge {

    private CityProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CityUpdate changes(ProtoBridge bridge, com.mysms.master.v1.CityUpdate request) {
        CityUpdate changes = new CityUpdate();
        changes.setCountryId(bridge.uuid(request.hasCountryId(), request.getCountryId(), "country_id"));
        changes.setStateProvinceId(bridge.uuid(
                request.hasStateProvinceId(), request.getStateProvinceId(), "state_province_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
