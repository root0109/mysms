package com.mysms.assessment.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of assessment.homework. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class HomeworkEntity extends BaseEntity {

    public static final String TABLE = "assessment.homework";

    private UUID tenantId;
    private UUID institutionId;
    private UUID academicYearId;
    private UUID classSectionId;
    private UUID subjectId;
    private UUID assignedByStaffId;
    private String description;
    private UUID attachmentFileId;
    private OffsetDateTime assignedOn;
    private OffsetDateTime dueAt;
    private String status;
}
