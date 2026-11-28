package com.mysms.campus.facility.grpc;

import com.mysms.campus.facility.api.model.RoomAssetUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateRoomAsset over gRPC (JAVA-CODING-RULES §7.2): the {@code facility.v1.RoomAssetUpdate} message as REST's {@link
 * RoomAssetUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class RoomAssetProtoBridge {

    private RoomAssetProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static RoomAssetUpdate changes(ProtoBridge bridge, com.mysms.facility.v1.RoomAssetUpdate request) {
        RoomAssetUpdate changes = new RoomAssetUpdate();
        changes.setAssetItemId(bridge.uuid(request.hasAssetItemId(), request.getAssetItemId(), "asset_item_id"));
        changes.setTagCode(ProtoBridge.text(request.hasTagCode(), request.getTagCode()));
        changes.setQuantity(ProtoBridge.text(request.hasQuantity(), request.getQuantity()));
        return bridge.patch(changes);
    }
}
