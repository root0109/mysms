package com.mysms.master.domain;

import com.mysms.core.entity.BaseEntity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of master.country (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CountryEntity extends BaseEntity {

    public static final String TABLE = "master.country";

    private String iso2;
    private String iso3;
    private String name;
    private String shortName;
    private String phonePrefix;
    private Integer sortOrder;
    private boolean active;
}
