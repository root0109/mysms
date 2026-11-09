package com.mysms.master.domain;

import com.mysms.core.entity.BaseEntity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of master.currency (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CurrencyEntity extends BaseEntity {

    public static final String TABLE = "master.currency";

    private String code;
    private String name;
    private String symbol;
    private Integer minorUnits;
    private boolean active;
}
