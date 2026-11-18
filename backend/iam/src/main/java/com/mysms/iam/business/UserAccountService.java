package com.mysms.iam.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
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
import com.mysms.core.web.UpdateMask;
import com.mysms.iam.api.model.ActivateUserRequest;
import com.mysms.iam.api.model.DeactivateUserRequest;
import com.mysms.iam.api.model.UnlockUserRequest;
import com.mysms.iam.api.model.UserAccount;
import com.mysms.iam.api.model.UserAccountUpdate;
import com.mysms.iam.api.model.UserDepartmentAccess;
import com.mysms.iam.api.model.UserDepartmentAccessUpdate;
import com.mysms.iam.api.model.UserDevice;
import com.mysms.iam.api.model.UserDeviceUpdate;
import com.mysms.iam.api.model.UserPermission;
import com.mysms.iam.api.model.UserPermissionUpdate;
import com.mysms.iam.api.model.UserPreference;
import com.mysms.iam.api.model.UserPreferenceUpdate;
import com.mysms.iam.api.model.UserRole;
import com.mysms.iam.api.model.UserRoleUpdate;
import com.mysms.iam.api.model.UserSession;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.config.IamEventsConfig;
import com.mysms.iam.domain.UserAccountEntity;
import com.mysms.iam.domain.UserDepartmentAccessEntity;
import com.mysms.iam.domain.UserDeviceEntity;
import com.mysms.iam.domain.UserPermissionEntity;
import com.mysms.iam.domain.UserPreferenceEntity;
import com.mysms.iam.domain.UserRoleEntity;
import com.mysms.iam.domain.UserSessionEntity;
import com.mysms.iam.events.IamEvents;
import com.mysms.iam.mapper.UserAccountMapper;
import com.mysms.iam.mapper.UserDepartmentAccessMapper;
import com.mysms.iam.mapper.UserDeviceMapper;
import com.mysms.iam.mapper.UserPermissionMapper;
import com.mysms.iam.mapper.UserPreferenceMapper;
import com.mysms.iam.mapper.UserRoleMapper;
import com.mysms.iam.mapper.UserSessionMapper;
import com.mysms.iam.repository.UserAccountRepository;
import com.mysms.iam.repository.UserDepartmentAccessRepository;
import com.mysms.iam.repository.UserDeviceRepository;
import com.mysms.iam.repository.UserPermissionRepository;
import com.mysms.iam.repository.UserPreferenceRepository;
import com.mysms.iam.repository.UserRoleRepository;
import com.mysms.iam.repository.UserSessionRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of iam.user_account and of the records that exist only as part of a UserAccount
 * (UserDepartmentAccess, UserDevice, UserPermission, UserPreference, UserRole, UserSession): audit and domain event
 * commit in the same transaction as every change. Rows outside the caller's institutions are invisible; tenant-level
 * rows change only through a tenant-wide grant.
 */
@Service
public class UserAccountService extends AppTransactionManager {

    public static final String AGGREGATE = UserAccountEntity.TABLE;
    static final String PERMISSION_LIST = "iam.user_account.read";
    static final String PERMISSION_GET = "iam.user_account.read";
    static final String PERMISSION_CREATE = "iam.user_account.create";
    static final String PERMISSION_UPDATE = "iam.user_account.update";
    static final String PERMISSION_DELETE = "iam.user_account.delete";
    static final String PERMISSION_ACTIVATE_USER = "iam.user_account.activate_user";
    static final String PERMISSION_DEACTIVATE_USER = "iam.user_account.deactivate_user";
    static final String PERMISSION_UNLOCK_USER = "iam.user_account.unlock_user";
    /** The contract's UserAccountUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("userType", "personId", "username", "email", "mobileNo", "referredByUserId", "locale", "timezone",
            "joinedOn", "externalRef", "signature", "useSignature");

    public static final String USER_DEPARTMENT_ACCESS_AGGREGATE = UserDepartmentAccessEntity.TABLE;
    static final String USER_DEPARTMENT_ACCESS_PERMISSION_LIST = "iam.user_department_access.read";
    static final String USER_DEPARTMENT_ACCESS_PERMISSION_GET = "iam.user_department_access.read";
    static final String USER_DEPARTMENT_ACCESS_PERMISSION_CREATE = "iam.user_department_access.create";
    static final String USER_DEPARTMENT_ACCESS_PERMISSION_UPDATE = "iam.user_department_access.update";
    static final String USER_DEPARTMENT_ACCESS_PERMISSION_DELETE = "iam.user_department_access.delete";
    /** The contract's UserDepartmentAccessUpdate fields, in apply order. */
    static final List<String> USER_DEPARTMENT_ACCESS_WRITABLE = List.of("departmentId", "isPrimary");

    public static final String USER_DEVICE_AGGREGATE = UserDeviceEntity.TABLE;
    static final String USER_DEVICE_PERMISSION_LIST = "iam.user_device.read";
    static final String USER_DEVICE_PERMISSION_GET = "iam.user_device.read";
    static final String USER_DEVICE_PERMISSION_CREATE = "iam.user_device.create";
    static final String USER_DEVICE_PERMISSION_UPDATE = "iam.user_device.update";
    static final String USER_DEVICE_PERMISSION_DELETE = "iam.user_device.delete";
    /** The contract's UserDeviceUpdate fields, in apply order. */
    static final List<String> USER_DEVICE_WRITABLE = List.of("deviceIdentifier", "platform", "pushToken", "lastSeenAt",
            "isActive");

    public static final String USER_PERMISSION_AGGREGATE = UserPermissionEntity.TABLE;
    static final String USER_PERMISSION_PERMISSION_LIST = "iam.user_permission.read";
    static final String USER_PERMISSION_PERMISSION_GET = "iam.user_permission.read";
    static final String USER_PERMISSION_PERMISSION_CREATE = "iam.user_permission.create";
    static final String USER_PERMISSION_PERMISSION_UPDATE = "iam.user_permission.update";
    static final String USER_PERMISSION_PERMISSION_DELETE = "iam.user_permission.delete";
    /** The contract's UserPermissionUpdate fields, in apply order. */
    static final List<String> USER_PERMISSION_WRITABLE = List.of("permissionId", "effect");

    public static final String USER_PREFERENCE_AGGREGATE = UserPreferenceEntity.TABLE;
    static final String USER_PREFERENCE_PERMISSION_LIST = "iam.user_preference.read";
    static final String USER_PREFERENCE_PERMISSION_GET = "iam.user_preference.read";
    static final String USER_PREFERENCE_PERMISSION_CREATE = "iam.user_preference.create";
    static final String USER_PREFERENCE_PERMISSION_UPDATE = "iam.user_preference.update";
    static final String USER_PREFERENCE_PERMISSION_DELETE = "iam.user_preference.delete";
    /** The contract's UserPreferenceUpdate fields, in apply order. */
    static final List<String> USER_PREFERENCE_WRITABLE = List.of("prefKey", "prefValue");

    public static final String USER_ROLE_AGGREGATE = UserRoleEntity.TABLE;
    static final String USER_ROLE_PERMISSION_LIST = "iam.user_role.read";
    static final String USER_ROLE_PERMISSION_GET = "iam.user_role.read";
    static final String USER_ROLE_PERMISSION_CREATE = "iam.user_role.create";
    static final String USER_ROLE_PERMISSION_UPDATE = "iam.user_role.update";
    static final String USER_ROLE_PERMISSION_DELETE = "iam.user_role.delete";
    /** The contract's UserRoleUpdate fields, in apply order. */
    static final List<String> USER_ROLE_WRITABLE = List.of("roleId", "scopeType", "scopeId", "validFrom", "validTo", "isActive");

    public static final String USER_SESSION_AGGREGATE = UserSessionEntity.TABLE;
    static final String USER_SESSION_PERMISSION_LIST = "iam.user_session.read";
    static final String USER_SESSION_PERMISSION_GET = "iam.user_session.read";
    static final String USER_SESSION_PERMISSION_DELETE = "iam.user_session.delete";

    private final UserAccountRepository repository;
    private final UserAccountMapper mapper;
    private final UserAccountRules rules;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final UserDepartmentAccessRepository userDepartmentAccessRepository;
    private final UserDepartmentAccessMapper userDepartmentAccessMapper;
    private final UserDeviceRepository userDeviceRepository;
    private final UserDeviceMapper userDeviceMapper;
    private final UserPermissionRepository userPermissionRepository;
    private final UserPermissionMapper userPermissionMapper;
    private final UserPermissionRules userPermissionRules;
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserPreferenceMapper userPreferenceMapper;
    private final UserRoleRepository userRoleRepository;
    private final UserRoleMapper userRoleMapper;
    private final UserRoleRules userRoleRules;
    private final UserSessionRepository userSessionRepository;
    private final UserSessionMapper userSessionMapper;
    private final UserSessionRules userSessionRules;

    public UserAccountService(UserAccountRepository repository,
            UserAccountMapper mapper,
            UserAccountRules rules,
            @Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(IamDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(IamEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(IamEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            UserDepartmentAccessRepository userDepartmentAccessRepository,
            UserDepartmentAccessMapper userDepartmentAccessMapper,
            UserDeviceRepository userDeviceRepository,
            UserDeviceMapper userDeviceMapper,
            UserPermissionRepository userPermissionRepository,
            UserPermissionMapper userPermissionMapper,
            UserPermissionRules userPermissionRules,
            UserPreferenceRepository userPreferenceRepository,
            UserPreferenceMapper userPreferenceMapper,
            UserRoleRepository userRoleRepository,
            UserRoleMapper userRoleMapper,
            UserRoleRules userRoleRules,
            UserSessionRepository userSessionRepository,
            UserSessionMapper userSessionMapper,
            UserSessionRules userSessionRules) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.rules = rules;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.userDepartmentAccessRepository = userDepartmentAccessRepository;
        this.userDepartmentAccessMapper = userDepartmentAccessMapper;
        this.userDeviceRepository = userDeviceRepository;
        this.userDeviceMapper = userDeviceMapper;
        this.userPermissionRepository = userPermissionRepository;
        this.userPermissionMapper = userPermissionMapper;
        this.userPermissionRules = userPermissionRules;
        this.userPreferenceRepository = userPreferenceRepository;
        this.userPreferenceMapper = userPreferenceMapper;
        this.userRoleRepository = userRoleRepository;
        this.userRoleMapper = userRoleMapper;
        this.userRoleRules = userRoleRules;
        this.userSessionRepository = userSessionRepository;
        this.userSessionMapper = userSessionMapper;
        this.userSessionRules = userSessionRules;
    }

    public UserAccount getUserAccount(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<UserAccount> listUserAccounts(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, UserAccountRepository.SORT_FIELDS, UserAccountRepository.DEFAULT_ORDER,
                UserAccountRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, UserAccountRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<UserAccountEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public UserAccount createUserAccount(UserAccount request, boolean validateOnly) {
        validator.validate(request);
        UserAccountEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity, true);
            scope.requireWritable(institutionOf(entity));
            rules.beforeWrite(entity);
            assertUnique(entity);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            UserAccountEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + IamEvents.CREATED, AGGREGATE, stored.getId(), IamEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public UserAccount updateUserAccount(UUID id, String ifMatch, UserAccountUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            UserAccountEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            rules.beforeUpdate(entity);
            String emailBefore = entity.getEmail();
            String mobileBefore = entity.getMobileNo();
            apply(entity, changes, fields);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            rules.beforeWrite(entity);
            assertUnique(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            if (repository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + IamEvents.UPDATED, AGGREGATE, id, IamEvents.payload(id));
            rules.afterUpdate(entity, emailBefore, mobileBefore);
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteUserAccount(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            UserAccountEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            rules.beforeDelete(entity);
            if (repository.referencedByLegacyCredentialUserId(id)) {
                throw new InvalidStateException(
                        "The user account is still referenced by iam.legacy_credential rows; remove those first.");
            }
            if (repository.referencedByMfaCredentialUserId(id)) {
                throw new InvalidStateException(
                        "The user account is still referenced by iam.mfa_credential rows; remove those first.");
            }
            if (repository.referencedByUserAccountReferredByUserId(id)) {
                throw new InvalidStateException(
                        "The user account is still referenced by iam.user_account rows; remove those first.");
            }
            if (repository.referencedByUserDepartmentAccessUserId(id)) {
                throw new InvalidStateException(
                        "The user account is still referenced by iam.user_department_access rows; remove those first.");
            }
            if (repository.referencedByUserPermissionUserId(id)) {
                throw new InvalidStateException(
                        "The user account is still referenced by iam.user_permission rows; remove those first.");
            }
            if (repository.referencedByUserRoleUserId(id)) {
                throw new InvalidStateException(
                        "The user account is still referenced by iam.user_role rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + IamEvents.DELETED, AGGREGATE, id, IamEvents.payload(id));
            rules.afterDelete(entity);
        });
    }

    /**
     * iam.ActivateUser: Set status active. Allowed while status is inactive, on a row the caller may see and write;
     * otherwise 409 and nothing changes.
     */
    public UserAccount activateUser(UUID id, ActivateUserRequest request) {
        DataScope scope = DataScope.current(PERMISSION_ACTIVATE_USER);
        return doInWriteTransaction(() -> {
            UserAccountEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("inactive").contains(entity.getStatus()))) {
                throw new InvalidStateException("The user account cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.activateUser(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "activateUser", List.of("status"), null);
            outbox.publish(AGGREGATE + ".activateUser", AGGREGATE, id, IamEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * iam.DeactivateUser: Set status inactive (login blocked) and sign the account out everywhere, its API tokens
     * included ({@link UserAccountRules#afterDeactivate}), so reactivating it revives no old session or token. Allowed
     * while status is active or locked, on a row the caller may see and write; otherwise 409 and nothing changes.
     */
    public UserAccount deactivateUser(UUID id, DeactivateUserRequest request) {
        DataScope scope = DataScope.current(PERMISSION_DEACTIVATE_USER);
        return doInWriteTransaction(() -> {
            UserAccountEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("active", "locked").contains(entity.getStatus()))) {
                throw new InvalidStateException("The user account cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.deactivateUser(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "deactivateUser", List.of("status"), request == null ? null
                    : request.getReason());
            outbox.publish(AGGREGATE + ".deactivateUser", AGGREGATE, id, IamEvents.payload(id));
            rules.afterDeactivate(id);
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * iam.UnlockUser: Clear a lock caused by failed logins. Allowed in every state, on a row the caller may see and
     * write.
     */
    public UserAccount unlockUser(UUID id, UnlockUserRequest request) {
        DataScope scope = DataScope.current(PERMISSION_UNLOCK_USER);
        return doInWriteTransaction(() -> {
            UserAccountEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.unlockUser(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "unlockUser", List.of("failedLoginCount", "lockedUntil"), null);
            outbox.publish(AGGREGATE + ".unlockUser", AGGREGATE, id, IamEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private UserAccountEntity loadVisible(DataScope scope, UUID id) {
        UserAccountEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOf(UserAccountEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected UserAccountUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(UserAccountEntity entity, UserAccountUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "userType" -> entity.setUserType(Optional.ofNullable(changes.getUserType()).map(v -> v.getValue())
                        .orElse(null));
                case "personId" -> entity.setPersonId(changes.getPersonId());
                case "username" -> entity.setUsername(changes.getUsername());
                case "email" -> entity.setEmail(changes.getEmail());
                case "mobileNo" -> entity.setMobileNo(changes.getMobileNo());
                case "referredByUserId" -> entity.setReferredByUserId(changes.getReferredByUserId());
                case "locale" -> entity.setLocale(changes.getLocale());
                case "timezone" -> entity.setTimezone(changes.getTimezone());
                case "joinedOn" -> entity.setJoinedOn(changes.getJoinedOn());
                case "externalRef" -> entity.setExternalRef(changes.getExternalRef());
                case "signature" -> entity.setSignature(changes.getSignature());
                case "useSignature" -> entity.setUseSignature(changes.getUseSignature());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(UserAccountEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (!creating && entity.getUseSignature() == null) {
            errors.add(new FieldError("useSignature", "must not be null", "NotNull"));
        }
        if (entity.getLastLoginRoleId() != null && !repository.existsLastLoginRoleId(entity.getLastLoginRoleId())) {
            errors.add(new FieldError("lastLoginRoleId", "role does not exist", "NOT_FOUND"));
        }
        if (entity.getReferredByUserId() != null && !repository.existsReferredByUserId(entity.getReferredByUserId())) {
            errors.add(new FieldError("referredByUserId", "user account does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(UserAccountEntity entity) {

    }

    // ---- UserDepartmentAccess: a record of the UserAccount aggregate ----

    public UserDepartmentAccess getUserDepartmentAccess(UUID userId, UUID id) {
        return doInReadTransaction(() -> {
            return userDepartmentAccessMapper.toDto(loadVisibleUserDepartmentAccess(DataScope.current(
                    USER_DEPARTMENT_ACCESS_PERMISSION_GET), userId, id));
        });
    }

    public Page<UserDepartmentAccess> listUserDepartmentAccesss(UUID userId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, UserDepartmentAccessRepository.SORT_FIELDS,
                        UserDepartmentAccessRepository.DEFAULT_ORDER,
                UserDepartmentAccessRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, UserDepartmentAccessRepository.FILTER_FIELDS)
                .and(UserDepartmentAccessRepository.ofParent(userId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + userId, clock);
        return doInReadTransaction(() -> {
            Page<UserDepartmentAccessEntity> rows = userDepartmentAccessRepository.list(page, where);
            return rows.map(userDepartmentAccessMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public UserDepartmentAccess createUserDepartmentAccess(UUID userId, UserDepartmentAccess request,
            boolean validateOnly) {
        validator.validate(request);
        UserDepartmentAccessEntity entity = userDepartmentAccessMapper.toEntity(request);
        entity.setUserId(userId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(USER_DEPARTMENT_ACCESS_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!userDepartmentAccessRepository.existsUserId(entity.getUserId())) {
                throw new ResourceNotFoundException("iam.user_account", entity.getUserId());
            }
            assertValidUserDepartmentAccess(entity, true);
            scope.requireWritable(institutionOfUserDepartmentAccess(entity));
            assertUniqueUserDepartmentAccess(entity);
            if (validateOnly) {
                return userDepartmentAccessMapper.toDto(entity);
            }
            UserDepartmentAccessEntity stored = userDepartmentAccessRepository.insert(entity);
            audit.recordCreate(USER_DEPARTMENT_ACCESS_AGGREGATE, stored.getId());
            outbox.publish(USER_DEPARTMENT_ACCESS_AGGREGATE + IamEvents.CREATED, USER_DEPARTMENT_ACCESS_AGGREGATE,
                    stored.getId(), IamEvents.payload(stored.getId()));
            return userDepartmentAccessMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public UserDepartmentAccess updateUserDepartmentAccess(UUID userId, UUID id, String ifMatch,
            UserDepartmentAccessUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, USER_DEPARTMENT_ACCESS_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(USER_DEPARTMENT_ACCESS_PERMISSION_UPDATE);
            UserDepartmentAccessEntity entity = loadVisibleUserDepartmentAccess(scope, userId, id);
            scope.requireWritable(institutionOfUserDepartmentAccess(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyUserDepartmentAccess(entity, changes, fields);
            validator.validate(userDepartmentAccessMapper.toDto(entity));
            assertValidUserDepartmentAccess(entity, false);
            scope.requireWritable(institutionOfUserDepartmentAccess(entity));
            assertUniqueUserDepartmentAccess(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return userDepartmentAccessMapper.toDto(entity);
            }
            if (userDepartmentAccessRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(USER_DEPARTMENT_ACCESS_AGGREGATE, id, fields);
            outbox.publish(USER_DEPARTMENT_ACCESS_AGGREGATE + IamEvents.UPDATED, USER_DEPARTMENT_ACCESS_AGGREGATE, id,
                    IamEvents.payload(id));
            return userDepartmentAccessMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteUserDepartmentAccess(UUID userId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(USER_DEPARTMENT_ACCESS_PERMISSION_DELETE);
            UserDepartmentAccessEntity entity = loadVisibleUserDepartmentAccess(scope, userId, id);
            scope.requireWritable(institutionOfUserDepartmentAccess(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (userDepartmentAccessRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(USER_DEPARTMENT_ACCESS_AGGREGATE, id);
            outbox.publish(USER_DEPARTMENT_ACCESS_AGGREGATE + IamEvents.DELETED, USER_DEPARTMENT_ACCESS_AGGREGATE, id,
                    IamEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private UserDepartmentAccessEntity loadVisibleUserDepartmentAccess(DataScope scope, UUID userId, UUID id) {
        UserDepartmentAccessEntity entity = userDepartmentAccessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_DEPARTMENT_ACCESS_AGGREGATE, id));
        if (!scope.canRead(institutionOfUserDepartmentAccess(entity)) || !userId.equals(entity.getUserId())) {
            throw new ResourceNotFoundException(USER_DEPARTMENT_ACCESS_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfUserDepartmentAccess(UserDepartmentAccessEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected UserDepartmentAccessUpdate fields; explicit per field so a mask can clear values. */
    private static void applyUserDepartmentAccess(UserDepartmentAccessEntity entity, UserDepartmentAccessUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "departmentId" -> entity.setDepartmentId(changes.getDepartmentId());
                case "isPrimary" -> entity.setIsPrimary(changes.getIsPrimary());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidUserDepartmentAccess(UserDepartmentAccessEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getDepartmentId() == null) {
            errors.add(new FieldError("departmentId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsPrimary() == null) {
            errors.add(new FieldError("isPrimary", "must not be null", "NotNull"));
        }
        if (entity.getUserId() != null && !userDepartmentAccessRepository.existsUserId(entity.getUserId())) {
            errors.add(new FieldError("userId", "user account does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueUserDepartmentAccess(UserDepartmentAccessEntity entity) {
        if (userDepartmentAccessRepository.takenUserIdDepartmentId1(entity)) {
            throw new DuplicateResourceException(
                    "A user department access with the same user id and department id already exists.");
        }
    }

    // ---- UserDevice: a record of the UserAccount aggregate ----

    public UserDevice getUserDevice(UUID userId, UUID id) {
        return doInReadTransaction(() -> {
            return userDeviceMapper.toDto(loadVisibleUserDevice(DataScope.current(USER_DEVICE_PERMISSION_GET), userId, id));
        });
    }

    public Page<UserDevice> listUserDevices(UUID userId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, UserDeviceRepository.SORT_FIELDS, UserDeviceRepository.DEFAULT_ORDER,
                UserDeviceRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, UserDeviceRepository.FILTER_FIELDS)
                .and(UserDeviceRepository.ofParent(userId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + userId, clock);
        return doInReadTransaction(() -> {
            Page<UserDeviceEntity> rows = userDeviceRepository.list(page, where);
            return rows.map(userDeviceMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public UserDevice createUserDevice(UUID userId, UserDevice request, boolean validateOnly) {
        validator.validate(request);
        UserDeviceEntity entity = userDeviceMapper.toEntity(request);
        entity.setUserId(userId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(USER_DEVICE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!userDeviceRepository.existsUserId(entity.getUserId())) {
                throw new ResourceNotFoundException("iam.user_account", entity.getUserId());
            }
            assertValidUserDevice(entity, true);
            scope.requireWritable(institutionOfUserDevice(entity));
            assertUniqueUserDevice(entity);
            if (validateOnly) {
                return userDeviceMapper.toDto(entity);
            }
            UserDeviceEntity stored = userDeviceRepository.insert(entity);
            audit.recordCreate(USER_DEVICE_AGGREGATE, stored.getId());
            outbox.publish(USER_DEVICE_AGGREGATE + IamEvents.CREATED, USER_DEVICE_AGGREGATE, stored.getId(),
                    IamEvents.payload(stored.getId()));
            return userDeviceMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public UserDevice updateUserDevice(UUID userId, UUID id, String ifMatch, UserDeviceUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, USER_DEVICE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(USER_DEVICE_PERMISSION_UPDATE);
            UserDeviceEntity entity = loadVisibleUserDevice(scope, userId, id);
            scope.requireWritable(institutionOfUserDevice(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyUserDevice(entity, changes, fields);
            validator.validate(userDeviceMapper.toDto(entity));
            assertValidUserDevice(entity, false);
            scope.requireWritable(institutionOfUserDevice(entity));
            assertUniqueUserDevice(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return userDeviceMapper.toDto(entity);
            }
            if (userDeviceRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(USER_DEVICE_AGGREGATE, id, fields);
            outbox.publish(USER_DEVICE_AGGREGATE + IamEvents.UPDATED, USER_DEVICE_AGGREGATE, id, IamEvents.payload(id));
            return userDeviceMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteUserDevice(UUID userId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(USER_DEVICE_PERMISSION_DELETE);
            UserDeviceEntity entity = loadVisibleUserDevice(scope, userId, id);
            scope.requireWritable(institutionOfUserDevice(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (userDeviceRepository.referencedByUserSessionDeviceId(id)) {
                throw new InvalidStateException(
                        "The user device is still referenced by iam.user_session rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (userDeviceRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(USER_DEVICE_AGGREGATE, id);
            outbox.publish(USER_DEVICE_AGGREGATE + IamEvents.DELETED, USER_DEVICE_AGGREGATE, id, IamEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private UserDeviceEntity loadVisibleUserDevice(DataScope scope, UUID userId, UUID id) {
        UserDeviceEntity entity = userDeviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_DEVICE_AGGREGATE, id));
        if (!scope.canRead(institutionOfUserDevice(entity)) || !userId.equals(entity.getUserId())) {
            throw new ResourceNotFoundException(USER_DEVICE_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfUserDevice(UserDeviceEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected UserDeviceUpdate fields; explicit per field so a mask can clear values. */
    private static void applyUserDevice(UserDeviceEntity entity, UserDeviceUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "deviceIdentifier" -> entity.setDeviceIdentifier(changes.getDeviceIdentifier());
                case "platform" -> entity.setPlatform(Optional.ofNullable(changes.getPlatform()).map(v -> v.getValue())
                        .orElse(null));
                case "pushToken" -> entity.setPushToken(changes.getPushToken());
                case "lastSeenAt" -> entity.setLastSeenAt(changes.getLastSeenAt());
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidUserDevice(UserDeviceEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getDeviceIdentifier() == null) {
            errors.add(new FieldError("deviceIdentifier", "must not be null", "NotNull"));
        }
        if (entity.getPlatform() == null) {
            errors.add(new FieldError("platform", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (entity.getUserId() != null && !userDeviceRepository.existsUserId(entity.getUserId())) {
            errors.add(new FieldError("userId", "user account does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueUserDevice(UserDeviceEntity entity) {
        if (userDeviceRepository.takenUserIdDeviceIdentifier1(entity)) {
            throw new DuplicateResourceException(
                    "A user device with the same user id and device identifier already exists.");
        }
    }

    // ---- UserPermission: a record of the UserAccount aggregate ----

    public UserPermission getUserPermission(UUID userId, UUID id) {
        return doInReadTransaction(() -> {
            return userPermissionMapper.toDto(loadVisibleUserPermission(DataScope.current(USER_PERMISSION_PERMISSION_GET),
                    userId, id));
        });
    }

    public Page<UserPermission> listUserPermissions(UUID userId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, UserPermissionRepository.SORT_FIELDS, UserPermissionRepository.DEFAULT_ORDER,
                UserPermissionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, UserPermissionRepository.FILTER_FIELDS)
                .and(UserPermissionRepository.ofParent(userId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + userId, clock);
        return doInReadTransaction(() -> {
            Page<UserPermissionEntity> rows = userPermissionRepository.list(page, where);
            return rows.map(userPermissionMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public UserPermission createUserPermission(UUID userId, UserPermission request, boolean validateOnly) {
        validator.validate(request);
        UserPermissionEntity entity = userPermissionMapper.toEntity(request);
        entity.setUserId(userId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(USER_PERMISSION_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!userPermissionRepository.existsUserId(entity.getUserId())) {
                throw new ResourceNotFoundException("iam.user_account", entity.getUserId());
            }
            assertValidUserPermission(entity, true);
            scope.requireWritable(institutionOfUserPermission(entity));
            userPermissionRules.beforeWrite(entity);
            assertUniqueUserPermission(entity);
            if (validateOnly) {
                return userPermissionMapper.toDto(entity);
            }
            UserPermissionEntity stored = userPermissionRepository.insert(entity);
            audit.recordCreate(USER_PERMISSION_AGGREGATE, stored.getId());
            outbox.publish(USER_PERMISSION_AGGREGATE + IamEvents.CREATED, USER_PERMISSION_AGGREGATE, stored.getId(),
                    IamEvents.payload(stored.getId()));
            return userPermissionMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public UserPermission updateUserPermission(UUID userId, UUID id, String ifMatch, UserPermissionUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, USER_PERMISSION_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(USER_PERMISSION_PERMISSION_UPDATE);
            UserPermissionEntity entity = loadVisibleUserPermission(scope, userId, id);
            scope.requireWritable(institutionOfUserPermission(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            userPermissionRules.beforeUpdate(entity);
            applyUserPermission(entity, changes, fields);
            validator.validate(userPermissionMapper.toDto(entity));
            assertValidUserPermission(entity, false);
            scope.requireWritable(institutionOfUserPermission(entity));
            userPermissionRules.beforeWrite(entity);
            assertUniqueUserPermission(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return userPermissionMapper.toDto(entity);
            }
            if (userPermissionRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(USER_PERMISSION_AGGREGATE, id, fields);
            outbox.publish(USER_PERMISSION_AGGREGATE + IamEvents.UPDATED, USER_PERMISSION_AGGREGATE, id, IamEvents.payload(id));
            return userPermissionMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteUserPermission(UUID userId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(USER_PERMISSION_PERMISSION_DELETE);
            UserPermissionEntity entity = loadVisibleUserPermission(scope, userId, id);
            scope.requireWritable(institutionOfUserPermission(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            userPermissionRules.beforeDelete(entity);
            entity.onDeletingData(Actor.current(), clock);
            if (userPermissionRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(USER_PERMISSION_AGGREGATE, id);
            outbox.publish(USER_PERMISSION_AGGREGATE + IamEvents.DELETED, USER_PERMISSION_AGGREGATE, id, IamEvents.payload(id));
            userPermissionRules.afterDelete(entity);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private UserPermissionEntity loadVisibleUserPermission(DataScope scope, UUID userId, UUID id) {
        UserPermissionEntity entity = userPermissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_PERMISSION_AGGREGATE, id));
        if (!scope.canRead(institutionOfUserPermission(entity)) || !userId.equals(entity.getUserId())) {
            throw new ResourceNotFoundException(USER_PERMISSION_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfUserPermission(UserPermissionEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected UserPermissionUpdate fields; explicit per field so a mask can clear values. */
    private static void applyUserPermission(UserPermissionEntity entity, UserPermissionUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "permissionId" -> entity.setPermissionId(changes.getPermissionId());
                case "effect" -> entity.setEffect(Optional.ofNullable(changes.getEffect()).map(v -> v.getValue())
                        .orElse(null));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidUserPermission(UserPermissionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getPermissionId() == null) {
            errors.add(new FieldError("permissionId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getEffect() == null) {
            errors.add(new FieldError("effect", "must not be null", "NotNull"));
        }
        if (entity.getPermissionId() != null && !userPermissionRepository.existsPermissionId(entity.getPermissionId())) {
            errors.add(new FieldError("permissionId", "permission does not exist", "NOT_FOUND"));
        }
        if (entity.getUserId() != null && !userPermissionRepository.existsUserId(entity.getUserId())) {
            errors.add(new FieldError("userId", "user account does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueUserPermission(UserPermissionEntity entity) {
        if (userPermissionRepository.takenUserIdPermissionId1(entity)) {
            throw new DuplicateResourceException(
                    "A user permission with the same user id and permission id already exists.");
        }
    }

    // ---- UserPreference: a record of the UserAccount aggregate ----

    public UserPreference getUserPreference(UUID userId, UUID id) {
        return doInReadTransaction(() -> {
            return userPreferenceMapper.toDto(loadVisibleUserPreference(DataScope.current(USER_PREFERENCE_PERMISSION_GET),
                    userId, id));
        });
    }

    public Page<UserPreference> listUserPreferences(UUID userId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, UserPreferenceRepository.SORT_FIELDS, UserPreferenceRepository.DEFAULT_ORDER,
                UserPreferenceRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, UserPreferenceRepository.FILTER_FIELDS)
                .and(UserPreferenceRepository.ofParent(userId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + userId, clock);
        return doInReadTransaction(() -> {
            Page<UserPreferenceEntity> rows = userPreferenceRepository.list(page, where);
            return rows.map(userPreferenceMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public UserPreference createUserPreference(UUID userId, UserPreference request, boolean validateOnly) {
        validator.validate(request);
        UserPreferenceEntity entity = userPreferenceMapper.toEntity(request);
        entity.setUserId(userId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(USER_PREFERENCE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!userPreferenceRepository.existsUserId(entity.getUserId())) {
                throw new ResourceNotFoundException("iam.user_account", entity.getUserId());
            }
            assertValidUserPreference(entity, true);
            scope.requireWritable(institutionOfUserPreference(entity));
            assertUniqueUserPreference(entity);
            if (validateOnly) {
                return userPreferenceMapper.toDto(entity);
            }
            UserPreferenceEntity stored = userPreferenceRepository.insert(entity);
            audit.recordCreate(USER_PREFERENCE_AGGREGATE, stored.getId());
            outbox.publish(USER_PREFERENCE_AGGREGATE + IamEvents.CREATED, USER_PREFERENCE_AGGREGATE, stored.getId(),
                    IamEvents.payload(stored.getId()));
            return userPreferenceMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public UserPreference updateUserPreference(UUID userId, UUID id, String ifMatch, UserPreferenceUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, USER_PREFERENCE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(USER_PREFERENCE_PERMISSION_UPDATE);
            UserPreferenceEntity entity = loadVisibleUserPreference(scope, userId, id);
            scope.requireWritable(institutionOfUserPreference(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyUserPreference(entity, changes, fields);
            validator.validate(userPreferenceMapper.toDto(entity));
            assertValidUserPreference(entity, false);
            scope.requireWritable(institutionOfUserPreference(entity));
            assertUniqueUserPreference(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return userPreferenceMapper.toDto(entity);
            }
            if (userPreferenceRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(USER_PREFERENCE_AGGREGATE, id, fields);
            outbox.publish(USER_PREFERENCE_AGGREGATE + IamEvents.UPDATED, USER_PREFERENCE_AGGREGATE, id, IamEvents.payload(id));
            return userPreferenceMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteUserPreference(UUID userId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(USER_PREFERENCE_PERMISSION_DELETE);
            UserPreferenceEntity entity = loadVisibleUserPreference(scope, userId, id);
            scope.requireWritable(institutionOfUserPreference(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (userPreferenceRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(USER_PREFERENCE_AGGREGATE, id);
            outbox.publish(USER_PREFERENCE_AGGREGATE + IamEvents.DELETED, USER_PREFERENCE_AGGREGATE, id, IamEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private UserPreferenceEntity loadVisibleUserPreference(DataScope scope, UUID userId, UUID id) {
        UserPreferenceEntity entity = userPreferenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_PREFERENCE_AGGREGATE, id));
        if (!scope.canRead(institutionOfUserPreference(entity)) || !userId.equals(entity.getUserId())) {
            throw new ResourceNotFoundException(USER_PREFERENCE_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfUserPreference(UserPreferenceEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected UserPreferenceUpdate fields; explicit per field so a mask can clear values. */
    private static void applyUserPreference(UserPreferenceEntity entity, UserPreferenceUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "prefKey" -> entity.setPrefKey(changes.getPrefKey());
                case "prefValue" -> entity.setPrefValue(changes.getPrefValue());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidUserPreference(UserPreferenceEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getPrefKey() == null) {
            errors.add(new FieldError("prefKey", "must not be null", "NotNull"));
        }
        if (entity.getUserId() != null && !userPreferenceRepository.existsUserId(entity.getUserId())) {
            errors.add(new FieldError("userId", "user account does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueUserPreference(UserPreferenceEntity entity) {
        if (userPreferenceRepository.takenUserIdPrefKey1(entity)) {
            throw new DuplicateResourceException(
                    "A user preference with the same user id and pref key already exists.");
        }
    }

    // ---- UserRole: a record of the UserAccount aggregate ----

    public UserRole getUserRole(UUID userId, UUID id) {
        return doInReadTransaction(() -> {
            return userRoleMapper.toDto(loadVisibleUserRole(DataScope.current(USER_ROLE_PERMISSION_GET), userId, id));
        });
    }

    public Page<UserRole> listUserRoles(UUID userId, Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, UserRoleRepository.SORT_FIELDS, UserRoleRepository.DEFAULT_ORDER,
                UserRoleRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, UserRoleRepository.FILTER_FIELDS)
                .and(UserRoleRepository.ofParent(userId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + userId, clock);
        return doInReadTransaction(() -> {
            Page<UserRoleEntity> rows = userRoleRepository.list(page, where);
            return rows.map(userRoleMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public UserRole createUserRole(UUID userId, UserRole request, boolean validateOnly) {
        validator.validate(request);
        UserRoleEntity entity = userRoleMapper.toEntity(request);
        entity.setUserId(userId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(USER_ROLE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!userRoleRepository.existsUserId(entity.getUserId())) {
                throw new ResourceNotFoundException("iam.user_account", entity.getUserId());
            }
            assertValidUserRole(entity, true);
            scope.requireWritable(institutionOfUserRole(entity));
            userRoleRules.beforeWrite(entity);
            assertUniqueUserRole(entity);
            if (validateOnly) {
                return userRoleMapper.toDto(entity);
            }
            UserRoleEntity stored = userRoleRepository.insert(entity);
            audit.recordCreate(USER_ROLE_AGGREGATE, stored.getId());
            outbox.publish(USER_ROLE_AGGREGATE + IamEvents.CREATED, USER_ROLE_AGGREGATE, stored.getId(),
                    IamEvents.payload(stored.getId()));
            return userRoleMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public UserRole updateUserRole(UUID userId, UUID id, String ifMatch, UserRoleUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, USER_ROLE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(USER_ROLE_PERMISSION_UPDATE);
            UserRoleEntity entity = loadVisibleUserRole(scope, userId, id);
            scope.requireWritable(institutionOfUserRole(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            userRoleRules.beforeUpdate(entity);
            applyUserRole(entity, changes, fields);
            validator.validate(userRoleMapper.toDto(entity));
            assertValidUserRole(entity, false);
            scope.requireWritable(institutionOfUserRole(entity));
            userRoleRules.beforeWrite(entity);
            assertUniqueUserRole(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return userRoleMapper.toDto(entity);
            }
            if (userRoleRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(USER_ROLE_AGGREGATE, id, fields);
            outbox.publish(USER_ROLE_AGGREGATE + IamEvents.UPDATED, USER_ROLE_AGGREGATE, id, IamEvents.payload(id));
            return userRoleMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteUserRole(UUID userId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(USER_ROLE_PERMISSION_DELETE);
            UserRoleEntity entity = loadVisibleUserRole(scope, userId, id);
            scope.requireWritable(institutionOfUserRole(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            userRoleRules.beforeDelete(entity);
            entity.onDeletingData(Actor.current(), clock);
            if (userRoleRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(USER_ROLE_AGGREGATE, id);
            outbox.publish(USER_ROLE_AGGREGATE + IamEvents.DELETED, USER_ROLE_AGGREGATE, id, IamEvents.payload(id));
            userRoleRules.afterDelete(entity);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private UserRoleEntity loadVisibleUserRole(DataScope scope, UUID userId, UUID id) {
        UserRoleEntity entity = userRoleRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException(USER_ROLE_AGGREGATE, id));
        if (!scope.canRead(institutionOfUserRole(entity)) || !userId.equals(entity.getUserId())) {
            throw new ResourceNotFoundException(USER_ROLE_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfUserRole(UserRoleEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected UserRoleUpdate fields; explicit per field so a mask can clear values. */
    private static void applyUserRole(UserRoleEntity entity, UserRoleUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "roleId" -> entity.setRoleId(changes.getRoleId());
                case "scopeType" -> entity.setScopeType(Optional.ofNullable(changes.getScopeType())
                        .map(v -> v.getValue()).orElse(null));
                case "scopeId" -> entity.setScopeId(changes.getScopeId());
                case "validFrom" -> entity.setValidFrom(changes.getValidFrom());
                case "validTo" -> entity.setValidTo(changes.getValidTo());
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidUserRole(UserRoleEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getRoleId() == null) {
            errors.add(new FieldError("roleId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getScopeType() == null) {
            errors.add(new FieldError("scopeType", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (entity.getRoleId() != null && !userRoleRepository.existsRoleId(entity.getRoleId())) {
            errors.add(new FieldError("roleId", "role does not exist", "NOT_FOUND"));
        }
        if (entity.getUserId() != null && !userRoleRepository.existsUserId(entity.getUserId())) {
            errors.add(new FieldError("userId", "user account does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueUserRole(UserRoleEntity entity) {
        if (userRoleRepository.takenUserIdRoleIdScopeTypeScopeIdAssignment(entity)) {
            throw new DuplicateResourceException(
                    "A user role with the same user id and role id and scope type and scope id already exists.");
        }
    }

    // ---- UserSession: a record of the UserAccount aggregate ----

    public UserSession getUserSession(UUID userId, UUID id) {
        return doInReadTransaction(() -> {
            return userSessionMapper.toDto(loadVisibleUserSession(DataScope.current(USER_SESSION_PERMISSION_GET), userId, id));
        });
    }

    public Page<UserSession> listUserSessions(UUID userId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, UserSessionRepository.SORT_FIELDS, UserSessionRepository.DEFAULT_ORDER,
                UserSessionRepository.ID_COLUMN);
        userSessionRules.beforeAccess(userId, DataScope.current(USER_SESSION_PERMISSION_LIST));
        SqlFragment where = FilterParser.parse(filter, UserSessionRepository.FILTER_FIELDS)
                .and(UserSessionRepository.ofParent(userId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + userId, clock);
        return doInReadTransaction(() -> {
            Page<UserSessionEntity> rows = userSessionRepository.list(page, where);
            return rows.map(userSessionMapper::toDto);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteUserSession(UUID userId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(USER_SESSION_PERMISSION_DELETE);
            UserSessionEntity entity = loadVisibleUserSession(scope, userId, id);
            // who may reach these rows was decided by the Rules' beforeAccess
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            userSessionRules.beforeDelete(entity);
            entity.onDeletingData(Actor.current(), clock);
            if (userSessionRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(USER_SESSION_AGGREGATE, id);
            outbox.publish(USER_SESSION_AGGREGATE + IamEvents.DELETED, USER_SESSION_AGGREGATE, id, IamEvents.payload(id));
            userSessionRules.afterDelete(entity);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private UserSessionEntity loadVisibleUserSession(DataScope scope, UUID userId, UUID id) {
        userSessionRules.beforeAccess(userId, scope);
        UserSessionEntity entity = userSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_SESSION_AGGREGATE, id));
        if (!scope.canRead(institutionOfUserSession(entity)) || !userId.equals(entity.getUserId())) {
            throw new ResourceNotFoundException(USER_SESSION_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfUserSession(UserSessionEntity entity) {
        return Optional.empty();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidUserSession(UserSessionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getSessionRef() == null) {
            errors.add(new FieldError("sessionRef", "must not be null", "NotNull"));
        }
        if (entity.getDeviceId() != null && !userSessionRepository.existsDeviceId(entity.getDeviceId())) {
            errors.add(new FieldError("deviceId", "user device does not exist", "NOT_FOUND"));
        }
        if (entity.getUserId() != null && !userSessionRepository.existsUserId(entity.getUserId())) {
            errors.add(new FieldError("userId", "user account does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueUserSession(UserSessionEntity entity) {
        if (userSessionRepository.takenSessionRef1(entity)) {
            throw new DuplicateResourceException("A user session with the same session ref already exists.");
        }
    }
}
