package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.learning_program_member. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class LearningProgramMemberEntity extends BaseEntity {

    public static final String TABLE = "lms.learning_program_member";

    private UUID tenantId;
    private UUID learningProgramId;
    private UUID userId;
    private BigDecimal progressPercent;
}
