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

/** A row of lms.quiz_question_history. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class QuizQuestionHistoryEntity extends BaseEntity {

    public static final String TABLE = "lms.quiz_question_history";

    private UUID tenantId;
    private UUID quizId;
    private UUID questionId;
    private BigDecimal marks;
    private Integer sortOrder;
    private OffsetDateTime supersededAt;
}
