package com.mysms.payroll.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of payroll.payroll_payment (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PayrollPaymentEntity extends BaseEntity {

    public static final String TABLE = "payroll.payroll_payment";

    private UUID tenantId;
    private UUID institutionId;
    private String transactionNumber;
    private UUID paymentTypeId;
    private UUID disbursementAccountId;
    private UUID staffId;
    private UUID payslipId;
    private LocalDate transactionDate;
    private BigDecimal amount;
    private String paymentMethod;
    private String transactionDetails;
    private String referenceNumber;
    private UUID attachmentFileId;
    private String remarks;
}
