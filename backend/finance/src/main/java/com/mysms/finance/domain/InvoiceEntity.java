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

/** A row of finance.invoice. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class InvoiceEntity extends BaseEntity {

    public static final String TABLE = "finance.invoice";

    private UUID tenantId;
    private UUID institutionId;
    private String invoiceNumber;
    private UUID studentId;
    private UUID billToUserId;
    private String billToName;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private UUID currencyId;
    private BigDecimal subtotal;
    private BigDecimal discountTotal;
    private BigDecimal taxTotal;
    private BigDecimal total;
    private BigDecimal amountPaid;
    private String status;
    private String invoiceType;
    private UUID creditForInvoiceId;
    private OffsetDateTime postedAt;
    private OffsetDateTime cancelledAt;
    private UUID documentFileId;
}
