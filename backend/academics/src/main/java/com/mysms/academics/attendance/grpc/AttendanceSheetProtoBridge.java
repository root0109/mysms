package com.mysms.academics.attendance.grpc;

import com.mysms.academics.attendance.api.model.AttendanceSheetUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateAttendanceSheet over gRPC (JAVA-CODING-RULES §7.2): the {@code attendance.v1.AttendanceSheetUpdate} message as
 * REST's {@link AttendanceSheetUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class AttendanceSheetProtoBridge {

    private AttendanceSheetProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static AttendanceSheetUpdate changes(ProtoBridge bridge, com.mysms.attendance.v1.AttendanceSheetUpdate request) {
        AttendanceSheetUpdate changes = new AttendanceSheetUpdate();
        changes.setRegisterId(bridge.uuid(request.hasRegisterId(), request.getRegisterId(), "register_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setAttendanceDate(bridge.date(
                request.hasAttendanceDate(), request.getAttendanceDate(), "attendance_date"));
        changes.setTimetableSessionId(bridge.uuid(
                request.hasTimetableSessionId(), request.getTimetableSessionId(), "timetable_session_id"));
        changes.setDayPart(bridge.choice(
                request.hasDayPart(), request.getDayPart(), AttendanceSheetUpdate.DayPartEnum::fromValue));
        changes.setReference(ProtoBridge.text(request.hasReference(), request.getReference()));
        changes.setTakenByStaffId(bridge.uuid(
                request.hasTakenByStaffId(), request.getTakenByStaffId(), "taken_by_staff_id"));
        return bridge.patch(changes);
    }
}
