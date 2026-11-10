package com.mysms.attendance.business;

import com.mysms.attendance.config.AttendanceDatabaseConfig;
import com.mysms.attendance.config.AttendanceEventsConfig;
import com.mysms.attendance.domain.AttendanceTypeEntity;
import com.mysms.attendance.events.AttendanceEvents;
import com.mysms.attendance.mapper.AttendanceTypeMapper;
import com.mysms.attendance.repository.AttendanceTypeRepository;
import com.mysms.authclient.DataScope;
import com.mysms.attendance.api.model.AttendanceType;
import com.mysms.attendance.api.model.AttendanceTypeUpdate;
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
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of attendance.attendance_type (JAVA-CODING-RULES §4): one plain class, explicit methods,
 * transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 */
@Service
public class AttendanceTypeService extends AppTransactionManager {

    public static final String AGGREGATE = AttendanceTypeEntity.TABLE;
    static final String PERMISSION_LIST = "attendance.attendance_type.read";
    static final String PERMISSION_GET = "attendance.attendance_type.read";
    static final String PERMISSION_CREATE = "attendance.attendance_type.create";
    static final String PERMISSION_UPDATE = "attendance.attendance_type.update";
    static final String PERMISSION_DELETE = "attendance.attendance_type.delete";
    /** The contract's AttendanceTypeUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("code", "sortOrder", "institutionId", "name", "countsPresent", "countsAbsent", "countsExcused",
            "countsLate", "isActive");


    private final AttendanceTypeRepository repository;
    private final AttendanceTypeMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public AttendanceTypeService(AttendanceTypeRepository repository, AttendanceTypeMapper mapper,
            @Qualifier(AttendanceDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AttendanceDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AttendanceEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(AttendanceEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public AttendanceType getAttendanceType(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<AttendanceType> listAttendanceTypes(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, AttendanceTypeRepository.SORT_FIELDS, AttendanceTypeRepository.DEFAULT_ORDER,
                AttendanceTypeRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, AttendanceTypeRepository.FILTER_FIELDS)
                .and(AttendanceTypeRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<AttendanceTypeEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public AttendanceType createAttendanceType(AttendanceType request, boolean validateOnly) {
        validator.validate(request);
        AttendanceTypeEntity entity = mapper.toEntity(request);
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
            AttendanceTypeEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + AttendanceEvents.CREATED, AGGREGATE, stored.getId(),
                    AttendanceEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public AttendanceType updateAttendanceType(UUID id, String ifMatch, AttendanceTypeUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            AttendanceTypeEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + AttendanceEvents.UPDATED, AGGREGATE, id, AttendanceEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteAttendanceType(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            AttendanceTypeEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByAttendanceRecordAttendanceTypeId(id)) {
                throw new InvalidStateException(
                        "The attendance type is still referenced by attendance.attendance_record rows; remove those "
                                + "first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + AttendanceEvents.DELETED, AGGREGATE, id, AttendanceEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private AttendanceTypeEntity loadVisible(DataScope scope, UUID id) {
        AttendanceTypeEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(AttendanceTypeEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /**
     * Applies the selected AttendanceTypeUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void apply(AttendanceTypeEntity entity, AttendanceTypeUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "code" -> entity.setCode(changes.getCode());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "name" -> entity.setName(changes.getName());
                case "countsPresent" -> entity.setCountsPresent(changes.getCountsPresent());
                case "countsAbsent" -> entity.setCountsAbsent(changes.getCountsAbsent());
                case "countsExcused" -> entity.setCountsExcused(changes.getCountsExcused());
                case "countsLate" -> entity.setCountsLate(changes.getCountsLate());
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(AttendanceTypeEntity entity, boolean creating) {
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
        if (!creating && entity.getCountsPresent() == null) {
            errors.add(new FieldError("countsPresent", "must not be null", "NotNull"));
        }
        if (!creating && entity.getCountsAbsent() == null) {
            errors.add(new FieldError("countsAbsent", "must not be null", "NotNull"));
        }
        if (!creating && entity.getCountsExcused() == null) {
            errors.add(new FieldError("countsExcused", "must not be null", "NotNull"));
        }
        if (!creating && entity.getCountsLate() == null) {
            errors.add(new FieldError("countsLate", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(AttendanceTypeEntity entity) {
        if (repository.takenInstitutionIdName1(entity)) {
            throw new DuplicateResourceException(
                    "A attendance type with the same institution id and name already exists.");
        }
        if (repository.takenInstitutionIdCodeCode(entity)) {
            throw new DuplicateResourceException(
                    "A attendance type with the same institution id and code already exists.");
        }
    }
}
