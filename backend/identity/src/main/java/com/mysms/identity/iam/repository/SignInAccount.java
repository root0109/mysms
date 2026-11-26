package com.mysms.identity.iam.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * The columns sign-in decides on. Holds the password hash only for the duration of one sign-in; it is never mapped
 * to a DTO, logged or returned.
 *
 * @param id               the user account
 * @param personId         linked person record, when there is one
 * @param passwordHash     argon2id hash; null for accounts without a password (single sign-on only)
 * @param status           active, inactive, locked or exited
 * @param lockedUntil      progressive lockout after repeated failures
 * @param accountExpiresAt the account cannot sign in after this time
 */
public record SignInAccount(UUID id, Optional<UUID> personId, String passwordHash, String status,
                            Optional<OffsetDateTime> lockedUntil, Optional<OffsetDateTime> accountExpiresAt) {

    public static final String ACTIVE = "active";

    public SignInAccount {
        personId = personId == null ? Optional.empty() : personId;
        lockedUntil = lockedUntil == null ? Optional.empty() : lockedUntil;
        accountExpiresAt = accountExpiresAt == null ? Optional.empty() : accountExpiresAt;
    }

    /** Active, not expired and not locked out at {@code now} (the password is checked separately). */
    public boolean canSignInAt(OffsetDateTime now) {
        return ACTIVE.equals(status)
                && accountExpiresAt.map(now::isBefore).orElse(true)
                && lockedUntil.map(until -> !now.isBefore(until)).orElse(true);
    }

    /**
     * Active and not expired at {@code now}. Deliberately ignores the progressive lockout: failed password attempts
     * by anyone must not end the owner's existing sessions (lockout gates password sign-in only).
     */
    public boolean canRefreshAt(OffsetDateTime now) {
        return ACTIVE.equals(status) && accountExpiresAt.map(now::isBefore).orElse(true);
    }

    @Override
    public String toString() {
        return "SignInAccount[id=" + id + ", status=" + status + "]";
    }
}
