package com.mysms.privacy.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A row of privacy.retention_run; the table has no soft delete (runs are evidence), so deletedAt stays null.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class RetentionRunEntity extends BaseEntity {

    public static final String TABLE = "privacy.retention_run";

    private UUID tenantId;
    private UUID policyId;
    private String module;
    private String action;
    private OffsetDateTime cutoff;
    private String status;
    private Integer affected;
    private Integer held;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
}
