package com.mysms.identity.iam.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.identity.iam.api.model.AcceptTermsRequest;
import com.mysms.identity.iam.api.model.ChangePasswordRequest;
import com.mysms.identity.iam.api.model.SetUserCredentialsRequest;
import com.mysms.identity.iam.api.model.UserAccount;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.identity.iam.config.IamDatabaseConfig;
import com.mysms.identity.iam.config.IamEventsConfig;
import com.mysms.identity.iam.events.IamEvents;
import com.mysms.identity.iam.mapper.UserAccountMapper;
import com.mysms.identity.iam.repository.AccountRepository;
import com.mysms.identity.iam.repository.GrantRepository;
import com.mysms.identity.iam.repository.SignInAccount;
import com.mysms.identity.iam.repository.UserAccountRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The account's own credentials and an administrator's reset of them (iam.ChangePassword, iam.SetUserCredentials,
 * iam.AcceptTerms):
 * <ul>
 *   <li>passwords are 12 to 1024 characters and stored only as argon2id hashes;</li>
 *   <li>changing one's password needs the current one (a wrong one counts towards the sign-in lockout, and a
 *       locked account cannot try) and signs every other session out, ending any open MFA step;</li>
 *   <li>an administrator may set another account's password only when they hold every permission of that account
 *       tenant-wide (no takeover of a more privileged account), never their own; it signs the account out
 *       everywhere and, by default, requires a new password at the next sign-in.</li>
 * </ul>
 */
@Service
public class AccountSecurityService extends AppTransactionManager {

    static final int MIN_PASSWORD = 12;
    static final int MAX_PASSWORD = 1024;
    private static final String PERMISSION_SET_CREDENTIALS = "iam.user_account.set_user_credentials";
    private final AccountRepository accounts;
    private final UserAccountRepository rows;
    private final UserAccountMapper mapper;
    private final AccountSignOutHandler signOut;
    private final GrantRepository grants;
    private final AccessRules rules;
    private final PasswordHasher passwords;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public AccountSecurityService(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(IamDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
                                  AccountRepository accounts, UserAccountRepository rows, UserAccountMapper mapper,
                                  AccountSignOutHandler signOut, GrantRepository grants,
                                  AccessRules rules, PasswordHasher passwords,
                                  @Qualifier(IamEventsConfig.OUTBOX) OutboxWriter outbox,
                                  @Qualifier(IamEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator,
                                  Clock clock) {

        super(database, transactions);
        this.accounts = accounts;
        this.rows = rows;
        this.mapper = mapper;
        this.signOut = signOut;
        this.grants = grants;
        this.rules = rules;
        this.passwords = passwords;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    /** The caller's sign-in state (password hash, lockout), read in its own transaction. */
    private Optional<SignInAccount> signInAccount(UUID user) {
        return doInPrimaryReadTransaction(() -> {
            return accounts.findSignInById(user);
        });
    }

    /** A wrong current password counts towards the lockout, in its own transaction so it stays when the call fails. */
    private void recordFailure(UUID user, OffsetDateTime now) {
        runInWriteTransaction(() -> {
            accounts.recordFailure(user, now, SignInService.LOCK_THRESHOLD, SignInService.MAX_LOCK_MINUTES);
            outbox.publish(IamEvents.SIGN_IN_FAILED, IamEvents.USER_ACCOUNT, user, IamEvents.payload(user));
        });
    }

    /** iam.ChangePassword: the caller's own password, given the current one. */
    public void changePassword(ChangePasswordRequest request) {
        validator.validate(request);
        AccessRules.requireOwnIdentity();
        requireStrong("newPassword", request.getNewPassword());
        AuthContext caller = AuthContext.require();
        UUID user = caller.userId();
        OffsetDateTime now = now();
        Optional<SignInAccount> account = signInAccount(user);
        if (account.isPresent() && !account.get().canSignInAt(now)) {
            throw new PermissionDeniedException("Too many failed attempts; try again later.");
        }
        String current = request.getCurrentPassword();
        boolean matches = account.isPresent() && current.length() <= MAX_PASSWORD
                && passwords.matches(current, account.get().passwordHash());
        if (!matches) {
            recordFailure(user, now);
            throw new ValidationFailedException(List.of(new FieldError("currentPassword", "does not match",
                    "INVALID_PASSWORD")));
        }
        String hash = passwords.hash(request.getNewPassword());
        runInWriteTransaction(() -> {
            if (accounts.setCredentials(user, hash, null, false, now, user) == 0) {
                throw new ResourceNotFoundException(IamEvents.USER_ACCOUNT, user);
            }
            Optional<UUID> session = caller.sessionId();
            if (session.isPresent()) {
                signOut.everywhereExcept(user, session.get(), now);
            } else {
                signOut.everywhere(user, now);
            }
            signOut.forgetResetCodes(user, now);
            audit.recordUpdate(IamEvents.USER_ACCOUNT, user, List.of("password"));
            outbox.publish(IamEvents.PASSWORD_CHANGED, IamEvents.USER_ACCOUNT, user, IamEvents.payload(user));
        });
    }

    /** iam.SetUserCredentials: an administrator sets another account's login name and a temporary password. */
    public UserAccount setUserCredentials(UUID userId, SetUserCredentialsRequest request) {
        validator.validate(request);
        AccessRules.requireOwnIdentity();
        requireStrong("temporaryPassword", request.getTemporaryPassword());
        String requested = request.getUsername();
        String username = requested == null ? null : requested.trim();
        if (username != null && (username.isEmpty() || username.length() > 150)) {
            throw new ValidationFailedException(List.of(new FieldError("username", "must be 1 to 150 characters",
                    "Size")));
        }
        boolean mustReset = !Boolean.FALSE.equals(request.getMustResetPassword());
        rules.requireNotSelf(userId);
        DataScope scope = DataScope.current(PERMISSION_SET_CREDENTIALS);
        String hash = passwords.hash(request.getTemporaryPassword());
        UUID actor = AuthContext.require().userId();
        OffsetDateTime now = now();
        return doInWriteTransaction(() -> {
            rows.findById(userId).orElseThrow(() -> new ResourceNotFoundException(IamEvents.USER_ACCOUNT, userId));
            // user accounts are tenant-level rows: changing their credentials needs the permission tenant-wide
            scope.requireWritable(Optional.empty());
            LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
            for (String code : grants.heldOrScheduledPermissions(userId, today)) {
                rules.requireTenantWide(code);
            }
            if (username != null && accounts.identifierTaken(username, userId)) {
                throw new DuplicateResourceException("Another user account already signs in with this username.");
            }
            accounts.setCredentials(userId, hash, username, mustReset, now, actor);
            signOut.everywhere(userId, now);
            signOut.forgetResetCodes(userId, now);
            audit.recordUpdate(IamEvents.USER_ACCOUNT, userId, username == null ? List.of("password", "mustResetPassword")
                    : List.of("username", "password", "mustResetPassword"));
            outbox.publish(IamEvents.CREDENTIALS_SET, IamEvents.USER_ACCOUNT, userId, IamEvents.payload(userId));
            return mapper.toDto(rows.findById(userId).orElseThrow());
        });
    }

    /** iam.AcceptTerms: records when the caller accepted the terms; the version accepted is kept in the audit. */
    public UserAccount acceptTerms(AcceptTermsRequest request) {
        validator.validate(request);
        AccessRules.requireOwnIdentity();
        String version = request.getTermsVersion().trim();
        if (version.isEmpty() || version.length() > 50) {
            throw new ValidationFailedException(List.of(new FieldError("termsVersion", "must be 1 to 50 characters",
                    "Size")));
        }
        UUID user = AuthContext.require().userId();
        OffsetDateTime now = now();
        return doInWriteTransaction(() -> {
            if (accounts.acceptTerms(user, now) == 0) {
                throw new ResourceNotFoundException(IamEvents.USER_ACCOUNT, user);
            }
            audit.recordTransition(IamEvents.USER_ACCOUNT, user, "acceptTerms", List.of("termsAcceptedAt"),
                    "terms version " + version);
            outbox.publish(IamEvents.TERMS_ACCEPTED, IamEvents.USER_ACCOUNT, user, IamEvents.payload(user));
            return mapper.toDto(rows.findById(user).orElseThrow());
        });
    }

    static void requireStrong(String field, String password) {
        if (password == null || password.length() < MIN_PASSWORD || password.length() > MAX_PASSWORD) {
            throw new ValidationFailedException(List.of(new FieldError(field, "must be " + MIN_PASSWORD + " to "
                    + MAX_PASSWORD + " characters", "PASSWORD_POLICY")));
        }
        if (password.isBlank()) {
            throw new ValidationFailedException(List.of(new FieldError(field, "must not be blank", "PASSWORD_POLICY")));
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
