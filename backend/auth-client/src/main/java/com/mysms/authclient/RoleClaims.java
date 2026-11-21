package com.mysms.authclient;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * What a person's token says to resolve their permissions from through iam's role data (JAVA-CODING-RULES §16.2): the
 * role grants ({@code rgr}), the direct grants ({@code dg}) and denies ({@code dd}), the tenant's authorisation
 * version the token was issued at ({@code azv}) and, for a limited personal API token, its abilities ({@code abl}).
 *
 * @param grants       the role grants; a tenant-wide one reaches every institution and tenant-level data
 * @param directGrants codes granted to the user directly (tenant-wide)
 * @param denies       codes denied to the user directly
 * @param version      the tenant's authorisation version at issue; a cached role older than it is fetched again
 * @param abilities    the only codes the token may use, when it is limited
 */
public record RoleClaims(List<RoleGrant> grants, Set<String> directGrants, Set<String> denies, long version,
                         Optional<Set<String>> abilities) {

    /** One role grant: the role and whether it is held tenant-wide ({@code <roleId>@tenant}). */
    public record RoleGrant(UUID roleId, boolean tenantWide) {

        private static final String TENANT = "@tenant";

        /** The claim form {@code <roleId>@tenant} (held tenant-wide) or {@code <roleId>}; anything else is invalid. */
        static RoleGrant parse(String claim) {
            boolean tenantWide = claim.endsWith(TENANT);
            return new RoleGrant(UUID.fromString(tenantWide ? claim.substring(0, claim.length() - TENANT.length())
                    : claim), tenantWide);
        }
    }

    public RoleClaims {
        grants = List.copyOf(grants);
        directGrants = Set.copyOf(directGrants);
        denies = Set.copyOf(denies);
        abilities = abilities == null ? Optional.empty() : abilities.map(Set::copyOf);
    }

    /** The roles of the grants, each once. */
    public Set<UUID> roleIds() {
        return grants.stream().map(RoleGrant::roleId).collect(Collectors.toUnmodifiableSet());
    }

    /** From the claims of a token; {@code roleGrants} in claim form. */
    public static RoleClaims of(List<String> roleGrants, List<String> directGrants, List<String> denies, long version,
                         Optional<List<String>> abilities) {
        return new RoleClaims(roleGrants.stream().map(RoleGrant::parse).toList(), Set.copyOf(directGrants),
                Set.copyOf(denies), version, abilities.map(Set::copyOf));
    }
}
