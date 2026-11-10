package com.mysms.iam.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of iam.user_permission (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class UserPermissionEntity extends BaseEntity {

    public static final String TABLE = "iam.user_permission";

    private UUID tenantId;
    private UUID userId;
    private UUID permissionId;
    private String effect;
}
