package com.mysms.academics.grpc;

import com.mysms.academics.api.model.TeacherSubjectUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateTeacherSubject over gRPC (JAVA-CODING-RULES §7.2): the {@code academics.v1.TeacherSubjectUpdate} message as
 * REST's {@link TeacherSubjectUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value
 * sets it.
 */
final class TeacherSubjectProtoBridge {

    private TeacherSubjectProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static TeacherSubjectUpdate changes(ProtoBridge bridge, com.mysms.academics.v1.TeacherSubjectUpdate request) {
        TeacherSubjectUpdate changes = new TeacherSubjectUpdate();
        changes.setSubjectId(bridge.uuid(request.hasSubjectId(), request.getSubjectId(), "subject_id"));
        return bridge.patch(changes);
    }
}
