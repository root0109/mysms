package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.sis.api.model.StudentLeaveRequestUpdate;

/**
 * UpdateStudentLeaveRequest over gRPC (JAVA-CODING-RULES §7.2): the {@code sis.v1.StudentLeaveRequestUpdate} message as
 * REST's {@link StudentLeaveRequestUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class StudentLeaveRequestProtoBridge {

    private StudentLeaveRequestProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static StudentLeaveRequestUpdate changes(ProtoBridge bridge, com.mysms.sis.v1.StudentLeaveRequestUpdate request) {
        StudentLeaveRequestUpdate changes = new StudentLeaveRequestUpdate();
        changes.setStudentId(bridge.uuid(request.hasStudentId(), request.getStudentId(), "student_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setFromDate(bridge.date(request.hasFromDate(), request.getFromDate(), "from_date"));
        changes.setToDate(bridge.date(request.hasToDate(), request.getToDate(), "to_date"));
        changes.setDayPart(bridge.choice(
                request.hasDayPart(), request.getDayPart(), StudentLeaveRequestUpdate.DayPartEnum::fromValue));
        changes.setReason(ProtoBridge.text(request.hasReason(), request.getReason()));
        return bridge.patch(changes);
    }
}
