package com.mysms.platform.org.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.platform.org.api.model.AcademicTermUpdate;

/**
 * UpdateAcademicTerm over gRPC (JAVA-CODING-RULES §7.2): the {@code org.v1.AcademicTermUpdate} message
 * as REST's {@link AcademicTermUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class AcademicTermProtoBridge {

    private AcademicTermProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static AcademicTermUpdate changes(ProtoBridge bridge, com.mysms.org.v1.AcademicTermUpdate request) {
        AcademicTermUpdate changes = new AcademicTermUpdate();
        changes.setParentTermId(bridge.uuid(request.hasParentTermId(), request.getParentTermId(), "parent_term_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setStartDate(bridge.date(request.hasStartDate(), request.getStartDate(), "start_date"));
        changes.setEndDate(bridge.date(request.hasEndDate(), request.getEndDate(), "end_date"));
        changes.setSequence(ProtoBridge.number(request.hasSequence(), request.getSequence()));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
