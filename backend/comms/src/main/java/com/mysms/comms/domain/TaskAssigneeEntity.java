package com.mysms.comms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of comms.task_assignee. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class TaskAssigneeEntity extends BaseEntity {

    public static final String TABLE = "comms.task_assignee";

    private UUID tenantId;
    private UUID taskId;
    private String assigneeType;
    private UUID userId;
    private UUID classSectionId;
    private UUID groupId;
    private UUID claimedByUserId;
    private String status;
}
