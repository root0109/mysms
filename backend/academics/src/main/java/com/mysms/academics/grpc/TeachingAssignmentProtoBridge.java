package com.mysms.academics.grpc;

import com.mysms.academics.api.model.TeachingAssignmentUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateTeachingAssignment over gRPC (JAVA-CODING-RULES §7.2): the {@code academics.v1.TeachingAssignmentUpdate}
 * message as REST's {@link TeachingAssignmentUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class TeachingAssignmentProtoBridge {

    private TeachingAssignmentProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static TeachingAssignmentUpdate changes(
            ProtoBridge bridge, com.mysms.academics.v1.TeachingAssignmentUpdate request) {
        TeachingAssignmentUpdate changes = new TeachingAssignmentUpdate();
        changes.setClassSubjectId(bridge.uuid(
                request.hasClassSubjectId(), request.getClassSubjectId(), "class_subject_id"));
        changes.setStaffId(bridge.uuid(request.hasStaffId(), request.getStaffId(), "staff_id"));
        changes.setPeriodsPerWeek(ProtoBridge.number(request.hasPeriodsPerWeek(), request.getPeriodsPerWeek()));
        changes.setTeacherRole(ProtoBridge.text(request.hasTeacherRole(), request.getTeacherRole()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
