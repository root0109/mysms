package com.mysms.academics.attendance.grpc;

import com.mysms.academics.attendance.api.model.AttendanceTypeUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateAttendanceType over gRPC (JAVA-CODING-RULES §7.2): the {@code attendance.v1.AttendanceTypeUpdate} message as
 * REST's {@link AttendanceTypeUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value
 * sets it.
 */
final class AttendanceTypeProtoBridge {

    private AttendanceTypeProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static AttendanceTypeUpdate changes(ProtoBridge bridge, com.mysms.attendance.v1.AttendanceTypeUpdate request) {
        AttendanceTypeUpdate changes = new AttendanceTypeUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setCountsPresent(ProtoBridge.flag(request.hasCountsPresent(), request.getCountsPresent()));
        changes.setCountsAbsent(ProtoBridge.flag(request.hasCountsAbsent(), request.getCountsAbsent()));
        changes.setCountsExcused(ProtoBridge.flag(request.hasCountsExcused(), request.getCountsExcused()));
        changes.setCountsLate(ProtoBridge.flag(request.hasCountsLate(), request.getCountsLate()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
