package com.mysms.finance.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of finance.fee_plan_installment. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class FeePlanInstallmentEntity extends BaseEntity {

    public static final String TABLE = "finance.fee_plan_installment";

    private UUID tenantId;
    private UUID feePlanId;
    private Integer sequence;
    private Integer dueDays;
    private LocalDate dueDate;
    private BigDecimal sharePercent;
}
