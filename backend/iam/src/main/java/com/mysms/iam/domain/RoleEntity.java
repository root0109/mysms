package com.mysms.iam.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of iam.role (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class RoleEntity extends BaseEntity {

    public static final String TABLE = "iam.role";

    private UUID tenantId;
    private String code;
    private String displayName;
    private String description;
    private Boolean isSystem;
    private Boolean isActive;
    private Integer authorityLevel;
    private UUID parentRoleId;
}
