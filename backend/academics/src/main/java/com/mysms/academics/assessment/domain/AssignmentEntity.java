package com.mysms.academics.assessment.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of assessment.assignment. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AssignmentEntity extends BaseEntity {

    public static final String TABLE = "assessment.assignment";

    private UUID tenantId;
    private UUID institutionId;
    private UUID academicYearId;
    private String title;
    private String description;
    private UUID assignmentTypeId;
    private UUID classSectionId;
    private UUID courseId;
    private UUID batchId;
    private UUID assignedByStaffId;
    private UUID reviewerStaffId;
    private BigDecimal maxMarks;
    private OffsetDateTime issuedOn;
    private OffsetDateTime dueAt;
    private UUID attachmentFileId;
    private String status;
}
