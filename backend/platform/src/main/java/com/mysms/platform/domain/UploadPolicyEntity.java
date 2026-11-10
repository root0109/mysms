package com.mysms.platform.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of platform.upload_policy (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class UploadPolicyEntity extends BaseEntity {

    public static final String TABLE = "platform.upload_policy";

    private UUID tenantId;
    private UUID roleId;
    private String moduleCode;
    private String operationCode;
    private Long maxSizeBytes;
    private String allowedExtensions;
}
