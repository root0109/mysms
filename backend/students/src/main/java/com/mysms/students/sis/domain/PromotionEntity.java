package com.mysms.students.sis.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of sis.promotion. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PromotionEntity extends BaseEntity {

    public static final String TABLE = "sis.promotion";

    private UUID tenantId;
    private UUID studentId;
    private UUID institutionId;
    private UUID fromAcademicYearId;
    private UUID fromGradeLevelId;
    private UUID fromSectionId;
    private UUID toAcademicYearId;
    private UUID toGradeLevelId;
    private UUID toSectionId;
    private UUID basedOnExamSessionId;
    private String remarks;
    private String status;
}
