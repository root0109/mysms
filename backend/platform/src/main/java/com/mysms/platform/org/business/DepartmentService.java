package com.mysms.platform.org.business;

import com.mysms.authclient.DataScope;
import com.mysms.platform.org.api.model.Department;
import com.mysms.platform.org.api.model.DepartmentUpdate;
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
import com.mysms.core.web.Patches;
import com.mysms.platform.org.config.OrgDatabaseConfig;
import com.mysms.platform.org.config.OrgEventsConfig;
import com.mysms.platform.org.domain.DepartmentEntity;
import com.mysms.platform.org.events.OrgEvents;
import com.mysms.platform.org.mapper.DepartmentMapper;
import com.mysms.platform.org.repository.DepartmentRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of org.department: audit and domain event in the same transaction as every change; rows outside the
 * caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class DepartmentService extends AppTransactionManager {

    public static final String AGGREGATE = DepartmentEntity.TABLE;
    static final String PERMISSION_LIST = "org.department.read";
    static final String PERMISSION_GET = "org.department.read";
    static final String PERMISSION_CREATE = "org.department.create";
    static final String PERMISSION_UPDATE = "org.department.update";
    static final String PERMISSION_DELETE = "org.department.delete";


    private final DepartmentRepository repository;
    private final DepartmentMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public DepartmentService(DepartmentRepository repository, DepartmentMapper mapper,
            @Qualifier(OrgDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(OrgDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(OrgEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(OrgEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public Department getDepartment(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Department> listDepartments(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, DepartmentRepository.SORT_FIELDS, DepartmentRepository.DEFAULT_ORDER,
                DepartmentRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, DepartmentRepository.FILTER_FIELDS)
                .and(DepartmentRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<DepartmentEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Department createDepartment(Department request, boolean validateOnly) {
        validator.validate(request);
        DepartmentEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity, true);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            DepartmentEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + OrgEvents.CREATED, AGGREGATE, stored.getId(), OrgEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. A field the request leaves out is kept, a null one is cleared and a
     * value is set (§7.2). The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Department updateDepartment(UUID id, String ifMatch, DepartmentUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            DepartmentEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = apply(entity, changes);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (fields.isEmpty()) {
                return mapper.toDto(entity);
            }
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            if (repository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + OrgEvents.UPDATED, AGGREGATE, id, OrgEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteDepartment(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            DepartmentEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByDepartmentParentDepartmentId(id)) {
                throw new InvalidStateException(
                        "The department is still referenced by org.department rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + OrgEvents.DELETED, AGGREGATE, id, OrgEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private DepartmentEntity loadVisible(DataScope scope, UUID id) {
        DepartmentEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(DepartmentEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(DepartmentEntity entity, DepartmentUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("parentDepartmentId", changes.getParentDepartmentId(), entity::setParentDepartmentId);
        patch.apply("code", changes.getCode(), entity::setCode);
        patch.apply("name", changes.getName(), entity::setName);
        patch.apply("isActive", changes.getIsActive(), entity::setIsActive);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(DepartmentEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (entity.getInstitutionId() != null && !repository.existsInstitutionId(entity.getInstitutionId())) {
            errors.add(new FieldError("institutionId", "institution does not exist", "NOT_FOUND"));
        }
        if (entity.getParentDepartmentId() != null
                && !repository.existsParentDepartmentId(entity.getParentDepartmentId())) {
            errors.add(new FieldError("parentDepartmentId", "department does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(DepartmentEntity entity) {
        if (repository.takenInstitutionIdCode1(entity)) {
            throw new DuplicateResourceException("A department with the same institution id and code already exists.");
        }
    }
}
