package com.mysms.privacy.domain;

import com.mysms.core.entity.BaseEntity;
import com.mysms.privacy.model.TaskResult;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A row of privacy.rights_request_task, a record of the rights request aggregate; the table has no soft delete (the
 * results are evidence), so deletedAt stays null.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class RightsRequestTaskEntity extends BaseEntity {

    public static final String TABLE = "privacy.rights_request_task";

    private UUID tenantId;
    private UUID requestId;
    private String module;
    private String action;
    private int wave;
    private String status;
    private int attempts;
    private OffsetDateTime dispatchedAt;
    private OffsetDateTime completedAt;
    private TaskResult result;
    private String lastErrorCode;
    private OffsetDateTime nextAttemptAt;
}
