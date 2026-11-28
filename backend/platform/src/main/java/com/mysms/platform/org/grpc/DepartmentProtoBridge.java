package com.mysms.platform.org.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.platform.org.api.model.DepartmentUpdate;

/**
 * UpdateDepartment over gRPC (JAVA-CODING-RULES §7.2): the {@code org.v1.DepartmentUpdate} message
 * as REST's {@link DepartmentUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class DepartmentProtoBridge {

    private DepartmentProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static DepartmentUpdate changes(ProtoBridge bridge, com.mysms.org.v1.DepartmentUpdate request) {
        DepartmentUpdate changes = new DepartmentUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setParentDepartmentId(bridge.uuid(
                request.hasParentDepartmentId(), request.getParentDepartmentId(), "parent_department_id"));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
