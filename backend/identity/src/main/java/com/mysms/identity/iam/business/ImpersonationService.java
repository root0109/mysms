package com.mysms.identity.iam.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.identity.iam.api.model.ImpersonationToken;
import com.mysms.identity.iam.api.model.StartImpersonationRequest;
import com.mysms.identity.iam.config.IamDatabaseConfig;
import com.mysms.identity.iam.config.IamEventsConfig;
import com.mysms.identity.iam.events.IamEvents;
import com.mysms.identity.iam.repository.AccountRepository;
import com.mysms.identity.iam.repository.GrantRepository;
import com.mysms.identity.iam.repository.SessionStateRepository;
import com.mysms.identity.iam.repository.SignInAccount;
import com.mysms.identity.iam.repository.SignInSessionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * An administrator acting as another user (iam.StartImpersonation, iam.StopImpersonation):
 * <ul>
 *   <li>only for an active account with strictly fewer rights: the administrator holds every permission the account
 *       holds or is scheduled to hold, tenant-wide, and the account lacks at least one of the administrator's — so
 *       never an equal or more privileged account (another administrator), never oneself, never from inside another
 *       impersonation;</li>
 *   <li>a reason is required and kept in the audit log;</li>
 *   <li>the answer is one access token of 15 minutes, without a refresh token, naming the administrator in RFC 8693
 *       {@code act}; it belongs to a session of its own (iam.user_session.impersonator_user_id), visible to the
 *       account in its sessions;</li>
 *   <li>every change made with it is audited with both identities (core's audit records carry the impersonator);</li>
 *   <li>iam.StopImpersonation ends that session. Known limit: access tokens are verified locally by every service, so
 *       the token itself stays valid until it expires; the apps discard it at once.</li>
 * </ul>
 */
@Service
public class ImpersonationService extends AppTransactionManager {

    static final Duration LIFETIME = Duration.ofMinutes(15);
    private static final String PERMISSION_START = "iam.user_account.start_impersonation";
    private static final int MAX_REASON = 500;

    private final AccountRepository accounts;
    private final GrantRepository grants;
    private final SessionStateRepository sessionState;
    private final SignInSessionRepository sessions;
    private final ClaimsFactory claims;
    private final TokenIssuer issuer;
    private final AccessRules rules;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public ImpersonationService(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(IamDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            AccountRepository accounts, GrantRepository grants, SessionStateRepository sessionState,
            SignInSessionRepository sessions, ClaimsFactory claims, TokenIssuer issuer, AccessRules rules,
            @Qualifier(IamEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(IamEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock) {
        super(database, transactions);
        this.accounts = accounts;
        this.grants = grants;
        this.sessionState = sessionState;
        this.sessions = sessions;
        this.claims = claims;
        this.issuer = issuer;
        this.rules = rules;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    /** iam.StartImpersonation. */
    public ImpersonationToken startImpersonation(UUID userId, StartImpersonationRequest request, String clientIp,
                                                 String userAgent) {
        validator.validate(request);
        String reason = request.getReason().trim();
        if (reason.isEmpty() || reason.length() > MAX_REASON) {
            throw new ValidationFailedException(List.of(new FieldError("reason", "must be 1 to " + MAX_REASON
                    + " characters", "Size")));
        }
        AuthContext caller = AuthContext.require();
        AccessRules.requireOwnIdentity();
        if (caller.userId().equals(userId)) {
            throw new PermissionDeniedException("You cannot act as yourself.");
        }
        DataScope scope = DataScope.current(PERMISSION_START);
        UUID tenant = caller.tenantId().orElseThrow();
        UUID session = UuidV7.next(clock);
        AccessClaims acting = doInWriteTransaction(() -> {
            OffsetDateTime now = TenantCalls.now(clock);
            SignInAccount target = accounts.findSignInById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException(IamEvents.USER_ACCOUNT, userId));
            // user accounts are tenant-level rows: acting as one needs the permission tenant-wide
            scope.requireWritable(Optional.empty());
            requireFewerRights(caller.userId(), userId, LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC));
            if (!target.canRefreshAt(now)) {
                throw new InvalidStateException("The account is not active.");
            }
            sessionState.insertImpersonation(session, tenant, userId, caller.userId(), clientIp, userAgent, now);
            audit.recordTransition(IamEvents.USER_ACCOUNT, userId, "startImpersonation", List.of(), reason);
            outbox.publish(IamEvents.IMPERSONATION_STARTED, IamEvents.USER_ACCOUNT, userId,
                    IamEvents.impersonation(userId, caller.userId(), session));
            return claims.full(userId, target.personId(), tenant, session, Optional.empty());
        });
        TokenIssuer.IssuedToken token = issuer.issueActing(acting, AuthContext.currentAuthMethods().stream().sorted().toList(),
                caller.userId(), LIFETIME);
        long seconds = Duration.between(clock.instant(), token.expiresAt()).toSeconds();
        return new ImpersonationToken(token.token(), Math.toIntExact(seconds), userId);
    }

    /** iam.StopImpersonation: called with the impersonation token; ends its session. */
    public void stopImpersonation() {
        AuthContext caller = AuthContext.require();
        UUID impersonator = caller.impersonatorId()
                .orElseThrow(() -> new InvalidStateException("You are not acting as another user."));
        UUID session = caller.sessionId()
                .orElseThrow(() -> new InvalidStateException("You are not acting as another user."));
        runInWriteTransaction(() -> {
            sessionState.find(session).filter(s -> s.impersonatorId().isPresent() && !s.ended())
                    .orElseThrow(() -> new InvalidStateException("The impersonation has already ended."));
            sessions.end(session, TenantCalls.now(clock));
            audit.recordTransition(IamEvents.USER_ACCOUNT, caller.userId(), "stopImpersonation", List.of(), null);
            outbox.publish(IamEvents.IMPERSONATION_ENDED, IamEvents.USER_ACCOUNT, caller.userId(),
                    IamEvents.impersonation(caller.userId(), impersonator, session));
        });
    }

    /**
     * The administrator holds, tenant-wide, every permission the account holds or will hold, and the account lacks at
     * least one of the administrator's: strictly fewer rights.
     */
    private void requireFewerRights(UUID administrator, UUID account, LocalDate today) {
        Set<String> theirs = new HashSet<>(grants.heldOrScheduledPermissions(account, today));
        for (String code : theirs) {
            rules.requireTenantWide(code);
        }
        if (theirs.containsAll(grants.effectivePermissions(administrator, today))) {
            throw new PermissionDeniedException("You can only act as an account with fewer rights than yours.");
        }
    }
}
