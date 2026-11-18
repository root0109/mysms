package com.mysms.assessment.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of assessment.assignment_approval. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AssignmentApprovalEntity extends BaseEntity {

    public static final String TABLE = "assessment.assignment_approval";

    private UUID tenantId;
    private UUID assignmentId;
    private String decision;
    private String comments;
    private UUID decidedByUserId;
    private OffsetDateTime decidedAt;
}
