package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.CohortHolidayUpdate;

/**
 * UpdateCohortHoliday over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.CohortHolidayUpdate} message as REST's
 * {@link CohortHolidayUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class CohortHolidayProtoBridge {

    private CohortHolidayProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CohortHolidayUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.CohortHolidayUpdate request) {
        CohortHolidayUpdate changes = new CohortHolidayUpdate();
        changes.setHolidayId(bridge.uuid(request.hasHolidayId(), request.getHolidayId(), "holiday_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setStartDate(bridge.date(request.hasStartDate(), request.getStartDate(), "start_date"));
        changes.setEndDate(bridge.date(request.hasEndDate(), request.getEndDate(), "end_date"));
        changes.setIsIncluded(ProtoBridge.flag(request.hasIsIncluded(), request.getIsIncluded()));
        return bridge.patch(changes);
    }
}
