package com.mysms.attendance.events;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Domain events attendance publishes through its outbox: {@code <schema>.<table>.created|updated|deleted} with the row
 * id.
 * Dependent modules keep their copies and logical references fresh from these (database/logical-references.md).
 */
public final class AttendanceEvents {

    public static final String CREATED = ".created";
    public static final String UPDATED = ".updated";
    public static final String DELETED = ".deleted";
    /** attendance.CompleteAttendanceSheet: the sheet is done (see {@link #completed}). */
    public static final String COMPLETE = ".complete";

    private AttendanceEvents() {
    }

    public static Map<String, Object> payload(UUID id) {
        return Map.of("id", id.toString());
    }

    /**
     * Payload of {@code attendance.attendance_sheet.complete}: the sheet, its date and the students marked absent —
     * ids only — so a consumer can notify their guardians without reading the sheet back. A sheet reopened and
     * completed again announces itself again; consumers deduplicate per sheet and student.
     */
    public static Map<String, Object> completed(UUID sheetId, LocalDate attendanceDate, List<UUID> absentStudentIds) {
        return Map.of("id", sheetId.toString(), "attendanceDate", attendanceDate.toString(),
                "absentStudentIds", absentStudentIds.stream().map(UUID::toString).toList());
    }
}
