package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.platform.api.model.ThemeUpdate;

/**
 * UpdateTheme over gRPC (JAVA-CODING-RULES §7.2): the {@code platform.v1.ThemeUpdate} message
 * as REST's {@link ThemeUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class ThemeProtoBridge {

    private ThemeProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ThemeUpdate changes(ProtoBridge bridge, com.mysms.platform.v1.ThemeUpdate request) {
        ThemeUpdate changes = new ThemeUpdate();
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setThumbnailFileId(bridge.uuid(
                request.hasThumbnailFileId(), request.getThumbnailFileId(), "thumbnail_file_id"));
        changes.setThumbnailAltFileId(bridge.uuid(
                request.hasThumbnailAltFileId(), request.getThumbnailAltFileId(), "thumbnail_alt_file_id"));
        changes.setLogoFileId(bridge.uuid(request.hasLogoFileId(), request.getLogoFileId(), "logo_file_id"));
        changes.setIsDefault(ProtoBridge.flag(request.hasIsDefault(), request.getIsDefault()));
        changes.setTokens(bridge.json(request.hasTokens(), request.getTokens()));
        return bridge.patch(changes);
    }
}
