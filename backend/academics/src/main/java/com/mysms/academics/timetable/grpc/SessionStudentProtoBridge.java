package com.mysms.academics.timetable.grpc;

import com.mysms.academics.timetable.api.model.SessionStudentUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateSessionStudent over gRPC (JAVA-CODING-RULES §7.2): the {@code timetable.v1.SessionStudentUpdate} message as
 * REST's {@link SessionStudentUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value
 * sets it.
 */
final class SessionStudentProtoBridge {

    private SessionStudentProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static SessionStudentUpdate changes(ProtoBridge bridge, com.mysms.timetable.v1.SessionStudentUpdate request) {
        SessionStudentUpdate changes = new SessionStudentUpdate();
        changes.setStudentId(bridge.uuid(request.hasStudentId(), request.getStudentId(), "student_id"));
        return bridge.patch(changes);
    }
}
