package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.lesson_activity (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class LessonActivityEntity extends BaseEntity {

    public static final String TABLE = "lms.lesson_activity";

    private UUID tenantId;
    private UUID lessonId;
    private UUID quizId;
    private UUID assignmentId;
    private UUID exerciseId;
    private Boolean teacherOnly;
    private Integer sortOrder;
}
