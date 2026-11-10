package com.mysms.iam.business;

import com.mysms.authclient.AuthContext;
import com.mysms.iam.api.model.DisableMfaRequest;
import com.mysms.iam.api.model.EnrollMfaRequest;
import com.mysms.iam.api.model.MfaCredential;
import com.mysms.iam.api.model.MfaCredentialList;
import com.mysms.iam.api.model.MfaEnrollment;
import com.mysms.iam.api.model.RecoveryCodes;
import com.mysms.iam.api.model.RegenerateRecoveryCodesRequest;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ConflictException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.config.IamEventsConfig;
import com.mysms.iam.domain.MeAccountEntity;
import com.mysms.iam.events.IamEvents;
import com.mysms.iam.repository.AccountRepository;
import com.mysms.iam.repository.AuthTokenRepository;
import com.mysms.iam.repository.MfaRepository.Credential;
import com.mysms.iam.repository.MfaRepository;
import com.mysms.iam.repository.SignInAccount;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Second factors (iam.EnrollMfa, iam.CompleteMfa's code check, iam.DisableMfa, iam.RegenerateRecoveryCodes,
 * iam.ListMfaCredentials), JAVA-CODING-RULES §16 security floor:
 * <ul>
 *   <li>authenticator-app codes (TOTP, RFC 6238); the shared secret is stored only as a per-tenant envelope
 *       ({@link FieldCipher}) and shown once, at enrollment;</li>
 *   <li>ten one-time recovery codes, shown once, kept as SHA-256 hashes inside the encrypted credential;</li>
 *   <li>a factor counts only after one code proved it (enrollment finishes in iam.CompleteMfa, which then issues
 *       no tokens); a further authenticator can only be added from a session signed in with a second factor; a code
 *       is never accepted twice; disabling a factor or replacing recovery codes needs a current code, and a locked
 *       account cannot try;</li>
 *   <li>every wrong code counts towards the sign-in lockout of the account;</li>
 *   <li>the MFA step token is opaque, stored as SHA-256 only, single-use and valid for five minutes.</li>
 * </ul>
 * SMS and e-mail factors need message delivery, which this release does not have: enrolling one answers 501.
 */
@Service
public class MfaService extends AppTransactionManager implements InitializingBean {

    /** The one-time step a sign-in waits on. */
    public static final String SIGN_IN_STEP = "mfa";
    /** The one-time step that verifies a new credential: {@code mfa_enroll:<credential id>}. */
    public static final String ENROLL_STEP = "mfa_enroll:";
    static final Duration STEP_LIFETIME = Duration.ofMinutes(5);
    static final int MAX_FACTORS = 5;
    private static final String COLUMN = "iam.mfa_credential.secret_enc";
    private static final String ISSUER = "MySMS";
    private static final Logger LOG = LoggerFactory.getLogger(MfaService.class);
    private final MfaRepository credentials;
    private final AuthTokenRepository tokens;
    private final AccountRepository users;
    private final FieldCipher cipher;
    private final OutboxWriter outbox;
    private final DtoValidator validator;
    private final Clock clock;

    public MfaService(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(IamDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions, MfaRepository credentials,
                      AuthTokenRepository tokens, AccountRepository users, FieldCipher cipher,
                      @Qualifier(IamEventsConfig.OUTBOX) OutboxWriter outbox, DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.credentials = credentials;
        this.tokens = tokens;
        this.users = users;
        this.cipher = cipher;
        this.outbox = outbox;
        this.validator = validator;
        this.clock = clock;
    }

    /** MFA secrets are stored encrypted: iam does not start without a master key (DEPLOYMENT.md "Secrets"). */
    @Override
    public void afterPropertiesSet() {
        cipher.requireConfigured();
    }

    /** iam.ListMfaCredentials: the caller's factors (type, label, state); never a secret. */
    public MfaCredentialList list() {
        UUID user = AuthContext.require().userId();
        return doInReadTransaction(() -> {
            return new MfaCredentialList(credentials.list(user).stream().map(c -> {
            MfaCredential dto = new MfaCredential();
            dto.setId(c.id());
            dto.setCredentialType(MfaCredential.CredentialTypeEnum.fromValue(c.type()));
            dto.setLabel(c.label());
            dto.setVerifiedAt(c.verifiedAt().orElse(null));
            dto.setLastUsedAt(c.lastUsedAt().orElse(null));
            dto.setIsActive(c.active());
            return dto;
        }).toList());
        });
    }

    /**
     * iam.EnrollMfa: a new, not yet usable authenticator-app factor. Returns its secret (and recovery codes when the
     * user has none) once; the factor counts from the first valid code given to iam.CompleteMfa with the returned
     * token. An unfinished earlier enrollment is discarded.
     */
    public MfaEnrollment enroll(EnrollMfaRequest request) {
        validator.validate(request);
        if (!MfaRepository.TOTP.equals(request.getCredentialType().getValue())) {
            throw new NotImplementedException("Only authenticator-app (totp) factors can be enrolled in this release.");
        }
        AuthContext caller = AuthContext.require();
        UUID user = caller.userId();
        UUID tenant = caller.tenantId().orElseThrow();
        OffsetDateTime now = now();
        return doInWriteTransaction(() -> {
            credentials.retirePending(user, now);
            List<Credential> live = credentials.forUpdate(user);
            if (live.stream().anyMatch(c -> MfaRepository.TOTP.equals(c.type()) && c.usable())
                    && !AuthContext.currentAuthMethods().contains("mfa")) {
                // another authenticator only from a session that proved the current one: a stolen password session
                // can never add a factor of its own
                throw new PermissionDeniedException("Sign in with your current second factor to add another one.");
            }
            if (live.stream().filter(c -> MfaRepository.TOTP.equals(c.type())).count() >= MAX_FACTORS) {
                throw new ConflictException("TOO_MANY_FACTORS", "At most " + MAX_FACTORS
                        + " authenticator apps can be enrolled; disable one first.");
            }
            String secret = Totp.newSecret();
            UUID id = UuidV7.next(clock);
            credentials.insert(id, tenant, user, MfaRepository.TOTP, request.getLabel(),
                    cipher.encrypt(secret, tenant, COLUMN), null, now);
            MfaEnrollment enrollment = new MfaEnrollment(id);
            enrollment.setSecret(secret);
            MeAccountEntity account = users.findById(user).orElseThrow(
                    () -> new ResourceNotFoundException(IamEvents.USER_ACCOUNT, user));
            enrollment.setOtpauthUrl(Totp.otpauthUrl(ISSUER, label(account), secret));
            if (live.stream().noneMatch(c -> MfaRepository.RECOVERY.equals(c.type()))) {
                List<String> codes = RecoveryCodeSet.generate();
                credentials.insert(UuidV7.next(clock), tenant, user, MfaRepository.RECOVERY, null,
                        cipher.encrypt(RecoveryCodeSet.hashes(codes), tenant, COLUMN), null, now);
                enrollment.setRecoveryCodes(codes);
            }
            enrollment.setMfaToken(startStep(tenant, user, ENROLL_STEP + id, now));
            return enrollment;
        });
    }

    /** iam.DisableMfa: ends one of the caller's factors, given a current code (or recovery code). */
    public void disable(DisableMfaRequest request) {
        validator.validate(request);
        AuthContext caller = AuthContext.require();
        UUID user = caller.userId();
        OffsetDateTime now = now();
        if (!retireFactor(caller, request, now)) {
            throw wrongCode(user, now);
        }
    }

    /** Retires the factor in one transaction when the code is accepted; false (nothing changed) when it is not. */
    private boolean retireFactor(AuthContext caller, DisableMfaRequest request, OffsetDateTime now) {
        UUID user = caller.userId();
        return doInWriteTransaction(() -> {
            requireNotLocked(user, now);
            List<Credential> live = credentials.forUpdate(user);
            Credential target = live.stream().filter(c -> c.id().equals(request.getMfaCredentialId())).findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("iam.mfa_credential",
                            request.getMfaCredentialId()));
            if (!acceptCode(caller.tenantId().orElseThrow(), user, live, request.getCode(), now)) {
                return false;
            }
            credentials.retire(target.id(), user, now);
            boolean factorLeft = live.stream().anyMatch(c -> !c.id().equals(target.id())
                    && MfaRepository.TOTP.equals(c.type()) && c.usable());
            if (!factorLeft) {
                // recovery codes only back up an authenticator: without one they go too, and sign-in is
                // password-only again
                credentials.retireType(user, MfaRepository.RECOVERY, now);
            }
            outbox.publish(IamEvents.MFA_DISABLED, IamEvents.USER_ACCOUNT, user, IamEvents.payload(user));
            return true;
        });
    }

    /** iam.RegenerateRecoveryCodes: a fresh set (shown once) replaces every earlier code, given a current code. */
    public RecoveryCodes regenerateRecoveryCodes(RegenerateRecoveryCodesRequest request) {
        validator.validate(request);
        AuthContext caller = AuthContext.require();
        UUID user = caller.userId();
        OffsetDateTime now = now();
        Optional<List<String>> fresh = replaceRecoveryCodes(caller, request, now);
        return new RecoveryCodes().recoveryCodes(fresh.orElseThrow(() -> wrongCode(user, now)));
    }

    /** A fresh set of recovery codes in one transaction when the code is accepted; empty when it is not. */
    private Optional<List<String>> replaceRecoveryCodes(AuthContext caller, RegenerateRecoveryCodesRequest request,
                                                        OffsetDateTime now) {
        UUID user = caller.userId();
        return doInWriteTransaction(() -> {
            requireNotLocked(user, now);
            List<Credential> live = credentials.forUpdate(user);
            if (live.stream().noneMatch(c -> MfaRepository.TOTP.equals(c.type()) && c.usable())) {
                throw new ConflictException("MFA_NOT_ENROLLED", "Enroll an authenticator app first.");
            }
            if (!acceptCode(caller.tenantId().orElseThrow(), user, live, request.getCode(), now)) {
                return Optional.<List<String>>empty();
            }
            credentials.retireType(user, MfaRepository.RECOVERY, now);
            List<String> codes = RecoveryCodeSet.generate();
            credentials.insert(UuidV7.next(clock), caller.tenantId().orElseThrow(), user, MfaRepository.RECOVERY, null,
                    cipher.encrypt(RecoveryCodeSet.hashes(codes), caller.tenantId().orElseThrow(), COLUMN), now, now);
            outbox.publish(IamEvents.RECOVERY_CODES_REGENERATED, IamEvents.USER_ACCOUNT, user,
                    IamEvents.payload(user));
            return Optional.of(codes);
        });
    }

    /**
     * A new one-time MFA step for the user (inside the caller's transaction): earlier open steps end, so only the
     * latest token works. Returns the token; only its hash is stored.
     */
    String startStep(UUID tenant, UUID user, String step, OffsetDateTime now) {
        tokens.revokeOneTime(user, now);
        String token = RefreshTokens.generate(tenant);
        tokens.insertOneTime(UuidV7.next(clock), tenant, user, step, RefreshTokens.hash(token),
                now.plus(STEP_LIFETIME), now);
        return token;
    }

    /**
     * Checks a code against an open MFA step (inside a write transaction bound to the token's tenant). Returns the
     * account when the step is complete; empty for any refusal, after recording a failed attempt. Never throws for
     * a wrong code, so the failure count is committed.
     */
    Optional<Completed> completeStep(UUID tenant, String token, String code, OffsetDateTime now,
                                     int lockThreshold, int maxLockMinutes) {
        Optional<AuthTokenRepository.OneTimeRow> found = tokens.findOneTimeForUpdate(RefreshTokens.hash(token));
        if (found.isEmpty() || found.get().revoked() || !found.get().expiresAt().isAfter(now)) {
            return Optional.empty();
        }
        AuthTokenRepository.OneTimeRow step = found.get();
        UUID user = step.userId();
        Optional<SignInAccount> account = users.findSignInById(user);
        if (account.isEmpty() || !account.get().canSignInAt(now)) {
            tokens.revokeOneTime(user, now);
            return Optional.empty();
        }
        List<Credential> live = credentials.forUpdate(user);
        boolean accepted;
        if (SIGN_IN_STEP.equals(step.name())) {
            accepted = acceptCode(tenant, user, live, code, now);
        } else if (step.name().startsWith(ENROLL_STEP)) {
            UUID pending = UUID.fromString(step.name().substring(ENROLL_STEP.length()));
            accepted = live.stream().filter(c -> c.id().equals(pending) && c.verifiedAt().isEmpty()).findFirst()
                    .map(c -> verifyEnrollment(tenant, user, c, live, code, now)).orElse(false);
        } else {
            accepted = false;
        }
        if (!accepted) {
            users.recordFailure(user, now, lockThreshold, maxLockMinutes);
            outbox.publish(IamEvents.SIGN_IN_FAILED, IamEvents.USER_ACCOUNT, user, IamEvents.payload(user));
            return Optional.empty();
        }
        tokens.revokeOneTime(user, now);
        return Optional.of(new Completed(account.get(), step.name().startsWith(ENROLL_STEP)));
    }

    /**
     * A completed MFA step: a sign-in (tokens follow) or the verification of a new factor (no tokens: enrolling
     * from a session must never mint a longer-lived one).
     */
    record Completed(SignInAccount account, boolean enrollment) {
    }

    private boolean verifyEnrollment(UUID tenant, UUID user, Credential pending, List<Credential> live, String code,
                                     OffsetDateTime now) {
        Optional<String> secret = readableSecret(tenant, pending);
        OptionalLong step = secret.isEmpty() ? OptionalLong.empty()
                : Totp.verify(secret.get(), code == null ? "" : code.trim(), now.toEpochSecond(), Long.MIN_VALUE);
        if (step.isEmpty()) {
            return false;
        }
        credentials.verify(pending.id(), user, stepStart(step.getAsLong()), now);
        live.stream().filter(c -> MfaRepository.RECOVERY.equals(c.type()) && c.verifiedAt().isEmpty())
                .forEach(c -> credentials.verify(c.id(), user, null, now));
        outbox.publish(IamEvents.MFA_ENROLLED, IamEvents.USER_ACCOUNT, user, IamEvents.payload(user));
        return true;
    }

    /** A current code of any usable authenticator, else one unused recovery code (which is then used up). */
    private boolean acceptCode(UUID tenant, UUID user, List<Credential> live, String code, OffsetDateTime now) {
        String trimmed = code == null ? "" : code.trim();
        for (Credential c : live) {
            if (MfaRepository.TOTP.equals(c.type()) && c.usable()) {
                long last = c.lastUsedAt().map(t -> Math.floorDiv(t.toEpochSecond(), Totp.STEP_SECONDS))
                        .orElse(Long.MIN_VALUE);
                Optional<String> secret = readableSecret(tenant, c);
                OptionalLong step = secret.isEmpty() ? OptionalLong.empty()
                        : Totp.verify(secret.get(), trimmed, now.toEpochSecond(), last);
                if (step.isPresent()) {
                    credentials.touch(c.id(), user, stepStart(step.getAsLong()), now);
                    return true;
                }
            }
        }
        for (Credential c : live) {
            if (MfaRepository.RECOVERY.equals(c.type()) && c.usable()) {
                Optional<String> remaining = readableSecret(tenant, c)
                        .flatMap(stored -> RecoveryCodeSet.consume(stored, trimmed));
                if (remaining.isPresent()) {
                    credentials.replaceSecret(c.id(), user, cipher.encrypt(remaining.get(), tenant, COLUMN), now);
                    credentials.touch(c.id(), user, now, now);
                    return true;
                }
            }
        }
        return false;
    }

    /** For changes to the factors themselves: a wrong code counts towards the lockout (own transaction), 422. */
    private ValidationFailedException wrongCode(UUID user, OffsetDateTime now) {
        runInWriteTransaction(() -> {
            users.recordFailure(user, now, SignInService.LOCK_THRESHOLD, SignInService.MAX_LOCK_MINUTES);
            outbox.publish(IamEvents.SIGN_IN_FAILED, IamEvents.USER_ACCOUNT, user, IamEvents.payload(user));
        });
        return new ValidationFailedException(List.of(new FieldError("code", "is not a valid current code",
                "INVALID_CODE")));
    }

    private String secretOf(UUID tenant, Credential credential) {
        return cipher.decrypt(credential.secretEnvelope(), tenant, COLUMN).orElseThrow();
    }

    /** The secret, or empty (logged by id) when the stored envelope does not open: that factor simply fails. */
    private Optional<String> readableSecret(UUID tenant, Credential credential) {
        try {
            return Optional.of(secretOf(tenant, credential));
        } catch (IllegalStateException e) {
            LOG.error("MFA credential {} does not decrypt; check mysms.secrets.keys", credential.id());
            return Optional.empty();
        }
    }

    /** A locked account (too many wrong passwords or codes) cannot try codes through its sessions either. */
    private void requireNotLocked(UUID user, OffsetDateTime now) {
        if (users.findSignInById(user).map(a -> !a.canSignInAt(now)).orElse(true)) {
            throw new PermissionDeniedException("Too many failed attempts; try again later.");
        }
    }

    private static OffsetDateTime stepStart(long step) {
        return OffsetDateTime.ofInstant(Instant.ofEpochSecond(step * Totp.STEP_SECONDS), ZoneOffset.UTC);
    }

    private static String label(MeAccountEntity account) {
        return Optional.ofNullable(account.getUsername()).or(() -> Optional.ofNullable(account.getEmail()))
                .or(() -> Optional.ofNullable(account.getMobileNo())).orElse(account.getId().toString());
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
