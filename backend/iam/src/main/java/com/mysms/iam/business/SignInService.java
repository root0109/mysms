package com.mysms.iam.business;

import com.mysms.authclient.AuthContext;
import com.mysms.contracts.iam.model.CompleteMfaRequest;
import com.mysms.contracts.iam.model.LoginRequest;
import com.mysms.contracts.iam.model.LoginResult;
import com.mysms.contracts.iam.model.TokenPair;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.UnauthenticatedException;
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.config.IamEventsConfig;
import com.mysms.iam.events.IamEvents;
import com.mysms.iam.mapper.MeAccountMapper;
import com.mysms.iam.repository.AuthTokenRepository;
import com.mysms.iam.repository.GrantRepository;
import com.mysms.iam.repository.RoleGrantRow;
import com.mysms.iam.repository.SignInAccount;
import com.mysms.iam.repository.TenantDirectoryRepository;
import com.mysms.iam.repository.AccountRepository;
import com.mysms.iam.repository.SignInSessionRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Sign-in, token refresh and sign-out (iam.Login, iam.RefreshToken, iam.Logout) — the issuer's half of the token
 * contract (JAVA-CODING-RULES §16 security floor):
 * <ul>
 *   <li>argon2id passwords: every refusal in a known tenant pays one verification and one write transaction, whether
 *       or not the account exists;</li>
 *   <li>one answer (401 INVALID_CREDENTIALS) for every failure, so accounts cannot be probed;</li>
 *   <li>progressive lockout: from the 5th consecutive failure sign-in is refused for 1, 2, 4... up to 60 minutes;</li>
 *   <li>short-lived RS256 access tokens; opaque refresh tokens stored as SHA-256 only, rotated on every use, and a
 *       reused (already rotated) refresh token revokes its whole session; refresh needs an active account in an
 *       active tenant but ignores the password lockout, so failed attempts by others never end a session;</li>
 *   <li>every sign-in, failure, sign-out and detected reuse is an event in the outbox (ids only).</li>
 * </ul>
 * Accounts with a verified second factor get an MFA token instead of tokens, and finish with iam.CompleteMfa
 * (TOTP or a recovery code); their access tokens then carry {@code amr: [pwd, mfa]}.
 */
@Service
public class SignInService {

    static final int LOCK_THRESHOLD = 5;
    static final int MAX_LOCK_MINUTES = 60;
    private static final int MAX_TENANT_CODE = 60;
    private static final int MAX_IDENTIFIER = 320;
    private static final int MAX_PASSWORD = 1024;
    private static final int MAX_CODE = 32;
    private static final String FAILED = "The sign-in details are not valid, or sign-in is not possible right now.";
    private static final String REFRESH_FAILED = "The refresh token is not valid.";
    private static final String BEARER = "Bearer";
    private static final Logger LOG = LoggerFactory.getLogger(SignInService.class);

    private final ModuleDatabase database;
    private final TenantDirectoryRepository tenants;
    private final AccountRepository users;
    private final GrantRepository grants;
    private final AuthTokenRepository refreshTokens;
    private final SignInSessionRepository sessions;
    private final MeAccountMapper userMapper;
    private final PasswordHasher passwords;
    private final TokenIssuer issuer;
    private final OutboxWriter outbox;
    private final DtoValidator validator;
    private final MfaService mfa;
    private final Clock clock;

    public SignInService(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database,
                         TenantDirectoryRepository tenants, AccountRepository users, GrantRepository grants,
                         AuthTokenRepository refreshTokens, SignInSessionRepository sessions,
                         MeAccountMapper userMapper, PasswordHasher passwords, TokenIssuer issuer,
                         @Qualifier(IamEventsConfig.OUTBOX) OutboxWriter outbox, DtoValidator validator,
                         MfaService mfa, Clock clock) {
        this.database = database;
        this.tenants = tenants;
        this.users = users;
        this.grants = grants;
        this.refreshTokens = refreshTokens;
        this.sessions = sessions;
        this.userMapper = userMapper;
        this.passwords = passwords;
        this.issuer = issuer;
        this.outbox = outbox;
        this.validator = validator;
        this.mfa = mfa;
        this.clock = clock;
    }

    /** iam.Login: password sign-in into one tenant. */
    public LoginResult login(LoginRequest request, String clientIp, String userAgent) {
        validator.validate(request);
        String password = request.getPassword();
        if (request.getTenantCode().length() > MAX_TENANT_CODE || request.getIdentifier().length() > MAX_IDENTIFIER
                || password.length() > MAX_PASSWORD) {
            throw invalid();
        }
        Optional<UUID> tenant = database.tx().readWithoutTenant(
                () -> tenants.findActiveTenant(request.getTenantCode().trim()));
        if (tenant.isEmpty()) {
            passwords.burn(password);
            throw invalid();
        }
        return asTenant(tenant.get(), null, () -> loginInTenant(tenant.get(), request.getIdentifier().trim(), password,
                clientIp, userAgent));
    }

    private LoginResult loginInTenant(UUID tenant, String identifier, String password, String clientIp,
                                      String userAgent) {
        List<SignInAccount> found = database.tx().read(() -> users.findForSignIn(identifier));
        if (found.size() != 1) {
            if (found.size() > 1) {
                LOG.warn("sign-in identifier matches more than one account in tenant {}", tenant);
            }
            passwords.burn(password);
            // the same write a known account's failure costs, so timing does not tell the two apart
            database.tx().write(() -> {
                outbox.publish(IamEvents.SIGN_IN_FAILED, IamEvents.USER_ACCOUNT, IamEvents.UNKNOWN_ACCOUNT,
                        IamEvents.payload(IamEvents.UNKNOWN_ACCOUNT));
                return null;
            });
            throw invalid();
        }
        SignInAccount account = found.get(0);
        OffsetDateTime now = now();
        boolean allowed = account.canSignInAt(now);
        // always pay for one verification, also when the account cannot sign in anyway
        boolean passwordMatches = allowed ? passwords.matches(password, account.passwordHash()) : burnFalse(password);
        if (!allowed || !passwordMatches) {
            asTenant(tenant, account.id(), () -> database.tx().write(() -> {
                if (!passwordMatches && account.lockedUntil().map(until -> !now.isBefore(until)).orElse(true)) {
                    users.recordFailure(account.id(), now, LOCK_THRESHOLD, MAX_LOCK_MINUTES);
                }
                outbox.publish(IamEvents.SIGN_IN_FAILED, IamEvents.USER_ACCOUNT, account.id(),
                        IamEvents.payload(account.id()));
                return null;
            }));
            throw invalid();
        }
        if (database.tx().read(() -> users.hasActiveSecondFactor(account.id()))) {
            // the password is right; tokens wait for the second factor (iam.CompleteMfa, one try budget shared with
            // the password lockout). Nothing else about the account is returned yet.
            String mfaToken = asTenant(tenant, account.id(), () -> database.tx().write(
                    () -> mfa.startStep(tenant, account.id(), MfaService.SIGN_IN_STEP, now)));
            LoginResult result = new LoginResult();
            result.setMfaRequired(true);
            result.setMfaToken(mfaToken);
            return result;
        }
        return startSession(tenant, account, clientIp, userAgent, now, List.of("pwd"));
    }

    /**
     * iam.CompleteMfa: the MFA token from Login plus a current code or a recovery code gives the token pair; the
     * token from EnrollMfa plus the new authenticator's first code verifies that factor (and issues no tokens).
     * Every refusal is the same 401; a wrong code counts towards the sign-in lockout, and a locked, inactive or
     * expired account cannot complete. The token works once and for five minutes.
     */
    public LoginResult completeMfa(CompleteMfaRequest request, String clientIp, String userAgent) {
        validator.validate(request);
        Optional<UUID> tenant = RefreshTokens.tenantOf(request.getMfaToken());
        if (tenant.isEmpty() || request.getCode().length() > MAX_CODE) {
            throw invalid();
        }
        if (!database.tx().readWithoutTenant(() -> tenants.isActive(tenant.get()))) {
            throw invalid();
        }
        OffsetDateTime now = now();
        Optional<MfaService.Completed> done = asTenant(tenant.get(), null, () -> database.tx().write(
                () -> mfa.completeStep(tenant.get(), request.getMfaToken(), request.getCode(), now, LOCK_THRESHOLD,
                        MAX_LOCK_MINUTES)));
        if (done.isEmpty()) {
            throw invalid();
        }
        if (done.get().enrollment()) {
            // the new factor is verified; the session that enrolled it goes on, and no new tokens are issued
            LoginResult verified = new LoginResult();
            verified.setMfaRequired(false);
            return verified;
        }
        return startSession(tenant.get(), done.get().account(), clientIp, userAgent, now, List.of("pwd", "mfa"));
    }

    /** A new session with a refresh token and an access token; records the successful sign-in. */
    private LoginResult startSession(UUID tenant, SignInAccount account, String clientIp, String userAgent,
                                     OffsetDateTime now, List<String> authMethods) {
        UUID session = UuidV7.next(clock);
        String refresh = RefreshTokens.generate(tenant);
        AccessClaims claims = asTenant(tenant, account.id(), () -> database.tx().write(() -> {
            users.recordSuccess(account.id(), now);
            sessions.insert(session, tenant, account.id(), clientIp, userAgent, now);
            refreshTokens.insertRefresh(UuidV7.next(clock), tenant, account.id(), session, RefreshTokens.hash(refresh),
                    clientIp, now.plus(issuer.refreshTokenTtl()), now);
            outbox.publish(IamEvents.SIGNED_IN, IamEvents.USER_ACCOUNT, account.id(),
                    IamEvents.session(account.id(), session));
            return claims(account, tenant, session);
        }));
        LoginResult result = new LoginResult();
        TokenIssuer.IssuedToken access = issuer.issue(claims, authMethods);
        result.setTokens(new TokenPair(access.token(), refresh, Math.toIntExact(issuer.accessTokenTtl().toSeconds()),
                BEARER));
        result.setUser(asTenant(tenant, account.id(), () -> database.tx().read(
                () -> users.findById(account.id()).map(userMapper::toDto).orElseThrow())));
        result.setMfaRequired(false);
        return result;
    }

    /** iam.RefreshToken: rotates the refresh token and issues a fresh access token with current permissions. */
    public TokenPair refresh(String presented) {
        Optional<UUID> tenant = RefreshTokens.tenantOf(presented);
        if (tenant.isEmpty()) {
            throw refreshInvalid();
        }
        String rotated = RefreshTokens.generate(tenant.get());
        // a suspended or closed tenant signs nobody in — and keeps nobody signed in
        boolean tenantActive = database.tx().readWithoutTenant(() -> tenants.isActive(tenant.get()));
        Optional<AccessClaims> claims = asTenant(tenant.get(), null, () -> database.tx().write(
                () -> rotate(tenant.get(), RefreshTokens.hash(presented), rotated, tenantActive)));
        return claims.map(c -> tokenPair(c, rotated)).orElseThrow(SignInService::refreshInvalid);
    }

    /** Inside one transaction; returns empty for every refusal, after recording any revocation it caused. */
    private Optional<AccessClaims> rotate(UUID tenant, String hash, String rotated, boolean tenantActive) {
        Optional<AuthTokenRepository.RefreshRow> row = refreshTokens.findForUpdate(hash);
        if (row.isEmpty()) {
            return Optional.empty();
        }
        AuthTokenRepository.RefreshRow token = row.get();
        OffsetDateTime now = now();
        if (token.rotated()) {
            refreshTokens.revokeFamily(token.family(), now);
            sessions.end(token.family(), now);
            outbox.publish(IamEvents.REFRESH_REUSE_DETECTED, IamEvents.USER_ACCOUNT, token.userId(),
                    IamEvents.session(token.userId(), token.family()));
            LOG.warn("refresh token reuse detected; session {} of tenant {} revoked", token.family(), tenant);
            return Optional.empty();
        }
        if (token.revokedAt().isPresent()) {
            return Optional.empty();
        }
        Optional<SignInAccount> account = users.findSignInById(token.userId());
        if (!tenantActive || !token.expiresAt().isAfter(now) || account.isEmpty() || !account.get().canRefreshAt(now)
                || !sessions.isOpen(token.family())) {
            refreshTokens.revokeFamily(token.family(), now);
            sessions.end(token.family(), now);
            return Optional.empty();
        }
        refreshTokens.revoke(token.id(), now);
        refreshTokens.insertRefresh(UuidV7.next(clock), tenant, token.userId(), token.family(),
                RefreshTokens.hash(rotated), null, now.plus(issuer.refreshTokenTtl()), now);
        sessions.touch(token.family(), now);
        return Optional.of(claims(account.get(), tenant, token.family()));
    }

    /** iam.Logout: ends the token's session, or every session of the user; access tokens expire on their own. */
    public void logout(boolean allDevices) {
        AuthContext caller = AuthContext.require();
        database.tx().write(() -> {
            OffsetDateTime now = now();
            if (allDevices) {
                refreshTokens.revokeAll(caller.userId(), now);
                sessions.deactivateDevices(caller.userId(), now);
                sessions.endAll(caller.userId(), now);
            } else {
                caller.sessionId().ifPresent(session -> {
                    refreshTokens.revokeFamily(session, now);
                    sessions.deactivateSessionDevice(session, caller.userId(), now);
                    sessions.end(session, now);
                });
            }
            outbox.publish(IamEvents.SIGNED_OUT, IamEvents.USER_ACCOUNT, caller.userId(),
                    IamEvents.payload(caller.userId()));
            return null;
        });
    }

    /** Claims from the database as of now: active grants, effective permissions (compacted), linked person. */
    private AccessClaims claims(SignInAccount account, UUID tenant, UUID session) {
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        List<RoleGrantRow> grantRows = grants.activeGrants(account.id(), today);
        Set<String> permissions = PermissionCompactor.compact(grants.effectivePermissions(account.id(), today),
                grants.catalogue());
        Set<UUID> institutions = grantRows.stream()
                .filter(g -> "institution".equals(g.scopeType()))
                .map(g -> g.scopeId().orElseThrow())
                .collect(Collectors.toUnmodifiableSet());
        return new AccessClaims(account.id(), tenant, account.personId(), session, permissions, institutions,
                grantRows.stream().map(RoleGrantRow::claim).distinct().toList(),
                grantRows.stream().anyMatch(g -> RoleGrantRow.TENANT.equals(g.scopeType())),
                PermissionCompactor.compact(grants.tenantWidePermissions(account.id(), today), grants.catalogue()));
    }

    private TokenPair tokenPair(AccessClaims claims, String refresh) {
        TokenIssuer.IssuedToken access = issuer.issue(claims);
        return new TokenPair(access.token(), refresh, Math.toIntExact(issuer.accessTokenTtl().toSeconds()), BEARER);
    }

    private boolean burnFalse(String password) {
        passwords.burn(password);
        return false;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    /** Runs work as the given tenant (and user, once known): sign-in establishes identity itself. */
    private static <T> T asTenant(UUID tenant, UUID user, java.util.function.Supplier<T> work) {
        CallContext base = CallContextHolder.current().orElseGet(() -> CallContext.anonymous(
                UUID.randomUUID().toString().replace("-", "")));
        return CallContextHolder.callAs(base.withIdentity(tenant, user), work);
    }

    private static UnauthenticatedException invalid() {
        return new UnauthenticatedException(UnauthenticatedException.INVALID_CREDENTIALS, FAILED);
    }

    private static UnauthenticatedException refreshInvalid() {
        return new UnauthenticatedException(UnauthenticatedException.INVALID_CREDENTIALS, REFRESH_FAILED);
    }
}
