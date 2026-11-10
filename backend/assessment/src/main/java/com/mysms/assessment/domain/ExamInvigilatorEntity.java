package com.mysms.assessment.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of assessment.exam_invigilator (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ExamInvigilatorEntity extends BaseEntity {

    public static final String TABLE = "assessment.exam_invigilator";

    private UUID tenantId;
    private UUID examId;
    private UUID staffId;
}
