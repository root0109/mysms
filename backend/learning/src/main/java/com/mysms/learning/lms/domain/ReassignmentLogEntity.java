package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.reassignment_log. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ReassignmentLogEntity extends BaseEntity {

    public static final String TABLE = "lms.reassignment_log";

    private UUID tenantId;
    private UUID contentItemId;
    private UUID userId;
    private UUID groupId;
    private UUID quizSubmissionId;
    private String comment;
    private UUID reassignedBy;
    private OffsetDateTime reassignedAt;
}
