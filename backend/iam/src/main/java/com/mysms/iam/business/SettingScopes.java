package com.mysms.iam.business;

import com.mysms.authclient.AuthContext;
import com.mysms.core.paging.SqlFragment;
import com.mysms.iam.repository.GrantRepository;
import com.mysms.iam.repository.RoleGrantRow;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Data-level authorization of setting values (on top of the edge's x-permission check): a value at a scope may be
 * read or changed by a caller who holds the permission through a direct grant or a tenant-wide role grant, or a
 * role granted on exactly that unit. User-level values of other users need a tenant-wide grant.
 */
@Component
public class SettingScopes {

    private final GrantRepository grants;
    private final Clock clock;

    public SettingScopes(GrantRepository grants, Clock clock) {
        this.grants = grants;
        this.clock = clock;
    }

    /** Must run inside a transaction of the caller's tenant. */
    public boolean permitted(String permission, String scopeType, UUID scopeId) {
        UUID user = AuthContext.require().userId();
        String type = SettingRules.USER.equals(scopeType) ? SettingRules.TENANT : scopeType;
        UUID unit = SettingRules.USER.equals(scopeType) ? null : scopeId;
        return grants.permittedAt(user, permission, SettingRules.TENANT, null, today())
                || type.equals(scopeType) && grants.permittedAt(user, permission, type, unit, today());
    }

    /**
     * The scope types the caller may administer values at with this permission: every scope with a direct or
     * tenant-wide grant, otherwise the types of the units of the caller's scoped grants (user values always need a
     * tenant-wide grant). Must run inside a transaction of the caller's tenant.
     */
    public java.util.Set<String> administeredScopeTypes(String permission) {
        UUID user = AuthContext.require().userId();
        if (grants.permittedAt(user, permission, SettingRules.TENANT, null, today())) {
            return java.util.Set.copyOf(SettingRules.CHAIN);
        }
        java.util.Set<String> types = new java.util.HashSet<>();
        for (RoleGrantRow scope : grants.grantScopes(user, permission, today())) {
            if (scope.scopeId().isPresent() && SettingRules.CHAIN.contains(scope.scopeType())
                    && !SettingRules.USER.equals(scope.scopeType())) {
                types.add(scope.scopeType());
            }
        }
        return java.util.Set.copyOf(types);
    }

    /**
     * SQL restricting a list to the scopes the caller may read: TRUE with a tenant-wide grant, otherwise the units of
     * the caller's scoped grants (FALSE when there are none). Must run inside a transaction of the caller's tenant.
     */
    public SqlFragment readableFilter(String permission) {
        UUID user = AuthContext.require().userId();
        if (grants.permittedAt(user, permission, SettingRules.TENANT, null, today())) {
            return SqlFragment.TRUE;
        }
        List<String> terms = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();
        int i = 0;
        for (RoleGrantRow scope : grants.grantScopes(user, permission, today())) {
            if (scope.scopeId().isEmpty() || !SettingRules.CHAIN.contains(scope.scopeType())) {
                continue;
            }
            terms.add("(r.scope_type = :rsType" + i + " AND r.scope_id = :rsId" + i + ")");
            params.put("rsType" + i, scope.scopeType());
            params.put("rsId" + i, scope.scopeId().get());
            i++;
        }
        return terms.isEmpty() ? new SqlFragment("FALSE", Map.of()) : new SqlFragment(String.join(" OR ", terms), params);
    }

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
