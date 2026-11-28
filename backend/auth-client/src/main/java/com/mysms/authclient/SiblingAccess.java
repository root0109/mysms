package com.mysms.authclient;

/**
 * The edge's check for an operation of a sibling area that a module now calls in process instead of over its gRPC
 * server (JAVA-CODING-RULES §2.2): the current caller — a tenant's person, as the operation is neither internal nor
 * open to platform staff ({@code bearerAuth} only) — holds the operation's permission. The sibling's business service
 * then applies the caller's scope, as it did behind the edge. Each caller turns a refusal into its own answer, as it
 * did with the gRPC 403.
 */
public class SiblingAccess {

    private final PermissionChecker permissions;

    public SiblingAccess(PermissionChecker permissions) {
        this.permissions = permissions;
    }

    /** Whether the current caller may perform the sibling operation guarded by this permission code. */
    public boolean permits(String permission) {
        return AuthContext.current()
                .filter(caller -> !caller.isService() && !caller.platformOperator() && caller.tenantId().isPresent())
                .map(caller -> permissions.isPermitted(caller, permission)).orElse(false);
    }
}
