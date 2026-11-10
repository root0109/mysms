package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.exercise_submission (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ExerciseSubmissionEntity extends BaseEntity {

    public static final String TABLE = "lms.exercise_submission";

    private UUID tenantId;
    private UUID exerciseId;
    private UUID userId;
    private String code;
    private Boolean isFullCode;
    private String status;
}
