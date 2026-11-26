package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.quiz_submission. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class QuizSubmissionEntity extends BaseEntity {

    public static final String TABLE = "lms.quiz_submission";

    private UUID tenantId;
    private Integer attemptNumber;
    private UUID quizId;
    private UUID userId;
    private UUID courseId;
    private BigDecimal score;
    private BigDecimal scoreOutOf;
    private BigDecimal percentage;
    private BigDecimal passingPercentage;
    private Integer violationCount;
    private String submissionReason;
    private UUID contentItemId;
    private UUID cohortId;
    private String status;
    private OffsetDateTime startedAt;
    private OffsetDateTime submittedAt;
    private Integer timeTakenSeconds;
    private Integer answeredCount;
    private Integer correctCount;
    private Integer incorrectCount;
    private Boolean isReassigned;
    private UUID reassignedFromId;
    private String teacherComment;
}
