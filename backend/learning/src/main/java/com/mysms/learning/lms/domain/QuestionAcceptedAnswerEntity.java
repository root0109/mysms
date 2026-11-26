package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.question_accepted_answer. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class QuestionAcceptedAnswerEntity extends BaseEntity {

    public static final String TABLE = "lms.question_accepted_answer";

    private UUID tenantId;
    private UUID questionId;
    private Integer sortOrder;
    private String answerText;
}
