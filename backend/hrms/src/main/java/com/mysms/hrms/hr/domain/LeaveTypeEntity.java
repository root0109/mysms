package com.mysms.hrms.hr.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of hr.leave_type. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class LeaveTypeEntity extends BaseEntity {

    public static final String TABLE = "hr.leave_type";

    private UUID tenantId;
    private String code;
    private Integer sortOrder;
    private UUID institutionId;
    private UUID academicYearId;
    private String name;
    private BigDecimal maxDaysPerYear;
    private Boolean isPaid;
    private Boolean isActive;
}
