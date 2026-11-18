package com.mysms.finance.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of finance.refund. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class RefundEntity extends BaseEntity {

    public static final String TABLE = "finance.refund";

    private UUID tenantId;
    private UUID paymentId;
    private BigDecimal amount;
    private String reason;
    private String status;
    private String gatewayRefundId;
    private String refundMethod;
    private UUID refundedByUserId;
    private OffsetDateTime refundedAt;
    private UUID requestKey;
}
