package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.question_blank. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class QuestionBlankEntity extends BaseEntity {

    public static final String TABLE = "lms.question_blank";

    private UUID tenantId;
    private UUID questionId;
    private String blankText;
    private Integer sortOrder;
}
