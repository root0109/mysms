package com.mysms.identity.people.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.identity.people.api.model.PersonAddressUpdate;

/**
 * UpdatePersonAddress over gRPC (JAVA-CODING-RULES §7.2): the {@code people.v1.PersonAddressUpdate} message
 * as REST's {@link PersonAddressUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class PersonAddressProtoBridge {

    private PersonAddressProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PersonAddressUpdate changes(ProtoBridge bridge, com.mysms.people.v1.PersonAddressUpdate request) {
        PersonAddressUpdate changes = new PersonAddressUpdate();
        changes.setAddressType(bridge.choice(
                request.hasAddressType(), request.getAddressType(), PersonAddressUpdate.AddressTypeEnum::fromValue));
        changes.setLine1(ProtoBridge.text(request.hasLine1(), request.getLine1()));
        changes.setLine2(ProtoBridge.text(request.hasLine2(), request.getLine2()));
        changes.setCityId(bridge.uuid(request.hasCityId(), request.getCityId(), "city_id"));
        changes.setCityName(ProtoBridge.text(request.hasCityName(), request.getCityName()));
        changes.setStateProvinceId(bridge.uuid(
                request.hasStateProvinceId(), request.getStateProvinceId(), "state_province_id"));
        changes.setCountryId(bridge.uuid(request.hasCountryId(), request.getCountryId(), "country_id"));
        changes.setPostalCode(ProtoBridge.text(request.hasPostalCode(), request.getPostalCode()));
        changes.setIsPrimary(ProtoBridge.flag(request.hasIsPrimary(), request.getIsPrimary()));
        changes.setStateName(ProtoBridge.text(request.hasStateName(), request.getStateName()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
