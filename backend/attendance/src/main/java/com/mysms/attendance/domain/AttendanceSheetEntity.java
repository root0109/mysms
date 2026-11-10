package com.mysms.attendance.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of attendance.attendance_sheet (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AttendanceSheetEntity extends BaseEntity {

    public static final String TABLE = "attendance.attendance_sheet";

    private UUID tenantId;
    private UUID registerId;
    private UUID academicYearId;
    private LocalDate attendanceDate;
    private UUID timetableSessionId;
    private String dayPart;
    private String reference;
    private UUID takenByStaffId;
    private String status;
}
