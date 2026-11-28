package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.CohortTeacherUpdate;

/**
 * UpdateCohortTeacher over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.CohortTeacherUpdate} message as REST's
 * {@link CohortTeacherUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class CohortTeacherProtoBridge {

    private CohortTeacherProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CohortTeacherUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.CohortTeacherUpdate request) {
        CohortTeacherUpdate changes = new CohortTeacherUpdate();
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        changes.setRoleId(bridge.uuid(request.hasRoleId(), request.getRoleId(), "role_id"));
        changes.setRoleLabel(ProtoBridge.text(request.hasRoleLabel(), request.getRoleLabel()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setHasOfficeHours(ProtoBridge.flag(request.hasHasOfficeHours(), request.getHasOfficeHours()));
        changes.setRemovedAt(ProtoBridge.time(request.hasRemovedAt(), request.getRemovedAt()));
        return bridge.patch(changes);
    }
}
