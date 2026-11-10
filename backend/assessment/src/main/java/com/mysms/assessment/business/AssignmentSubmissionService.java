package com.mysms.assessment.business;

import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.config.AssessmentEventsConfig;
import com.mysms.assessment.domain.AssignmentSubmissionEntity;
import com.mysms.assessment.events.AssessmentEvents;
import com.mysms.assessment.mapper.AssignmentSubmissionMapper;
import com.mysms.assessment.repository.AssignmentSubmissionRepository;
import com.mysms.authclient.DataScope;
import com.mysms.assessment.api.model.AcceptAssignmentSubmissionRequest;
import com.mysms.assessment.api.model.AssignmentSubmission;
import com.mysms.assessment.api.model.AssignmentSubmissionUpdate;
import com.mysms.assessment.api.model.RejectAssignmentSubmissionRequest;
import com.mysms.assessment.api.model.SubmitAssignmentSubmissionRequest;
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
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of assessment.assignment_submission (JAVA-CODING-RULES §4): one plain class, explicit methods,
 * transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 */
@Service
public class AssignmentSubmissionService extends AppTransactionManager {

    public static final String AGGREGATE = AssignmentSubmissionEntity.TABLE;
    static final String PERMISSION_LIST = "assessment.assignment_submission.read";
    static final String PERMISSION_GET = "assessment.assignment_submission.read";
    static final String PERMISSION_CREATE = "assessment.assignment_submission.create";
    static final String PERMISSION_UPDATE = "assessment.assignment_submission.update";
    static final String PERMISSION_DELETE = "assessment.assignment_submission.delete";
    static final String PERMISSION_SUBMIT_ASSIGNMENT_SUBMISSION =
            "assessment.assignment_submission.submit_assignment_submission";
    static final String PERMISSION_ACCEPT_ASSIGNMENT_SUBMISSION =
            "assessment.assignment_submission.accept_assignment_submission";
    static final String PERMISSION_REJECT_ASSIGNMENT_SUBMISSION =
            "assessment.assignment_submission.reject_assignment_submission";
    /** The contract's AssignmentSubmissionUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("assignmentId", "studentId", "content", "submittedAt", "marksObtained", "gradedByUserId",
                    "gradedOn",
            "feedback");


    private final AssignmentSubmissionRepository repository;
    private final AssignmentSubmissionMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public AssignmentSubmissionService(AssignmentSubmissionRepository repository, AssignmentSubmissionMapper mapper,
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

    public AssignmentSubmission getAssignmentSubmission(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<AssignmentSubmission> listAssignmentSubmissions(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, AssignmentSubmissionRepository.SORT_FIELDS,
                        AssignmentSubmissionRepository.DEFAULT_ORDER,
                AssignmentSubmissionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, AssignmentSubmissionRepository.FILTER_FIELDS)
                .and(AssignmentSubmissionRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<AssignmentSubmissionEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public AssignmentSubmission createAssignmentSubmission(AssignmentSubmission request, boolean validateOnly) {
        validator.validate(request);
        AssignmentSubmissionEntity entity = mapper.toEntity(request);
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
            AssignmentSubmissionEntity stored = repository.insert(entity);
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
    public AssignmentSubmission updateAssignmentSubmission(UUID id, String ifMatch, AssignmentSubmissionUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            AssignmentSubmissionEntity entity = loadVisible(scope, id);
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
    public void deleteAssignmentSubmission(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            AssignmentSubmissionEntity entity = loadVisible(scope, id);
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

    /**
     * assessment.SubmitAssignmentSubmission: Student submits. The row must be visible and writable for the caller, and
     * the step is allowed while status is draft or change_requested; in any other state
     * the answer is 409 and nothing changes.
     */
    public AssignmentSubmission submitAssignmentSubmission(UUID id, SubmitAssignmentSubmissionRequest request) {
        DataScope scope = DataScope.current(PERMISSION_SUBMIT_ASSIGNMENT_SUBMISSION);
        return doInWriteTransaction(() -> {
            AssignmentSubmissionEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("draft", "change_requested").contains(entity.getStatus()))) {
                throw new InvalidStateException(
                        "The assignment submission cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.submitAssignmentSubmission(id, entity.getRowVersion(), now, Actor.current()
                    .orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "submit", List.of("status", "submittedAt"), null);
            outbox.publish(AGGREGATE + ".submit", AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * assessment.AcceptAssignmentSubmission: Accept. The row must be visible and writable for the caller, and the step
     * is allowed while status is submitted; in any other state
     * the answer is 409 and nothing changes.
     */
    public AssignmentSubmission acceptAssignmentSubmission(UUID id, AcceptAssignmentSubmissionRequest request) {
        DataScope scope = DataScope.current(PERMISSION_ACCEPT_ASSIGNMENT_SUBMISSION);
        return doInWriteTransaction(() -> {
            AssignmentSubmissionEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("submitted").contains(entity.getStatus()))) {
                throw new InvalidStateException(
                        "The assignment submission cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.acceptAssignmentSubmission(id, entity.getRowVersion(), now, Actor.current()
                    .orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "accept", List.of("status"), null);
            outbox.publish(AGGREGATE + ".accept", AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * assessment.RejectAssignmentSubmission: Reject. The row must be visible and writable for the caller, and the step
     * is allowed while status is submitted; in any other state
     * the answer is 409 and nothing changes.
     */
    public AssignmentSubmission rejectAssignmentSubmission(UUID id, RejectAssignmentSubmissionRequest request) {
        DataScope scope = DataScope.current(PERMISSION_REJECT_ASSIGNMENT_SUBMISSION);
        return doInWriteTransaction(() -> {
            AssignmentSubmissionEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("submitted").contains(entity.getStatus()))) {
                throw new InvalidStateException(
                        "The assignment submission cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.rejectAssignmentSubmission(id, entity.getRowVersion(), now, Actor.current()
                    .orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "reject", List.of("status"), request == null ? null
                    : request.getReason());
            outbox.publish(AGGREGATE + ".reject", AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private AssignmentSubmissionEntity loadVisible(DataScope scope, UUID id) {
        AssignmentSubmissionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the assessment.assignment it belongs to). */
    private Optional<UUID> institutionOf(AssignmentSubmissionEntity entity) {
        return repository.institutionOf(entity.getAssignmentId());
    }

    /**
     * Applies the selected AssignmentSubmissionUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void apply(AssignmentSubmissionEntity entity, AssignmentSubmissionUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "assignmentId" -> entity.setAssignmentId(changes.getAssignmentId());
                case "studentId" -> entity.setStudentId(changes.getStudentId());
                case "content" -> entity.setContent(changes.getContent());
                case "submittedAt" -> entity.setSubmittedAt(changes.getSubmittedAt());
                case "marksObtained" -> entity.setMarksObtained(WireValues.decimal(changes.getMarksObtained()));
                case "gradedByUserId" -> entity.setGradedByUserId(changes.getGradedByUserId());
                case "gradedOn" -> entity.setGradedOn(changes.getGradedOn());
                case "feedback" -> entity.setFeedback(changes.getFeedback());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(AssignmentSubmissionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getAssignmentId() == null) {
            errors.add(new FieldError("assignmentId", "must not be null", "NotNull"));
        }
        if (entity.getStudentId() == null) {
            errors.add(new FieldError("studentId", "must not be null", "NotNull"));
        }
        if (entity.getSubmittedAt() == null) {
            errors.add(new FieldError("submittedAt", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("marksObtained", WireValues.decimal(entity.getMarksObtained()), 8, 2, errors);
        if (entity.getAssignmentId() != null && !repository.existsAssignmentId(entity.getAssignmentId())) {
            errors.add(new FieldError("assignmentId", "assignment does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(AssignmentSubmissionEntity entity) {
        if (repository.takenAssignmentIdStudentId1(entity)) {
            throw new DuplicateResourceException(
                    "A assignment submission with the same assignment id and student id already exists.");
        }
    }
}
