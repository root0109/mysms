package com.mysms.platform.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of platform.import_job: one bulk import and the running totals of its result (column for column). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ImportJobEntity extends BaseEntity {

    public static final String TABLE = "platform.import_job";

    private UUID tenantId;
    private UUID operationId;
    private String entityType;
    private UUID institutionId;
    private UUID fileId;
    private boolean dryRun;
    private int rowCount;
    private int batchCount;
    private int appliedBatchCount;
    private int createdCount;
    private int errorCount;
}
