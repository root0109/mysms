package com.mysms.identity.iam.business;

import com.mysms.authclient.RolePermissionSource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * The role data source of the process that serves iam (Strategy of {@link RolePermissionSource}): the permissions
 * filter reads iam directly instead of calling itself over gRPC — in the iam microservice and in the monolith, where
 * every module's filter shares it. The token is not needed here: the filter has already verified it, and the tenant
 * and roles come from its claims.
 */
@Component
public class LocalRolePermissions implements RolePermissionSource {

    private final AuthzService authz;

    public LocalRolePermissions(AuthzService authz) {
        this.authz = authz;
    }

    @Override
    public RolePermissions rolePermissions(UUID tenant, Set<UUID> roleIds, String bearerToken) {
        AuthzService.RoleAuthz answer = TenantCalls.as(tenant, null,
                () -> authz.rolePermissions(tenant, List.copyOf(roleIds)));
        Map<UUID, Set<String>> codesByRole = answer.roles().stream().collect(Collectors.toUnmodifiableMap(
                AuthzService.RoleCodes::roleId, role -> Set.copyOf(role.permissionCodes())));
        return new RolePermissions(answer.authzVersion(), codesByRole, Set.copyOf(answer.commonCodes()));
    }
}
