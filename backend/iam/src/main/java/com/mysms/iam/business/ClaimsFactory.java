package com.mysms.iam.business;

import com.mysms.iam.repository.ActiveRoleRepository;
import com.mysms.iam.repository.GrantRepository;
import com.mysms.iam.repository.RoleGrantRow;
import com.mysms.iam.repository.SessionStateRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Builds what an access token says about its user (Factory of {@link AccessClaims}), from the database as of now and
 * inside the caller's transaction: every counted grant, or — once the session switched role (iam.SwitchRole) — the
 * grants of that role alone, and, for a personal API token, only the permissions the token was limited to. Sign-in,
 * refresh, role switch, impersonation and the API-token exchange all take their claims from here, so a token never
 * says more than the database does.
 */
@Component
public class ClaimsFactory {

    private static final String INSTITUTION = "institution";

    private final GrantRepository grants;
    private final ActiveRoleRepository roles;
    private final SessionStateRepository sessions;
    private final Clock clock;

    public ClaimsFactory(GrantRepository grants, ActiveRoleRepository roles, SessionStateRepository sessions,
                         Clock clock) {
        this.grants = grants;
        this.roles = roles;
        this.sessions = sessions;
        this.clock = clock;
    }

    /** The claims of a session: the role it switched to, or every grant of the user. */
    public AccessClaims forSession(UUID user, Optional<UUID> person, UUID tenant, UUID session) {
        Optional<SessionStateRepository.State> state = sessions.find(session);
        Optional<UUID> role = state.flatMap(SessionStateRepository.State::activeRoleId);
        if (role.isPresent()) {
            return forRole(user, person, tenant, session, role.get(), state.get().activeScopeId());
        }
        return full(user, person, tenant, session, Optional.empty());
    }

    /**
     * Every counted grant of the user; with {@code abilities}, only the permissions among them (a personal API token
     * limited to some of its owner's permissions).
     */
    public AccessClaims full(UUID user, Optional<UUID> person, UUID tenant, UUID session,
                             Optional<Set<String>> abilities) {
        LocalDate today = today();
        return build(user, person, tenant, session, grants.activeGrants(user, today),
                grants.effectivePermissions(user, today), grants.tenantWidePermissions(user, today), abilities);
    }

    /**
     * Working in one role: its counted grants (narrowed to one unit when {@code scope} is given), its permissions
     * and every role's self-service ones. A role no longer granted leaves only the self-service permissions, so a
     * revoked grant stops working at the next refresh.
     */
    public AccessClaims forRole(UUID user, Optional<UUID> person, UUID tenant, UUID session, UUID role,
                                Optional<UUID> scope) {
        List<RoleGrantRow> selected = selectedGrants(user, role, scope).stream()
                .map(ActiveRoleRepository.Grant::row).toList();
        List<String> permissions = roles.rolePermissions(user, selected.isEmpty() ? null : role);
        boolean tenantWide = selected.stream().anyMatch(g -> RoleGrantRow.TENANT.equals(g.scopeType()));
        return build(user, person, tenant, session, selected, permissions, tenantWide ? permissions : List.of(),
                Optional.empty());
    }

    /** The user's counted grants of the role, narrowed to the unit when one is given. */
    public List<ActiveRoleRepository.Grant> selectedGrants(UUID user, UUID role, Optional<UUID> scope) {
        return roles.grants(user, today()).stream()
                .filter(g -> g.roleId().equals(role))
                .filter(g -> scope.isEmpty() || scope.equals(g.scopeId()))
                .toList();
    }

    private AccessClaims build(UUID user, Optional<UUID> person, UUID tenant, UUID session, List<RoleGrantRow> rows,
                               List<String> permissions, List<String> tenantWide, Optional<Set<String>> abilities) {
        List<String> catalogue = grants.catalogue();
        List<String> held = abilities.map(a -> permissions.stream().filter(a::contains).toList()).orElse(permissions);
        List<String> heldTenantWide = abilities.map(a -> tenantWide.stream().filter(a::contains).toList())
                .orElse(tenantWide);
        Set<UUID> institutions = rows.stream()
                .filter(g -> INSTITUTION.equals(g.scopeType()))
                .map(g -> g.scopeId().orElseThrow())
                .collect(Collectors.toUnmodifiableSet());
        return new AccessClaims(user, tenant, person, session, PermissionCompactor.compact(held, catalogue),
                institutions, rows.stream().map(RoleGrantRow::claim).distinct().toList(),
                rows.stream().anyMatch(g -> RoleGrantRow.TENANT.equals(g.scopeType())),
                PermissionCompactor.compact(heldTenantWide, catalogue));
    }

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
