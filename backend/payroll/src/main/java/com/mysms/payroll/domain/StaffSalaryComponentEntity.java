package com.mysms.payroll.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of payroll.staff_salary_component (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class StaffSalaryComponentEntity extends BaseEntity {

    public static final String TABLE = "payroll.staff_salary_component";

    private UUID tenantId;
    private UUID staffSalaryId;
    private UUID salaryTemplateComponentId;
    private BigDecimal amount;
}
