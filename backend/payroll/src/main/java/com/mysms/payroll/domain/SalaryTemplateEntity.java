package com.mysms.payroll.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of payroll.salary_template. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SalaryTemplateEntity extends BaseEntity {

    public static final String TABLE = "payroll.salary_template";

    private UUID tenantId;
    private UUID institutionId;
    private String name;
    private Boolean isActive;
}
