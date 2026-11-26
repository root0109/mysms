package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.exercise_test_case. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ExerciseTestCaseEntity extends BaseEntity {

    public static final String TABLE = "lms.exercise_test_case";

    private UUID tenantId;
    private UUID exerciseId;
    private Integer sortOrder;
    private String input;
    private String expectedOutput;
    private Boolean isHidden;
}
