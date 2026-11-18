package com.mysms.iam.business;

import com.mysms.core.context.Actor;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.iam.config.IamEventsConfig;
import com.mysms.iam.events.IamEvents;
import com.mysms.iam.repository.AccountSecretRepository;
import com.mysms.iam.repository.ApiTokenRepository;
import com.mysms.iam.repository.AuthTokenRepository;
import com.mysms.iam.repository.SignInSessionRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Ends everything that keeps an account signed in — refresh tokens, MFA steps, sessions, devices and personal API
 * tokens — in the caller's transaction, and the mailed codes that a new address or new credentials make stale.
 */
@Component
public class AccountSignOutHandler {

    private final AuthTokenRepository refreshTokens;
    private final ApiTokenRepository apiTokens;
    private final SignInSessionRepository sessions;
    private final AccountSecretRepository secrets;
    private final OutboxWriter outbox;
    private final AuditWriter audit;

    public AccountSignOutHandler(AuthTokenRepository refreshTokens, ApiTokenRepository apiTokens,
            SignInSessionRepository sessions, AccountSecretRepository secrets,
            @Qualifier(IamEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(IamEventsConfig.AUDIT) AuditWriter audit) {
        this.refreshTokens = refreshTokens;
        this.apiTokens = apiTokens;
        this.sessions = sessions;
        this.secrets = secrets;
        this.outbox = outbox;
        this.audit = audit;
    }

    /** Every refresh token, API token, open MFA step, session and push device of the account ends. */
    public void everywhere(UUID user, OffsetDateTime now) {
        refreshTokens.revokeAll(user, now);
        refreshTokens.revokeOneTime(user, now);
        revokeApiTokens(user, now);
        sessions.deactivateDevices(user, now);
        sessions.endAll(user, now);
    }

    /** As {@link #everywhere}, keeping the session {@code keep} and the devices (a password change keeps its own). */
    public void everywhereExcept(UUID user, UUID keep, OffsetDateTime now) {
        refreshTokens.revokeAllExcept(user, keep, now);
        refreshTokens.revokeOneTime(user, now);
        revokeApiTokens(user, now);
        sessions.endAllExcept(user, keep, now);
    }

    /** Ends every open password-reset code: the credentials changed by other means. */
    public void forgetResetCodes(UUID user, OffsetDateTime now) {
        secrets.revokeOpen(user, AccountSecretRepository.PASSWORD_RESET, now);
    }

    /** Ends every open reset and verification code: the address they were mailed to changed. */
    public void forgetCodes(UUID user, OffsetDateTime now) {
        forgetResetCodes(user, now);
        secrets.revokeOpen(user, AccountSecretRepository.EMAIL_VERIFICATION, now);
    }

    private void revokeApiTokens(UUID user, OffsetDateTime now) {
        List<UUID> revoked = apiTokens.revokeAllOf(user, now, Actor.current().orElse(null));
        for (UUID token : revoked) {
            audit.recordTransition(ApiTokenService.AGGREGATE, token, "revoke", List.of("revokedAt"), null);
            outbox.publish(IamEvents.API_TOKEN_REVOKED, IamEvents.USER_ACCOUNT, user, IamEvents.session(user, token));
        }
    }
}
