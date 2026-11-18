package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.lms.api.model.LmsAssignmentSubmission;
import com.mysms.lms.api.model.LmsAssignmentSubmissionUpdate;
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
import com.mysms.lms.domain.LmsAssignmentSubmissionEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.LmsAssignmentSubmissionMapper;
import com.mysms.lms.repository.LmsAssignmentSubmissionRepository;
import com.mysms.lms.repository.TeachingReach;
import com.mysms.core.error.PermissionDeniedException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import com.mysms.lms.repository.AssignmentSubmissionWorkflowRepository;
import com.mysms.lms.api.model.GradeLearningSubmissionRequest;
import com.mysms.core.error.InvalidStateException;
import com.mysms.authclient.AuthContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.assignment_submission: audit and domain event commit in the same transaction as every change.
 * Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class LmsAssignmentSubmissionService extends AppTransactionManager {

    public static final String AGGREGATE = LmsAssignmentSubmissionEntity.TABLE;
    static final String PERMISSION_LIST = "lms.assignment_submission.read";
    static final String PERMISSION_GET = "lms.assignment_submission.read";
    static final String PERMISSION_CREATE = "lms.assignment_submission.create";
    static final String PERMISSION_UPDATE = "lms.assignment_submission.update";
    static final String PERMISSION_DELETE = "lms.assignment_submission.delete";
    static final String PERMISSION_FINALIZE = "lms.assignment_submission.finalize_submission";
    static final String PERMISSION_GRADE = "lms.assignment_submission.grade_learning_submission";
    private static final int MAX_COMMENTS = 10000;
    /**
     * The contract's LmsAssignmentSubmissionUpdate fields, in apply order: the student's own answer. Who submitted
     * comes from the token; finalizing and grading are the workflows below.
     */
    static final List<String> WRITABLE =
            List.of("assignmentId", "lessonId", "courseId", "submissionType", "questionSnapshot", "answer",
            "attachmentFileId", "contentItemId", "groupId", "title");


    private final LmsAssignmentSubmissionRepository repository;
    private final LmsAssignmentSubmissionMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    private final AssignmentSubmissionWorkflowRepository workflows;

    public LmsAssignmentSubmissionService(LmsAssignmentSubmissionRepository repository,
            LmsAssignmentSubmissionMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock, AssignmentSubmissionWorkflowRepository workflows) {

        super(database, transactions);
        this.workflows = workflows;
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    /** A submission the caller made (alone or with their group) or one of a cohort they teach; others are 404. */
    public LmsAssignmentSubmission getLmsAssignmentSubmission(UUID id) {
        UUID caller = AuthContext.require().userId();
        TeachingReach reach = CohortAccess.reach();
        return doInReadTransaction(() -> {
            LmsAssignmentSubmissionEntity entity = loadVisible(DataScope.current(PERMISSION_GET), id);
            if (!owns(entity, caller) && !repository.taught(id, reach)) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            return mapper.toDto(entity);
        });
    }

    public Page<LmsAssignmentSubmission> listLmsAssignmentSubmissions(Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, LmsAssignmentSubmissionRepository.SORT_FIELDS,
                        LmsAssignmentSubmissionRepository.DEFAULT_ORDER,
                LmsAssignmentSubmissionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, LmsAssignmentSubmissionRepository.FILTER_FIELDS)
                .and(LmsAssignmentSubmissionRepository.scope(DataScope.current(PERMISSION_LIST)))
                .and(LmsAssignmentSubmissionRepository.ownOrTaught(CohortAccess.reach()));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<LmsAssignmentSubmissionEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /**
     * Creates the caller's own draft (for their group when they are an active member of it); the grading and
     * finalizing fields start empty. With validateOnly every check runs and nothing is written.
     */
    public LmsAssignmentSubmission createLmsAssignmentSubmission(LmsAssignmentSubmission request,
            boolean validateOnly) {
        validator.validate(request);
        UUID caller = AuthContext.require().userId();
        LmsAssignmentSubmissionEntity entity = mapper.toEntity(request);
        entity.setUserId(caller);
        entity.setStatus(null);
        entity.setEvaluatorUserId(null);
        entity.setEvaluatorComments(null);
        entity.setSubmittedAt(null);
        entity.setIsFinal(false);
        entity.setFinalizedAt(null);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity, true);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (entity.getGroupId() != null && !workflows.activeMember(entity.getGroupId(), caller)) {
                throw new ValidationFailedException(List.of(new FieldError("groupId",
                        "the caller is not an active member of the group", "NOT_A_MEMBER")));
            }
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            LmsAssignmentSubmissionEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update of the caller's own draft under If-Match (anyone else's is 404). With an updateMask the listed
     * fields are set (absent ones cleared); without one, the non-null body fields apply.
     */
    public LmsAssignmentSubmission updateLmsAssignmentSubmission(UUID id, String ifMatch,
            LmsAssignmentSubmissionUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        UUID caller = AuthContext.require().userId();
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            LmsAssignmentSubmissionEntity entity = ownSubmission(scope, id, caller);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (entity.getFinalizedAt() != null) {
                throw new InvalidStateException("A finalized submission does not change.");
            }
            apply(entity, changes, fields);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (entity.getGroupId() != null && !workflows.activeMember(entity.getGroupId(), caller)) {
                throw new ValidationFailedException(List.of(new FieldError("groupId",
                        "the caller is not an active member of the group", "NOT_A_MEMBER")));
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
            outbox.publish(AGGREGATE + LmsEvents.UPDATED, AGGREGATE, id, LmsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private LmsAssignmentSubmissionEntity loadVisible(DataScope scope, UUID id) {
        LmsAssignmentSubmissionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.assignment the row belongs to. */
    private Optional<UUID> institutionOf(LmsAssignmentSubmissionEntity entity) {
        return repository.institutionOf(entity.getAssignmentId());
    }

    /** Applies the selected LmsAssignmentSubmissionUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(LmsAssignmentSubmissionEntity entity, LmsAssignmentSubmissionUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "assignmentId" -> entity.setAssignmentId(changes.getAssignmentId());
                case "lessonId" -> entity.setLessonId(changes.getLessonId());
                case "courseId" -> entity.setCourseId(changes.getCourseId());
                case "submissionType" -> entity.setSubmissionType(Optional.ofNullable(changes.getSubmissionType())
                        .map(v -> v.getValue()).orElse(null));
                case "questionSnapshot" -> entity.setQuestionSnapshot(changes.getQuestionSnapshot());
                case "answer" -> entity.setAnswer(changes.getAnswer());
                case "attachmentFileId" -> entity.setAttachmentFileId(changes.getAttachmentFileId());
                case "contentItemId" -> entity.setContentItemId(changes.getContentItemId());
                case "groupId" -> entity.setGroupId(changes.getGroupId());
                case "title" -> entity.setTitle(changes.getTitle());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(LmsAssignmentSubmissionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getAssignmentId() == null) {
            errors.add(new FieldError("assignmentId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getStatus() == null) {
            errors.add(new FieldError("status", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsFinal() == null) {
            errors.add(new FieldError("isFinal", "must not be null", "NotNull"));
        }
        if (entity.getAssignmentId() != null && !repository.existsAssignmentId(entity.getAssignmentId())) {
            errors.add(new FieldError("assignmentId", "assignment does not exist", "NOT_FOUND"));
        }
        if (entity.getContentItemId() != null && !repository.existsContentItemId(entity.getContentItemId())) {
            errors.add(new FieldError("contentItemId", "content item does not exist", "NOT_FOUND"));
        }
        if (entity.getCourseId() != null && !repository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (entity.getGroupId() != null && !repository.existsGroupId(entity.getGroupId())) {
            errors.add(new FieldError("groupId", "assignment group does not exist", "NOT_FOUND"));
        }
        if (entity.getLessonId() != null && !repository.existsLessonId(entity.getLessonId())) {
            errors.add(new FieldError("lessonId", "lesson does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(LmsAssignmentSubmissionEntity entity) {

    }

    // ---- Workflows: delete a draft, finalize, grade ----

    /**
     * Deletes a draft: the caller's own (the submitter, or an active member of its group), or any draft of a cohort
     * the caller teaches; a finalized or evaluated submission is evidence and stays (409). Anyone else's is 404.
     */
    public void deleteLmsAssignmentSubmission(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        UUID caller = AuthContext.require().userId();
        TeachingReach reach = CohortAccess.reach();
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            LmsAssignmentSubmissionEntity entity = loadVisible(scope, id);
            if (!owns(entity, caller) && !repository.taught(id, reach)) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (entity.getFinalizedAt() != null || !"not_graded".equals(entity.getStatus())) {
                throw new InvalidStateException("A finalized or evaluated submission is kept.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + LmsEvents.DELETED, AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** Marks the caller's own (or their group's) submission final: from then on it no longer changes. */
    public LmsAssignmentSubmission finalizeSubmission(UUID id) {
        UUID caller = AuthContext.require().userId();
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_FINALIZE);
            LmsAssignmentSubmissionEntity entity = ownSubmission(scope, id, caller);
            scope.requireWritable(institutionOf(entity));
            if (entity.getFinalizedAt() != null) {
                throw new InvalidStateException("The submission is already final.");
            }
            OffsetDateTime now = now();
            if (workflows.finalizeSubmission(id, entity.getRowVersion(), now, caller) == 0) {
                throw new PreconditionFailedException("The submission changed meanwhile; read it again.");
            }
            return transitioned(id, "finalize", List.of("isFinal", "finalizedAt", "submittedAt"));
        });
    }

    /**
     * A teacher's evaluation of a final submission of a cohort they teach: pass, fail or not applicable, with
     * comments. The submitter gets 403; anyone else 404.
     */
    public LmsAssignmentSubmission gradeLearningSubmission(UUID id, GradeLearningSubmissionRequest request) {
        validator.validate(request);
        String comments = request.getEvaluatorComments();
        if (comments != null && comments.length() > MAX_COMMENTS) {
            throw new ValidationFailedException(List.of(new FieldError("evaluatorComments",
                    "must be at most " + MAX_COMMENTS + " characters", "Size")));
        }
        UUID teacher = AuthContext.require().userId();
        TeachingReach reach = CohortAccess.reach();
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_GRADE);
            LmsAssignmentSubmissionEntity entity = loadVisible(scope, id);
            if (!repository.taught(id, reach)) {
                if (owns(entity, teacher)) {
                    throw new PermissionDeniedException("Only the cohort's teachers evaluate a submission.");
                }
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            if (Boolean.FALSE.equals(entity.getIsFinal())) {
                throw new InvalidStateException("A draft is evaluated once it is final.");
            }
            if (workflows.grade(id, entity.getRowVersion(), request.getStatus().getValue(), comments, now(),
                    teacher) == 0) {
                throw new PreconditionFailedException("The submission changed meanwhile; read it again.");
            }
            return transitioned(id, "grade", List.of("status", "evaluatorUserId", "evaluatorComments"));
        });
    }

    /** The caller's own submission, or one of a group the caller is an active member of; anything else is 404. */
    private LmsAssignmentSubmissionEntity ownSubmission(DataScope scope, UUID id, UUID caller) {
        LmsAssignmentSubmissionEntity entity = loadVisible(scope, id);
        if (!owns(entity, caller)) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    private boolean owns(LmsAssignmentSubmissionEntity entity, UUID caller) {
        return caller.equals(entity.getUserId())
                || (entity.getGroupId() != null && workflows.activeMember(entity.getGroupId(), caller));
    }

    private LmsAssignmentSubmission transitioned(UUID id, String step, List<String> fields) {
        audit.recordTransition(AGGREGATE, id, step, fields, null);
        outbox.publish(AGGREGATE + "." + step, AGGREGATE, id, LmsEvents.payload(id));
        return mapper.toDto(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
