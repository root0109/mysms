package com.mysms.iam.business;

import com.mysms.authclient.AuthContext;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.iam.domain.UserAccountEntity;
import com.mysms.iam.repository.AccountCleanupRepository;
import com.mysms.iam.repository.AccountRepository;
import com.mysms.iam.repository.AuthTokenRepository;
import com.mysms.iam.repository.MemberRecordRepository;
import com.mysms.iam.repository.SignInSessionRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

/** Accounts: nobody deletes their own account, and a deleted account is signed out everywhere at once. */
@Component
public class UserAccountRules {

    private final AuthTokenRepository refreshTokens;
    private final SignInSessionRepository sessions;
    private final AccountCleanupRepository owned;
    private final AccountRepository accounts;
    private final MemberRecordRepository members;
    private final Clock clock;

    public UserAccountRules(AuthTokenRepository refreshTokens, SignInSessionRepository sessions,
            AccountCleanupRepository owned, AccountRepository accounts, MemberRecordRepository members, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.sessions = sessions;
        this.owned = owned;
        this.accounts = accounts;
        this.members = members;
        this.clock = clock;
    }

    /**
     * Each sign-in identifier names one account: a username, e-mail or mobile number that another account already
     * uses in any of the three is 409 (otherwise sign-in with it would match two accounts and lock both out). The
     * generated pre-checks cover the same column; passwords never travel through here.
     */
    public void beforeWrite(UserAccountEntity account) {
        for (String identifier : new String[] {account.getUsername(), account.getEmail(), account.getMobileNo()}) {
            if (identifier != null && !identifier.isBlank() && accounts.identifierTaken(identifier, account.getId())) {
                throw new DuplicateResourceException("Another user account already signs in with this identifier.");
            }
        }
    }

    public void beforeUpdate(UserAccountEntity current) {
        // contact and profile fields only: status, verification and sign-in fields are read-only in the contract
    }

    public void beforeDelete(UserAccountEntity current) {
        if (AuthContext.require().userId().equals(current.getId())) {
            throw new PermissionDeniedException("You cannot delete your own account.");
        }
    }

    /**
     * In the same transaction: every refresh token revoked, every session ended, the account's devices and preferences
     * retired (these rows belong to the account and never block its deletion; role grants and direct permissions do),
     * and the account taken off the staff and student records iam keeps for it (hr and sis clear their own column
     * from the deletion event, and announce only the row id, which iam.member_record cannot apply).
     */
    public void afterDelete(UserAccountEntity deleted) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        refreshTokens.revokeAll(deleted.getId(), now);
        refreshTokens.revokeOneTime(deleted.getId(), now);
        sessions.endAll(deleted.getId(), now);
        owned.retireOwnedRows(deleted.getId(), AuthContext.require().userId(), now);
        members.forgetAccount(deleted.getId(), now);
    }
}
