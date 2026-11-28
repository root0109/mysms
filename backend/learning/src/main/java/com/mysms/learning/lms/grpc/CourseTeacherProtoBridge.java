package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.CourseTeacherUpdate;

/**
 * UpdateCourseTeacher over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.CourseTeacherUpdate} message as REST's
 * {@link CourseTeacherUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class CourseTeacherProtoBridge {

    private CourseTeacherProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CourseTeacherUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.CourseTeacherUpdate request) {
        CourseTeacherUpdate changes = new CourseTeacherUpdate();
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
