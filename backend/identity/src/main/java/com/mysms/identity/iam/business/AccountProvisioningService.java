package com.mysms.identity.iam.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnauthenticatedException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.identity.iam.api.model.Availability;
import com.mysms.identity.iam.api.model.ImportUserRow;
import com.mysms.identity.iam.api.model.ImportUsersRequest;
import com.mysms.identity.iam.api.model.Operation;
import com.mysms.identity.iam.api.model.RoleGrant;
import com.mysms.identity.iam.api.model.SignUpInstitutionList;
import com.mysms.identity.iam.api.model.SignUpRequest;
import com.mysms.identity.iam.api.model.UserAccount;
import com.mysms.identity.iam.config.IamDatabaseConfig;
import com.mysms.identity.iam.config.IamEventsConfig;
import com.mysms.identity.iam.domain.OperationEntity;
import com.mysms.identity.iam.domain.UserAccountEntity;
import com.mysms.identity.iam.domain.UserRoleEntity;
import com.mysms.identity.iam.events.IamEvents;
import com.mysms.identity.iam.mapper.OperationMapper;
import com.mysms.identity.iam.repository.AccountContactRepository;
import com.mysms.identity.iam.repository.AccountContactRepository.Contact;
import com.mysms.identity.iam.repository.AccountRepository;
import com.mysms.identity.iam.repository.AccountSecretRepository;
import com.mysms.identity.iam.repository.OperationRepository;
import com.mysms.identity.iam.repository.SettingDefinitionRepository;
import com.mysms.identity.iam.repository.SettingValueRepository;
import com.mysms.identity.iam.repository.TenantAuthzVersionRepository;
import com.mysms.identity.iam.repository.TenantDirectoryRepository;
import com.mysms.identity.iam.repository.UserAccountRepository;
import com.mysms.identity.iam.repository.UserRoleRepository;
import com.mysms.identity.iam.domain.SettingDefinitionEntity;
import com.mysms.identity.iam.domain.SettingValueEntity;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Accounts created outside the standard create (iam.SignUp, iam.ImportUsers) and the login-name check administrators
 * use while creating them (iam.CheckUsernameAvailable):
 * <ul>
 *   <li>sign-up is open only where the tenant administrator set {@code security.selfSignUp}; it creates an active
 *       student account without a password and mails a 24-hour code that sets the first password and proves the
 *       address (iam.ResetPassword); the role in {@code security.selfSignUpRole}, if any, is granted at the one
 *       school the student names, which must be an active institution of the tenant (iam.ListSignUpInstitutions
 *       offers them), never tenant-wide;</li>
 *   <li>an address that already has an account gets a reset code instead, and the answer is the same receipt either
 *       way (the address and the student account type, never an id), mailed in the background, so sign-up never
 *       tells whether an address is registered;</li>
 *   <li>the full name of the sign-up form is not kept: an account holds no personal data (people.person does, and iam
 *       cannot create one for an anonymous caller);</li>
 *   <li>an import creates up to 500 accounts and their role grants in one transaction, all or nothing, under the same
 *       no-escalation rules as a single grant; rows come inline, because iam cannot read the platform's file
 *       store;</li>
 *   <li>the login-name check answers only signed-in administrators of accounts (anonymous: 401), so it is no
 *       enumeration oracle for the public.</li>
 * </ul>
 */
@Service
public class AccountProvisioningService extends AppTransactionManager {

    static final Duration SIGN_UP_TTL = Duration.ofHours(24);
    static final String SELF_SIGN_UP = "security.selfSignUp";
    static final String SELF_SIGN_UP_ROLE = "security.selfSignUpRole";
    private static final String PERMISSION_IMPORT = "iam.user_account.import_users";
    private static final List<String> ACCOUNT_ADMINISTRATION = List.of("iam.user_account.create",
            "iam.user_account.update", "iam.user_account.set_user_credentials", PERMISSION_IMPORT);
    private static final String STUDENT = "student";
    private static final String TENANT = "tenant";
    private static final String INSTITUTION = "institution";
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAX_TENANT_CODE = 60;
    private static final int MAX_EMAIL = 320;
    private static final int MAX_NAME = 200;
    private static final int MAX_USERNAME = 150;
    private static final int SUGGESTIONS = 10;
    private static final String SIGN_UP_CLOSED = "Sign-up is not open for this tenant.";
    private static final Logger LOG = LoggerFactory.getLogger(AccountProvisioningService.class);

    private final TenantDirectoryRepository tenants;
    private final AccountContactRepository contacts;
    private final AccountSecretRepository secrets;
    private final AccountRepository accounts;
    private final UserAccountRepository rows;
    private final UserRoleRepository grants;
    private final SettingDefinitionRepository definitions;
    private final SettingValueRepository values;
    private final OperationRepository operations;
    private final OperationMapper operationMapper;
    private final AccessRules rules;
    private final MailDelivery mail;
    private final SignUpSchools schools;
    private final AccountMessages messages;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final TenantAuthzVersionRepository authzVersions;

    public AccountProvisioningService(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(IamDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            TenantDirectoryRepository tenants, AccountContactRepository contacts, AccountSecretRepository secrets,
            AccountRepository accounts, UserAccountRepository rows, UserRoleRepository grants,
            SettingDefinitionRepository definitions, SettingValueRepository values, OperationRepository operations,
            OperationMapper operationMapper, AccessRules rules, MailDelivery mail, SignUpSchools schools,
            AccountMessages messages,
            @Qualifier(IamEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(IamEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock,
            TenantAuthzVersionRepository authzVersions) {
        super(database, transactions);
        this.tenants = tenants;
        this.contacts = contacts;
        this.secrets = secrets;
        this.accounts = accounts;
        this.rows = rows;
        this.grants = grants;
        this.definitions = definitions;
        this.values = values;
        this.operations = operations;
        this.operationMapper = operationMapper;
        this.rules = rules;
        this.mail = mail;
        this.schools = schools;
        this.messages = messages;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.authzVersions = authzVersions;
    }

    /** iam.SignUp: a student account in a tenant that allows self-registration; see the class comment. */
    public UserAccount signUp(SignUpRequest request, String clientIp) {
        validator.validate(request);
        String email = request.getEmail().trim();
        List<FieldError> errors = new ArrayList<>();
        if (email.length() > MAX_EMAIL || !EMAIL.matcher(email).matches()) {
            errors.add(new FieldError("email", "must be an e-mail address", "INVALID_EMAIL"));
        }
        String name = request.getFullName().trim();
        if (name.isEmpty() || name.length() > MAX_NAME) {
            errors.add(new FieldError("fullName", "must be 1 to " + MAX_NAME + " characters", "Size"));
        }
        if (!Boolean.TRUE.equals(request.getAcceptTerms())) {
            errors.add(new FieldError("acceptTerms", "the terms must be accepted", "TERMS_REQUIRED"));
        }
        if (request.getPersonaCategory() != null && !STUDENT.equals(request.getPersonaCategory())) {
            errors.add(new FieldError("personaCategory", "only students can sign up themselves", "NOT_ALLOWED"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        UUID tenant = openTenant(request.getTenantCode());
        UUID school = request.getInstitutionId();
        Locale locale = LocaleContextHolder.getLocale();
        return TenantCalls.as(tenant, null, () -> {
            String role = doInPrimaryReadTransaction(() -> {
                requireOpen();
                return tenantSetting(SELF_SIGN_UP_ROLE).trim();
            });
            mail.requireConfigured();
            if (!schools.isActive(school)) {
                // the same answer for an unknown, inactive or other tenant's institution
                throw new ValidationFailedException(List.of(new FieldError("institutionId",
                        "must be an active school of this tenant", "NOT_FOUND")));
            }
            String secret = RefreshTokens.generate(tenant);
            try {
                doInWriteTransaction(() -> register(tenant, school, email, role, secret, clientIp, locale))
                        .ifPresent(mail::sendLater);
            } catch (DuplicateKeyException e) {
                // a concurrent sign-up with the same address won the unique index: it got the code
                LOG.info("concurrent sign-up for one address in tenant {}", tenant);
            }
            return receipt(email);
        });
    }

    /**
     * iam.ListSignUpInstitutions: the schools a guest may sign up at, where sign-up is open (403 elsewhere, as for
     * iam.SignUp).
     */
    public SignUpInstitutionList listSignUpInstitutions(String tenantCode) {
        UUID tenant = openTenant(tenantCode);
        return TenantCalls.as(tenant, null, () -> {
            runInReadTransaction(this::requireOpen);
            return new SignUpInstitutionList(schools.all());
        });
    }

    /** The active tenant the code names; 403 for none, like a closed sign-up, so codes are not probed here. */
    private UUID openTenant(String tenantCode) {
        String code = tenantCode == null ? "" : tenantCode.trim();
        return (code.isEmpty() || code.length() > MAX_TENANT_CODE ? Optional.<UUID>empty()
                : doInGlobalPrimaryReadTransaction(() -> tenants.findActiveTenant(code)))
                .orElseThrow(() -> new PermissionDeniedException(SIGN_UP_CLOSED));
    }

    /** Inside a transaction of the tenant: 403 unless its administrator opened sign-up. */
    private void requireOpen() {
        if (!"true".equals(tenantSetting(SELF_SIGN_UP))) {
            throw new PermissionDeniedException(SIGN_UP_CLOSED);
        }
    }

    /** Inside the write transaction: the new account and its code, or a reset code for an existing one. */
    private Optional<AccountMessages.Message> register(UUID tenant, UUID school, String email, String role,
                                                      String secret, String clientIp, Locale locale) {
        OffsetDateTime now = TenantCalls.now(clock);
        List<Contact> existing = contacts.findByIdentifier(email);
        if (!existing.isEmpty()) {
            Contact holder = existing.get(0);
            if (existing.size() > 1 || holder.email() == null || !holder.usableAt(now)
                    || secrets.countSince(holder.id(), AccountSecretRepository.PASSWORD_RESET,
                    now.minus(AccountRecoveryService.THROTTLE_WINDOW)) >= AccountRecoveryService.THROTTLE_LIMIT) {
                return Optional.empty();
            }
            secrets.insert(UuidV7.next(clock), tenant, holder.id(), AccountSecretRepository.PASSWORD_RESET, "reset",
                    RefreshTokens.hash(secret), holder.email(), clientIp, now.plus(AccountRecoveryService.RESET_TTL),
                    now, holder.id());
            return Optional.of(messages.signUpExisting(holder.email(), secret,
                    AccountRecoveryService.RESET_TTL.toMinutes(), AccountRecoveryService.locale(holder, locale)));
        }
        Optional<AccountContactRepository.RoleRef> grant = role.isEmpty() ? Optional.empty()
                : contacts.roleByCode(role).filter(r -> !r.system());
        if (!role.isEmpty() && grant.isEmpty()) {
            LOG.warn("{} names no grantable role in tenant {}; the new account gets no role", SELF_SIGN_UP_ROLE,
                    tenant);
        }
        UUID id = UuidV7.next(clock);
        return TenantCalls.as(tenant, id, () -> {
            UserAccountEntity account = UserAccountEntity.builder().userType(STUDENT)
                    .username(email.toLowerCase(Locale.ROOT)).email(email).build();
            account.onCreatingData(Optional.of(id), clock);
            account.setId(id);
            account.setTenantId(tenant);
            rows.insert(account);
            contacts.markSelfRegistered(id, now);
            audit.recordCreate(IamEvents.USER_ACCOUNT, id);
            outbox.publish(IamEvents.USER_ACCOUNT + IamEvents.CREATED, IamEvents.USER_ACCOUNT, id,
                    IamEvents.payload(id));
            if (grant.isPresent()) {
                insertGrant(id, grant.get().id(), INSTITUTION, school);
                authzVersions.bump();
            }
            secrets.insert(UuidV7.next(clock), tenant, id, AccountSecretRepository.PASSWORD_RESET, "sign_up",
                    RefreshTokens.hash(secret), email, clientIp, now.plus(SIGN_UP_TTL), now, id);
            outbox.publish(IamEvents.SIGNED_UP, IamEvents.USER_ACCOUNT, id, IamEvents.payload(id));
            return Optional.of(messages.signUp(email, secret, SIGN_UP_TTL.toHours(), locale));
        });
    }

    /** The same answer for a new and an existing address: what the student signs in with, nothing more. */
    private static UserAccount receipt(String email) {
        UserAccount receipt = new UserAccount().username(email.toLowerCase(Locale.ROOT));
        receipt.setEmail(email);
        receipt.setUserType(UserAccount.UserTypeEnum.STUDENT);
        return receipt;
    }

    /** The tenant-level value of a setting, or its catalogue default. Inside a transaction. */
    private String tenantSetting(String key) {
        return values.findAtScope(TENANT, null, key).map(SettingValueEntity::getValue)
                .or(() -> definitions.findByKey(key).map(SettingDefinitionEntity::getDefaultValue))
                .orElse("");
    }

    /**
     * iam.CheckUsernameAvailable: whether no account signs in with the name (as login name, e-mail or mobile), with
     * a free variant when it is taken. Only for a signed-in caller who administers accounts.
     */
    public Availability checkUsernameAvailable(String username) {
        AuthContext caller = AuthContext.current().orElseThrow(() -> new UnauthenticatedException("UNAUTHENTICATED",
                "Sign in to check login names."));
        if (ACCOUNT_ADMINISTRATION.stream().noneMatch(caller::hasPermission)) {
            throw new PermissionDeniedException("Only administrators of user accounts can check login names.");
        }
        String wanted = username == null ? "" : username.trim();
        if (wanted.isEmpty() || wanted.length() > MAX_USERNAME) {
            throw new ValidationFailedException(List.of(new FieldError("username", "must be 1 to " + MAX_USERNAME
                    + " characters", "Size")));
        }
        return doInReadTransaction(() -> {
            if (!accounts.identifierTaken(wanted, null)) {
                return new Availability(true);
            }
            Availability taken = new Availability(false);
            for (int n = 2; n < 2 + SUGGESTIONS; n++) {
                String candidate = wanted + n;
                if (candidate.length() <= MAX_USERNAME && !accounts.identifierTaken(candidate, null)) {
                    taken.setSuggestion(candidate);
                    break;
                }
            }
            return taken;
        });
    }

    /**
     * iam.ImportUsers: creates the rows' accounts (no password: an administrator sets credentials or requires a
     * reset) with their role grants, all or nothing; answered as a finished operation. A file is refused (422).
     */
    public Operation importUsers(ImportUsersRequest request) {
        validator.validate(request);
        List<ImportUserRow> rowsIn = request.getRows();
        if (request.getFileId() != null || rowsIn == null || rowsIn.isEmpty()) {
            throw new ValidationFailedException(List.of(request.getFileId() != null
                    ? new FieldError("fileId", "file imports are not available; send the users in rows",
                            "FILE_IMPORT_UNAVAILABLE")
                    : new FieldError("rows", "must list 1 to 500 users", "Size")));
        }
        DataScope scope = DataScope.current(PERMISSION_IMPORT);
        boolean dryRun = Boolean.TRUE.equals(request.getValidateOnly());
        UUID actor = AuthContext.require().userId();
        UUID tenant = AuthContext.require().tenantId().orElseThrow();
        return doInWriteTransaction(() -> {
            // accounts are tenant-level rows: creating them needs the permission tenant-wide
            scope.requireWritable(Optional.empty());
            List<List<Grant>> resolved = validateRows(rowsIn);
            for (List<Grant> rowGrants : resolved) {
                for (Grant g : rowGrants) {
                    rules.requireGrantable(g.roleId(), g.scopeType(), g.scopeId());
                }
            }
            OffsetDateTime now = TenantCalls.now(clock);
            List<String> created = new ArrayList<>();
            if (!dryRun) {
                for (int i = 0; i < rowsIn.size(); i++) {
                    created.add(createImported(tenant, rowsIn.get(i), resolved.get(i)).toString());
                }
                if (resolved.stream().anyMatch(rowGrants -> !rowGrants.isEmpty())) {
                    authzVersions.bump();
                }
            }
            OperationEntity operation = OperationEntity.builder().tenantId(tenant)
                    .operationName("iam.ImportUsers").resourceType(IamEvents.USER_ACCOUNT)
                    .status("succeeded").progressPercent(100).startedByUserId(actor).startedAt(now).finishedAt(now)
                    .build();
            operation.onCreatingData(Optional.of(actor), clock);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("validateOnly", dryRun);
            result.put("count", rowsIn.size());
            result.put("userAccountIds", created);
            if (dryRun) {
                Operation answer = operationMapper.toDto(operation);
                answer.setResult(result);
                return answer;
            }
            OperationEntity stored = operations.insertCompleted(operation);
            audit.recordCreate(OperationService.AGGREGATE, stored.getId());
            outbox.publish(IamEvents.USERS_IMPORTED, IamEvents.USER_ACCOUNT, stored.getId(),
                    IamEvents.payload(stored.getId()));
            Operation answer = operationMapper.toDto(stored);
            answer.setResult(result);
            return answer;
        });
    }

    /** Every row's problems at once (field names {@code rows[i].<field>}); the grants of each row, resolved. */
    private List<List<Grant>> validateRows(List<ImportUserRow> input) {
        List<FieldError> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<List<Grant>> resolved = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            ImportUserRow row = input.get(i);
            String prefix = "rows[" + i + "].";
            List<String> identifiers = new ArrayList<>();
            for (String[] field : new String[][] {{"username", row.getUsername()}, {"email", row.getEmail()},
                {"mobileNo", row.getMobileNo()}}) {
                String value = field[1] == null ? null : field[1].trim();
                if (value == null || value.isEmpty()) {
                    continue;
                }
                identifiers.add(value);
                if ("email".equals(field[0]) && !EMAIL.matcher(value).matches()) {
                    errors.add(new FieldError(prefix + field[0], "must be an e-mail address", "INVALID_EMAIL"));
                } else if (!seen.add(value.toLowerCase(Locale.ROOT))) {
                    errors.add(new FieldError(prefix + field[0], "is used by another row", "DUPLICATE"));
                } else if (accounts.identifierTaken(value, null)) {
                    errors.add(new FieldError(prefix + field[0], "is used by an existing account", "DUPLICATE"));
                }
            }
            if (identifiers.isEmpty()) {
                errors.add(new FieldError(prefix + "username", "a username, e-mail or mobile number is required",
                        "REQUIRED"));
            }
            List<Grant> rowGrants = new ArrayList<>();
            List<RoleGrant> listed = row.getGrants();
            List<RoleGrant> wanted = listed == null ? List.of() : listed;
            for (int g = 0; g < wanted.size(); g++) {
                RoleGrant grant = wanted.get(g);
                String at = prefix + "grants[" + g + "].";
                String scopeType = grant.getScopeType().getValue();
                if (TENANT.equals(scopeType) != (grant.getScopeId() == null)) {
                    errors.add(new FieldError(at + "scopeId", "is required for every scope but tenant, and only then",
                            "INVALID_SCOPE"));
                }
                Optional<AccountContactRepository.RoleRef> role = contacts.roleByCode(grant.getRoleCode().trim());
                if (role.isEmpty()) {
                    errors.add(new FieldError(at + "roleCode", "no active role has this code", "NOT_FOUND"));
                } else if (role.get().system()) {
                    errors.add(new FieldError(at + "roleCode", "system roles are not granted by import", "NOT_ALLOWED"));
                } else {
                    rowGrants.add(new Grant(role.get().id(), scopeType, grant.getScopeId()));
                }
            }
            resolved.add(rowGrants);
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        return resolved;
    }

    private UUID createImported(UUID tenant, ImportUserRow row, List<Grant> rowGrants) {
        UserAccount.UserTypeEnum type = row.getUserType();
        UserAccountEntity account = UserAccountEntity.builder()
                .userType(type == null ? null : type.getValue())
                .username(blankToNull(row.getUsername())).email(blankToNull(row.getEmail()))
                .mobileNo(blankToNull(row.getMobileNo())).build();
        account.onCreatingData(Actor.current(), clock);
        account.setTenantId(tenant);
        UserAccountEntity stored = rows.insert(account);
        audit.recordCreate(IamEvents.USER_ACCOUNT, stored.getId());
        outbox.publish(IamEvents.USER_ACCOUNT + IamEvents.CREATED, IamEvents.USER_ACCOUNT, stored.getId(),
                IamEvents.payload(stored.getId()));
        for (Grant g : rowGrants) {
            insertGrant(stored.getId(), g.roleId(), g.scopeType(), g.scopeId());
        }
        return stored.getId();
    }

    private void insertGrant(UUID user, UUID role, String scopeType, UUID scopeId) {
        UserRoleEntity grant = UserRoleEntity.builder().userId(user).roleId(role).scopeType(scopeType)
                .scopeId(scopeId).isActive(true).build();
        grant.onCreatingData(Actor.current(), clock);
        grant.setTenantId(rowsTenant());
        UserRoleEntity stored = grants.insert(grant);
        audit.recordCreate(UserAccountService.USER_ROLE_AGGREGATE, stored.getId());
        outbox.publish(UserAccountService.USER_ROLE_AGGREGATE + IamEvents.CREATED,
                UserAccountService.USER_ROLE_AGGREGATE, stored.getId(), IamEvents.payload(stored.getId()));
    }

    private static UUID rowsTenant() {
        return CallContextHolder.require().tenantId().orElseThrow();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** A grant of a row, its role resolved. */
    private record Grant(UUID roleId, String scopeType, UUID scopeId) {
    }
}
