package com.mysms.identity.iam.business;

import com.mysms.authclient.AuthContext;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.identity.iam.domain.UserAccountEntity;
import com.mysms.identity.iam.repository.AccountCleanupRepository;
import com.mysms.identity.iam.repository.AccountRepository;
import com.mysms.identity.iam.repository.MemberRecordRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Accounts: nobody deletes their own account, and a deleted account is signed out everywhere at once. */
@Component
public class UserAccountRules {

    private final AccountSignOutHandler signOut;
    private final AccountCleanupRepository owned;
    private final AccountRepository accounts;
    private final MemberRecordRepository members;
    private final Clock clock;

    public UserAccountRules(AccountSignOutHandler signOut, AccountCleanupRepository owned, AccountRepository accounts,
            MemberRecordRepository members, Clock clock) {
        this.signOut = signOut;
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

    /** A changed e-mail or mobile number ends the codes mailed to the old address. */
    public void afterUpdate(UserAccountEntity updated, String emailBefore, String mobileBefore) {
        if (!sameAddress(emailBefore, updated.getEmail()) || !Objects.equals(mobileBefore, updated.getMobileNo())) {
            signOut.forgetCodes(updated.getId(), now());
        }
    }

    /** A deactivated account is signed out everywhere, so reactivating it revives no session or token. */
    public void afterDeactivate(UUID account) {
        signOut.everywhere(account, now());
    }

    public void beforeDelete(UserAccountEntity current) {
        if (AuthContext.require().userId().equals(current.getId())) {
            throw new PermissionDeniedException("You cannot delete your own account.");
        }
    }

    /**
     * In the same transaction: signed out everywhere (refresh and API tokens revoked, sessions and devices ended,
     * {@link AccountSignOutHandler}), the account's devices and preferences
     * retired (these rows belong to the account and never block its deletion; role grants and direct permissions do),
     * and the account taken off the staff and student records iam keeps for it (hr and sis clear their own column
     * from the deletion event, and announce only the row id, which iam.member_record cannot apply).
     */
    public void afterDelete(UserAccountEntity deleted) {
        OffsetDateTime now = now();
        signOut.everywhere(deleted.getId(), now);
        owned.retireOwnedRows(deleted.getId(), AuthContext.require().userId(), now);
        members.forgetAccount(deleted.getId(), now);
    }

    private static boolean sameAddress(String before, String after) {
        return before == null ? after == null : before.equalsIgnoreCase(after);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }
}
