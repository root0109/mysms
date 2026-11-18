package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.cohort_feedback. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CohortFeedbackEntity extends BaseEntity {

    public static final String TABLE = "lms.cohort_feedback";

    private UUID tenantId;
    private UUID cohortId;
    private UUID userId;
    private String feedback;
    private BigDecimal contentRating;
    private BigDecimal teacherRating;
    private BigDecimal valueRating;
}
