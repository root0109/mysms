package com.mysms.iam.business;

import com.mysms.authclient.AuthContext;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.iam.api.model.SwitchRoleRequest;
import com.mysms.iam.api.model.SwitchRoleResult;
import com.mysms.iam.api.model.TokenPair;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.config.IamEventsConfig;
import com.mysms.iam.events.IamEvents;
import com.mysms.iam.repository.AccountContactRepository;
import com.mysms.iam.repository.AuthTokenRepository;
import com.mysms.iam.repository.SessionStateRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * iam.SwitchRole (the persona switcher): the caller's session works in one of the caller's own role grants from now
 * on. The session remembers the role (iam.user_session.active_role_id), so every token refreshed from it carries only
 * that role's grants and permissions (plus the self-service ones every role has); the account remembers it as
 * last_login_role_id. The session's earlier refresh tokens stop working and a new pair is answered, with the caller's
 * Me as of the switch. Not while an administrator acts as the user.
 */
@Service
public class RoleSwitchService extends AppTransactionManager {

    private static final String BEARER = "Bearer";

    private final ClaimsFactory claims;
    private final SessionStateRepository sessionState;
    private final AuthTokenRepository refreshTokens;
    private final AccountContactRepository contacts;
    private final TokenIssuer issuer;
    private final MeService me;
    private final AccessRules rules;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public RoleSwitchService(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(IamDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions, ClaimsFactory claims,
            SessionStateRepository sessionState, AuthTokenRepository refreshTokens, AccountContactRepository contacts,
            TokenIssuer issuer, MeService me, AccessRules rules, @Qualifier(IamEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(IamEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock) {
        super(database, transactions);
        this.claims = claims;
        this.sessionState = sessionState;
        this.refreshTokens = refreshTokens;
        this.contacts = contacts;
        this.issuer = issuer;
        this.me = me;
        this.rules = rules;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public SwitchRoleResult switchRole(SwitchRoleRequest request) {
        validator.validate(request);
        AuthContext caller = AuthContext.require();
        AccessRules.requireOwnIdentity();
        UUID session = caller.sessionId()
                .orElseThrow(() -> new InvalidStateException("This token has no sign-in session to switch."));
        UUID user = caller.userId();
        UUID tenant = caller.tenantId().orElseThrow();
        UUID role = request.getRoleId();
        Optional<UUID> scope = Optional.ofNullable(request.getScopeId());
        String refresh = RefreshTokens.generate(tenant);
        AccessClaims switched = doInWriteTransaction(() -> {
            OffsetDateTime now = TenantCalls.now(clock);
            if (claims.selectedGrants(user, role, scope).isEmpty()) {
                throw new ValidationFailedException(List.of(new FieldError(scope.isPresent() ? "scopeId" : "roleId",
                        "is not one of your role grants", "NOT_GRANTED")));
            }
            if (sessionState.setActiveRole(session, user, role, scope.orElse(null), now) == 0) {
                throw new InvalidStateException("The sign-in session has ended; sign in again.");
            }
            contacts.setLastRole(user, role);
            refreshTokens.revokeFamily(session, now);
            refreshTokens.insertRefresh(UuidV7.next(clock), tenant, user, session, RefreshTokens.hash(refresh), null,
                    now.plus(issuer.refreshTokenTtl()), now);
            audit.recordTransition(IamEvents.USER_ACCOUNT, user, "switchRole", List.of("lastLoginRoleId"), null);
            outbox.publish(IamEvents.ROLE_SWITCHED, IamEvents.USER_ACCOUNT, user, IamEvents.session(user, session));
            return claims.forRole(user, caller.personId(), tenant, session, role, scope);
        });
        TokenIssuer.IssuedToken access = issuer.issue(switched,
                AuthContext.currentAuthMethods().stream().sorted().toList());
        TokenPair tokens = new TokenPair(access.token(), refresh, Math.toIntExact(issuer.accessTokenTtl().toSeconds()),
                BEARER);
        return new SwitchRoleResult(tokens, me.getMe());
    }
}
