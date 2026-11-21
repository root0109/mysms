package com.mysms.authclient;

import com.mysms.core.error.UnavailableException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Where the permissions filter fetches role data from when its cache cannot answer (Strategy): iam itself in the
 * process that serves iam (the monolith included), iam's {@code IamAuthzService} over gRPC everywhere else.
 */
public interface RolePermissionSource {

    /**
     * The codes of the given roles of one tenant and the codes every role holds, read together with the tenant's
     * authorisation version.
     *
     * @param bearerToken the caller's own access token: the source answers only about the caller's tenant and roles
     * @throws UnavailableException when iam cannot answer; the filter then refuses the call (fail closed)
     */
    RolePermissions rolePermissions(UUID tenant, Set<UUID> roleIds, String bearerToken);

    /**
     * One answer of the source.
     *
     * @param version     the tenant's authorisation version the answer was read at
     * @param codesByRole each live role asked about that has codes of its own; a role absent here contributes nothing
     * @param commonCodes the codes every role holds (self-service)
     */
    record RolePermissions(long version, Map<UUID, Set<String>> codesByRole, Set<String> commonCodes) {

        public RolePermissions {
            codesByRole = Map.copyOf(codesByRole.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> Set.copyOf(e.getValue()))));
            commonCodes = Set.copyOf(commonCodes);
        }
    }
}
