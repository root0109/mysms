package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.learning_program. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class LearningProgramEntity extends BaseEntity {

    public static final String TABLE = "lms.learning_program";

    private UUID tenantId;
    private String title;
    private Boolean isPublished;
    private Boolean enforceCourseOrder;
    private Integer courseCount;
    private Integer memberCount;
}
