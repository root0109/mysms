package com.mysms.campus.transport.grpc;

import com.mysms.campus.transport.api.model.StudentTransportUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateStudentTransport over gRPC (JAVA-CODING-RULES §7.2): the {@code transport.v1.StudentTransportUpdate} message as
 * REST's {@link StudentTransportUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class StudentTransportProtoBridge {

    private StudentTransportProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static StudentTransportUpdate changes(ProtoBridge bridge, com.mysms.transport.v1.StudentTransportUpdate request) {
        StudentTransportUpdate changes = new StudentTransportUpdate();
        changes.setStudentId(bridge.uuid(request.hasStudentId(), request.getStudentId(), "student_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setTransportMode(bridge.choice(
                request.hasTransportMode(), request.getTransportMode(),
                StudentTransportUpdate.TransportModeEnum::fromValue));
        changes.setRouteId(bridge.uuid(request.hasRouteId(), request.getRouteId(), "route_id"));
        changes.setRouteStopId(bridge.uuid(request.hasRouteStopId(), request.getRouteStopId(), "route_stop_id"));
        changes.setBusPassIssued(ProtoBridge.flag(request.hasBusPassIssued(), request.getBusPassIssued()));
        changes.setDetails(ProtoBridge.text(request.hasDetails(), request.getDetails()));
        return bridge.patch(changes);
    }
}
