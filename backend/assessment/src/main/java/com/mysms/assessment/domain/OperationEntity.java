package com.mysms.assessment.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of assessment.operation: one long-running job of this module. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class OperationEntity extends BaseEntity {

    public static final String TABLE = "assessment.operation";

    private UUID tenantId;
    private String operationName;
    private String resourceType;
    private UUID resourceId;
    private String status;
    private Integer progressPercent;
    private String errorCode;
    private String errorMessage;
    private String resultEntityType;
    private UUID resultEntityId;
    private UUID startedByUserId;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
}
