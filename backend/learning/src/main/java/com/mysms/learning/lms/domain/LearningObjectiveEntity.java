package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.learning_objective. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class LearningObjectiveEntity extends BaseEntity {

    public static final String TABLE = "lms.learning_objective";

    private UUID tenantId;
    private UUID courseId;
    private UUID chapterId;
    private String objective;
    private Integer sortOrder;
    private UUID copiedFromId;
}
