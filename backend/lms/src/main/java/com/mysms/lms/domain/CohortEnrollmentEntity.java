package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.cohort_enrollment. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CohortEnrollmentEntity extends BaseEntity {

    public static final String TABLE = "lms.cohort_enrollment";

    private UUID tenantId;
    private UUID cohortId;
    private UUID userId;
    private UUID paymentId;
    private UUID acquisitionSourceId;
    private Boolean confirmationEmailSent;
    private Boolean isActive;
    private Boolean isSelfEnrolled;
    private String finalGrade;
    private BigDecimal finalGpa;
    private UUID gradedBy;
    private OffsetDateTime gradedAt;
    private Boolean gradePublished;
    private OffsetDateTime gradePublishedAt;
    private String teacherComment;
    private Boolean hasAccommodation;
    private String accommodationRef;
    private Integer surveyStatus;
    private OffsetDateTime removedAt;
    private String removalReason;
}
