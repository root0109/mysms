package com.mysms.academics.business;

import com.mysms.academics.config.AcademicsDatabaseConfig;
import com.mysms.academics.config.AcademicsEventsConfig;
import com.mysms.academics.domain.ProgramEntity;
import com.mysms.academics.events.AcademicsEvents;
import com.mysms.academics.mapper.ProgramMapper;
import com.mysms.academics.repository.ProgramRepository;
import com.mysms.authclient.DataScope;
import com.mysms.academics.api.model.Program;
import com.mysms.academics.api.model.ProgramUpdate;
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
import com.mysms.core.web.Patches;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of academics.program: audit and domain event in the same transaction as every change; rows outside
 * the caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class ProgramService extends AppTransactionManager {

    public static final String AGGREGATE = ProgramEntity.TABLE;
    static final String PERMISSION_LIST = "academics.program.read";
    static final String PERMISSION_GET = "academics.program.read";
    static final String PERMISSION_CREATE = "academics.program.create";
    static final String PERMISSION_UPDATE = "academics.program.update";
    static final String PERMISSION_DELETE = "academics.program.delete";


    private final ProgramRepository repository;
    private final ProgramMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public ProgramService(ProgramRepository repository, ProgramMapper mapper,
            @Qualifier(AcademicsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AcademicsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AcademicsEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(AcademicsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public Program getProgram(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Program> listPrograms(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, ProgramRepository.SORT_FIELDS, ProgramRepository.DEFAULT_ORDER,
                ProgramRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ProgramRepository.FILTER_FIELDS)
                .and(ProgramRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ProgramEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Program createProgram(Program request, boolean validateOnly) {
        validator.validate(request);
        ProgramEntity entity = mapper.toEntity(request);
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
            ProgramEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + AcademicsEvents.CREATED, AGGREGATE, stored.getId(),
                    AcademicsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. A field the request leaves out is kept, a null one is cleared and a
     * value is set (§7.2). The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Program updateProgram(UUID id, String ifMatch, ProgramUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ProgramEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + AcademicsEvents.UPDATED, AGGREGATE, id, AcademicsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteProgram(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            ProgramEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByCourseProgramId(id)) {
                throw new InvalidStateException(
                        "The program is still referenced by academics.course rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + AcademicsEvents.DELETED, AGGREGATE, id, AcademicsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ProgramEntity loadVisible(DataScope scope, UUID id) {
        ProgramEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(ProgramEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(ProgramEntity entity, ProgramUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("departmentId", changes.getDepartmentId(), entity::setDepartmentId);
        patch.apply("programLevelId", changes.getProgramLevelId(), entity::setProgramLevelId);
        patch.apply("code", changes.getCode(), entity::setCode);
        patch.apply("name", changes.getName(), entity::setName);
        patch.apply("minUnitLoad", changes.getMinUnitLoad(), v -> entity.setMinUnitLoad(WireValues.decimal(v)));
        patch.apply("maxUnitLoad", changes.getMaxUnitLoad(), v -> entity.setMaxUnitLoad(WireValues.decimal(v)));
        patch.apply("imageFileId", changes.getImageFileId(), entity::setImageFileId);
        patch.apply("isActive", changes.getIsActive(), entity::setIsActive);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ProgramEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getProgramLevelId() == null) {
            errors.add(new FieldError("programLevelId", "must not be null", "NotNull"));
        }
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("minUnitLoad", WireValues.decimal(entity.getMinUnitLoad()), 6, 2, errors);
        WireValues.checkDecimal("maxUnitLoad", WireValues.decimal(entity.getMaxUnitLoad()), 6, 2, errors);
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (entity.getProgramLevelId() != null && !repository.existsProgramLevelId(entity.getProgramLevelId())) {
            errors.add(new FieldError("programLevelId", "program level does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ProgramEntity entity) {
        if (repository.takenInstitutionIdCode1(entity)) {
            throw new DuplicateResourceException("A program with the same institution id and code already exists.");
        }
    }
}
