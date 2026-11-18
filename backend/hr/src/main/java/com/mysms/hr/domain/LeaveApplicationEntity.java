package com.mysms.hr.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of hr.leave_application. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class LeaveApplicationEntity extends BaseEntity {

    public static final String TABLE = "hr.leave_application";

    private UUID tenantId;
    private UUID institutionId;
    private UUID academicYearId;
    private UUID staffId;
    private UUID leaveTypeId;
    private UUID absenceReasonId;
    private UUID classSectionId;
    private OffsetDateTime fromAt;
    private OffsetDateTime toAt;
    private String dayPart;
    private String remarks;
    private String status;
    private UUID decidedByUserId;
    private LocalDate decidedOn;
    private String decisionComments;
}
