package com.mysms.campus.facility.grpc;

import com.mysms.campus.facility.api.model.RoomFacilityUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateRoomFacility over gRPC (JAVA-CODING-RULES §7.2): the {@code facility.v1.RoomFacilityUpdate} message as REST's
 * {@link RoomFacilityUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class RoomFacilityProtoBridge {

    private RoomFacilityProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static RoomFacilityUpdate changes(ProtoBridge bridge, com.mysms.facility.v1.RoomFacilityUpdate request) {
        RoomFacilityUpdate changes = new RoomFacilityUpdate();
        changes.setFacilityId(bridge.uuid(request.hasFacilityId(), request.getFacilityId(), "facility_id"));
        changes.setQuantity(ProtoBridge.text(request.hasQuantity(), request.getQuantity()));
        return bridge.patch(changes);
    }
}
