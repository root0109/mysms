package com.mysms.identity.iam.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.RoleClaims;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.identity.iam.config.IamDatabaseConfig;
import com.mysms.identity.iam.repository.RoleAuthzRepository;
import com.mysms.identity.iam.repository.RolePermissionCodeRow;
import com.mysms.identity.iam.repository.TenantAuthzVersionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * What the permissions filter of every service resolves its role-to-permission cache from
 * (JAVA-CODING-RULES §16.2): the permission codes of a batch of roles and the tenant's authorisation version, read
 * together so a cache entry and the version it was read at always belong to each other. Served over gRPC
 * (iam_authz.proto, {@link #callerRolePermissions}: the caller's own tenant and roles only) and, in the process that
 * serves iam, read directly by the filter ({@code LocalRolePermissions}).
 */
@Service
public class AuthzService extends AppTransactionManager {

    /** More roles than a user can hold (iam.SetUserRoles allows 100 grants), so one call always suffices. */
    public static final int MAX_ROLES = 100;

    private final RoleAuthzRepository roles;
    private final TenantAuthzVersionRepository versions;

    public AuthzService(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(IamDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            RoleAuthzRepository roles, TenantAuthzVersionRepository versions) {
        super(database, transactions);
        this.roles = roles;
        this.versions = versions;
    }

    /** A role's own permission codes (the codes every role holds come once, in {@link RoleAuthz#commonCodes}). */
    public record RoleCodes(UUID roleId, List<String> permissionCodes) {

        public RoleCodes {
            permissionCodes = List.copyOf(permissionCodes);
        }
    }

    /**
     * The answer of one lookup: the roles that could be resolved, the codes every role holds implicitly, and the
     * version they were read at. The common codes come separately so a caller without any role grant still gets them,
     * and so they are never taken for a tenant-wide role's own codes.
     */
    public record RoleAuthz(long authzVersion, List<RoleCodes> roles, List<String> commonCodes) {

        public RoleAuthz {
            roles = List.copyOf(roles);
            commonCodes = List.copyOf(commonCodes);
        }
    }

    /**
     * {@link #rolePermissions} for a caller over iam's gRPC edge: only roles the caller's own token holds, so no user
     * learns what another role of the tenant may do.
     */
    public RoleAuthz callerRolePermissions(UUID tenantId, List<UUID> roleIds) {
        Set<UUID> held = AuthContext.currentRoleClaims().map(RoleClaims::roleIds).orElse(Set.of());
        if (!held.containsAll(roleIds)) {
            throw new PermissionDeniedException("Role data is readable for the caller's own roles only.");
        }
        return rolePermissions(tenantId, roleIds);
    }

    /**
     * The permissions of the given roles of {@code tenantId}; roles that are deleted, inactive or another tenant's
     * are left out of the answer, so a cache built from it can only narrow what a caller may do, and so are live roles
     * without codes of their own (they add nothing to the common codes).
     */
    public RoleAuthz rolePermissions(UUID tenantId, List<UUID> roleIds) {
        UUID caller = CallContextHolder.require().tenantId().orElseThrow();
        if (!caller.equals(tenantId)) {
            throw new PermissionDeniedException("Role permissions are readable in the caller's own tenant only.");
        }
        if (roleIds.size() > MAX_ROLES) {
            throw new InvalidArgumentException("Ask about at most " + MAX_ROLES + " roles per call.");
        }
        UUID[] wanted = roleIds.stream().distinct().toArray(UUID[]::new);
        return doInPrimaryReadTransaction(() -> {
            // the primary, never the replica: a filter that admits a caller must not decide on a lagging read
            return new RoleAuthz(versions.current(), group(roles.codesOfRoles(wanted)), roles.commonCodes());
        });
    }

    /** The rows, ordered by role and code, folded into one entry per role. */
    private static List<RoleCodes> group(List<RolePermissionCodeRow> rows) {
        List<RoleCodes> grouped = new ArrayList<>();
        UUID current = null;
        List<String> codes = new ArrayList<>();
        for (RolePermissionCodeRow row : rows) {
            if (!row.roleId().equals(current)) {
                if (current != null) {
                    grouped.add(new RoleCodes(current, codes));
                }
                current = row.roleId();
                codes = new ArrayList<>();
            }
            codes.add(row.code());
        }
        if (current != null) {
            grouped.add(new RoleCodes(current, codes));
        }
        return grouped;
    }
}
