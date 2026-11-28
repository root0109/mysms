package com.mysms.hrms.hr.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.hrms.hr.api.model.DesignationUpdate;

/**
 * UpdateDesignation over gRPC (JAVA-CODING-RULES §7.2): the {@code hr.v1.DesignationUpdate} message as REST's {@link
 * DesignationUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class DesignationProtoBridge {

    private DesignationProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static DesignationUpdate changes(ProtoBridge bridge, com.mysms.hr.v1.DesignationUpdate request) {
        DesignationUpdate changes = new DesignationUpdate();
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setStaffCategory(bridge.choice(
                request.hasStaffCategory(), request.getStaffCategory(),
                DesignationUpdate.StaffCategoryEnum::fromValue));
        return bridge.patch(changes);
    }
}
