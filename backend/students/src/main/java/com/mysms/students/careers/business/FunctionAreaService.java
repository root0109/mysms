package com.mysms.students.careers.business;

import com.mysms.authclient.DataScope;
import com.mysms.students.careers.config.CareersDatabaseConfig;
import com.mysms.students.careers.config.CareersEventsConfig;
import com.mysms.students.careers.domain.FunctionAreaEntity;
import com.mysms.students.careers.events.CareersEvents;
import com.mysms.students.careers.mapper.FunctionAreaMapper;
import com.mysms.students.careers.repository.FunctionAreaRepository;
import com.mysms.students.careers.api.model.FunctionArea;
import com.mysms.students.careers.api.model.FunctionAreaUpdate;
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
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of careers.function_area: audit and domain event commit in the same transaction as every change.
 * Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class FunctionAreaService extends AppTransactionManager {

    public static final String AGGREGATE = FunctionAreaEntity.TABLE;
    static final String PERMISSION_LIST = "careers.function_area.read";
    static final String PERMISSION_GET = "careers.function_area.read";
    static final String PERMISSION_CREATE = "careers.function_area.create";
    static final String PERMISSION_UPDATE = "careers.function_area.update";
    static final String PERMISSION_DELETE = "careers.function_area.delete";


    private final FunctionAreaRepository repository;
    private final FunctionAreaMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public FunctionAreaService(FunctionAreaRepository repository, FunctionAreaMapper mapper,
            @Qualifier(CareersDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CareersDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CareersEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(CareersEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public FunctionArea getFunctionArea(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<FunctionArea> listFunctionAreas(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, FunctionAreaRepository.SORT_FIELDS, FunctionAreaRepository.DEFAULT_ORDER,
                FunctionAreaRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, FunctionAreaRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<FunctionAreaEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public FunctionArea createFunctionArea(FunctionArea request, boolean validateOnly) {
        validator.validate(request);
        FunctionAreaEntity entity = mapper.toEntity(request);
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
            FunctionAreaEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + CareersEvents.CREATED, AGGREGATE, stored.getId(),
                    CareersEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public FunctionArea updateFunctionArea(UUID id, String ifMatch, FunctionAreaUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            FunctionAreaEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + CareersEvents.UPDATED, AGGREGATE, id, CareersEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteFunctionArea(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            FunctionAreaEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByCandidatePreferredFunctionFunctionAreaId(id)) {
                throw new InvalidStateException(
                        "The function area is still referenced by careers.candidate_preferred_function rows; remove "
                                + "those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + CareersEvents.DELETED, AGGREGATE, id, CareersEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private FunctionAreaEntity loadVisible(DataScope scope, UUID id) {
        FunctionAreaEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOf(FunctionAreaEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(FunctionAreaEntity entity, FunctionAreaUpdate changes) {
        Patches patch = new Patches();
        patch.apply("code", changes.getCode(), entity::setCode);
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        patch.apply("isActive", changes.getIsActive(), entity::setIsActive);
        patch.apply("name", changes.getName(), entity::setName);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(FunctionAreaEntity entity, boolean creating) {
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

    private void assertUnique(FunctionAreaEntity entity) {
        if (repository.takenName1(entity)) {
            throw new DuplicateResourceException("A function area with the same name already exists.");
        }
        if (repository.takenCodeCode(entity)) {
            throw new DuplicateResourceException("A function area with the same code already exists.");
        }
    }
}
