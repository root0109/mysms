package com.mysms.campus.transport.grpc;

import com.mysms.campus.transport.api.model.RouteUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateRoute over gRPC (JAVA-CODING-RULES §7.2): the {@code transport.v1.RouteUpdate} message as REST's {@link
 * RouteUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class RouteProtoBridge {

    private RouteProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static RouteUpdate changes(ProtoBridge bridge, com.mysms.transport.v1.RouteUpdate request) {
        RouteUpdate changes = new RouteUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setVehicleNumber(ProtoBridge.text(request.hasVehicleNumber(), request.getVehicleNumber()));
        changes.setStartTime(ProtoBridge.text(request.hasStartTime(), request.getStartTime()));
        changes.setEndTime(ProtoBridge.text(request.hasEndTime(), request.getEndTime()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
