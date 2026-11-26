package com.mysms.identity.iam.domain;

import com.mysms.core.entity.BaseEntity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of the global permission catalogue iam.permission (maintained by migrations, read-only through the API). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PermissionEntity extends BaseEntity {

    public static final String TABLE = "iam.permission";

    private String code;
    private String displayName;
    private String description;
    private String moduleCode;
    private Boolean isNavigation;
    private Boolean isModifiable;
    private Boolean appliesToAllRoles;
    private Boolean isObjectLevel;
}
