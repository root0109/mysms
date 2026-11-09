package com.mysms.iam.business;

import com.mysms.iam.domain.UserPermissionEntity;
import org.springframework.stereotype.Component;

/** Direct grants and denials apply tenant-wide: only for permissions the caller holds tenant-wide, never one's own. */
@Component
public class UserPermissionRules {

    private final AccessRules access;

    public UserPermissionRules(AccessRules access) {
        this.access = access;
    }

    public void beforeWrite(UserPermissionEntity direct) {
        access.requireNotSelf(direct.getUserId());
        access.requireTenantWide(access.permissionCode(direct.getPermissionId()));
    }

    public void beforeUpdate(UserPermissionEntity current) {
        beforeWrite(current);
    }

    public void beforeDelete(UserPermissionEntity current) {
        beforeWrite(current);
    }

    public void afterDelete(UserPermissionEntity deleted) {
        // the user's permissions change at their next token refresh
    }
}
