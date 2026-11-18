package com.mysms.payroll.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of payroll.pay_category. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PayCategoryEntity extends BaseEntity {

    public static final String TABLE = "payroll.pay_category";

    private UUID tenantId;
    private String code;
    private Integer sortOrder;
    private Boolean isActive;
    private String name;
}
