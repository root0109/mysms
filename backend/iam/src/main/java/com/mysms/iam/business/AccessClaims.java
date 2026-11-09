package com.mysms.iam.business;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * What an access token says about the caller (api/README "Claims"). Built by the sign-in flow from the database;
 * the issuer only signs it.
 *
 * @param userId       `sub`
 * @param tenantId     `tid`
 * @param personId     `pid`, when the account is linked to a person record
 * @param sessionId    `sid`: the sign-in session (refresh family) the token belongs to
 * @param permissions  `perms`, already compacted
 * @param institutions `ins`: institutions the caller's grants are scoped to
 * @param allInstitutions a tenant-wide grant: `ins` carries `*`, every institution of the tenant
 * @param roles        `roles`: role code with scope, e.g. {@code teacher@department:<uuid>} or {@code admin@tenant}
 * @param tenantWidePermissions `tperms`, compacted: the permissions carried by tenant-wide or direct grants — the only
 *                     ones the data scope lets reach every institution and tenant-level rows
 */
public record AccessClaims(UUID userId, UUID tenantId, Optional<UUID> personId, UUID sessionId,
                           Set<String> permissions, Set<UUID> institutions, List<String> roles,
                           boolean allInstitutions, Set<String> tenantWidePermissions) {

    public AccessClaims {
        personId = personId == null ? Optional.empty() : personId;
        permissions = Set.copyOf(permissions);
        institutions = Set.copyOf(institutions);
        roles = List.copyOf(roles);
        tenantWidePermissions = Set.copyOf(tenantWidePermissions);
    }
}
