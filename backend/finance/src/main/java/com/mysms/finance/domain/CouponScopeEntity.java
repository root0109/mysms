package com.mysms.finance.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of finance.coupon_scope (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CouponScopeEntity extends BaseEntity {

    public static final String TABLE = "finance.coupon_scope";

    private UUID tenantId;
    private UUID couponId;
    private UUID lmsCourseId;
    private UUID lmsCohortId;
}
