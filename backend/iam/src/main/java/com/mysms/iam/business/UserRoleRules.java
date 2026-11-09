package com.mysms.iam.business;

import com.mysms.iam.domain.UserRoleEntity;
import org.springframework.stereotype.Component;

/** Role grants: no escalation (AccessRules#requireGrantable) and never one's own grants. */
@Component
public class UserRoleRules {

    private final AccessRules access;

    public UserRoleRules(AccessRules access) {
        this.access = access;
    }

    public void beforeWrite(UserRoleEntity grant) {
        access.requireNotSelf(grant.getUserId());
        access.requireGrantable(grant.getRoleId(), grant.getScopeType(), grant.getScopeId());
    }

    /** The grant as it stands must also be one the caller could have made. */
    public void beforeUpdate(UserRoleEntity current) {
        beforeWrite(current);
    }

    public void beforeDelete(UserRoleEntity current) {
        beforeWrite(current);
    }

    public void afterDelete(UserRoleEntity deleted) {
        // the user's permissions shrink at their next token refresh
    }
}
