package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.CoursePreviewUpdate;

/**
 * UpdateCoursePreview over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.CoursePreviewUpdate} message as REST's
 * {@link CoursePreviewUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class CoursePreviewProtoBridge {

    private CoursePreviewProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CoursePreviewUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.CoursePreviewUpdate request) {
        CoursePreviewUpdate changes = new CoursePreviewUpdate();
        changes.setPreviewKind(bridge.choice(
                request.hasPreviewKind(), request.getPreviewKind(), CoursePreviewUpdate.PreviewKindEnum::fromValue));
        changes.setFileId(bridge.uuid(request.hasFileId(), request.getFileId(), "file_id"));
        changes.setIcon(ProtoBridge.text(request.hasIcon(), request.getIcon()));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
