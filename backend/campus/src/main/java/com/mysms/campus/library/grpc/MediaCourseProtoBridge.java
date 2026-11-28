package com.mysms.campus.library.grpc;

import com.mysms.campus.library.api.model.MediaCourseUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateMediaCourse over gRPC (JAVA-CODING-RULES §7.2): the {@code library.v1.MediaCourseUpdate} message as REST's
 * {@link MediaCourseUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class MediaCourseProtoBridge {

    private MediaCourseProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static MediaCourseUpdate changes(ProtoBridge bridge, com.mysms.library.v1.MediaCourseUpdate request) {
        MediaCourseUpdate changes = new MediaCourseUpdate();
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        return bridge.patch(changes);
    }
}
