package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.sis.api.model.StudentActivityUpdate;

/**
 * UpdateStudentActivity over gRPC (JAVA-CODING-RULES §7.2): the {@code sis.v1.StudentActivityUpdate} message as REST's
 * {@link StudentActivityUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class StudentActivityProtoBridge {

    private StudentActivityProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static StudentActivityUpdate changes(ProtoBridge bridge, com.mysms.sis.v1.StudentActivityUpdate request) {
        StudentActivityUpdate changes = new StudentActivityUpdate();
        changes.setStudentId(bridge.uuid(request.hasStudentId(), request.getStudentId(), "student_id"));
        changes.setStaffId(bridge.uuid(request.hasStaffId(), request.getStaffId(), "staff_id"));
        changes.setActivityTypeId(bridge.uuid(
                request.hasActivityTypeId(), request.getActivityTypeId(), "activity_type_id"));
        changes.setActivityDate(bridge.date(request.hasActivityDate(), request.getActivityDate(), "activity_date"));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        return bridge.patch(changes);
    }
}
