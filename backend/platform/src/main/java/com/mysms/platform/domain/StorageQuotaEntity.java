package com.mysms.platform.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of platform.storage_quota. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class StorageQuotaEntity extends BaseEntity {

    public static final String TABLE = "platform.storage_quota";

    private UUID tenantId;
    private UUID userId;
    private UUID institutionId;
    private Long quotaBytes;
    private Long usedBytes;
    private Boolean isActive;
}
