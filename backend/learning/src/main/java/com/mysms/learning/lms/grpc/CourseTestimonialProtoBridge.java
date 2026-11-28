package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.CourseTestimonialUpdate;

/**
 * UpdateCourseTestimonial over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.CourseTestimonialUpdate} message as
 * REST's {@link CourseTestimonialUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class CourseTestimonialProtoBridge {

    private CourseTestimonialProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CourseTestimonialUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.CourseTestimonialUpdate request) {
        CourseTestimonialUpdate changes = new CourseTestimonialUpdate();
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setQuote(ProtoBridge.text(request.hasQuote(), request.getQuote()));
        changes.setPhotoFileId(bridge.uuid(request.hasPhotoFileId(), request.getPhotoFileId(), "photo_file_id"));
        changes.setCurrentPosition(ProtoBridge.text(request.hasCurrentPosition(), request.getCurrentPosition()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
