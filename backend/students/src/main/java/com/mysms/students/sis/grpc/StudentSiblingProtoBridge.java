package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.sis.api.model.StudentSiblingUpdate;

/**
 * UpdateStudentSibling over gRPC (JAVA-CODING-RULES §7.2): the {@code sis.v1.StudentSiblingUpdate} message as REST's
 * {@link StudentSiblingUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class StudentSiblingProtoBridge {

    private StudentSiblingProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static StudentSiblingUpdate changes(ProtoBridge bridge, com.mysms.sis.v1.StudentSiblingUpdate request) {
        StudentSiblingUpdate changes = new StudentSiblingUpdate();
        changes.setSiblingStudentId(bridge.uuid(
                request.hasSiblingStudentId(), request.getSiblingStudentId(), "sibling_student_id"));
        changes.setSiblingName(ProtoBridge.text(request.hasSiblingName(), request.getSiblingName()));
        changes.setDetails(ProtoBridge.text(request.hasDetails(), request.getDetails()));
        return bridge.patch(changes);
    }
}
