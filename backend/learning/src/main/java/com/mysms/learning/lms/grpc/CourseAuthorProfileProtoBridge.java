package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.CourseAuthorProfileUpdate;

/**
 * UpdateCourseAuthorProfile over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.CourseAuthorProfileUpdate} message as
 * REST's {@link CourseAuthorProfileUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class CourseAuthorProfileProtoBridge {

    private CourseAuthorProfileProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CourseAuthorProfileUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.CourseAuthorProfileUpdate request) {
        CourseAuthorProfileUpdate changes = new CourseAuthorProfileUpdate();
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setBio(ProtoBridge.text(request.hasBio(), request.getBio()));
        changes.setPhotoFileId(bridge.uuid(request.hasPhotoFileId(), request.getPhotoFileId(), "photo_file_id"));
        changes.setQualification(ProtoBridge.text(request.hasQualification(), request.getQualification()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
