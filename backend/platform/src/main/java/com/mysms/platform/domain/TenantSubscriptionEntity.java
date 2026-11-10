package com.mysms.platform.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of platform.tenant_subscription (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class TenantSubscriptionEntity extends BaseEntity {

    public static final String TABLE = "platform.tenant_subscription";

    private UUID tenantId;
    private Integer maxMembers;
    private BigDecimal price;
    private UUID currencyId;
    private Integer billingCycleMonths;
    private String gatewayName;
    private String gatewayTxnRef;
    private BigDecimal amountPaid;
    private String gatewayStatus;
    private OffsetDateTime paidAt;
    private String payerName;
    private UUID institutionId;
    private UUID planId;
    private UUID requestedByUserId;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
}
