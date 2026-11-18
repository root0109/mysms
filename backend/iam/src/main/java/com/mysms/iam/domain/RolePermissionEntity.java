package com.mysms.iam.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of iam.role_permission. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class RolePermissionEntity extends BaseEntity {

    public static final String TABLE = "iam.role_permission";

    private UUID tenantId;
    private UUID roleId;
    private UUID permissionId;
}
