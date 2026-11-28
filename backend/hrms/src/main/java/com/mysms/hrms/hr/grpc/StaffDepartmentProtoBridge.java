package com.mysms.hrms.hr.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.hrms.hr.api.model.StaffDepartmentUpdate;

/**
 * UpdateStaffDepartment over gRPC (JAVA-CODING-RULES §7.2): the {@code hr.v1.StaffDepartmentUpdate} message as REST's
 * {@link StaffDepartmentUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class StaffDepartmentProtoBridge {

    private StaffDepartmentProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static StaffDepartmentUpdate changes(ProtoBridge bridge, com.mysms.hr.v1.StaffDepartmentUpdate request) {
        StaffDepartmentUpdate changes = new StaffDepartmentUpdate();
        changes.setDepartmentId(bridge.uuid(request.hasDepartmentId(), request.getDepartmentId(), "department_id"));
        return bridge.patch(changes);
    }
}
