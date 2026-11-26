package com.mysms.hrms.payroll.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of payroll.salary_template_component. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SalaryTemplateComponentEntity extends BaseEntity {

    public static final String TABLE = "payroll.salary_template_component";

    private UUID tenantId;
    private UUID salaryTemplateId;
    private UUID payComponentId;
    private UUID payCategoryId;
    private BigDecimal value;
}
