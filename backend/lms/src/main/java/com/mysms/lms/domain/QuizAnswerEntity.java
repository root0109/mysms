package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.quiz_answer (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class QuizAnswerEntity extends BaseEntity {

    public static final String TABLE = "lms.quiz_answer";

    private UUID tenantId;
    private UUID quizSubmissionId;
    private UUID questionId;
    private String questionText;
    private String questionType;
    private String answer;
    private Boolean isCorrect;
    private BigDecimal marksAwarded;
    private BigDecimal marksPossible;
    private Integer sortOrder;
    private String feedback;
    private UUID gradedBy;
    private UUID learningObjectiveId;
    private Long timeSpentMs;
}
