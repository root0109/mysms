package com.mysms.hrms.timeattendance.grpc;

import com.mysms.hrms.timeattendance.api.model.StaffAttendanceUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateStaffAttendance over gRPC (JAVA-CODING-RULES §7.2): the {@code time_attendance.v1.StaffAttendanceUpdate} message
 * as REST's {@link StaffAttendanceUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class StaffAttendanceProtoBridge {

    private StaffAttendanceProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static StaffAttendanceUpdate changes(ProtoBridge bridge, com.mysms.time_attendance.v1.StaffAttendanceUpdate request) {
        StaffAttendanceUpdate changes = new StaffAttendanceUpdate();
        changes.setStatus(bridge.choice(
                request.hasStatus(), request.getStatus(), StaffAttendanceUpdate.StatusEnum::fromValue));
        changes.setCheckInAt(ProtoBridge.time(request.hasCheckInAt(), request.getCheckInAt()));
        changes.setCheckOutAt(ProtoBridge.time(request.hasCheckOutAt(), request.getCheckOutAt()));
        changes.setRemark(ProtoBridge.text(request.hasRemark(), request.getRemark()));
        return bridge.patch(changes);
    }
}
