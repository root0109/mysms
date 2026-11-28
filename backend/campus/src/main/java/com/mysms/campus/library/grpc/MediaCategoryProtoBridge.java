package com.mysms.campus.library.grpc;

import com.mysms.campus.library.api.model.MediaCategoryUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateMediaCategory over gRPC (JAVA-CODING-RULES §7.2): the {@code library.v1.MediaCategoryUpdate} message as REST's
 * {@link MediaCategoryUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class MediaCategoryProtoBridge {

    private MediaCategoryProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static MediaCategoryUpdate changes(ProtoBridge bridge, com.mysms.library.v1.MediaCategoryUpdate request) {
        MediaCategoryUpdate changes = new MediaCategoryUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        return bridge.patch(changes);
    }
}
