package com.mysms.identity.iam.business;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * What an access token says about the caller. Built by the sign-in flow from the database; the issuer only signs it.
 *
 * @param userId       `sub`
 * @param tenantId     `tid`
 * @param personId     `pid`, when the account is linked to a person record
 * @param sessionId    `sid`: the sign-in session (refresh family) the token belongs to
 * @param permissions  the effective permissions, compacted; no longer signed into the token (the filter resolves
 *                     them from {@code authorization}), kept for what iam compares against the database
 * @param institutions `ins`: institutions the caller's grants are scoped to
 * @param allInstitutions a tenant-wide grant: `ins` carries `*`, every institution of the tenant
 * @param roles        `roles`: role code with scope, e.g. {@code teacher@department:<uuid>} or {@code admin@tenant}
 * @param tenantWidePermissions the permissions carried by tenant-wide or direct grants, compacted; like
 *                     {@code permissions}, not signed into the token
 * @param authorization what the permissions filter resolves the permissions from through iam's role cache (SEC-0)
 */
public record AccessClaims(UUID userId, UUID tenantId, Optional<UUID> personId, UUID sessionId,
                           Set<String> permissions, Set<UUID> institutions, List<String> roles,
                           boolean allInstitutions, Set<String> tenantWidePermissions, Authorization authorization) {

    public AccessClaims {
        personId = personId == null ? Optional.empty() : personId;
        permissions = Set.copyOf(permissions);
        institutions = Set.copyOf(institutions);
        roles = List.copyOf(roles);
        tenantWidePermissions = Set.copyOf(tenantWidePermissions);
    }

    /**
     * The role-based claims (JAVA-CODING-RULES §16.2): the filter's permissions are every role's codes (from iam's
     * role cache, the codes common to every role included) plus the direct grants, minus the denies, capped by the
     * abilities; its tenant-wide permissions are those of the tenant-scope role grants plus the direct grants, minus
     * the denies, capped likewise.
     *
     * @param roleGrants   `rgr`: the roles of the counted grants the token works with, each once:
     *                     {@code <roleId>@tenant} when any of its grants is tenant-wide, else {@code <roleId>}
     * @param directGrants `dg`: codes granted to the user directly (none while working in one switched role)
     * @param denies       `dd`: codes denied to the user directly; non-modifiable codes are never denied
     * @param version      `azv`: the tenant's authorisation version the claims were read at
     * @param abilities    `abl`: the only codes a personal API token may use, when it is limited
     */
    public record Authorization(List<String> roleGrants, List<String> directGrants, List<String> denies, long version,
                                Optional<Set<String>> abilities) {

        public Authorization {
            roleGrants = List.copyOf(roleGrants);
            directGrants = List.copyOf(directGrants);
            denies = List.copyOf(denies);
            abilities = abilities == null ? Optional.empty() : abilities.map(Set::copyOf);
        }

        /** A role in its claim form. */
        public static String roleGrant(UUID roleId, boolean tenantWide) {
            return tenantWide ? roleId + "@tenant" : roleId.toString();
        }
    }
}
