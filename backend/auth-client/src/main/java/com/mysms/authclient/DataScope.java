package com.mysms.authclient;

import com.mysms.core.error.PermissionDeniedException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The rows a caller may touch beyond the operation's permission (api/README "Authorization": data-level rules on top of
 * the permission check). Institution-owned rows are visible only inside the caller's institutions — narrowed to the one
 * selected with {@code X-Institution-Id} — and tenant-level rows (no institution) are read by every holder of the
 * permission but changed only by a tenant-wide grant, so a school-level role can never alter what applies to the
 * whole tenant.
 *
 * @param all          every institution of the tenant (a tenant-wide grant and no institution selected)
 * @param institutions the institutions the caller may read when {@code all} is false
 * @param tenantWide   the caller holds a tenant-wide grant (may change tenant-level rows)
 */
public record DataScope(boolean all, Set<UUID> institutions, boolean tenantWide) {

    public DataScope {
        institutions = Set.copyOf(institutions);
    }

    /**
     * The scope of a global table (no tenant, no institution: tenants, SaaS plans, notification types…). Its rows are
     * not narrowed: who may call the operation at all — platform staff for every write — was decided at the edge from
     * the operation's scheme and permission (JAVA-CODING-RULES §16.2), and the service applies its own data rules (the
     * guest projection of public reads).
     */
    public static DataScope global() {
        return new DataScope(true, Set.of(), true);
    }

    /** The scope of the current request's caller for the operation's permission. */
    public static DataScope current(String permission) {
        return of(AuthContext.require(), permission);
    }

    /**
     * Only a tenant-wide grant that carries this very permission reaches every institution and tenant-level rows; a
     * tenant-wide grant of some other permission (a read-only role, say) never widens it.
     */
    public static DataScope of(AuthContext context, String permission) {
        boolean tenantWide = context.hasTenantWidePermission(permission);
        if (context.activeInstitutionId().isPresent()) {
            UUID active = context.activeInstitutionId().get();
            boolean reachable = tenantWide || context.institutionIds().contains(active);
            return new DataScope(false, reachable ? Set.of(active) : Set.of(), tenantWide);
        }
        return new DataScope(tenantWide, context.institutionIds(), tenantWide);
    }

    /** Whether a row owned by this institution (empty: a tenant-level row) is visible. */
    public boolean canRead(Optional<UUID> institution) {
        return institution.isEmpty() || all || institutions.contains(institution.get());
    }

    /** The institutions as a SQL array parameter (bound together with {@link #all()}). */
    public UUID[] institutionArray() {
        return institutions.toArray(UUID[]::new);
    }

    /**
     * Refuses a change to a row owned by this institution (empty: a tenant-level row) that the caller may not make:
     * institution rows need the institution in scope, tenant-level rows a tenant-wide grant.
     */
    public void requireWritable(Optional<UUID> institution) {
        if (institution.isEmpty()) {
            if (!tenantWide) {
                throw new PermissionDeniedException("Only a tenant-wide administrator may change tenant-level data.");
            }
            return;
        }
        if (!all && !institutions.contains(institution.get())) {
            throw new PermissionDeniedException("The institution is outside the caller's scope.");
        }
    }
}
