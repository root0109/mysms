package com.mysms.finance.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of finance.invoice_line. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class InvoiceLineEntity extends BaseEntity {

    public static final String TABLE = "finance.invoice_line";

    private UUID tenantId;
    private BigDecimal taxRatePercent;
    private String taxCode;
    private UUID invoiceId;
    private UUID feeHeadId;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal lineTotal;
}
