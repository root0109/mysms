package com.mysms.campus.facility.grpc;

import com.mysms.campus.facility.api.model.AssetItemUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateAssetItem over gRPC (JAVA-CODING-RULES §7.2): the {@code facility.v1.AssetItemUpdate} message as REST's {@link
 * AssetItemUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class AssetItemProtoBridge {

    private AssetItemProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static AssetItemUpdate changes(ProtoBridge bridge, com.mysms.facility.v1.AssetItemUpdate request) {
        AssetItemUpdate changes = new AssetItemUpdate();
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setUnitOfMeasure(ProtoBridge.text(request.hasUnitOfMeasure(), request.getUnitOfMeasure()));
        return bridge.patch(changes);
    }
}
