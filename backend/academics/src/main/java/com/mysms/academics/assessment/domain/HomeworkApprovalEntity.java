package com.mysms.academics.assessment.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of assessment.homework_approval. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class HomeworkApprovalEntity extends BaseEntity {

    public static final String TABLE = "assessment.homework_approval";

    private UUID tenantId;
    private UUID homeworkId;
    private String decision;
    private String comments;
    private UUID decidedByUserId;
    private OffsetDateTime decidedAt;
}
