package com.mysms.org.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of org.attribute_definition. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AttributeDefinitionEntity extends BaseEntity {

    public static final String TABLE = "org.attribute_definition";

    private UUID tenantId;
    private String name;
    private String dataType;
    private Integer sortOrder;
}
