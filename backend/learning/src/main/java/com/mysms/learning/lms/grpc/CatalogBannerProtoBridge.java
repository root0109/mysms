package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.CatalogBannerUpdate;

/**
 * UpdateCatalogBanner over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.CatalogBannerUpdate} message as REST's
 * {@link CatalogBannerUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class CatalogBannerProtoBridge {

    private CatalogBannerProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CatalogBannerUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.CatalogBannerUpdate request) {
        CatalogBannerUpdate changes = new CatalogBannerUpdate();
        changes.setPlacement(bridge.choice(
                request.hasPlacement(), request.getPlacement(), CatalogBannerUpdate.PlacementEnum::fromValue));
        changes.setImageFileId(bridge.uuid(request.hasImageFileId(), request.getImageFileId(), "image_file_id"));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
