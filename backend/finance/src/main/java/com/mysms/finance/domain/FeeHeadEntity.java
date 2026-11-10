package com.mysms.finance.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of finance.fee_head (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class FeeHeadEntity extends BaseEntity {

    public static final String TABLE = "finance.fee_head";

    private UUID tenantId;
    private UUID institutionId;
    private String code;
    private String name;
    private String description;
    private BigDecimal defaultAmount;
    private UUID currencyId;
    private String ledgerAccountCode;
    private Boolean isActive;
}
