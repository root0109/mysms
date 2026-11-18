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
import com.mysms.iam.api.model.Role;
import com.mysms.iam.api.model.RolePermission;
import com.mysms.iam.api.model.RolePermissionUpdate;
import com.mysms.iam.api.model.RoleUpdate;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.config.IamEventsConfig;
import com.mysms.iam.domain.RoleEntity;
import com.mysms.iam.domain.RolePermissionEntity;
import com.mysms.iam.events.IamEvents;
import com.mysms.iam.mapper.RoleMapper;
import com.mysms.iam.mapper.RolePermissionMapper;
import com.mysms.iam.repository.RolePermissionRepository;
import com.mysms.iam.repository.RoleRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of iam.role and of the records that exist only as part of a Role (RolePermission): audit and domain
 * event commit in the same transaction as every change. Rows outside the caller's institutions are invisible;
 * tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class RoleService extends AppTransactionManager {

    public static final String AGGREGATE = RoleEntity.TABLE;
    static final String PERMISSION_LIST = "iam.role.read";
    static final String PERMISSION_GET = "iam.role.read";
    static final String PERMISSION_CREATE = "iam.role.create";
    static final String PERMISSION_UPDATE = "iam.role.update";
    static final String PERMISSION_DELETE = "iam.role.delete";
    /** The contract's RoleUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("code", "displayName", "description", "isSystem", "isActive", "authorityLevel", "parentRoleId");

    public static final String ROLE_PERMISSION_AGGREGATE = RolePermissionEntity.TABLE;
    static final String ROLE_PERMISSION_PERMISSION_LIST = "iam.role_permission.read";
    static final String ROLE_PERMISSION_PERMISSION_GET = "iam.role_permission.read";
    static final String ROLE_PERMISSION_PERMISSION_CREATE = "iam.role_permission.create";
    static final String ROLE_PERMISSION_PERMISSION_UPDATE = "iam.role_permission.update";
    static final String ROLE_PERMISSION_PERMISSION_DELETE = "iam.role_permission.delete";
    /** The contract's RolePermissionUpdate fields, in apply order. */
    static final List<String> ROLE_PERMISSION_WRITABLE = List.of("permissionId");

    private final RoleRepository repository;
    private final RoleMapper mapper;
    private final RoleRules rules;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final RolePermissionRepository rolePermissionRepository;
    private final RolePermissionMapper rolePermissionMapper;
    private final RolePermissionRules rolePermissionRules;

    public RoleService(RoleRepository repository,
            RoleMapper mapper,
            RoleRules rules,
            @Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(IamDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(IamEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(IamEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            RolePermissionRepository rolePermissionRepository,
            RolePermissionMapper rolePermissionMapper,
            RolePermissionRules rolePermissionRules) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.rules = rules;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.rolePermissionRepository = rolePermissionRepository;
        this.rolePermissionMapper = rolePermissionMapper;
        this.rolePermissionRules = rolePermissionRules;
    }

    public Role getRole(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Role> listRoles(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, RoleRepository.SORT_FIELDS, RoleRepository.DEFAULT_ORDER,
                RoleRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, RoleRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<RoleEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Role createRole(Role request, boolean validateOnly) {
        validator.validate(request);
        RoleEntity entity = mapper.toEntity(request);
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
            RoleEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + IamEvents.CREATED, AGGREGATE, stored.getId(), IamEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public Role updateRole(UUID id, String ifMatch, RoleUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            RoleEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            rules.beforeUpdate(entity);
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
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteRole(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            RoleEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            rules.beforeDelete(entity);
            if (repository.referencedByRoleParentRoleId(id)) {
                throw new InvalidStateException("The role is still referenced by iam.role rows; remove those first.");
            }
            if (repository.referencedByRolePermissionRoleId(id)) {
                throw new InvalidStateException(
                        "The role is still referenced by iam.role_permission rows; remove those first.");
            }
            if (repository.referencedByUserAccountLastLoginRoleId(id)) {
                throw new InvalidStateException(
                        "The role is still referenced by iam.user_account rows; remove those first.");
            }
            if (repository.referencedByUserRoleRoleId(id)) {
                throw new InvalidStateException(
                        "The role is still referenced by iam.user_role rows; remove those first.");
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

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private RoleEntity loadVisible(DataScope scope, UUID id) {
        RoleEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOf(RoleEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected RoleUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(RoleEntity entity, RoleUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "code" -> entity.setCode(changes.getCode());
                case "displayName" -> entity.setDisplayName(changes.getDisplayName());
                case "description" -> entity.setDescription(changes.getDescription());
                case "isSystem" -> entity.setIsSystem(changes.getIsSystem());
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                case "authorityLevel" -> entity.setAuthorityLevel(changes.getAuthorityLevel());
                case "parentRoleId" -> entity.setParentRoleId(changes.getParentRoleId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(RoleEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsSystem() == null) {
            errors.add(new FieldError("isSystem", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (entity.getParentRoleId() != null && !repository.existsParentRoleId(entity.getParentRoleId())) {
            errors.add(new FieldError("parentRoleId", "role does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(RoleEntity entity) {
        if (repository.takenCode1(entity)) {
            throw new DuplicateResourceException("A role with the same code already exists.");
        }
    }

    // ---- RolePermission: a record of the Role aggregate ----

    public RolePermission getRolePermission(UUID roleId, UUID id) {
        return doInReadTransaction(() -> {
            return rolePermissionMapper.toDto(loadVisibleRolePermission(DataScope.current(ROLE_PERMISSION_PERMISSION_GET),
                    roleId, id));
        });
    }

    public Page<RolePermission> listRolePermissions(UUID roleId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, RolePermissionRepository.SORT_FIELDS, RolePermissionRepository.DEFAULT_ORDER,
                RolePermissionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, RolePermissionRepository.FILTER_FIELDS)
                .and(RolePermissionRepository.ofParent(roleId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + roleId, clock);
        return doInReadTransaction(() -> {
            Page<RolePermissionEntity> rows = rolePermissionRepository.list(page, where);
            return rows.map(rolePermissionMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public RolePermission createRolePermission(UUID roleId, RolePermission request, boolean validateOnly) {
        validator.validate(request);
        RolePermissionEntity entity = rolePermissionMapper.toEntity(request);
        entity.setRoleId(roleId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(ROLE_PERMISSION_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!rolePermissionRepository.existsRoleId(entity.getRoleId())) {
                throw new ResourceNotFoundException("iam.role", entity.getRoleId());
            }
            assertValidRolePermission(entity, true);
            scope.requireWritable(institutionOfRolePermission(entity));
            rolePermissionRules.beforeWrite(entity);
            assertUniqueRolePermission(entity);
            if (validateOnly) {
                return rolePermissionMapper.toDto(entity);
            }
            RolePermissionEntity stored = rolePermissionRepository.insert(entity);
            audit.recordCreate(ROLE_PERMISSION_AGGREGATE, stored.getId());
            outbox.publish(ROLE_PERMISSION_AGGREGATE + IamEvents.CREATED, ROLE_PERMISSION_AGGREGATE, stored.getId(),
                    IamEvents.payload(stored.getId()));
            return rolePermissionMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public RolePermission updateRolePermission(UUID roleId, UUID id, String ifMatch, RolePermissionUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, ROLE_PERMISSION_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ROLE_PERMISSION_PERMISSION_UPDATE);
            RolePermissionEntity entity = loadVisibleRolePermission(scope, roleId, id);
            scope.requireWritable(institutionOfRolePermission(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            rolePermissionRules.beforeUpdate(entity);
            applyRolePermission(entity, changes, fields);
            validator.validate(rolePermissionMapper.toDto(entity));
            assertValidRolePermission(entity, false);
            scope.requireWritable(institutionOfRolePermission(entity));
            rolePermissionRules.beforeWrite(entity);
            assertUniqueRolePermission(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return rolePermissionMapper.toDto(entity);
            }
            if (rolePermissionRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(ROLE_PERMISSION_AGGREGATE, id, fields);
            outbox.publish(ROLE_PERMISSION_AGGREGATE + IamEvents.UPDATED, ROLE_PERMISSION_AGGREGATE, id, IamEvents.payload(id));
            return rolePermissionMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteRolePermission(UUID roleId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ROLE_PERMISSION_PERMISSION_DELETE);
            RolePermissionEntity entity = loadVisibleRolePermission(scope, roleId, id);
            scope.requireWritable(institutionOfRolePermission(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            rolePermissionRules.beforeDelete(entity);
            entity.onDeletingData(Actor.current(), clock);
            if (rolePermissionRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(ROLE_PERMISSION_AGGREGATE, id);
            outbox.publish(ROLE_PERMISSION_AGGREGATE + IamEvents.DELETED, ROLE_PERMISSION_AGGREGATE, id, IamEvents.payload(id));
            rolePermissionRules.afterDelete(entity);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private RolePermissionEntity loadVisibleRolePermission(DataScope scope, UUID roleId, UUID id) {
        RolePermissionEntity entity = rolePermissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ROLE_PERMISSION_AGGREGATE, id));
        if (!scope.canRead(institutionOfRolePermission(entity)) || !roleId.equals(entity.getRoleId())) {
            throw new ResourceNotFoundException(ROLE_PERMISSION_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfRolePermission(RolePermissionEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected RolePermissionUpdate fields; explicit per field so a mask can clear values. */
    private static void applyRolePermission(RolePermissionEntity entity, RolePermissionUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "permissionId" -> entity.setPermissionId(changes.getPermissionId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidRolePermission(RolePermissionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getRoleId() == null) {
            errors.add(new FieldError("roleId", "must not be null", "NotNull"));
        }
        if (entity.getPermissionId() == null) {
            errors.add(new FieldError("permissionId", "must not be null", "NotNull"));
        }
        if (entity.getPermissionId() != null && !rolePermissionRepository.existsPermissionId(entity.getPermissionId())) {
            errors.add(new FieldError("permissionId", "permission does not exist", "NOT_FOUND"));
        }
        if (entity.getRoleId() != null && !rolePermissionRepository.existsRoleId(entity.getRoleId())) {
            errors.add(new FieldError("roleId", "role does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueRolePermission(RolePermissionEntity entity) {
        if (rolePermissionRepository.takenRoleIdPermissionId1(entity)) {
            throw new DuplicateResourceException(
                    "A role permission with the same role id and permission id already exists.");
        }
    }
}
