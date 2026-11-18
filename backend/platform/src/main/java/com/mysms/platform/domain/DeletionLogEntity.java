package com.mysms.platform.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of platform.deletion_log. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class DeletionLogEntity extends BaseEntity {

    public static final String TABLE = "platform.deletion_log";

    private UUID tenantId;
    private String entityType;
    private UUID entityId;
    private UUID cohortId;
    private OffsetDateTime deletedAtTs;
}
