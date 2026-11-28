package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.CourseReviewUpdate;

/**
 * UpdateCourseReview over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.CourseReviewUpdate} message as REST's {@link
 * CourseReviewUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class CourseReviewProtoBridge {

    private CourseReviewProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CourseReviewUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.CourseReviewUpdate request) {
        CourseReviewUpdate changes = new CourseReviewUpdate();
        changes.setRating(ProtoBridge.text(request.hasRating(), request.getRating()));
        changes.setReview(ProtoBridge.text(request.hasReview(), request.getReview()));
        return bridge.patch(changes);
    }
}
