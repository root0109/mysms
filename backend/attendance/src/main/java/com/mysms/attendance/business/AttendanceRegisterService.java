package com.mysms.attendance.business;

import com.mysms.attendance.config.AttendanceDatabaseConfig;
import com.mysms.attendance.config.AttendanceEventsConfig;
import com.mysms.attendance.domain.AttendanceRegisterEntity;
import com.mysms.attendance.events.AttendanceEvents;
import com.mysms.attendance.mapper.AttendanceRegisterMapper;
import com.mysms.attendance.repository.AttendanceRegisterRepository;
import com.mysms.authclient.DataScope;
import com.mysms.attendance.api.model.AttendanceRegister;
import com.mysms.attendance.api.model.AttendanceRegisterUpdate;
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
 * Business service of attendance.attendance_register: audit and domain event commit in the same transaction as every
 * change. Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide
 * grant.
 */
@Service
public class AttendanceRegisterService extends AppTransactionManager {

    public static final String AGGREGATE = AttendanceRegisterEntity.TABLE;
    static final String PERMISSION_LIST = "attendance.attendance_register.read";
    static final String PERMISSION_GET = "attendance.attendance_register.read";
    static final String PERMISSION_CREATE = "attendance.attendance_register.create";
    static final String PERMISSION_UPDATE = "attendance.attendance_register.update";
    static final String PERMISSION_DELETE = "attendance.attendance_register.delete";
    /** The contract's AttendanceRegisterUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "code", "name", "registerScope", "classSectionId", "courseId", "batchId",
                    "subjectId",
            "isActive");


    private final AttendanceRegisterRepository repository;
    private final AttendanceRegisterMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public AttendanceRegisterService(AttendanceRegisterRepository repository, AttendanceRegisterMapper mapper,
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

    public AttendanceRegister getAttendanceRegister(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<AttendanceRegister> listAttendanceRegisters(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, AttendanceRegisterRepository.SORT_FIELDS,
                        AttendanceRegisterRepository.DEFAULT_ORDER,
                AttendanceRegisterRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, AttendanceRegisterRepository.FILTER_FIELDS)
                .and(AttendanceRegisterRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<AttendanceRegisterEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public AttendanceRegister createAttendanceRegister(AttendanceRegister request, boolean validateOnly) {
        validator.validate(request);
        AttendanceRegisterEntity entity = mapper.toEntity(request);
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
            AttendanceRegisterEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + AttendanceEvents.CREATED, AGGREGATE, stored.getId(),
                    AttendanceEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public AttendanceRegister updateAttendanceRegister(UUID id, String ifMatch, AttendanceRegisterUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            AttendanceRegisterEntity entity = loadVisible(scope, id);
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
    public void deleteAttendanceRegister(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            AttendanceRegisterEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByAttendanceSheetRegisterId(id)) {
                throw new InvalidStateException(
                        "The attendance register is still referenced by attendance.attendance_sheet rows; remove "
                                + "those first.");
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
    private AttendanceRegisterEntity loadVisible(DataScope scope, UUID id) {
        AttendanceRegisterEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(AttendanceRegisterEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected AttendanceRegisterUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(AttendanceRegisterEntity entity, AttendanceRegisterUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "code" -> entity.setCode(changes.getCode());
                case "name" -> entity.setName(changes.getName());
                case "registerScope" -> entity.setRegisterScope(Optional.ofNullable(changes.getRegisterScope())
                        .map(v -> v.getValue()).orElse(null));
                case "classSectionId" -> entity.setClassSectionId(changes.getClassSectionId());
                case "courseId" -> entity.setCourseId(changes.getCourseId());
                case "batchId" -> entity.setBatchId(changes.getBatchId());
                case "subjectId" -> entity.setSubjectId(changes.getSubjectId());
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(AttendanceRegisterEntity entity, boolean creating) {
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
        if (entity.getRegisterScope() == null) {
            errors.add(new FieldError("registerScope", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(AttendanceRegisterEntity entity) {
        if (repository.takenInstitutionIdCode1(entity)) {
            throw new DuplicateResourceException(
                    "A attendance register with the same institution id and code already exists.");
        }
    }
}
