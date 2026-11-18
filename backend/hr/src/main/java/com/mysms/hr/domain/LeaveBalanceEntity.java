package com.mysms.hr.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of hr.leave_balance. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class LeaveBalanceEntity extends BaseEntity {

    public static final String TABLE = "hr.leave_balance";

    private UUID tenantId;
    private UUID staffId;
    private UUID leaveTypeId;
    private UUID academicYearId;
    private BigDecimal entitledDays;
    private BigDecimal usedDays;
    private BigDecimal carriedForwardDays;
}
