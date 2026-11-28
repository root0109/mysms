package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.CourseMentorUpdate;

/**
 * UpdateCourseMentor over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.CourseMentorUpdate} message as REST's {@link
 * CourseMentorUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class CourseMentorProtoBridge {

    private CourseMentorProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CourseMentorUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.CourseMentorUpdate request) {
        CourseMentorUpdate changes = new CourseMentorUpdate();
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        return bridge.patch(changes);
    }
}
