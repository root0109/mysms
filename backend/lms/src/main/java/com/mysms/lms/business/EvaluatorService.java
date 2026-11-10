package com.mysms.lms.business;

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
import com.mysms.lms.api.model.Evaluator;
import com.mysms.lms.api.model.EvaluatorAvailability;
import com.mysms.lms.api.model.EvaluatorAvailabilityUpdate;
import com.mysms.lms.api.model.EvaluatorUpdate;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.EvaluatorAvailabilityEntity;
import com.mysms.lms.domain.EvaluatorEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.EvaluatorAvailabilityMapper;
import com.mysms.lms.mapper.EvaluatorMapper;
import com.mysms.lms.repository.EvaluatorAvailabilityRepository;
import com.mysms.lms.repository.EvaluatorRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.evaluator (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a Evaluator
 * are served here too — EvaluatorAvailability.
 */
@Service
public class EvaluatorService extends AppTransactionManager {

    public static final String AGGREGATE = EvaluatorEntity.TABLE;
    static final String PERMISSION_LIST = "lms.evaluator.read";
    static final String PERMISSION_GET = "lms.evaluator.read";
    static final String PERMISSION_CREATE = "lms.evaluator.create";
    static final String PERMISSION_UPDATE = "lms.evaluator.update";
    static final String PERMISSION_DELETE = "lms.evaluator.delete";
    /** The contract's EvaluatorUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("userId", "unavailableFrom", "unavailableTo");

    public static final String EVALUATOR_AVAILABILITY_AGGREGATE = EvaluatorAvailabilityEntity.TABLE;
    static final String EVALUATOR_AVAILABILITY_PERMISSION_LIST = "lms.evaluator_availability.read";
    static final String EVALUATOR_AVAILABILITY_PERMISSION_GET = "lms.evaluator_availability.read";
    static final String EVALUATOR_AVAILABILITY_PERMISSION_CREATE = "lms.evaluator_availability.create";
    static final String EVALUATOR_AVAILABILITY_PERMISSION_UPDATE = "lms.evaluator_availability.update";
    static final String EVALUATOR_AVAILABILITY_PERMISSION_DELETE = "lms.evaluator_availability.delete";
    /** The contract's EvaluatorAvailabilityUpdate fields, in apply order. */
    static final List<String> EVALUATOR_AVAILABILITY_WRITABLE = List.of("weekday", "startTime", "endTime");

    private final EvaluatorRepository repository;
    private final EvaluatorMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final EvaluatorAvailabilityRepository evaluatorAvailabilityRepository;
    private final EvaluatorAvailabilityMapper evaluatorAvailabilityMapper;

    public EvaluatorService(EvaluatorRepository repository,
            EvaluatorMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            EvaluatorAvailabilityRepository evaluatorAvailabilityRepository,
            EvaluatorAvailabilityMapper evaluatorAvailabilityMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.evaluatorAvailabilityRepository = evaluatorAvailabilityRepository;
        this.evaluatorAvailabilityMapper = evaluatorAvailabilityMapper;
    }

    public Evaluator getEvaluator(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Evaluator> listEvaluators(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, EvaluatorRepository.SORT_FIELDS, EvaluatorRepository.DEFAULT_ORDER,
                EvaluatorRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, EvaluatorRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<EvaluatorEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Evaluator createEvaluator(Evaluator request, boolean validateOnly) {
        validator.validate(request);
        EvaluatorEntity entity = mapper.toEntity(request);
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
            EvaluatorEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Evaluator updateEvaluator(UUID id, String ifMatch, EvaluatorUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            EvaluatorEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + LmsEvents.UPDATED, AGGREGATE, id, LmsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteEvaluator(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            EvaluatorEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByCertificateEvaluatorId(id)) {
                throw new InvalidStateException(
                        "The evaluator is still referenced by lms.certificate rows; remove those first.");
            }
            if (repository.referencedByCohortCourseEvaluatorId(id)) {
                throw new InvalidStateException(
                        "The evaluator is still referenced by lms.cohort_course rows; remove those first.");
            }
            if (repository.referencedByCourseDefaultEvaluatorId(id)) {
                throw new InvalidStateException(
                        "The evaluator is still referenced by lms.course rows; remove those first.");
            }
            if (repository.referencedByEvaluatorAvailabilityEvaluatorId(id)) {
                throw new InvalidStateException(
                        "The evaluator is still referenced by lms.evaluator_availability rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + LmsEvents.DELETED, AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private EvaluatorEntity loadVisible(DataScope scope, UUID id) {
        EvaluatorEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOf(EvaluatorEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected EvaluatorUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(EvaluatorEntity entity, EvaluatorUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "userId" -> entity.setUserId(changes.getUserId());
                case "unavailableFrom" -> entity.setUnavailableFrom(changes.getUnavailableFrom());
                case "unavailableTo" -> entity.setUnavailableTo(changes.getUnavailableTo());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(EvaluatorEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(EvaluatorEntity entity) {
        if (repository.takenUserId1(entity)) {
            throw new DuplicateResourceException("A evaluator with the same user id already exists.");
        }
    }

    // ---- EvaluatorAvailability: a record of the Evaluator aggregate ----

    public EvaluatorAvailability getEvaluatorAvailability(UUID evaluatorId, UUID id) {
        return doInReadTransaction(() -> {
            return evaluatorAvailabilityMapper.toDto(loadVisibleEvaluatorAvailability(DataScope.current(
                    EVALUATOR_AVAILABILITY_PERMISSION_GET), evaluatorId, id));
        });
    }

    public Page<EvaluatorAvailability> listEvaluatorAvailabilitys(UUID evaluatorId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, EvaluatorAvailabilityRepository.SORT_FIELDS,
                        EvaluatorAvailabilityRepository.DEFAULT_ORDER,
                EvaluatorAvailabilityRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, EvaluatorAvailabilityRepository.FILTER_FIELDS)
                .and(EvaluatorAvailabilityRepository.ofParent(evaluatorId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + evaluatorId, clock);
        return doInReadTransaction(() -> {
            Page<EvaluatorAvailabilityEntity> rows = evaluatorAvailabilityRepository.list(page, where);
            return rows.map(evaluatorAvailabilityMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public EvaluatorAvailability createEvaluatorAvailability(UUID evaluatorId, EvaluatorAvailability request,
            boolean validateOnly) {
        validator.validate(request);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", request.getStartTime(), wire);
        WireValues.checkTime("endTime", request.getEndTime(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        EvaluatorAvailabilityEntity entity = evaluatorAvailabilityMapper.toEntity(request);
        entity.setEvaluatorId(evaluatorId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(EVALUATOR_AVAILABILITY_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!evaluatorAvailabilityRepository.existsEvaluatorId(entity.getEvaluatorId())) {
                throw new ResourceNotFoundException("lms.evaluator", entity.getEvaluatorId());
            }
            assertValidEvaluatorAvailability(entity, true);
            scope.requireWritable(institutionOfEvaluatorAvailability(entity));
            assertUniqueEvaluatorAvailability(entity);
            if (validateOnly) {
                return evaluatorAvailabilityMapper.toDto(entity);
            }
            EvaluatorAvailabilityEntity stored = evaluatorAvailabilityRepository.insert(entity);
            audit.recordCreate(EVALUATOR_AVAILABILITY_AGGREGATE, stored.getId());
            outbox.publish(EVALUATOR_AVAILABILITY_AGGREGATE + LmsEvents.CREATED, EVALUATOR_AVAILABILITY_AGGREGATE,
                    stored.getId(), LmsEvents.payload(stored.getId()));
            return evaluatorAvailabilityMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public EvaluatorAvailability updateEvaluatorAvailability(UUID evaluatorId, UUID id, String ifMatch,
            EvaluatorAvailabilityUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, EVALUATOR_AVAILABILITY_WRITABLE);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", changes.getStartTime(), wire);
        WireValues.checkTime("endTime", changes.getEndTime(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EVALUATOR_AVAILABILITY_PERMISSION_UPDATE);
            EvaluatorAvailabilityEntity entity = loadVisibleEvaluatorAvailability(scope, evaluatorId, id);
            scope.requireWritable(institutionOfEvaluatorAvailability(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyEvaluatorAvailability(entity, changes, fields);
            validator.validate(evaluatorAvailabilityMapper.toDto(entity));
            assertValidEvaluatorAvailability(entity, false);
            scope.requireWritable(institutionOfEvaluatorAvailability(entity));
            assertUniqueEvaluatorAvailability(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return evaluatorAvailabilityMapper.toDto(entity);
            }
            if (evaluatorAvailabilityRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(EVALUATOR_AVAILABILITY_AGGREGATE, id, fields);
            outbox.publish(EVALUATOR_AVAILABILITY_AGGREGATE + LmsEvents.UPDATED, EVALUATOR_AVAILABILITY_AGGREGATE, id,
                    LmsEvents.payload(id));
            return evaluatorAvailabilityMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteEvaluatorAvailability(UUID evaluatorId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EVALUATOR_AVAILABILITY_PERMISSION_DELETE);
            EvaluatorAvailabilityEntity entity = loadVisibleEvaluatorAvailability(scope, evaluatorId, id);
            scope.requireWritable(institutionOfEvaluatorAvailability(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (evaluatorAvailabilityRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(EVALUATOR_AVAILABILITY_AGGREGATE, id);
            outbox.publish(EVALUATOR_AVAILABILITY_AGGREGATE + LmsEvents.DELETED, EVALUATOR_AVAILABILITY_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private EvaluatorAvailabilityEntity loadVisibleEvaluatorAvailability(DataScope scope, UUID evaluatorId, UUID id) {
        EvaluatorAvailabilityEntity entity = evaluatorAvailabilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(EVALUATOR_AVAILABILITY_AGGREGATE, id));
        if (!scope.canRead(institutionOfEvaluatorAvailability(entity)) || !evaluatorId.equals(entity.getEvaluatorId())) {
            throw new ResourceNotFoundException(EVALUATOR_AVAILABILITY_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOfEvaluatorAvailability(EvaluatorAvailabilityEntity entity) {
        return Optional.empty();
    }

    /**
     * Applies the selected EvaluatorAvailabilityUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyEvaluatorAvailability(EvaluatorAvailabilityEntity entity, EvaluatorAvailabilityUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "weekday" -> entity.setWeekday(Optional.ofNullable(changes.getWeekday()).map(v -> v.getValue())
                        .orElse(null));
                case "startTime" -> entity.setStartTime(WireValues.time(changes.getStartTime()));
                case "endTime" -> entity.setEndTime(WireValues.time(changes.getEndTime()));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidEvaluatorAvailability(EvaluatorAvailabilityEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getEvaluatorId() == null) {
            errors.add(new FieldError("evaluatorId", "must not be null", "NotNull"));
        }
        if (entity.getWeekday() == null) {
            errors.add(new FieldError("weekday", "must not be null", "NotNull"));
        }
        if (entity.getStartTime() == null) {
            errors.add(new FieldError("startTime", "must not be null", "NotNull"));
        }
        if (entity.getEndTime() == null) {
            errors.add(new FieldError("endTime", "must not be null", "NotNull"));
        }
        if (entity.getEvaluatorId() != null && !evaluatorAvailabilityRepository.existsEvaluatorId(entity.getEvaluatorId())) {
            errors.add(new FieldError("evaluatorId", "evaluator does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueEvaluatorAvailability(EvaluatorAvailabilityEntity entity) {

    }
}
