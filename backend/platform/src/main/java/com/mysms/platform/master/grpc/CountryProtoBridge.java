package com.mysms.platform.master.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.platform.master.api.model.CountryUpdate;

/**
 * UpdateCountry over gRPC (JAVA-CODING-RULES §7.2): the {@code master.v1.CountryUpdate} message
 * as REST's {@link CountryUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class CountryProtoBridge {

    private CountryProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CountryUpdate changes(ProtoBridge bridge, com.mysms.master.v1.CountryUpdate request) {
        CountryUpdate changes = new CountryUpdate();
        changes.setIso2(ProtoBridge.text(request.hasIso2(), request.getIso2()));
        changes.setIso3(ProtoBridge.text(request.hasIso3(), request.getIso3()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setShortName(ProtoBridge.text(request.hasShortName(), request.getShortName()));
        changes.setPhonePrefix(ProtoBridge.text(request.hasPhonePrefix(), request.getPhonePrefix()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
