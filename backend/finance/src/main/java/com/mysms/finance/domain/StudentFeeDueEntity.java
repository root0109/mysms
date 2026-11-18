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

/** A row of finance.student_fee_due. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class StudentFeeDueEntity extends BaseEntity {

    public static final String TABLE = "finance.student_fee_due";

    private UUID tenantId;
    private UUID institutionId;
    private UUID studentId;
    private UUID programEnrollmentId;
    private UUID studentEnrollmentId;
    private UUID installmentId;
    private UUID feeHeadId;
    private LocalDate dueDate;
    private BigDecimal amount;
    private UUID currencyId;
    private BigDecimal discountPercent;
    private BigDecimal feeFactor;
    private String status;
    private UUID invoiceId;
    private UUID academicYearId;
    private UUID feeStructureId;
    private UUID concessionId;
    private BigDecimal discountAmount;
    private BigDecimal netAmount;
}
