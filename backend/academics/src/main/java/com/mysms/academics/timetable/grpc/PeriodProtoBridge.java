package com.mysms.academics.timetable.grpc;

import com.mysms.academics.timetable.api.model.PeriodUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdatePeriod over gRPC (JAVA-CODING-RULES §7.2): the {@code timetable.v1.PeriodUpdate} message as REST's {@link
 * PeriodUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class PeriodProtoBridge {

    private PeriodProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PeriodUpdate changes(ProtoBridge bridge, com.mysms.timetable.v1.PeriodUpdate request) {
        PeriodUpdate changes = new PeriodUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setStartTime(ProtoBridge.text(request.hasStartTime(), request.getStartTime()));
        changes.setDurationMinutes(ProtoBridge.text(request.hasDurationMinutes(), request.getDurationMinutes()));
        changes.setSequence(ProtoBridge.number(request.hasSequence(), request.getSequence()));
        return bridge.patch(changes);
    }
}
