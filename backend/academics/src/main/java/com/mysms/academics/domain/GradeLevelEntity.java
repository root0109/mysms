package com.mysms.academics.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of academics.grade_level (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class GradeLevelEntity extends BaseEntity {

    public static final String TABLE = "academics.grade_level";

    private UUID tenantId;
    private UUID institutionId;
    private String name;
    private String slug;
    private Integer sortOrder;
    private Boolean isActive;
}
