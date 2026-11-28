package com.mysms.academics.timetable.grpc;

import com.mysms.academics.timetable.api.model.SessionUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateSession over gRPC (JAVA-CODING-RULES §7.2): the {@code timetable.v1.SessionUpdate} message as REST's {@link
 * SessionUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class SessionProtoBridge {

    private SessionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static SessionUpdate changes(ProtoBridge bridge, com.mysms.timetable.v1.SessionUpdate request) {
        SessionUpdate changes = new SessionUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setPeriodId(bridge.uuid(request.hasPeriodId(), request.getPeriodId(), "period_id"));
        changes.setStartAt(ProtoBridge.time(request.hasStartAt(), request.getStartAt()));
        changes.setEndAt(ProtoBridge.time(request.hasEndAt(), request.getEndAt()));
        changes.setWeekday(ProtoBridge.number(request.hasWeekday(), request.getWeekday()));
        changes.setClassSectionId(bridge.uuid(
                request.hasClassSectionId(), request.getClassSectionId(), "class_section_id"));
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        changes.setBatchId(bridge.uuid(request.hasBatchId(), request.getBatchId(), "batch_id"));
        changes.setSubjectId(bridge.uuid(request.hasSubjectId(), request.getSubjectId(), "subject_id"));
        changes.setStaffId(bridge.uuid(request.hasStaffId(), request.getStaffId(), "staff_id"));
        changes.setRoomId(bridge.uuid(request.hasRoomId(), request.getRoomId(), "room_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setSessionType(ProtoBridge.text(request.hasSessionType(), request.getSessionType()));
        changes.setColor(ProtoBridge.number(request.hasColor(), request.getColor()));
        return bridge.patch(changes);
    }
}
