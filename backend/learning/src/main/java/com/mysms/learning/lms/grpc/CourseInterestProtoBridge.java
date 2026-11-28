package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.CourseInterestUpdate;

/**
 * UpdateCourseInterest over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.CourseInterestUpdate} message as REST's
 * {@link CourseInterestUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class CourseInterestProtoBridge {

    private CourseInterestProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CourseInterestUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.CourseInterestUpdate request) {
        CourseInterestUpdate changes = new CourseInterestUpdate();
        changes.setEmailSent(ProtoBridge.flag(request.hasEmailSent(), request.getEmailSent()));
        return bridge.patch(changes);
    }
}
