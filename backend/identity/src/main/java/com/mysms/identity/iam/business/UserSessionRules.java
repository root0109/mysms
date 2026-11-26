package com.mysms.identity.iam.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.identity.iam.domain.UserSessionEntity;
import com.mysms.identity.iam.repository.AuthTokenRepository;
import com.mysms.identity.iam.repository.SignInSessionRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Sign-in sessions. Every user reaches their own sessions (list them, end one); another user's sessions —
 * their devices and network addresses are personal data — only with the operation's permission held tenant-wide
 * (an administrator). Ending a session through the API signs that device out at once (its refresh family is revoked).
 */
@Component
public class UserSessionRules {

    private final AuthTokenRepository refreshTokens;
    private final SignInSessionRepository sessions;
    private final Clock clock;

    public UserSessionRules(AuthTokenRepository refreshTokens, SignInSessionRepository sessions, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.sessions = sessions;
        this.clock = clock;
    }

    /** The caller's own sessions, or anyone's with the permission tenant-wide; otherwise 404 (nothing to probe). */
    public void beforeAccess(UUID userId, DataScope scope) {
        boolean own = CallContextHolder.current().flatMap(c -> c.userId()).map(userId::equals).orElse(false);
        if (!own && !scope.tenantWide()) {
            throw new ResourceNotFoundException("iam.user_account", userId);
        }
    }

    public void beforeWrite(UserSessionEntity session) {
        // sessions are created only by sign-in; the API lists, reads and ends them
    }

    public void beforeUpdate(UserSessionEntity current) {
        // no update operation exists for sessions
    }

    public void beforeDelete(UserSessionEntity current) {
        // reaching the row already passed beforeAccess: one's own session, or an administrator
    }

    public void afterDelete(UserSessionEntity ended) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        refreshTokens.revokeFamily(ended.getId(), now);
        sessions.end(ended.getId(), now);
        sessions.deactivateSessionDevice(ended.getId(), ended.getUserId(), now);
    }
}
