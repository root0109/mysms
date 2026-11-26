package com.mysms.identity.iam.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.id.UuidV7;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.identity.iam.api.model.ConfirmVerificationRequest;
import com.mysms.identity.iam.api.model.ForcePasswordResetRequest;
import com.mysms.identity.iam.api.model.RequestPasswordResetRequest;
import com.mysms.identity.iam.api.model.RequestVerificationRequest;
import com.mysms.identity.iam.api.model.ResetPasswordRequest;
import com.mysms.identity.iam.api.model.UserAccount;
import com.mysms.identity.iam.config.IamDatabaseConfig;
import com.mysms.identity.iam.config.IamEventsConfig;
import com.mysms.identity.iam.errors.NotConfiguredException;
import com.mysms.identity.iam.errors.RateLimitedException;
import com.mysms.identity.iam.events.IamEvents;
import com.mysms.identity.iam.mapper.UserAccountMapper;
import com.mysms.identity.iam.repository.AccountContactRepository;
import com.mysms.identity.iam.repository.AccountContactRepository.Contact;
import com.mysms.identity.iam.repository.AccountRepository;
import com.mysms.identity.iam.repository.AccountSecretRepository;
import com.mysms.identity.iam.repository.GrantRepository;
import com.mysms.identity.iam.repository.TenantDirectoryRepository;
import com.mysms.identity.iam.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The one-time secrets iam mails to an account holder (iam.RequestPasswordReset, iam.ResetPassword,
 * iam.ForcePasswordReset, iam.RequestVerification, iam.ConfirmVerification):
 * <ul>
 *   <li>a secret is {@code <tenant>.<256 random bits>}, stored only as its SHA-256 together with the address it was
 *       mailed to, works once (consumed by one guarded statement) and briefly: a reset 30 minutes, a verification
 *       24 hours;</li>
 *   <li>a reset request answers the same whether or not the account exists, can be reached or was throttled, and
 *       mails in the background, so neither the answer nor its timing tells an account apart;</li>
 *   <li>at most three secrets of a kind per account in 30 minutes: further reset requests are silently not mailed,
 *       further verification requests are 429;</li>
 *   <li>a new password signs the account out everywhere, API tokens included, and ends every other open reset; a
 *       code works only while the account still has the address it was mailed to, and using it proves that address;
 *       </li>
 *   <li>mobile numbers cannot be verified: no SMS provider is configured (503 NOT_CONFIGURED).</li>
 * </ul>
 */
@Service
public class AccountRecoveryService extends AppTransactionManager {

    static final Duration RESET_TTL = Duration.ofMinutes(30);
    static final Duration VERIFICATION_TTL = Duration.ofHours(24);
    static final Duration THROTTLE_WINDOW = Duration.ofMinutes(30);
    static final int THROTTLE_LIMIT = 3;
    private static final String PERMISSION_FORCE_RESET = "iam.user_account.force_password_reset";
    private static final String MOBILE = "mobile";
    private static final int MAX_TENANT_CODE = 60;
    private static final int MAX_IDENTIFIER = 320;
    private static final Logger LOG = LoggerFactory.getLogger(AccountRecoveryService.class);

    private final TenantDirectoryRepository tenants;
    private final AccountContactRepository contacts;
    private final AccountSecretRepository secrets;
    private final AccountRepository accounts;
    private final UserAccountRepository rows;
    private final UserAccountMapper mapper;
    private final AccountSignOutHandler signOut;
    private final GrantRepository grants;
    private final AccessRules rules;
    private final PasswordHasher passwords;
    private final MailDelivery mail;
    private final AccountMessages messages;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public AccountRecoveryService(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(IamDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            TenantDirectoryRepository tenants, AccountContactRepository contacts, AccountSecretRepository secrets,
            AccountRepository accounts, UserAccountRepository rows, UserAccountMapper mapper,
            AccountSignOutHandler signOut, GrantRepository grants, AccessRules rules,
            PasswordHasher passwords, MailDelivery mail, AccountMessages messages,
            @Qualifier(IamEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(IamEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock) {
        super(database, transactions);
        this.tenants = tenants;
        this.contacts = contacts;
        this.secrets = secrets;
        this.accounts = accounts;
        this.rows = rows;
        this.mapper = mapper;
        this.signOut = signOut;
        this.grants = grants;
        this.rules = rules;
        this.passwords = passwords;
        this.mail = mail;
        this.messages = messages;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    /**
     * iam.RequestPasswordReset: mails a reset code to the account's e-mail address when the identifier (login name,
     * e-mail or mobile number) names exactly one usable account that has one. Every other case answers the same.
     */
    public void requestPasswordReset(RequestPasswordResetRequest request, String clientIp) {
        validator.validate(request);
        String code = request.getTenantCode().trim();
        String identifier = request.getIdentifier().trim();
        if (code.length() > MAX_TENANT_CODE || identifier.isEmpty() || identifier.length() > MAX_IDENTIFIER) {
            return;
        }
        Optional<UUID> tenant = doInGlobalPrimaryReadTransaction(() -> tenants.findActiveTenant(code));
        if (tenant.isEmpty()) {
            return;
        }
        if (!mail.configured()) {
            LOG.warn("password reset requested in tenant {}, but e-mail delivery is not configured", tenant.get());
        }
        String secret = RefreshTokens.generate(tenant.get());
        Locale requestLocale = LocaleContextHolder.getLocale();
        Optional<AccountMessages.Message> message = TenantCalls.as(tenant.get(), null, () -> doInWriteTransaction(() -> {
            OffsetDateTime now = TenantCalls.now(clock);
            List<Contact> found = contacts.findByIdentifier(identifier);
            Optional<Contact> account = found.size() == 1 ? Optional.of(found.get(0)) : Optional.empty();
            UUID subject = account.map(Contact::id).orElse(IamEvents.UNKNOWN_ACCOUNT);
            outbox.publish(IamEvents.PASSWORD_RESET_REQUESTED, IamEvents.USER_ACCOUNT, subject,
                    IamEvents.payload(subject));
            if (account.isEmpty() || !mail.configured() || !reachable(account.get(), now)
                    || throttled(account.get().id(), AccountSecretRepository.PASSWORD_RESET, now)) {
                return Optional.<AccountMessages.Message>empty();
            }
            Contact holder = account.get();
            secrets.insert(UuidV7.next(clock), tenant.get(), holder.id(), AccountSecretRepository.PASSWORD_RESET,
                    "reset", RefreshTokens.hash(secret), holder.email(), clientIp, now.plus(RESET_TTL), now,
                    holder.id());
            return Optional.of(messages.passwordReset(holder.email(), secret, RESET_TTL.toMinutes(),
                    locale(holder, requestLocale)));
        }));
        message.ifPresent(mail::sendLater);
    }

    /**
     * iam.ResetPassword: a valid code, mailed to the address the account still has, sets the new password once and signs
     * the account out everywhere. The password is hashed only after the code is taken.
     */
    public void resetPassword(ResetPasswordRequest request) {
        validator.validate(request);
        AccountSecurityService.requireStrong("newPassword", request.getNewPassword());
        UUID tenant = RefreshTokens.tenantOf(request.getToken()).orElseThrow(() -> invalidCode("token"));
        if (!doInGlobalPrimaryReadTransaction(() -> tenants.isActive(tenant))) {
            throw invalidCode("token");
        }
        TenantCalls.as(tenant, null, () -> doInWriteTransaction(() -> {
            OffsetDateTime now = TenantCalls.now(clock);
            AccountSecretRepository.Consumed used = secrets.consume(RefreshTokens.hash(request.getToken()),
                    AccountSecretRepository.PASSWORD_RESET, now).orElseThrow(() -> invalidCode("token"));
            UUID user = used.userId();
            Contact account = contacts.findById(user).filter(c -> c.usableAt(now) && sameAddress(c, used))
                    .orElseThrow(() -> invalidCode("token"));
            String hash = passwords.hash(request.getNewPassword());
            return TenantCalls.as(tenant, user, () -> {
                accounts.setCredentials(user, hash, null, false, now, user);
                signOut.everywhere(user, now);
                secrets.revokeOpen(user, AccountSecretRepository.PASSWORD_RESET, now);
                List<String> changed = new ArrayList<>(List.of("password", "mustResetPassword"));
                // the code reached the address the account still has: using it proves that address
                if (!account.emailVerified() && contacts.markEmailVerified(user, used.sentTo(), now) == 1) {
                    changed.add("emailVerifiedAt");
                }
                audit.recordTransition(IamEvents.USER_ACCOUNT, user, "resetPassword", changed, null);
                outbox.publish(IamEvents.PASSWORD_RESET, IamEvents.USER_ACCOUNT, user, IamEvents.payload(user));
                return null;
            });
        }));
    }

    /**
     * iam.ForcePasswordReset: an administrator requires a new password at the next sign-in, signs the account out
     * everywhere and mails it a reset code. Like setting credentials, only for an account whose every permission the
     * administrator holds tenant-wide, never their own, and only where e-mail can be sent (503 otherwise).
     */
    public UserAccount forcePasswordReset(UUID userId, ForcePasswordResetRequest request) {
        validator.validate(request);
        AccessRules.requireOwnIdentity();
        rules.requireNotSelf(userId);
        DataScope scope = DataScope.current(PERMISSION_FORCE_RESET);
        mail.requireConfigured();
        UUID tenant = AuthContext.require().tenantId().orElseThrow();
        UUID actor = AuthContext.require().userId();
        String secret = RefreshTokens.generate(tenant);
        Locale requestLocale = LocaleContextHolder.getLocale();
        Forced forced = doInWriteTransaction(() -> {
            OffsetDateTime now = TenantCalls.now(clock);
            Contact account = contacts.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException(IamEvents.USER_ACCOUNT, userId));
            // user accounts are tenant-level rows: requiring a reset needs the permission tenant-wide
            scope.requireWritable(Optional.empty());
            if (request.getEtag() != null && Etags.expectedVersion(request.getEtag()) != account.rowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            for (String code : grants.heldOrScheduledPermissions(userId, LocalDate.ofInstant(clock.instant(),
                    ZoneOffset.UTC))) {
                rules.requireTenantWide(code);
            }
            if (account.email() == null) {
                throw new ValidationFailedException(List.of(new FieldError("email",
                        "the account has no e-mail address to send the reset code to", "NO_EMAIL")));
            }
            if (!account.usableAt(now)) {
                throw new InvalidStateException("The account is not active.");
            }
            if (contacts.requireReset(userId, account.rowVersion(), now, actor) == 0) {
                throw new PreconditionFailedException("The account changed meanwhile; read it again.");
            }
            signOut.everywhere(userId, now);
            secrets.revokeOpen(userId, AccountSecretRepository.PASSWORD_RESET, now);
            secrets.insert(UuidV7.next(clock), tenant, userId, AccountSecretRepository.PASSWORD_RESET, "forced",
                    RefreshTokens.hash(secret), account.email(), null, now.plus(RESET_TTL), now, actor);
            audit.recordTransition(IamEvents.USER_ACCOUNT, userId, "forcePasswordReset", List.of("mustResetPassword"),
                    null);
            outbox.publish(IamEvents.PASSWORD_RESET_REQUIRED, IamEvents.USER_ACCOUNT, userId,
                    IamEvents.payload(userId));
            return new Forced(mapper.toDto(rows.findById(userId).orElseThrow()), messages.forcedReset(account.email(),
                    secret, RESET_TTL.toMinutes(), locale(account, requestLocale)));
        });
        mail.send(forced.message());
        return forced.account();
    }

    /** iam.RequestVerification: mails a code that proves the caller's e-mail address. */
    public void requestVerification(RequestVerificationRequest request) {
        validator.validate(request);
        requireEmailChannel(request.getChannel().getValue());
        mail.requireConfigured();
        AuthContext caller = AuthContext.require();
        UUID tenant = caller.tenantId().orElseThrow();
        String secret = RefreshTokens.generate(tenant);
        Locale requestLocale = LocaleContextHolder.getLocale();
        AccountMessages.Message message = doInWriteTransaction(() -> {
            OffsetDateTime now = TenantCalls.now(clock);
            Contact account = contacts.findById(caller.userId())
                    .orElseThrow(() -> new ResourceNotFoundException(IamEvents.USER_ACCOUNT, caller.userId()));
            if (account.email() == null) {
                throw new ValidationFailedException(List.of(new FieldError("channel",
                        "the account has no e-mail address", "NO_EMAIL")));
            }
            if (account.emailVerified()) {
                throw new InvalidStateException("The e-mail address is already verified.");
            }
            if (throttled(account.id(), AccountSecretRepository.EMAIL_VERIFICATION, now)) {
                throw new RateLimitedException("Several codes were sent recently; try again later.");
            }
            secrets.revokeOpen(account.id(), AccountSecretRepository.EMAIL_VERIFICATION, now);
            secrets.insert(UuidV7.next(clock), tenant, account.id(), AccountSecretRepository.EMAIL_VERIFICATION,
                    "verify", RefreshTokens.hash(secret), account.email(), null, now.plus(VERIFICATION_TTL), now,
                    account.id());
            outbox.publish(IamEvents.VERIFICATION_SENT, IamEvents.USER_ACCOUNT, account.id(),
                    IamEvents.payload(account.id()));
            return messages.verification(account.email(), secret, VERIFICATION_TTL.toHours(),
                    locale(account, requestLocale));
        });
        mail.send(message);
    }

    /** iam.ConfirmVerification: a valid code marks the address it was mailed to verified, once. */
    public UserAccount confirmVerification(ConfirmVerificationRequest request) {
        validator.validate(request);
        requireEmailChannel(request.getChannel().getValue());
        UUID tenant = RefreshTokens.tenantOf(request.getCode()).orElseThrow(() -> invalidCode("code"));
        if (!doInGlobalPrimaryReadTransaction(() -> tenants.isActive(tenant))) {
            throw invalidCode("code");
        }
        return TenantCalls.as(tenant, null, () -> doInWriteTransaction(() -> {
            OffsetDateTime now = TenantCalls.now(clock);
            AccountSecretRepository.Consumed used = secrets.consume(RefreshTokens.hash(request.getCode()),
                    AccountSecretRepository.EMAIL_VERIFICATION, now).orElseThrow(() -> invalidCode("code"));
            UUID user = used.userId();
            Contact account = contacts.findById(user).filter(c -> c.usableAt(now) && sameAddress(c, used))
                    .orElseThrow(() -> invalidCode("code"));
            return TenantCalls.as(tenant, user, () -> {
                if (!account.emailVerified() && contacts.markEmailVerified(user, used.sentTo(), now) == 1) {
                    audit.recordTransition(IamEvents.USER_ACCOUNT, user, "confirmVerification",
                            List.of("emailVerifiedAt"), null);
                    outbox.publish(IamEvents.EMAIL_VERIFIED, IamEvents.USER_ACCOUNT, user, IamEvents.payload(user));
                }
                return mapper.toDto(rows.findById(user).orElseThrow());
            });
        }));
    }

    private boolean reachable(Contact account, OffsetDateTime now) {
        return account.email() != null && account.usableAt(now);
    }

    private boolean throttled(UUID user, String type, OffsetDateTime now) {
        return secrets.countSince(user, type, now.minus(THROTTLE_WINDOW)) >= THROTTLE_LIMIT;
    }

    private static boolean sameAddress(Contact account, AccountSecretRepository.Consumed used) {
        return account.email() != null && used.sentTo() != null && account.email().equalsIgnoreCase(used.sentTo());
    }

    /** The account's own language when it chose one, otherwise the request's. */
    static Locale locale(Contact account, Locale requestLocale) {
        return account.locale() == null || account.locale().isBlank() ? requestLocale
                : Locale.forLanguageTag(account.locale());
    }

    private static void requireEmailChannel(String channel) {
        if (MOBILE.equals(channel)) {
            throw new NotConfiguredException("No SMS provider is configured, so mobile numbers cannot be verified.");
        }
    }

    /** The account as changed, and the message to send once the change has committed. */
    private record Forced(UserAccount account, AccountMessages.Message message) {
    }

    /** One answer for a code that is malformed, unknown, used, expired, of another kind or of an unusable account. */
    static ValidationFailedException invalidCode(String field) {
        return new ValidationFailedException(List.of(new FieldError(field, "is not valid or has expired",
                "INVALID_CODE")));
    }
}
