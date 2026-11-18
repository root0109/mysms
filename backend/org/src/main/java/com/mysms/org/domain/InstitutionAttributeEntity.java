package com.mysms.org.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of org.institution_attribute. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class InstitutionAttributeEntity extends BaseEntity {

    public static final String TABLE = "org.institution_attribute";

    private UUID tenantId;
    private UUID institutionId;
    private String attrKey;
    private String attrValue;
    private UUID attributeDefinitionId;
}
