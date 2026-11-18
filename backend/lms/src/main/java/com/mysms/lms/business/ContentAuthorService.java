package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.lms.api.model.ContentAuthor;
import com.mysms.lms.api.model.ContentAuthorUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
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
import com.mysms.lms.domain.ContentAuthorEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.ContentAuthorMapper;
import com.mysms.lms.repository.ContentAuthorRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.content_author: audit and domain event commit in the same transaction as every change. Rows
 * outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class ContentAuthorService extends AppTransactionManager {

    public static final String AGGREGATE = ContentAuthorEntity.TABLE;
    static final String PERMISSION_LIST = "lms.content_author.read";
    static final String PERMISSION_GET = "lms.content_author.read";
    static final String PERMISSION_CREATE = "lms.content_author.create";
    static final String PERMISSION_UPDATE = "lms.content_author.update";
    static final String PERMISSION_DELETE = "lms.content_author.delete";
    /** The contract's ContentAuthorUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("userId", "quizId", "questionId", "assignmentId", "exerciseId", "learningProgramId");


    private final ContentAuthorRepository repository;
    private final ContentAuthorMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public ContentAuthorService(ContentAuthorRepository repository, ContentAuthorMapper mapper,
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

    public ContentAuthor getContentAuthor(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<ContentAuthor> listContentAuthors(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ContentAuthorRepository.SORT_FIELDS, ContentAuthorRepository.DEFAULT_ORDER,
                ContentAuthorRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ContentAuthorRepository.FILTER_FIELDS)
                .and(ContentAuthorRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ContentAuthorEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ContentAuthor createContentAuthor(ContentAuthor request, boolean validateOnly) {
        validator.validate(request);
        ContentAuthorEntity entity = mapper.toEntity(request);
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
            ContentAuthorEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public ContentAuthor updateContentAuthor(UUID id, String ifMatch, ContentAuthorUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ContentAuthorEntity entity = loadVisible(scope, id);
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
    public void deleteContentAuthor(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            ContentAuthorEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
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
    private ContentAuthorEntity loadVisible(DataScope scope, UUID id) {
        ContentAuthorEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.quiz the row belongs to. */
    private Optional<UUID> institutionOf(ContentAuthorEntity entity) {
        return repository.institutionOf(entity.getQuizId());
    }

    /** Applies the selected ContentAuthorUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(ContentAuthorEntity entity, ContentAuthorUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "userId" -> entity.setUserId(changes.getUserId());
                case "quizId" -> entity.setQuizId(changes.getQuizId());
                case "questionId" -> entity.setQuestionId(changes.getQuestionId());
                case "assignmentId" -> entity.setAssignmentId(changes.getAssignmentId());
                case "exerciseId" -> entity.setExerciseId(changes.getExerciseId());
                case "learningProgramId" -> entity.setLearningProgramId(changes.getLearningProgramId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ContentAuthorEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getAssignmentId() != null && !repository.existsAssignmentId(entity.getAssignmentId())) {
            errors.add(new FieldError("assignmentId", "assignment does not exist", "NOT_FOUND"));
        }
        if (entity.getExerciseId() != null && !repository.existsExerciseId(entity.getExerciseId())) {
            errors.add(new FieldError("exerciseId", "programming exercise does not exist", "NOT_FOUND"));
        }
        if (entity.getLearningProgramId() != null
                && !repository.existsLearningProgramId(entity.getLearningProgramId())) {
            errors.add(new FieldError("learningProgramId", "learning program does not exist", "NOT_FOUND"));
        }
        if (entity.getQuestionId() != null && !repository.existsQuestionId(entity.getQuestionId())) {
            errors.add(new FieldError("questionId", "question does not exist", "NOT_FOUND"));
        }
        if (entity.getQuizId() != null && !repository.existsQuizId(entity.getQuizId())) {
            errors.add(new FieldError("quizId", "quiz does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ContentAuthorEntity entity) {

    }
}
