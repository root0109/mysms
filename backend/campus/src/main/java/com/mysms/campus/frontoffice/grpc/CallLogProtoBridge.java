package com.mysms.campus.frontoffice.grpc;

import com.mysms.campus.frontoffice.api.model.CallLogUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateCallLog over gRPC (JAVA-CODING-RULES §7.2): the {@code frontoffice.v1.CallLogUpdate} message as REST's {@link
 * CallLogUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class CallLogProtoBridge {

    private CallLogProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CallLogUpdate changes(ProtoBridge bridge, com.mysms.frontoffice.v1.CallLogUpdate request) {
        CallLogUpdate changes = new CallLogUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setDirection(bridge.choice(
                request.hasDirection(), request.getDirection(), CallLogUpdate.DirectionEnum::fromValue));
        changes.setPurpose(ProtoBridge.text(request.hasPurpose(), request.getPurpose()));
        changes.setCallerName(ProtoBridge.text(request.hasCallerName(), request.getCallerName()));
        changes.setIncomingNumber(ProtoBridge.text(request.hasIncomingNumber(), request.getIncomingNumber()));
        changes.setOutgoingNumber(ProtoBridge.text(request.hasOutgoingNumber(), request.getOutgoingNumber()));
        changes.setCallDate(bridge.date(request.hasCallDate(), request.getCallDate(), "call_date"));
        changes.setStartTime(ProtoBridge.text(request.hasStartTime(), request.getStartTime()));
        changes.setEndTime(ProtoBridge.text(request.hasEndTime(), request.getEndTime()));
        changes.setDurationSeconds(ProtoBridge.duration(request.hasDurationSeconds(), request.getDurationSeconds()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setLoggedByUserId(bridge.uuid(
                request.hasLoggedByUserId(), request.getLoggedByUserId(), "logged_by_user_id"));
        return bridge.patch(changes);
    }
}
