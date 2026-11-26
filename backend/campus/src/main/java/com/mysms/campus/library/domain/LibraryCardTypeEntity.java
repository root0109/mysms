package com.mysms.campus.library.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of library.library_card_type. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class LibraryCardTypeEntity extends BaseEntity {

    public static final String TABLE = "library.library_card_type";

    private UUID tenantId;
    private String code;
    private Integer sortOrder;
    private Boolean isActive;
    private String name;
    private Integer maxItems;
    private Integer loanDays;
    private BigDecimal finePerDay;
}
