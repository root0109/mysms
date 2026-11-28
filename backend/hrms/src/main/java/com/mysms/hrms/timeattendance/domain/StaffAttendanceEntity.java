package com.mysms.hrms.timeattendance.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of time_attendance.staff_attendance. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class StaffAttendanceEntity extends BaseEntity {

    public static final String TABLE = "time_attendance.staff_attendance";

    private UUID tenantId;
    private UUID staffId;
    private LocalDate attendanceDate;
    private String status;
    private OffsetDateTime checkInAt;
    private OffsetDateTime checkOutAt;
    private String source;
    private UUID leaveApplicationId;
    private UUID recordedByUserId;
    private String remark;
}
