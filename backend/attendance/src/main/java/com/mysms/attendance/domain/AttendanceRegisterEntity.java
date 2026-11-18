package com.mysms.attendance.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of attendance.attendance_register. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AttendanceRegisterEntity extends BaseEntity {

    public static final String TABLE = "attendance.attendance_register";

    private UUID tenantId;
    private UUID institutionId;
    private String code;
    private String name;
    private String registerScope;
    private UUID classSectionId;
    private UUID courseId;
    private UUID batchId;
    private UUID subjectId;
    private Boolean isActive;
}
