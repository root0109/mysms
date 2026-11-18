package com.mysms.finance.domain;

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

/** A row of finance.payment. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PaymentEntity extends BaseEntity {

    public static final String TABLE = "finance.payment";

    private UUID tenantId;
    private UUID institutionId;
    private UUID payerUserId;
    private String purpose;
    private UUID invoiceId;
    private UUID lmsCourseId;
    private UUID lmsCohortId;
    private UUID admissionApplicationId;
    private String gatewayName;
    private String gatewayOrderId;
    private String gatewayPaymentId;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal amount;
    private BigDecimal amountWithTax;
    private UUID currencyId;
    private UUID couponId;
    private String couponCode;
    private String billingName;
    private UUID billingAddressId;
    private String taxRegistrationNumber;
    private String taxIdentityEnc;
    private UUID acquisitionSourceId;
    private boolean payerConsent;
    private String status;
    private OffsetDateTime receivedAt;
    private UUID payerPersonId;
    private String payerName;
    private String paymentMethod;
    private String receiptNumber;
    private String instrumentReference;
    private LocalDate instrumentDate;
    private String bankName;
    private UUID gatewayAccountId;
    private UUID receivedByUserId;
    private String gatewayCurrency;
    private Long gatewayAmountMinor;
}
