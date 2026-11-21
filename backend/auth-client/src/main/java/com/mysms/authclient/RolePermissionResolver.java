package com.mysms.authclient;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves a person's permissions from their token's role claims (JAVA-CODING-RULES §16.2), the way the reference
 * access-control service's filter does: each role's codes are looked up per (tenant, role) in a per-process Caffeine
 * cache and fetched from iam ({@link RolePermissionSource}) on a miss. An entry is fetched again once it is older
 * than the token's authorisation version, so a token issued after a role change never sees the old codes; any other
 * change shows within the TTL, as do global changes no tenant version records (a code made common, deleted or
 * retired). There is no invalidation event. While iam cannot answer, whatever the cache lacks is refused (503), so an
 * iam outage longer than the TTL refuses every person's call: fail closed by design (DEPLOYMENT §5.10).
 *
 * <p>Permissions are the codes every role holds, each role's codes and the direct grants, minus the denies;
 * tenant-wide permissions are the codes of the tenant-wide role grants and the direct grants, minus the denies (the
 * common codes are not tenant-wide). A limited API token's abilities cap both. When iam cannot answer what the cache
 * lacks, the source's {@link com.mysms.core.error.UnavailableException} reaches the edge, which refuses the call.
 */
public final class RolePermissionResolver {

    /** Entries kept per process: far more roles than a process serves in one TTL. */
    static final long MAXIMUM_ENTRIES = 100_000;

    private record RoleKey(UUID tenant, UUID role) {
    }

    private record Codes(long version, Set<String> codes) {
    }

    /** Permissions and tenant-wide permissions, as {@link AuthContext} holds them. */
    public record Resolved(Set<String> permissions, Set<String> tenantWidePermissions) {

        public Resolved {
            permissions = Set.copyOf(permissions);
            tenantWidePermissions = Set.copyOf(tenantWidePermissions);
        }
    }

    private final RolePermissionSource source;
    private final Cache<RoleKey, Codes> roles;
    private final Cache<UUID, Codes> common;

    public RolePermissionResolver(RolePermissionSource source, Duration ttl) {
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("mysms.auth.role-cache-ttl must be positive");
        }
        this.source = source;
        this.roles = Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(MAXIMUM_ENTRIES).build();
        this.common = Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(MAXIMUM_ENTRIES).build();
    }

    /** The caller's permissions in {@code tenant}; fetches from iam only what the cache lacks or holds too old. */
    public Resolved resolve(UUID tenant, RoleClaims claims, String bearerToken) {
        Map<UUID, Set<String>> codesByRole = new HashMap<>();
        Set<UUID> missing = new HashSet<>();
        for (UUID role : claims.roleIds()) {
            Codes cached = roles.getIfPresent(new RoleKey(tenant, role));
            if (outdated(cached, claims.version())) {
                missing.add(role);
            } else {
                codesByRole.put(role, cached.codes());
            }
        }
        Codes commonCodes = common.getIfPresent(tenant);
        if (!missing.isEmpty() || outdated(commonCodes, claims.version())) {
            RolePermissionSource.RolePermissions fetched = source.rolePermissions(tenant, missing, bearerToken);
            for (UUID role : missing) {
                Set<String> codes = fetched.codesByRole().getOrDefault(role, Set.of());
                roles.asMap().merge(new RoleKey(tenant, role), new Codes(fetched.version(), codes),
                        RolePermissionResolver::newer);
                codesByRole.put(role, codes);
            }
            commonCodes = new Codes(fetched.version(), fetched.commonCodes());
            common.asMap().merge(tenant, commonCodes, RolePermissionResolver::newer);
        }
        Set<String> permissions = new HashSet<>(commonCodes.codes());
        Set<String> tenantWide = new HashSet<>();
        for (RoleClaims.RoleGrant grant : claims.grants()) {
            Set<String> codes = codesByRole.get(grant.roleId());
            permissions.addAll(codes);
            if (grant.tenantWide()) {
                tenantWide.addAll(codes);
            }
        }
        permissions.addAll(claims.directGrants());
        tenantWide.addAll(claims.directGrants());
        permissions.removeAll(claims.denies());
        tenantWide.removeAll(claims.denies());
        claims.abilities().ifPresent(abilities -> {
            permissions.retainAll(abilities);
            tenantWide.retainAll(abilities);
        });
        return new Resolved(permissions, tenantWide);
    }

    /** Of two answers racing into the cache, the one read at the newer version stays. */
    private static Codes newer(Codes cached, Codes fetched) {
        return fetched.version() >= cached.version() ? fetched : cached;
    }

    private static boolean outdated(Codes cached, long version) {
        return cached == null || cached.version() < version;
    }
}
