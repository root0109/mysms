package com.mysms.finance.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of finance.fee_plan_installment_component. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class FeePlanInstallmentComponentEntity extends BaseEntity {

    public static final String TABLE = "finance.fee_plan_installment_component";

    private UUID tenantId;
    private UUID installmentId;
    private UUID feeHeadId;
    private Integer sequence;
    private BigDecimal sharePercent;
}
