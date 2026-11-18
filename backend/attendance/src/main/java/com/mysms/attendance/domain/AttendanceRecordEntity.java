package com.mysms.attendance.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of attendance.attendance_record. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AttendanceRecordEntity extends BaseEntity {

    public static final String TABLE = "attendance.attendance_record";

    private UUID tenantId;
    private UUID leaveRequestId;
    private UUID sheetId;
    private UUID studentId;
    private String status;
    private UUID attendanceTypeId;
    private UUID absenceReasonId;
    private String remark;
    private UUID recordedByUserId;
    private LocalDate attendanceDate;
}
