package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.platform.api.model.TagUpdate;

/**
 * UpdateTag over gRPC (JAVA-CODING-RULES §7.2): the {@code platform.v1.TagUpdate} message
 * as REST's {@link TagUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class TagProtoBridge {

    private TagProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static TagUpdate changes(ProtoBridge bridge, com.mysms.platform.v1.TagUpdate request) {
        TagUpdate changes = new TagUpdate();
        changes.setTagType(ProtoBridge.text(request.hasTagType(), request.getTagType()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setSlug(ProtoBridge.text(request.hasSlug(), request.getSlug()));
        changes.setTranslations(bridge.json(request.hasTranslations(), request.getTranslations()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        return bridge.patch(changes);
    }
}
