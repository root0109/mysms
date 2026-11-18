package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.course_review. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CourseReviewEntity extends BaseEntity {

    public static final String TABLE = "lms.course_review";

    private UUID tenantId;
    private UUID courseId;
    private UUID userId;
    private BigDecimal rating;
    private String review;
}
