package com.mysms.iam.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.iam.repository.AccessRuleRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * The no-escalation rules of access management: nobody hands out a permission they do not
 * hold themselves, at a reach they do not have, and nobody changes their own access. Shared by the role, role-permission,
 * role-grant and direct-permission rules.
 */
@Component
public class AccessRules {

    static final String TENANT = "tenant";
    static final String INSTITUTION = "institution";

    private final AccessRuleRepository lookups;

    public AccessRules(AccessRuleRepository lookups) {
        this.lookups = lookups;
    }

    /** A permission that will apply tenant-wide (a role definition, a direct grant): the caller must hold it so. */
    void requireTenantWide(String code) {
        if (!AuthContext.require().hasTenantWidePermission(code)) {
            throw new PermissionDeniedException("You can only hand out permissions you hold tenant-wide (" + code + ").");
        }
    }

    /** The code of the catalogue permission a row refers to. */
    String permissionCode(UUID permissionId) {
        return lookups.permissionCode(permissionId)
                .orElseThrow(() -> new InvalidStateException("The permission does not exist."));
    }

    /** A system role (the installation's administrator role) never changes through the API. */
    void requireNotSystemRole(UUID roleId) {
        if (lookups.roleIsSystem(roleId).orElse(false)) {
            throw new InvalidStateException("System roles cannot be changed.");
        }
    }

    /**
     * Granting (or changing, or removing) a role at a scope: the caller must hold every permission of the role, and
     * for each one reach the scope — tenant-wide for the tenant scope and for units this module cannot place in an
     * institution, the institution itself for an institution scope.
     */
    void requireGrantable(UUID roleId, String scopeType, UUID scopeId) {
        AuthContext caller = AuthContext.require();
        List<String> codes = lookups.rolePermissionCodes(roleId);
        for (String code : codes) {
            if (!caller.hasPermission(code)) {
                throw new PermissionDeniedException("You can only grant roles whose permissions you hold (" + code + ").");
            }
            if (INSTITUTION.equals(scopeType) && scopeId != null) {
                DataScope.of(caller, code).requireWritable(Optional.of(scopeId));
            } else {
                requireTenantWide(code);
            }
        }
    }

    /**
     * What only the account holder may do, never an administrator acting as them (iam.StartImpersonation): their
     * password, second factors, terms, tokens that outlive the impersonation, and another impersonation.
     */
    public static void requireOwnIdentity() {
        if (AuthContext.require().impersonatorId().isPresent()) {
            throw new PermissionDeniedException("This is not possible while acting as another user.");
        }
    }

    /** Nobody widens, narrows or removes their own access: that is always someone else's decision. */
    void requireNotSelf(UUID userId) {
        if (AuthContext.require().userId().equals(userId)) {
            throw new PermissionDeniedException("You cannot change your own access.");
        }
    }
}
