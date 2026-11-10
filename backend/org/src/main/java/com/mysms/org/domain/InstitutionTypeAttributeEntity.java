package com.mysms.org.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of org.institution_type_attribute (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class InstitutionTypeAttributeEntity extends BaseEntity {

    public static final String TABLE = "org.institution_type_attribute";

    private UUID tenantId;
    private UUID institutionTypeId;
    private UUID attributeDefinitionId;
    private Boolean isRequired;
}
