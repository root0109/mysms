package com.mysms.academics.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of academics.lesson_plan. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class LessonPlanEntity extends BaseEntity {

    public static final String TABLE = "academics.lesson_plan";

    private UUID tenantId;
    private UUID teachingAssignmentId;
    private UUID curriculumUnitId;
    private String unitNo;
    private String unitName;
    private String title;
    private Integer plannedDurationMinutes;
    private String description;
    private String objective;
    private String materialsRequired;
    private String introduction;
    private String procedureText;
    private String conclusion;
    private String assessmentText;
    private String modification;
    private String notes;
    private String status;
    private Boolean isPublished;
    private OffsetDateTime publishedAt;
    private LocalDate startDate;
    private LocalDate endDate;
}
