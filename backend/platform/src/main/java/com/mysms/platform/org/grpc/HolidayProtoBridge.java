package com.mysms.platform.org.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.platform.org.api.model.HolidayUpdate;

/**
 * UpdateHoliday over gRPC (JAVA-CODING-RULES §7.2): the {@code org.v1.HolidayUpdate} message
 * as REST's {@link HolidayUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class HolidayProtoBridge {

    private HolidayProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static HolidayUpdate changes(ProtoBridge bridge, com.mysms.org.v1.HolidayUpdate request) {
        HolidayUpdate changes = new HolidayUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setStartDate(bridge.date(request.hasStartDate(), request.getStartDate(), "start_date"));
        changes.setEndDate(bridge.date(request.hasEndDate(), request.getEndDate(), "end_date"));
        return bridge.patch(changes);
    }
}
