package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.rubric_assessment (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class RubricAssessmentEntity extends BaseEntity {

    public static final String TABLE = "lms.rubric_assessment";

    private UUID tenantId;
    private Integer rubricVersion;
    private BigDecimal pointsAwarded;
    private UUID contentItemId;
    private UUID userId;
    private UUID groupId;
    private UUID rubricCellId;
    private String comment;
}
