package com.mysms.payroll.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of payroll.staff_salary (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class StaffSalaryEntity extends BaseEntity {

    public static final String TABLE = "payroll.staff_salary";

    private UUID tenantId;
    private UUID staffId;
    private UUID salaryTemplateId;
    private BigDecimal grossSalary;
    private LocalDate effectiveDate;
    private String remarks;
}
