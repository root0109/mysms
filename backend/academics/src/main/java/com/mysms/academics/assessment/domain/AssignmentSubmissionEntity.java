package com.mysms.academics.assessment.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of assessment.assignment_submission. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AssignmentSubmissionEntity extends BaseEntity {

    public static final String TABLE = "assessment.assignment_submission";

    private UUID tenantId;
    private UUID assignmentId;
    private UUID studentId;
    private String content;
    private OffsetDateTime submittedAt;
    private String status;
    private BigDecimal marksObtained;
    private UUID gradedByUserId;
    private LocalDate gradedOn;
    private String feedback;
}
