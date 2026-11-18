package com.mysms.iam.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.UnauthenticatedException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.iam.api.model.ApiTokenCreated;
import com.mysms.iam.api.model.AuthToken;
import com.mysms.iam.api.model.CreateApiTokenRequest;
import com.mysms.iam.api.model.TokenPair;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.config.IamEventsConfig;
import com.mysms.iam.domain.AuthTokenEntity;
import com.mysms.iam.events.IamEvents;
import com.mysms.iam.mapper.AuthTokenMapper;
import com.mysms.iam.repository.AccountRepository;
import com.mysms.iam.repository.ApiTokenRepository;
import com.mysms.iam.repository.GrantRepository;
import com.mysms.iam.repository.SignInAccount;
import com.mysms.iam.repository.TenantDirectoryRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Personal API tokens (iam.CreateApiToken, iam.ListAuthTokens, iam.DeleteAuthToken, and their exchange at
 * iam.RefreshToken):
 * <ul>
 *   <li>a user creates tokens for themselves only, never while an administrator acts as them; a token may be limited
 *       to some of the creator's own permissions, lives 90 days by default and a year at most; a call made with an API
 *       token creates only a token limited to some of that token's abilities (its own by default, never unlimited)
 *       that ends no later than it;</li>
 *   <li>the secret is answered once, at creation; only its SHA-256 is stored and no answer ever contains it again;</li>
 *   <li>the owner lists and revokes their tokens; another user's only with the permission held tenant-wide (an
 *       administrator) — anyone else gets 404;</li>
 *   <li>presented at iam.RefreshToken, a usable token of an active account gives a 15-minute access token whose
 *       permissions are the owner's current ones, within the token's limits; the token itself is not rotated.</li>
 * </ul>
 */
@Service
public class ApiTokenService extends AppTransactionManager {

    static final String AGGREGATE = AuthTokenEntity.TABLE;
    /** The {@code amr} of an access token exchanged for an API token. */
    static final String API_TOKEN_METHOD = "pat";
    static final Duration DEFAULT_LIFETIME = Duration.ofDays(90);
    static final Duration MAX_LIFETIME = Duration.ofDays(366);
    private static final String PERMISSION_READ = "iam.auth_token.read";
    private static final String PERMISSION_DELETE = "iam.auth_token.delete";
    private static final int MAX_NAME = 120;
    private static final int MAX_ABILITY = 100;
    private static final String BEARER = "Bearer";
    private static final String INVALID = "The refresh token is not valid.";

    private final ApiTokenRepository tokens;
    private final AuthTokenMapper mapper;
    private final TenantDirectoryRepository tenants;
    private final AccountRepository accounts;
    private final GrantRepository grants;
    private final ClaimsFactory claims;
    private final TokenIssuer issuer;
    private final AccessRules rules;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public ApiTokenService(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(IamDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            ApiTokenRepository tokens, AuthTokenMapper mapper, TenantDirectoryRepository tenants,
            AccountRepository accounts, GrantRepository grants, ClaimsFactory claims, TokenIssuer issuer,
            AccessRules rules, @Qualifier(IamEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(IamEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock) {
        super(database, transactions);
        this.tokens = tokens;
        this.mapper = mapper;
        this.tenants = tenants;
        this.accounts = accounts;
        this.grants = grants;
        this.claims = claims;
        this.issuer = issuer;
        this.rules = rules;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    /**
     * iam.CreateApiToken: a token of the caller's own, its secret answered once. Called with an API token, the new
     * token is limited to that token's abilities (its own by default, never unlimited: 422) and ends no later (422).
     */
    public ApiTokenCreated createApiToken(UUID userId, CreateApiTokenRequest request, String clientIp) {
        validator.validate(request);
        AuthContext caller = AuthContext.require();
        AccessRules.requireOwnIdentity();
        if (!caller.userId().equals(userId)) {
            throw new PermissionDeniedException("API tokens are created by their owner only.");
        }
        OffsetDateTime now = TenantCalls.now(clock);
        Optional<AuthTokenEntity> parent = AuthContext.currentAuthMethods().contains(API_TOKEN_METHOD)
                ? Optional.of(callingToken(caller, now)) : Optional.empty();
        String name = request.getName().trim();
        List<FieldError> errors = new ArrayList<>();
        if (name.isEmpty() || name.length() > MAX_NAME) {
            errors.add(new FieldError("name", "must be 1 to " + MAX_NAME + " characters", "Size"));
        }
        OffsetDateTime asked = request.getExpiresAt();
        OffsetDateTime latest = parent.map(AuthTokenEntity::getExpiresAt).map(end -> end.withOffsetSameInstant(
                now.getOffset())).orElse(null);
        OffsetDateTime expires = asked != null ? asked.withOffsetSameInstant(now.getOffset())
                : latest != null && latest.isBefore(now.plus(DEFAULT_LIFETIME)) ? latest : now.plus(DEFAULT_LIFETIME);
        if (!expires.isAfter(now) || expires.isAfter(now.plus(MAX_LIFETIME))) {
            errors.add(new FieldError("expiresAt", "must be in the future and at most a year away", "OUT_OF_RANGE"));
        } else if (latest != null && expires.isAfter(latest)) {
            errors.add(new FieldError("expiresAt", "must not outlive the API token this call was made with",
                    "OUTLIVES_TOKEN"));
        }
        Set<String> abilities = new LinkedHashSet<>();
        List<String> named = request.getAbilities() == null ? List.of() : request.getAbilities();
        List<String> parentAbilities = parent.map(AuthTokenEntity::getScopes).orElse(null);
        List<String> wanted = !named.isEmpty() || parentAbilities == null ? named : parentAbilities;
        if (parent.isPresent() && wanted.isEmpty()) {
            errors.add(new FieldError("abilities", "name the abilities: a token created with an unlimited API token "
                    + "is never unlimited itself", "REQUIRED"));
        }
        Set<String> catalogue = new HashSet<>(doInReadTransaction(grants::catalogue));
        for (int i = 0; i < wanted.size(); i++) {
            String code = wanted.get(i) == null ? "" : wanted.get(i).trim();
            if (code.length() > MAX_ABILITY || !catalogue.contains(code)) {
                errors.add(new FieldError("abilities[" + i + "]", "is not a permission", "NOT_FOUND"));
            } else if (!caller.hasPermission(code)) {
                errors.add(new FieldError("abilities[" + i + "]", "is not one of your permissions", "NOT_HELD"));
            } else if (parentAbilities != null && !parentAbilities.isEmpty() && !parentAbilities.contains(code)) {
                errors.add(new FieldError("abilities[" + i + "]", "is not one of the abilities of the API token this "
                        + "call was made with", "NOT_IN_TOKEN"));
            } else {
                abilities.add(code);
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        UUID tenant = caller.tenantId().orElseThrow();
        String secret = ApiTokens.generate(tenant);
        AuthTokenEntity entity = AuthTokenEntity.builder().userId(userId).tokenType("api").name(name)
                .scopes(abilities.isEmpty() ? null : List.copyOf(abilities)).ipAddress(clientIp).expiresAt(expires)
                .build();
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(tenant);
        AuthToken created = doInWriteTransaction(() -> {
            AuthTokenEntity stored = tokens.insert(entity, RefreshTokens.hash(secret));
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(IamEvents.API_TOKEN_CREATED, IamEvents.USER_ACCOUNT, userId,
                    IamEvents.session(userId, stored.getId()));
            return mapper.toDto(stored);
        });
        return new ApiTokenCreated(secret, created);
    }

    /** The live, unrevoked, unexpired API token the call was made with (its session); 403 when it no longer works. */
    private AuthTokenEntity callingToken(AuthContext caller, OffsetDateTime now) {
        return caller.sessionId()
                .flatMap(token -> doInPrimaryReadTransaction(() -> tokens.find(caller.userId(), token)))
                .filter(token -> token.getRevokedAt() == null && token.getExpiresAt().isAfter(now))
                .orElseThrow(() -> new PermissionDeniedException(
                        "The API token this call was made with no longer works."));
    }

    /** iam.ListAuthTokens: the user's API tokens (never their secrets), to the owner or a tenant-wide reader. */
    public Page<AuthToken> listAuthTokens(UUID userId, Integer pageNumber, Integer pageSize, String cursor,
                                          String filter, List<String> sortBy) {
        requireOwnerOrAdministrator(userId, DataScope.current(PERMISSION_READ));
        SortSpec sort = SortSpec.parse(sortBy, ApiTokenRepository.SORT_FIELDS, ApiTokenRepository.DEFAULT_ORDER,
                ApiTokenRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ApiTokenRepository.FILTER_FIELDS)
                .and(ApiTokenRepository.ofUser(userId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + userId, clock);
        return doInReadTransaction(() -> tokens.list(page, where).map(mapper::toDto));
    }

    /** iam.DeleteAuthToken: revokes the token at once (soft delete under If-Match). */
    public void deleteAuthToken(UUID userId, UUID id, String ifMatch) {
        requireOwnerOrAdministrator(userId, DataScope.current(PERMISSION_DELETE));
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            AuthTokenEntity token = tokens.find(userId, id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (token.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            token.onDeletingData(Actor.current(), clock);
            if (tokens.revoke(token, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(IamEvents.API_TOKEN_REVOKED, IamEvents.USER_ACCOUNT, userId, IamEvents.session(userId, id));
        });
    }

    /**
     * The exchange at iam.RefreshToken: an access token for the token's owner, limited to the token's permissions,
     * with the token's id as its session. Every refusal is the refresh operation's one 401.
     */
    public TokenPair exchange(String presented) {
        UUID tenant = ApiTokens.tenantOf(presented).orElseThrow(ApiTokenService::invalid);
        if (!doInGlobalPrimaryReadTransaction(() -> tenants.isActive(tenant))) {
            throw invalid();
        }
        AccessClaims granted = TenantCalls.as(tenant, null, () -> doInWriteTransaction(() -> {
            OffsetDateTime now = TenantCalls.now(clock);
            AuthTokenEntity token = tokens.use(RefreshTokens.hash(presented), now).orElseThrow(ApiTokenService::invalid);
            SignInAccount owner = accounts.findSignInById(token.getUserId()).filter(a -> a.canRefreshAt(now))
                    .orElseThrow(ApiTokenService::invalid);
            return claims.full(owner.id(), owner.personId(), tenant, token.getId(),
                    Optional.ofNullable(token.getScopes()).map(Set::copyOf));
        }));
        TokenIssuer.IssuedToken access = issuer.issue(granted, List.of(API_TOKEN_METHOD));
        return new TokenPair(access.token(), presented, Math.toIntExact(issuer.accessTokenTtl().toSeconds()), BEARER);
    }

    /** The owner, or a holder of the permission tenant-wide; anyone else is told nothing exists (404). */
    private static void requireOwnerOrAdministrator(UUID userId, DataScope scope) {
        if (!AuthContext.require().userId().equals(userId) && !scope.tenantWide()) {
            throw new ResourceNotFoundException(IamEvents.USER_ACCOUNT, userId);
        }
    }

    private static UnauthenticatedException invalid() {
        return new UnauthenticatedException(UnauthenticatedException.INVALID_CREDENTIALS, INVALID);
    }
}
