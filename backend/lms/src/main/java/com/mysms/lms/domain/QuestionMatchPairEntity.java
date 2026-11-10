package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.question_match_pair (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class QuestionMatchPairEntity extends BaseEntity {

    public static final String TABLE = "lms.question_match_pair";

    private UUID tenantId;
    private UUID questionId;
    private String leftText;
    private String rightText;
    private Integer sortOrder;
}
