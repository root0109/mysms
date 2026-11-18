package com.mysms.hr.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of hr.staff. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class StaffEntity extends BaseEntity {

    public static final String TABLE = "hr.staff";

    private UUID tenantId;
    private UUID personId;
    private UUID userId;
    private UUID institutionId;
    private String employeeCode;
    private UUID designationId;
    private String subDesignation;
    private String employmentType;
    private LocalDate joiningDate;
    private UUID reportingToStaffId;
    private UUID primaryDepartmentId;
    private UUID highestQualificationId;
    private String qualificationDetail;
    private String specialization;
    private String interests;
    private Integer maxPeriodsPerWeek;
    private String status;
}
