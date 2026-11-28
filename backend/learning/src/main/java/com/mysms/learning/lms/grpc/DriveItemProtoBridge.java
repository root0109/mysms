package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.DriveItemUpdate;

/**
 * UpdateDriveItem over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.DriveItemUpdate} message as REST's {@link
 * DriveItemUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class DriveItemProtoBridge {

    private DriveItemProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static DriveItemUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.DriveItemUpdate request) {
        DriveItemUpdate changes = new DriveItemUpdate();
        changes.setFileId(bridge.uuid(request.hasFileId(), request.getFileId(), "file_id"));
        changes.setItemType(ProtoBridge.text(request.hasItemType(), request.getItemType()));
        changes.setIcon(ProtoBridge.text(request.hasIcon(), request.getIcon()));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setKeywords(ProtoBridge.text(request.hasKeywords(), request.getKeywords()));
        changes.setSizeBytes(ProtoBridge.int64(request.hasSizeBytes(), request.getSizeBytes()));
        changes.setLinkUrl(ProtoBridge.text(request.hasLinkUrl(), request.getLinkUrl()));
        changes.setUploadedAt(ProtoBridge.time(request.hasUploadedAt(), request.getUploadedAt()));
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        changes.setIsShared(ProtoBridge.flag(request.hasIsShared(), request.getIsShared()));
        return bridge.patch(changes);
    }
}
