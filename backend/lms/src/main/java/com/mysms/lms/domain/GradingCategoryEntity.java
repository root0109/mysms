package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.grading_category (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class GradingCategoryEntity extends BaseEntity {

    public static final String TABLE = "lms.grading_category";

    private UUID tenantId;
    private UUID courseId;
    private UUID cohortId;
    private String name;
    private BigDecimal weightPercent;
    private Boolean equalItemWeighting;
}
