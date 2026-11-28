package com.mysms.academics.timetable.business;

import com.mysms.authclient.DataScope;
import com.mysms.academics.timetable.api.model.Period;
import com.mysms.academics.timetable.api.model.PeriodUpdate;
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
import com.mysms.academics.timetable.config.TimetableDatabaseConfig;
import com.mysms.academics.timetable.config.TimetableEventsConfig;
import com.mysms.academics.timetable.domain.PeriodEntity;
import com.mysms.academics.timetable.events.TimetableEvents;
import com.mysms.academics.timetable.mapper.PeriodMapper;
import com.mysms.academics.timetable.repository.PeriodRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of timetable.period: audit and domain event commit in the same transaction as every change. Rows
 * outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class PeriodService extends AppTransactionManager {

    public static final String AGGREGATE = PeriodEntity.TABLE;
    static final String PERMISSION_LIST = "timetable.period.read";
    static final String PERMISSION_GET = "timetable.period.read";
    static final String PERMISSION_CREATE = "timetable.period.create";
    static final String PERMISSION_UPDATE = "timetable.period.update";
    static final String PERMISSION_DELETE = "timetable.period.delete";


    private final PeriodRepository repository;
    private final PeriodMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public PeriodService(PeriodRepository repository, PeriodMapper mapper,
            @Qualifier(TimetableDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(TimetableDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(TimetableEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(TimetableEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public Period getPeriod(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Period> listPeriods(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, PeriodRepository.SORT_FIELDS, PeriodRepository.DEFAULT_ORDER,
                PeriodRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PeriodRepository.FILTER_FIELDS)
                .and(PeriodRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<PeriodEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Period createPeriod(Period request, boolean validateOnly) {
        validator.validate(request);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", request.getStartTime(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        PeriodEntity entity = mapper.toEntity(request);
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
            PeriodEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + TimetableEvents.CREATED, AGGREGATE, stored.getId(),
                    TimetableEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public Period updatePeriod(UUID id, String ifMatch, PeriodUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", Patches.value(changes.getStartTime()), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            PeriodEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + TimetableEvents.UPDATED, AGGREGATE, id, TimetableEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deletePeriod(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            PeriodEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedBySessionPeriodId(id)) {
                throw new InvalidStateException(
                        "The period is still referenced by timetable.session rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + TimetableEvents.DELETED, AGGREGATE, id, TimetableEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private PeriodEntity loadVisible(DataScope scope, UUID id) {
        PeriodEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(PeriodEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(PeriodEntity entity, PeriodUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("name", changes.getName(), entity::setName);
        patch.apply("startTime", changes.getStartTime(), v -> entity.setStartTime(WireValues.time(v)));
        patch.apply("durationMinutes", changes.getDurationMinutes(), v -> entity.setDurationMinutes(WireValues.decimal(v)));
        patch.apply("sequence", changes.getSequence(), entity::setSequence);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(PeriodEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (entity.getStartTime() == null) {
            errors.add(new FieldError("startTime", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("durationMinutes", WireValues.decimal(entity.getDurationMinutes()), 6, 2, errors);
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(PeriodEntity entity) {
        if (repository.takenInstitutionIdName1(entity)) {
            throw new DuplicateResourceException("A period with the same institution id and name already exists.");
        }
    }
}
