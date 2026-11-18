package com.mysms.org.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of org.institution_type. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class InstitutionTypeEntity extends BaseEntity {

    public static final String TABLE = "org.institution_type";

    private UUID tenantId;
    private String code;
    private Integer sortOrder;
    private Boolean isActive;
    private String name;
}
