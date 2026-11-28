package com.mysms.academics.attendance.grpc;

import com.mysms.academics.attendance.api.model.AbsenceReasonUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateAbsenceReason over gRPC (JAVA-CODING-RULES §7.2): the {@code attendance.v1.AbsenceReasonUpdate} message as
 * REST's {@link AbsenceReasonUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value
 * sets it.
 */
final class AbsenceReasonProtoBridge {

    private AbsenceReasonProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static AbsenceReasonUpdate changes(ProtoBridge bridge, com.mysms.attendance.v1.AbsenceReasonUpdate request) {
        AbsenceReasonUpdate changes = new AbsenceReasonUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
