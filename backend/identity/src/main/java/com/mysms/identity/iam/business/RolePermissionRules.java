package com.mysms.identity.iam.business;

import com.mysms.identity.iam.domain.RolePermissionEntity;
import org.springframework.stereotype.Component;

/** A role's permissions: only permissions the caller holds tenant-wide, never on a system role. */
@Component
public class RolePermissionRules {

    private final AccessRules access;

    public RolePermissionRules(AccessRules access) {
        this.access = access;
    }

    public void beforeWrite(RolePermissionEntity link) {
        access.requireNotSystemRole(link.getRoleId());
        access.requireTenantWide(access.permissionCode(link.getPermissionId()));
    }

    public void beforeUpdate(RolePermissionEntity current) {
        beforeWrite(current);
    }

    public void beforeDelete(RolePermissionEntity current) {
        beforeWrite(current);
    }

    public void afterDelete(RolePermissionEntity deleted) {
        // the permission disappears from holders' tokens at their next refresh
    }
}
