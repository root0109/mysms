package com.mysms.assessment.business;

import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.config.AssessmentEventsConfig;
import com.mysms.assessment.domain.HomeworkSubmissionEntity;
import com.mysms.assessment.events.AssessmentEvents;
import com.mysms.assessment.mapper.HomeworkSubmissionMapper;
import com.mysms.assessment.repository.HomeworkSubmissionRepository;
import com.mysms.authclient.DataScope;
import com.mysms.assessment.api.model.HomeworkSubmission;
import com.mysms.assessment.api.model.HomeworkSubmissionUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
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
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of assessment.homework_submission (JAVA-CODING-RULES §4): one plain class, explicit methods,
 * transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 */
@Service
public class HomeworkSubmissionService extends AppTransactionManager {

    public static final String AGGREGATE = HomeworkSubmissionEntity.TABLE;
    static final String PERMISSION_LIST = "assessment.homework_submission.read";
    static final String PERMISSION_GET = "assessment.homework_submission.read";
    static final String PERMISSION_CREATE = "assessment.homework_submission.create";
    static final String PERMISSION_UPDATE = "assessment.homework_submission.update";
    static final String PERMISSION_DELETE = "assessment.homework_submission.delete";
    /**
     * The contract's HomeworkSubmissionUpdate fields, in apply order. The check and the reply (who, when, the comments,
     * the student's answer) are not among them: only the workflow operations write those.
     */
    static final List<String> WRITABLE = List.of("homeworkId", "studentId", "submittedOn");


    private final HomeworkSubmissionRepository repository;
    private final HomeworkSubmissionMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public HomeworkSubmissionService(HomeworkSubmissionRepository repository, HomeworkSubmissionMapper mapper,
            @Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AssessmentDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AssessmentEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(AssessmentEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public HomeworkSubmission getHomeworkSubmission(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<HomeworkSubmission> listHomeworkSubmissions(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, HomeworkSubmissionRepository.SORT_FIELDS,
                        HomeworkSubmissionRepository.DEFAULT_ORDER,
                HomeworkSubmissionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, HomeworkSubmissionRepository.FILTER_FIELDS)
                .and(HomeworkSubmissionRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<HomeworkSubmissionEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public HomeworkSubmission createHomeworkSubmission(HomeworkSubmission request, boolean validateOnly) {
        validator.validate(request);
        HomeworkSubmissionEntity entity = mapper.toEntity(request);
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
            HomeworkSubmissionEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + AssessmentEvents.CREATED, AGGREGATE, stored.getId(),
                    AssessmentEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public HomeworkSubmission updateHomeworkSubmission(UUID id, String ifMatch, HomeworkSubmissionUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            HomeworkSubmissionEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + AssessmentEvents.UPDATED, AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteHomeworkSubmission(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            HomeworkSubmissionEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + AssessmentEvents.DELETED, AGGREGATE, id, AssessmentEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private HomeworkSubmissionEntity loadVisible(DataScope scope, UUID id) {
        HomeworkSubmissionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the assessment.homework it belongs to). */
    private Optional<UUID> institutionOf(HomeworkSubmissionEntity entity) {
        return repository.institutionOf(entity.getHomeworkId());
    }

    /**
     * Applies the selected HomeworkSubmissionUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void apply(HomeworkSubmissionEntity entity, HomeworkSubmissionUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "homeworkId" -> entity.setHomeworkId(changes.getHomeworkId());
                case "studentId" -> entity.setStudentId(changes.getStudentId());
                case "submittedOn" -> entity.setSubmittedOn(changes.getSubmittedOn());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(HomeworkSubmissionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getHomeworkId() == null) {
            errors.add(new FieldError("homeworkId", "must not be null", "NotNull"));
        }
        if (entity.getStudentId() == null) {
            errors.add(new FieldError("studentId", "must not be null", "NotNull"));
        }
        if (entity.getHomeworkId() != null && !repository.existsHomeworkId(entity.getHomeworkId())) {
            errors.add(new FieldError("homeworkId", "homework does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(HomeworkSubmissionEntity entity) {
        if (repository.takenHomeworkIdStudentId1(entity)) {
            throw new DuplicateResourceException(
                    "A homework submission with the same homework id and student id already exists.");
        }
    }
}
