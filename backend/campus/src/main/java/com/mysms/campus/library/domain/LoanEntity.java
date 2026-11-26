package com.mysms.campus.library.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of library.loan. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class LoanEntity extends BaseEntity {

    public static final String TABLE = "library.loan";

    private UUID tenantId;
    private UUID mediaUnitId;
    private UUID mediaId;
    private UUID libraryCardId;
    private UUID borrowerUserId;
    private LocalDate issuedOn;
    private LocalDate dueOn;
    private LocalDate returnedOn;
    private BigDecimal fineAmount;
    private UUID invoiceId;
    private UUID issuedByUserId;
    private String status;
}
