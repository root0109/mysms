package com.mysms.authclient;

import com.mysms.core.error.UnavailableException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/**
 * The edge's whole authentication and authorization decision for one call, shared by {@link AuthFilter} and
 * {@link GrpcAuthInterceptor} so REST and gRPC can never disagree. In order:
 * <ol>
 * <li>an operation whose contract gives it an {@code Authorization} header of its own (a provider's signed callback)
 * proceeds without an identity: the header is the operation's to verify, never read as a token here — no other
 * operation is admitted this way;</li>
 * <li>an {@code Authorization} value must be a bearer token, verified locally — else 401;</li>
 * <li>without a token, only an operation callable without one proceeds — else 401; such a guest may name the school
 * it reads with {@code X-Tenant-Code}, which the module's {@link TenantCodes} resolve to the active tenant bound for
 * the call — an unknown, suspended or closed school is 404, an identity service that cannot answer 503; without a
 * resolver in the module the code is not read;</li>
 * <li>the token's kind must suit the operation's audience: a service token only on {@code internal} operations, a
 * person's token never on them — else 403;</li>
 * <li>a person's token must suit the operation's scheme: tenant users ({@code bearerAuth}) or platform staff
 * ({@code platformAdminAuth}) — else 403;</li>
 * <li>{@code X-Institution-Id}, when sent, must lie within the caller's scopes — else 403 (400 if not a UUID);</li>
 * <li>a service call may name its tenant with {@code X-Tenant-Id} (400 if not a UUID); a person's tenant is always
 * the token's, so the header is ignored for people;</li>
 * <li>the operation's permission must be granted — else 403.</li>
 * </ol>
 */
public final class EdgeAuthorizer {

    public static final String BEARER_PREFIX = "Bearer ";
    private static final String PERMISSION_DENIED = "PERMISSION_DENIED";
    private static final String INVALID_ARGUMENT = "INVALID_ARGUMENT";

    private final TokenVerifier verifier;
    private final PermissionChecker permissions;
    private final TenantCodes tenantCodes;

    public EdgeAuthorizer(TokenVerifier verifier, PermissionChecker permissions) {
        this(verifier, permissions, null);
    }

    /** {@code tenantCodes} is null when the module serves no guest by school code. */
    public EdgeAuthorizer(TokenVerifier verifier, PermissionChecker permissions, TenantCodes tenantCodes) {
        this.verifier = verifier;
        this.permissions = permissions;
        this.tenantCodes = tenantCodes;
    }

    /** {@link #decide(OperationRule, String, String, String, String)} for a call that names no school code. */
    public EdgeDecision decide(OperationRule rule, String authorization, String institution, String tenant) {
        return decide(rule, authorization, institution, tenant, null);
    }

    /**
     * @param authorization the {@code Authorization} header / {@code authorization} metadata, or null
     * @param institution   the {@code X-Institution-Id} header / {@code x-institution-id} metadata, or null
     * @param tenant        the {@code X-Tenant-Id} header / {@code x-tenant-id} metadata, or null
     * @param tenantCode    the {@code X-Tenant-Code} header / {@code x-tenant-code} metadata, or null
     */
    public EdgeDecision decide(OperationRule rule, String authorization, String institution, String tenant,
                               String tenantCode) {
        if (rule.ownAuthorization()) {
            return new EdgeDecision.Admitted(Optional.empty(), null, Set.of());
        }
        if (authorization == null) {
            if (!rule.isPublic() || rule.isInternal()) {
                return unauthenticated("A bearer token is required.");
            }
            return tenantCode == null || tenantCodes == null
                    ? new EdgeDecision.Admitted(Optional.empty(), null, Set.of())
                    : guest(tenantCode);
        }
        if (!authorization.startsWith(BEARER_PREFIX) || authorization.length() == BEARER_PREFIX.length()) {
            return unauthenticated("Authorization must be a bearer token.");
        }
        String raw = authorization.substring(BEARER_PREFIX.length()).trim();
        VerifiedToken token;
        try {
            token = verifier.verify(raw);
        } catch (InvalidTokenException e) {
            return new EdgeDecision.Refused(HttpStatus.UNAUTHORIZED, e.code(), e.getMessage(), true);
        }
        AuthContext context = token.context();
        if (rule.isInternal() != context.isService()) {
            return forbidden(context.isService() ? "This operation is not available to service principals."
                    : "This operation is only available to MySMS services.");
        }
        if (!rule.isPublic() && !context.isService() && !schemeAllows(rule, token)) {
            return forbidden("This operation is not available to this kind of account.");
        }
        if (institution != null) {
            Optional<UUID> selected = uuid(institution);
            if (selected.isEmpty()) {
                return new EdgeDecision.Refused(HttpStatus.BAD_REQUEST, INVALID_ARGUMENT,
                        AuthFilter.INSTITUTION_HEADER + " is not a UUID.", false);
            }
            if (!context.coversInstitution(selected.get())) {
                return forbidden("The institution is outside the caller's scope.");
            }
            context = context.withActiveInstitution(selected.get());
        }
        if (tenant != null && context.isService()) {
            Optional<UUID> named = uuid(tenant);
            if (named.isEmpty()) {
                return new EdgeDecision.Refused(HttpStatus.BAD_REQUEST, INVALID_ARGUMENT,
                        AuthFilter.TENANT_HEADER + " is not a UUID.", false);
            }
            context = context.withTenant(named.get());
        }
        if (!rule.isPublic() && !permissions.isPermitted(context, rule.permission())) {
            return forbidden("Missing permission " + rule.permission() + ".");
        }
        return new EdgeDecision.Admitted(Optional.of(context), raw, token.authMethods());
    }

    /** A guest who named a school: admitted in its active tenant, else 404 (503 when iam cannot answer). */
    private EdgeDecision guest(String tenantCode) {
        Optional<UUID> tenant;
        try {
            tenant = tenantCodes.activeTenant(tenantCode);
        } catch (UnavailableException e) {
            return new EdgeDecision.Refused(HttpStatus.SERVICE_UNAVAILABLE, "UNAVAILABLE",
                    "The school code could not be checked; try again later.", false);
        }
        return tenant.<EdgeDecision>map(t -> EdgeDecision.Admitted.guest(t))
                .orElseGet(() -> new EdgeDecision.Refused(HttpStatus.NOT_FOUND, "NOT_FOUND",
                        "No active school has this code.", false));
    }

    /** Tenant users ({@code bearerAuth}: a tenant and audience mysms) or platform staff ({@code platformAdminAuth}). */
    private static boolean schemeAllows(OperationRule rule, VerifiedToken token) {
        AuthContext context = token.context();
        return context.platformOperator()
                ? rule.schemes().contains(OperationRule.PLATFORM_ADMIN)
                : !OperationRule.PLATFORM.equals(rule.audience()) && rule.schemes().contains(OperationRule.BEARER)
                        && context.tenantId().isPresent()
                        && token.audiences().contains(AuthClientProperties.TENANT_AUDIENCE);
    }

    private static Optional<UUID> uuid(String value) {
        try {
            return Optional.of(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static EdgeDecision.Refused unauthenticated(String detail) {
        return new EdgeDecision.Refused(HttpStatus.UNAUTHORIZED, InvalidTokenException.UNAUTHENTICATED, detail, true);
    }

    private static EdgeDecision.Refused forbidden(String detail) {
        return new EdgeDecision.Refused(HttpStatus.FORBIDDEN, PERMISSION_DENIED, detail, false);
    }
}
