package com.mysms.comms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of comms.task (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class TaskEntity extends BaseEntity {

    public static final String TABLE = "comms.task";

    private UUID tenantId;
    private UUID institutionId;
    private UUID academicYearId;
    private UUID ownerUserId;
    private String title;
    private String taskCategory;
    private OffsetDateTime dueAt;
    private String reminderOption;
    private OffsetDateTime reminderAt;
    private String checklist;
    private Boolean isCompleted;
    private Boolean isSnoozed;
    private String priority;
    private String assignmentMode;
}
