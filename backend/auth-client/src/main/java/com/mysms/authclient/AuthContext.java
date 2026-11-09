package com.mysms.authclient;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The identity of the current request — the only auth type business code ever sees (JAVA-CODING-RULES §16 X1).
 * Built by the edge filter from a verified token; read-only.
 *
 * @param userId              the signed-in user account (`sub`)
 * @param tenantId            the tenant (`tid`); empty for platform staff
 * @param institutionIds      institutions the caller's roles are scoped to
 * @param activeInstitutionId the institution selected with `X-Institution-Id`, validated against the scopes
 * @param permissions         effective permission codes (`perms`)
 * @param tokenId             the token id (`jti`), for audit and revocation
 * @param impersonatorId      the administrator acting as this user (`act.sub`), when impersonating
 * @param platformOperator    true for platform staff tokens (audience mysms-platform-admin, MFA)
 * @param personId            the person record linked to the account (`pid`), when there is one
 * @param sessionId           the sign-in session the token belongs to (`sid`), when the issuer sets one
 * @param allInstitutions     a tenant-wide grant covers every institution of the tenant (`ins` contains `*`)
 * @param tenantWidePermissions the permissions held through tenant-wide (or direct) grants (`tperms`, compacted): the
 *                            only ones that reach every institution and tenant-level data; the rest are as wide as the
 *                            institution grants that carry them
 */
public record AuthContext(UUID userId, Optional<UUID> tenantId, Set<UUID> institutionIds,
                          Optional<UUID> activeInstitutionId, Set<String> permissions, String tokenId,
                          Optional<UUID> impersonatorId, boolean platformOperator, Optional<UUID> personId,
                          Optional<UUID> sessionId, boolean allInstitutions, Set<String> tenantWidePermissions) {

    /** Wildcard permission: every permission (a compacted complete set). */
    public static final String ALL = "*";

    public AuthContext {
        tenantId = tenantId == null ? Optional.empty() : tenantId;
        activeInstitutionId = activeInstitutionId == null ? Optional.empty() : activeInstitutionId;
        impersonatorId = impersonatorId == null ? Optional.empty() : impersonatorId;
        personId = personId == null ? Optional.empty() : personId;
        sessionId = sessionId == null ? Optional.empty() : sessionId;
        institutionIds = Set.copyOf(institutionIds);
        permissions = Set.copyOf(permissions);
        tenantWidePermissions = tenantWidePermissions == null ? Set.of() : Set.copyOf(tenantWidePermissions);
    }

    /** A caller whose tenant-wide grants carry all of its permissions (or none, without a tenant-wide grant). */
    public AuthContext(UUID userId, Optional<UUID> tenantId, Set<UUID> institutionIds, Optional<UUID> activeInstitutionId,
                       Set<String> permissions, String tokenId, Optional<UUID> impersonatorId, boolean platformOperator,
                       Optional<UUID> personId, Optional<UUID> sessionId, boolean allInstitutions) {
        this(userId, tenantId, institutionIds, activeInstitutionId, permissions, tokenId, impersonatorId,
                platformOperator, personId, sessionId, allInstitutions, allInstitutions ? permissions : Set.of());
    }

    /** The same identity acting in the given (already validated) institution. */
    public AuthContext withActiveInstitution(UUID institution) {
        return new AuthContext(userId, tenantId, institutionIds, Optional.of(institution), permissions, tokenId,
                impersonatorId, platformOperator, personId, sessionId, allInstitutions, tenantWidePermissions);
    }

    /** Whether the caller may act in this institution (the edge checks X-Institution-Id with it). */
    public boolean coversInstitution(UUID institution) {
        return allInstitutions || institutionIds.contains(institution);
    }

    /**
     * True when the code is granted. The issuer compacts complete sets (JAVA-CODING-RULES §16): {@code *} is every
     * permission, {@code <module>.*} every permission of a module and {@code <module>.<resource>.*} every action on a
     * resource, so a broad role still fits in a request header.
     */
    public boolean hasPermission(String code) {
        return covers(permissions, code);
    }

    /** True when a tenant-wide (or direct) grant carries the code, so it reaches every institution. */
    public boolean hasTenantWidePermission(String code) {
        return allInstitutions && covers(tenantWidePermissions, code);
    }

    private static boolean covers(Set<String> permissions, String code) {
        if (permissions.contains(code) || permissions.contains(ALL)) {
            return true;
        }
        for (int dot = code.indexOf('.'); dot > 0; dot = code.indexOf('.', dot + 1)) {
            if (permissions.contains(code.substring(0, dot + 1) + "*")) {
                return true;
            }
        }
        return false;
    }

    /** The current request's identity, when the request carried a valid token. */
    public static Optional<AuthContext> current() {
        return AuthContextHolder.current();
    }

    /** How the current request's token was obtained (`amr`: pwd, mfa); empty without a token. */
    public static Set<String> currentAuthMethods() {
        return AuthContextHolder.authMethods();
    }

        /** The current identity; the edge filter guarantees one on every non-public operation. */
    public static AuthContext require() {
        return current().orElseThrow(() -> new IllegalStateException("no authenticated caller on this thread"));
    }
}
