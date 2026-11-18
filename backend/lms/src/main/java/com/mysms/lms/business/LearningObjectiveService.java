package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.lms.api.model.LearningObjective;
import com.mysms.lms.api.model.LearningObjectiveUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
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
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.LearningObjectiveEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.LearningObjectiveMapper;
import com.mysms.lms.repository.LearningObjectiveRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.learning_objective: audit and domain event commit in the same transaction as every change.
 * Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class LearningObjectiveService extends AppTransactionManager {

    public static final String AGGREGATE = LearningObjectiveEntity.TABLE;
    static final String PERMISSION_LIST = "lms.learning_objective.read";
    static final String PERMISSION_GET = "lms.learning_objective.read";
    static final String PERMISSION_CREATE = "lms.learning_objective.create";
    static final String PERMISSION_UPDATE = "lms.learning_objective.update";
    static final String PERMISSION_DELETE = "lms.learning_objective.delete";
    /** The contract's LearningObjectiveUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("courseId", "chapterId", "objective", "sortOrder", "copiedFromId");


    private final LearningObjectiveRepository repository;
    private final LearningObjectiveMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public LearningObjectiveService(LearningObjectiveRepository repository, LearningObjectiveMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public LearningObjective getLearningObjective(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<LearningObjective> listLearningObjectives(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, LearningObjectiveRepository.SORT_FIELDS,
                        LearningObjectiveRepository.DEFAULT_ORDER,
                LearningObjectiveRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, LearningObjectiveRepository.FILTER_FIELDS)
                .and(LearningObjectiveRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<LearningObjectiveEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public LearningObjective createLearningObjective(LearningObjective request, boolean validateOnly) {
        validator.validate(request);
        LearningObjectiveEntity entity = mapper.toEntity(request);
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
            LearningObjectiveEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public LearningObjective updateLearningObjective(UUID id, String ifMatch, LearningObjectiveUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            LearningObjectiveEntity entity = loadVisible(scope, id);
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
    public void deleteLearningObjective(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            LearningObjectiveEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByLearningObjectiveCopiedFromId(id)) {
                throw new InvalidStateException(
                        "The learning objective is still referenced by lms.learning_objective rows; remove those "
                                + "first.");
            }
            if (repository.referencedByQuizAnswerLearningObjectiveId(id)) {
                throw new InvalidStateException(
                        "The learning objective is still referenced by lms.quiz_answer rows; remove those first.");
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
    private LearningObjectiveEntity loadVisible(DataScope scope, UUID id) {
        LearningObjectiveEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.course the row belongs to. */
    private Optional<UUID> institutionOf(LearningObjectiveEntity entity) {
        return repository.institutionOf(entity.getCourseId());
    }

    /** Applies the selected LearningObjectiveUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(LearningObjectiveEntity entity, LearningObjectiveUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "courseId" -> entity.setCourseId(changes.getCourseId());
                case "chapterId" -> entity.setChapterId(changes.getChapterId());
                case "objective" -> entity.setObjective(changes.getObjective());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                case "copiedFromId" -> entity.setCopiedFromId(changes.getCopiedFromId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(LearningObjectiveEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getObjective() == null) {
            errors.add(new FieldError("objective", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getChapterId() != null && !repository.existsChapterId(entity.getChapterId())) {
            errors.add(new FieldError("chapterId", "chapter does not exist", "NOT_FOUND"));
        }
        if (entity.getCopiedFromId() != null && !repository.existsCopiedFromId(entity.getCopiedFromId())) {
            errors.add(new FieldError("copiedFromId", "learning objective does not exist", "NOT_FOUND"));
        }
        if (entity.getCourseId() != null && !repository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(LearningObjectiveEntity entity) {

    }
}
