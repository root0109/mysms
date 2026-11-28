package com.mysms.platform.org.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.platform.org.api.model.AcademicYearUpdate;

/**
 * UpdateAcademicYear over gRPC (JAVA-CODING-RULES §7.2): the {@code org.v1.AcademicYearUpdate} message
 * as REST's {@link AcademicYearUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class AcademicYearProtoBridge {

    private AcademicYearProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static AcademicYearUpdate changes(ProtoBridge bridge, com.mysms.org.v1.AcademicYearUpdate request) {
        AcademicYearUpdate changes = new AcademicYearUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setStartDate(bridge.date(request.hasStartDate(), request.getStartDate(), "start_date"));
        changes.setEndDate(bridge.date(request.hasEndDate(), request.getEndDate(), "end_date"));
        changes.setTermStructure(bridge.choice(
                request.hasTermStructure(), request.getTermStructure(), AcademicYearUpdate.TermStructureEnum::fromValue));
        changes.setIsCurrent(ProtoBridge.flag(request.hasIsCurrent(), request.getIsCurrent()));
        return bridge.patch(changes);
    }
}
