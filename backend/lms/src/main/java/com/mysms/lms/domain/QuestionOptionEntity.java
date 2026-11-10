package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.question_option (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class QuestionOptionEntity extends BaseEntity {

    public static final String TABLE = "lms.question_option";

    private UUID tenantId;
    private UUID questionId;
    private Integer sortOrder;
    private String optionText;
    private Boolean isCorrect;
    private String explanation;
    private String label;
}
