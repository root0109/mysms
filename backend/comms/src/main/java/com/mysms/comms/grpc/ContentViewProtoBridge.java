package com.mysms.comms.grpc;

import com.mysms.comms.api.model.ContentViewUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateContentView over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.ContentViewUpdate} message
 * as REST's {@link ContentViewUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class ContentViewProtoBridge {

    private ContentViewProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ContentViewUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.ContentViewUpdate request) {
        ContentViewUpdate changes = new ContentViewUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setStudentId(bridge.uuid(request.hasStudentId(), request.getStudentId(), "student_id"));
        changes.setViewerUserId(bridge.uuid(request.hasViewerUserId(), request.getViewerUserId(), "viewer_user_id"));
        changes.setContentType(bridge.choice(
                request.hasContentType(), request.getContentType(), ContentViewUpdate.ContentTypeEnum::fromValue));
        changes.setContentId(bridge.uuid(request.hasContentId(), request.getContentId(), "content_id"));
        changes.setViewedAt(ProtoBridge.time(request.hasViewedAt(), request.getViewedAt()));
        return bridge.patch(changes);
    }
}
