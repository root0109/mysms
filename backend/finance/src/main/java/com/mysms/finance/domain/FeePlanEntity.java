package com.mysms.finance.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of finance.fee_plan (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class FeePlanEntity extends BaseEntity {

    public static final String TABLE = "finance.fee_plan";

    private UUID tenantId;
    private UUID institutionId;
    private String code;
    private String name;
    private String scheduleBasis;
    private Integer offsetDays;
    private String offsetDirection;
    private BigDecimal discountPercent;
    private String notes;
    private Boolean isActive;
}
