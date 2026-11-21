package com.mysms.authclient;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The identity of the current request, the only auth type business code ever sees. Built by the edge from a verified
 * token; read-only.
 *
 * @param userId              the signed-in user account (`sub`)
 * @param tenantId            the tenant (`tid`); empty for platform staff
 * @param institutionIds      institutions the caller's roles are scoped to
 * @param activeInstitutionId the institution selected with `X-Institution-Id`, validated against the scopes
 * @param permissions         effective permission codes: for a tenant user resolved at the edge from the role
 *                            claims (JAVA-CODING-RULES §16.2); for platform staff and services the token's `perms`
 * @param tokenId             the token id (`jti`), for audit and revocation
 * @param impersonatorId      the administrator acting as this user (`act.sub`), when impersonating
 * @param platformOperator    true for platform staff tokens (audience mysms-platform-admin, MFA)
 * @param personId            the person record linked to the account (`pid`), when there is one
 * @param sessionId           the sign-in session the token belongs to (`sid`), when the issuer sets one
 * @param allInstitutions     a tenant-wide grant covers every institution of the tenant (`ins` contains `*`)
 * @param tenantWidePermissions the permissions held through tenant-wide (or direct) grants (`tperms`, compacted): the
 *                            only ones that reach every institution and tenant-level data; the rest are as wide as the
 *                            institution grants that carry them
 * @param service             the calling module when the caller is a service principal ({@code sub} =
 *                            {@code service:<module>}, audience {@code mysms-internal}); empty for people
 */
public record AuthContext(UUID userId, Optional<UUID> tenantId, Set<UUID> institutionIds,
                          Optional<UUID> activeInstitutionId, Set<String> permissions, String tokenId,
                          Optional<UUID> impersonatorId, boolean platformOperator, Optional<UUID> personId,
                          Optional<UUID> sessionId, boolean allInstitutions, Set<String> tenantWidePermissions,
                          Optional<String> service) {

    /** The prefix of a service principal's {@code sub}: {@code service:<module>}. */
    public static final String SERVICE_SUBJECT_PREFIX = "service:";

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
        service = service == null ? Optional.empty() : service;
    }

    /** A person's identity (not a service principal). */
    public AuthContext(UUID userId, Optional<UUID> tenantId, Set<UUID> institutionIds, Optional<UUID> activeInstitutionId,
                       Set<String> permissions, String tokenId, Optional<UUID> impersonatorId, boolean platformOperator,
                       Optional<UUID> personId, Optional<UUID> sessionId, boolean allInstitutions,
                       Set<String> tenantWidePermissions) {
        this(userId, tenantId, institutionIds, activeInstitutionId, permissions, tokenId, impersonatorId,
                platformOperator, personId, sessionId, allInstitutions, tenantWidePermissions, Optional.empty());
    }

    /** A caller whose tenant-wide grants carry all of its permissions (or none, without a tenant-wide grant). */
    public AuthContext(UUID userId, Optional<UUID> tenantId, Set<UUID> institutionIds, Optional<UUID> activeInstitutionId,
                       Set<String> permissions, String tokenId, Optional<UUID> impersonatorId, boolean platformOperator,
                       Optional<UUID> personId, Optional<UUID> sessionId, boolean allInstitutions) {
        this(userId, tenantId, institutionIds, activeInstitutionId, permissions, tokenId, impersonatorId,
                platformOperator, personId, sessionId, allInstitutions, allInstitutions ? permissions : Set.of());
    }

    /**
     * A module's service principal (iam's client-credentials token): it acts tenant-wide with the permissions its
     * client was granted, in no tenant until a call carries one ({@link #withTenant}). Its user id is a fixed
     * name-based UUID of {@code service:<module>}, so audit columns name the calling module consistently.
     */
    public static AuthContext service(String module, Set<String> permissions, String tokenId) {
        return new AuthContext(serviceUserId(module), Optional.empty(), Set.of(), Optional.empty(), permissions,
                tokenId, Optional.empty(), false, Optional.empty(), Optional.empty(), true, permissions,
                Optional.of(module));
    }

    /** The user id that stands for a module's service principal in audit columns. */
    public static UUID serviceUserId(String module) {
        return UUID.nameUUIDFromBytes((SERVICE_SUBJECT_PREFIX + module).getBytes(StandardCharsets.UTF_8));
    }

    /** True for a service principal. */
    public boolean isService() {
        return service.isPresent();
    }

    /** The same identity acting in the given (already validated) institution. */
    public AuthContext withActiveInstitution(UUID institution) {
        return new AuthContext(userId, tenantId, institutionIds, Optional.of(institution), permissions, tokenId,
                impersonatorId, platformOperator, personId, sessionId, allInstitutions, tenantWidePermissions, service);
    }

    /** A service principal acting in the tenant its call carries (the edge accepts it only from services). */
    public AuthContext withTenant(UUID tenant) {
        if (!isService()) {
            throw new IllegalStateException("only a service principal takes its tenant from the call");
        }
        return new AuthContext(userId, Optional.of(tenant), institutionIds, activeInstitutionId, permissions, tokenId,
                impersonatorId, platformOperator, personId, sessionId, allInstitutions, tenantWidePermissions, service);
    }

    /**
     * The same identity with the permissions the edge resolved from the token's role claims (JAVA-CODING-RULES
     * §16.2), replacing whatever the token listed itself.
     */
    public AuthContext withPermissions(Set<String> resolved, Set<String> resolvedTenantWide) {
        return new AuthContext(userId, tenantId, institutionIds, activeInstitutionId, resolved, tokenId,
                impersonatorId, platformOperator, personId, sessionId, allInstitutions, resolvedTenantWide, service);
    }

    /** Whether the caller may act in this institution (the edge checks X-Institution-Id with it). */
    public boolean coversInstitution(UUID institution) {
        return allInstitutions || institutionIds.contains(institution);
    }

    /**
     * True when the code is granted. The issuer compacts complete sets ({@code *}, {@code <module>.*},
     * {@code <module>.<resource>.*}), so a broad role still fits in a request header.
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

    /** The current caller's role claims, when the token carried them (iam's role-data service reads them). */
    public static Optional<RoleClaims> currentRoleClaims() {
        return AuthContextHolder.roleClaims();
    }

    /** The current identity; the edge filter guarantees one on every non-public operation. */
    public static AuthContext require() {
        return current().orElseThrow(() -> new IllegalStateException("no authenticated caller on this thread"));
    }
}
