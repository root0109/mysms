package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.rubric_criterion. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class RubricCriterionEntity extends BaseEntity {

    public static final String TABLE = "lms.rubric_criterion";

    private UUID tenantId;
    private UUID rubricId;
    private String name;
    private String groupName;
    private BigDecimal weightPercent;
    private BigDecimal groupWeightPercent;
    private Integer sortOrder;
}
