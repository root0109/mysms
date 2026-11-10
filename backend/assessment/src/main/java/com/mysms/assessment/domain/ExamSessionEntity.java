package com.mysms.assessment.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of assessment.exam_session (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ExamSessionEntity extends BaseEntity {

    public static final String TABLE = "assessment.exam_session";

    private UUID tenantId;
    private UUID institutionId;
    private UUID academicYearId;
    private UUID academicTermId;
    private UUID examTypeId;
    private String code;
    private String name;
    private UUID gradeLevelId;
    private UUID courseId;
    private UUID batchId;
    private String evaluationType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String venue;
    private String status;
}
