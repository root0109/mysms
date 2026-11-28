package com.mysms.campus.facility.grpc;

import com.mysms.campus.facility.api.model.FacilityUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateFacility over gRPC (JAVA-CODING-RULES §7.2): the {@code facility.v1.FacilityUpdate} message as REST's {@link
 * FacilityUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class FacilityProtoBridge {

    private FacilityProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static FacilityUpdate changes(ProtoBridge bridge, com.mysms.facility.v1.FacilityUpdate request) {
        FacilityUpdate changes = new FacilityUpdate();
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
