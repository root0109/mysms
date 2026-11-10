package com.mysms.academics.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of academics.lesson_plan_approval (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class LessonPlanApprovalEntity extends BaseEntity {

    public static final String TABLE = "academics.lesson_plan_approval";

    private UUID tenantId;
    private UUID lessonPlanId;
    private String decision;
    private String comments;
    private UUID decidedByUserId;
    private OffsetDateTime decidedAt;
}
