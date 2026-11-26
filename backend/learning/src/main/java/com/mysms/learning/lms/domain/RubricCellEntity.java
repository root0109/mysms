package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.rubric_cell. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class RubricCellEntity extends BaseEntity {

    public static final String TABLE = "lms.rubric_cell";

    private UUID tenantId;
    private UUID rubricId;
    private UUID rubricCriterionId;
    private UUID rubricLevelId;
    private String description;
    private Integer sortOrder;
}
