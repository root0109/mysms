package com.mysms.hr.business;

import com.mysms.authclient.DataScope;
import com.mysms.hr.api.model.LeaveType;
import com.mysms.hr.api.model.LeaveTypeUpdate;
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
import com.mysms.core.mapping.WireValues;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import com.mysms.hr.config.HrDatabaseConfig;
import com.mysms.hr.config.HrEventsConfig;
import com.mysms.hr.domain.LeaveTypeEntity;
import com.mysms.hr.events.HrEvents;
import com.mysms.hr.mapper.LeaveTypeMapper;
import com.mysms.hr.repository.LeaveTypeRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of hr.leave_type: audit and domain event commit in the same transaction as every change. Rows
 * outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class LeaveTypeService extends AppTransactionManager {

    public static final String AGGREGATE = LeaveTypeEntity.TABLE;
    static final String PERMISSION_LIST = "hr.leave_type.read";
    static final String PERMISSION_GET = "hr.leave_type.read";
    static final String PERMISSION_CREATE = "hr.leave_type.create";
    static final String PERMISSION_UPDATE = "hr.leave_type.update";
    static final String PERMISSION_DELETE = "hr.leave_type.delete";
    /** The contract's LeaveTypeUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("code", "sortOrder", "institutionId", "academicYearId", "name", "maxDaysPerYear", "isActive");


    private final LeaveTypeRepository repository;
    private final LeaveTypeMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public LeaveTypeService(LeaveTypeRepository repository, LeaveTypeMapper mapper,
            @Qualifier(HrDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(HrDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(HrEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(HrEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public LeaveType getLeaveType(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<LeaveType> listLeaveTypes(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, LeaveTypeRepository.SORT_FIELDS, LeaveTypeRepository.DEFAULT_ORDER,
                LeaveTypeRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, LeaveTypeRepository.FILTER_FIELDS)
                .and(LeaveTypeRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<LeaveTypeEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public LeaveType createLeaveType(LeaveType request, boolean validateOnly) {
        validator.validate(request);
        LeaveTypeEntity entity = mapper.toEntity(request);
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
            LeaveTypeEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + HrEvents.CREATED, AGGREGATE, stored.getId(), HrEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public LeaveType updateLeaveType(UUID id, String ifMatch, LeaveTypeUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            LeaveTypeEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            apply(entity, changes, fields);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
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
            outbox.publish(AGGREGATE + HrEvents.UPDATED, AGGREGATE, id, HrEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteLeaveType(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            LeaveTypeEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByLeaveApplicationLeaveTypeId(id)) {
                throw new InvalidStateException(
                        "The leave type is still referenced by hr.leave_application rows; remove those first.");
            }
            if (repository.referencedByLeaveBalanceLeaveTypeId(id)) {
                throw new InvalidStateException(
                        "The leave type is still referenced by hr.leave_balance rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + HrEvents.DELETED, AGGREGATE, id, HrEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private LeaveTypeEntity loadVisible(DataScope scope, UUID id) {
        LeaveTypeEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(LeaveTypeEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected LeaveTypeUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(LeaveTypeEntity entity, LeaveTypeUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "code" -> entity.setCode(changes.getCode());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "academicYearId" -> entity.setAcademicYearId(changes.getAcademicYearId());
                case "name" -> entity.setName(changes.getName());
                case "maxDaysPerYear" -> entity.setMaxDaysPerYear(WireValues.decimal(changes.getMaxDaysPerYear()));
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(LeaveTypeEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (entity.getMaxDaysPerYear() == null) {
            errors.add(new FieldError("maxDaysPerYear", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("maxDaysPerYear", WireValues.decimal(entity.getMaxDaysPerYear()), 5, 1, errors);
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(LeaveTypeEntity entity) {
        if (repository.takenInstitutionIdCodeCode(entity)) {
            throw new DuplicateResourceException("A leave type with the same institution id and code already exists.");
        }
    }
}
