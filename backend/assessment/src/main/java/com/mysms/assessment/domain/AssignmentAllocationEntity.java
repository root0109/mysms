package com.mysms.assessment.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of assessment.assignment_allocation. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AssignmentAllocationEntity extends BaseEntity {

    public static final String TABLE = "assessment.assignment_allocation";

    private UUID tenantId;
    private UUID assignmentId;
    private UUID studentId;
}
