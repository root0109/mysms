package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.RelatedCourseUpdate;

/**
 * UpdateRelatedCourse over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.RelatedCourseUpdate} message as REST's
 * {@link RelatedCourseUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class RelatedCourseProtoBridge {

    private RelatedCourseProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static RelatedCourseUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.RelatedCourseUpdate request) {
        RelatedCourseUpdate changes = new RelatedCourseUpdate();
        changes.setRelatedCourseId(bridge.uuid(
                request.hasRelatedCourseId(), request.getRelatedCourseId(), "related_course_id"));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setAuthorProfileId(bridge.uuid(
                request.hasAuthorProfileId(), request.getAuthorProfileId(), "author_profile_id"));
        return bridge.patch(changes);
    }
}
