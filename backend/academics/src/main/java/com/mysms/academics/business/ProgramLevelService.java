package com.mysms.academics.business;

import com.mysms.academics.config.AcademicsDatabaseConfig;
import com.mysms.academics.config.AcademicsEventsConfig;
import com.mysms.academics.domain.ProgramLevelEntity;
import com.mysms.academics.events.AcademicsEvents;
import com.mysms.academics.mapper.ProgramLevelMapper;
import com.mysms.academics.repository.ProgramLevelRepository;
import com.mysms.authclient.DataScope;
import com.mysms.academics.api.model.ProgramLevel;
import com.mysms.academics.api.model.ProgramLevelUpdate;
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
 * Business service of academics.program_level: audit and domain event in the same transaction as every change; rows
 * outside the caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class ProgramLevelService extends AppTransactionManager {

    public static final String AGGREGATE = ProgramLevelEntity.TABLE;
    static final String PERMISSION_LIST = "academics.program_level.read";
    static final String PERMISSION_GET = "academics.program_level.read";
    static final String PERMISSION_CREATE = "academics.program_level.create";
    static final String PERMISSION_UPDATE = "academics.program_level.update";
    static final String PERMISSION_DELETE = "academics.program_level.delete";
    /** The contract's ProgramLevelUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("code", "sortOrder", "isActive", "name");


    private final ProgramLevelRepository repository;
    private final ProgramLevelMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public ProgramLevelService(ProgramLevelRepository repository, ProgramLevelMapper mapper,
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

    public ProgramLevel getProgramLevel(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<ProgramLevel> listProgramLevels(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ProgramLevelRepository.SORT_FIELDS, ProgramLevelRepository.DEFAULT_ORDER,
                ProgramLevelRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ProgramLevelRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ProgramLevelEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ProgramLevel createProgramLevel(ProgramLevel request, boolean validateOnly) {
        validator.validate(request);
        ProgramLevelEntity entity = mapper.toEntity(request);
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
            ProgramLevelEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + AcademicsEvents.CREATED, AGGREGATE, stored.getId(),
                    AcademicsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public ProgramLevel updateProgramLevel(UUID id, String ifMatch, ProgramLevelUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ProgramLevelEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + AcademicsEvents.UPDATED, AGGREGATE, id, AcademicsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteProgramLevel(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            ProgramLevelEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByProgramProgramLevelId(id)) {
                throw new InvalidStateException(
                        "The program level is still referenced by academics.program rows; remove those first.");
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
    private ProgramLevelEntity loadVisible(DataScope scope, UUID id) {
        ProgramLevelEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOf(ProgramLevelEntity entity) {
        return Optional.empty();
    }

    /**
     * Applies the selected ProgramLevelUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void apply(ProgramLevelEntity entity, ProgramLevelUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "code" -> entity.setCode(changes.getCode());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                case "name" -> entity.setName(changes.getName());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ProgramLevelEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ProgramLevelEntity entity) {
        if (repository.takenName1(entity)) {
            throw new DuplicateResourceException("A program level with the same name already exists.");
        }
        if (repository.takenCodeCode(entity)) {
            throw new DuplicateResourceException("A program level with the same code already exists.");
        }
    }
}
