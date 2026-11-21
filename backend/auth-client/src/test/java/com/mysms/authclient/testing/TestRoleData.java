package com.mysms.authclient.testing;

import com.mysms.authclient.RolePermissionResolver;
import com.mysms.authclient.RolePermissionSource;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The role data of component tests (Decorator of the process's {@link RolePermissionSource}): roles of
 * {@link TestTokens} tokens are answered from {@link TestRoles}; every other role, the common codes and the version
 * come from the real source when the process serves iam (its own database), and are absent otherwise — a module's
 * component tests run without iam. The permissions filter, its cache and the token verifier stay real; only the hop
 * to iam is replaced, and only in component tests.
 */
public final class TestRoleData implements RolePermissionSource {

    private final RolePermissionSource iam;

    /** @param iam the process's own iam source, or null where iam does not run */
    public TestRoleData(RolePermissionSource iam) {
        this.iam = iam;
    }

    /** The role cache of a unit test of the edge: test roles only, no iam. */
    public static RolePermissionResolver resolver() {
        return new RolePermissionResolver(new TestRoleData(null), Duration.ofMinutes(1));
    }

    /** The source this one is in front of; null where iam does not run. */
    public RolePermissionSource iam() {
        return iam;
    }

    @Override
    public RolePermissions rolePermissions(UUID tenant, Set<UUID> roleIds, String bearerToken) {
        Map<UUID, Set<String>> codesByRole = new HashMap<>();
        Set<UUID> others = new HashSet<>();
        for (UUID role : roleIds) {
            TestRoles.codes(role).ifPresentOrElse(codes -> codesByRole.put(role, codes), () -> others.add(role));
        }
        if (iam == null) {
            return new RolePermissions(0, codesByRole, Set.of());
        }
        RolePermissions real = iam.rolePermissions(tenant, others, bearerToken);
        codesByRole.putAll(real.codesByRole());
        return new RolePermissions(real.version(), codesByRole, real.commonCodes());
    }
}
