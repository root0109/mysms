package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.quiz_question. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class QuizQuestionEntity extends BaseEntity {

    public static final String TABLE = "lms.quiz_question";

    private UUID tenantId;
    private UUID quizId;
    private UUID questionId;
    private BigDecimal marks;
    private Integer sortOrder;
}
