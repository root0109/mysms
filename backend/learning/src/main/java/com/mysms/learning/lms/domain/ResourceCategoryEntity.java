package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.resource_category. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ResourceCategoryEntity extends BaseEntity {

    public static final String TABLE = "lms.resource_category";

    private UUID tenantId;
    private String code;
    private Integer sortOrder;
    private Boolean isActive;
    private String name;
}
