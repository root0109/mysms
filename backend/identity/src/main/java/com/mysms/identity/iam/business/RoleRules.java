package com.mysms.identity.iam.business;

import com.mysms.core.error.FieldError;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.identity.iam.domain.RoleEntity;
import java.util.List;
import org.springframework.stereotype.Component;

/** Roles: system roles are fixed, and no role becomes a system role through the API. */
@Component
public class RoleRules {

    private final AccessRules access;

    public RoleRules(AccessRules access) {
        this.access = access;
    }

    public void beforeWrite(RoleEntity role) {
        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new ValidationFailedException(List.of(new FieldError("isSystem", "only the installation creates system"
                    + " roles", "Immutable")));
        }
    }

    public void beforeUpdate(RoleEntity current) {
        access.requireNotSystemRole(current.getId());
    }

    public void beforeDelete(RoleEntity current) {
        access.requireNotSystemRole(current.getId());
    }

    public void afterDelete(RoleEntity deleted) {
        // nothing further: a role still granted to anyone cannot be deleted (live iam.user_role rows refuse it)
    }
}
