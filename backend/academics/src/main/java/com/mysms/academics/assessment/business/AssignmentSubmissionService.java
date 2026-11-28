package com.mysms.academics.assessment.business;

import com.mysms.academics.assessment.api.model.AcceptAssignmentSubmissionRequest;
import com.mysms.academics.assessment.api.model.AssignmentSubmission;
import com.mysms.academics.assessment.api.model.AssignmentSubmissionUpdate;
import com.mysms.academics.assessment.api.model.GradeAssignmentSubmissionRequest;
import com.mysms.academics.assessment.api.model.RejectAssignmentSubmissionRequest;
import com.mysms.academics.assessment.api.model.RequestSubmissionChangesRequest;
import com.mysms.academics.assessment.api.model.ResetAssignmentSubmissionRequest;
import com.mysms.academics.assessment.api.model.SubmitAssignmentSubmissionRequest;
import com.mysms.academics.assessment.config.AssessmentDatabaseConfig;
import com.mysms.academics.assessment.config.AssessmentEventsConfig;
import com.mysms.academics.assessment.domain.AssignmentSubmissionEntity;
import com.mysms.academics.assessment.events.AssessmentEvents;
import com.mysms.academics.assessment.mapper.AssignmentSubmissionMapper;
import com.mysms.academics.assessment.repository.AssignmentSubmissionRepository;
import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
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
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
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
 * Business service of assessment.assignment_submission: audit and domain event commit in the same transaction as every
 * change. <p>A submission is a student's personal work. Staff (holding one of {@link #REVIEW_PERMISSIONS} in the
 * assignment's institution) work on every submission there; anyone else reaches only their own students' submissions,
 * as sis confirms with the caller's token ({@link StudentOwnershipLookup}): another student's is 404, creating one in
 * another student's name 403. sis is asked outside any transaction and the write re-checks its row. <p>Grading and the
 * submission time are written only by the workflow operations; the standard create and update take the assignment and
 * the content.
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
    static final String PERMISSION_REQUEST_CHANGES = "assessment.assignment_submission.request_submission_changes";
    static final String PERMISSION_RESET_SUBMISSION = "assessment.assignment_submission.reset_assignment_submission";
    static final String PERMISSION_GRADE_SUBMISSION = "assessment.assignment_submission.grade_assignment_submission";
    /** The permissions only staff hold: where the caller holds one, every submission of the institution is theirs. */
    static final String[] REVIEW_PERMISSIONS = {PERMISSION_GRADE_SUBMISSION, PERMISSION_ACCEPT_ASSIGNMENT_SUBMISSION,
        PERMISSION_REJECT_ASSIGNMENT_SUBMISSION, PERMISSION_REQUEST_CHANGES};
    /** The states in which a student may still change their own submission. */
    static final Set<String> OWNER_EDITABLE = Set.of("draft", "change_requested");

    /** A row as read before sis is asked: the submission and the institution it belongs to. */
    private record Visible(AssignmentSubmissionEntity row, Optional<UUID> institution) {
    }

    private final AssignmentSubmissionRepository repository;
    private final AssignmentSubmissionMapper mapper;
    private final StudentOwnershipLookup ownership;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public AssignmentSubmissionService(AssignmentSubmissionRepository repository, AssignmentSubmissionMapper mapper,
            StudentOwnershipLookup ownership,
            @Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AssessmentDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AssessmentEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AssessmentEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.ownership = ownership;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public AssignmentSubmission getAssignmentSubmission(UUID id) {
        DataScope scope = DataScope.current(PERMISSION_GET);
        Visible visible = doInReadTransaction(() -> loadVisible(scope, id));
        requireOwnOrStaff(visible);
        return mapper.toDto(visible.row());
    }

    /** Staff read every submission in their reach; anyone else the submissions of their own students only. */
    public Page<AssignmentSubmission> listAssignmentSubmissions(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        AuthContext caller = AuthContext.require();
        SortSpec sort = SortSpec.parse(sortBy, AssignmentSubmissionRepository.SORT_FIELDS,
                AssignmentSubmissionRepository.DEFAULT_ORDER, AssignmentSubmissionRepository.ID_COLUMN);
        DataScope scope = DataScope.current(PERMISSION_LIST);
        DataScope reach = StaffReach.of(caller, REVIEW_PERMISSIONS);
        SqlFragment where = FilterParser.parse(filter, AssignmentSubmissionRepository.FILTER_FIELDS)
                .and(AssignmentSubmissionRepository.scope(scope));
        if (!StaffReach.covers(reach, scope)) {
            where = where.and(AssignmentSubmissionRepository.reachableOrOwn(reach, ownership.ownStudents(caller)));
        }
        SqlFragment filtered = where;
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> repository.list(page, filtered).map(mapper::toDto));
    }

    /**
     * Creates the submission at the current time; with validateOnly every check runs and nothing is written. Outside
     * the caller's staff reach the student must be the caller's own.
     */
    public AssignmentSubmission createAssignmentSubmission(AssignmentSubmission request, boolean validateOnly) {
        validator.validate(request);
        AssignmentSubmissionEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        entity.setSubmittedAt(now());
        AuthContext caller = AuthContext.require();
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        Optional<UUID> institution = doInReadTransaction(() -> institutionOf(entity));
        if (institution.isPresent()) {
            scope.requireWritable(institution);
            if (!StaffReach.of(caller, REVIEW_PERMISSIONS).canRead(institution) && entity.getStudentId() != null
                    && ownership.relationTo(caller, entity.getStudentId()).isEmpty()) {
                throw new PermissionDeniedException("A submission can only be made for the caller's own student.");
            }
        }
        return doInWriteTransaction(() -> {
            assertValid(entity);
            requireSameInstitution(institution, institutionOf(entity));
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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope. A student changes their own
     * submission only while it is a draft or returned for changes (409 otherwise).
     */
    public AssignmentSubmission updateAssignmentSubmission(UUID id, String ifMatch, AssignmentSubmissionUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        DataScope scope = DataScope.current(PERMISSION_UPDATE);
        Visible before = doInReadTransaction(() -> loadVisible(scope, id));
        boolean owner = requireOwnOrStaff(before);
        return doInWriteTransaction(() -> {
            Visible visible = loadVisible(scope, id);
            requireSameInstitution(before.institution(), visible.institution());
            AssignmentSubmissionEntity entity = visible.row();
            scope.requireWritable(visible.institution());
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            requireOwnerEditable(owner, entity);
            List<String> fields = apply(entity, changes);
            validator.validate(mapper.toDto(entity));
            assertValid(entity);
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
            outbox.publish(AGGREGATE + AssessmentEvents.UPDATED, AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; a student deletes their own submission only while they may still change it. */
    public void deleteAssignmentSubmission(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        DataScope scope = DataScope.current(PERMISSION_DELETE);
        Visible before = doInReadTransaction(() -> loadVisible(scope, id));
        boolean owner = requireOwnOrStaff(before);
        runInWriteTransaction(() -> {
            Visible visible = loadVisible(scope, id);
            requireSameInstitution(before.institution(), visible.institution());
            AssignmentSubmissionEntity entity = visible.row();
            scope.requireWritable(visible.institution());
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            requireOwnerEditable(owner, entity);
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + AssessmentEvents.DELETED, AGGREGATE, id, AssessmentEvents.payload(id));
        });
    }

    /** assessment.SubmitAssignmentSubmission: the student (or staff) submits a draft or a returned submission. */
    public AssignmentSubmission submitAssignmentSubmission(UUID id, SubmitAssignmentSubmissionRequest request) {
        return transition(id, PERMISSION_SUBMIT_ASSIGNMENT_SUBMISSION, true, request == null ? null : request.getEtag(),
                OWNER_EDITABLE, "submit", List.of("status", "submittedAt"), null,
                (row, now, actor) -> repository.submitAssignmentSubmission(id, row.getRowVersion(), now, actor));
    }

    /** assessment.AcceptAssignmentSubmission: staff accept a submitted item. */
    public AssignmentSubmission acceptAssignmentSubmission(UUID id, AcceptAssignmentSubmissionRequest request) {
        return transition(id, PERMISSION_ACCEPT_ASSIGNMENT_SUBMISSION, false, request == null ? null : request.getEtag(),
                Set.of("submitted"), "accept", List.of("status"), null,
                (row, now, actor) -> repository.acceptAssignmentSubmission(id, row.getRowVersion(), now, actor));
    }

    /** assessment.RejectAssignmentSubmission: staff reject a submitted item; the reason is kept in the audit. */
    public AssignmentSubmission rejectAssignmentSubmission(UUID id, RejectAssignmentSubmissionRequest request) {
        return transition(id, PERMISSION_REJECT_ASSIGNMENT_SUBMISSION, false, request == null ? null : request.getEtag(),
                Set.of("submitted"), "reject", List.of("status"), request == null ? null : request.getReason(),
                (row, now, actor) -> repository.rejectAssignmentSubmission(id, row.getRowVersion(), now, actor));
    }

    /** Request corrections from a submitted student work item; the reason is retained in feedback. */
    public AssignmentSubmission requestSubmissionChanges(UUID id, RequestSubmissionChangesRequest request) {
        String reason = request == null ? null : trimmed(request.getReason());
        return transition(id, PERMISSION_REQUEST_CHANGES, false, request == null ? null : request.getEtag(),
                Set.of("submitted"), "requestChanges", List.of("status", "feedback"), reason,
                (row, now, actor) -> repository.requestChanges(id, row.getRowVersion(), reason, now, actor));
    }

    /** Reset a returned/rejected submission to editable draft while preserving its last submission time and content. */
    public AssignmentSubmission resetAssignmentSubmission(UUID id, ResetAssignmentSubmissionRequest request) {
        return transition(id, PERMISSION_RESET_SUBMISSION, true, request == null ? null : request.getEtag(),
                Set.of("rejected", "change_requested"), "resetToDraft",
                List.of("status", "marksObtained", "gradedByUserId", "gradedOn"), null,
                (row, now, actor) -> repository.resetToDraft(id, row.getRowVersion(), now, actor));
    }

    /** Record marks and grading metadata for a submitted or accepted item. */
    public AssignmentSubmission gradeAssignmentSubmission(UUID id, GradeAssignmentSubmissionRequest request) {
        if (request == null || request.getMarks() == null) {
            throw new ValidationFailedException(List.of(new FieldError("marks", "must not be null", "NotNull")));
        }
        BigDecimal marks = WireValues.decimal(request.getMarks());
        List<FieldError> errors = new ArrayList<>();
        WireValues.checkDecimal("marks", request.getMarks(), 8, 2, errors);
        if (marks == null || marks.signum() < 0) {
            errors.add(new FieldError("marks", "must be greater than or equal to 0", "PositiveOrZero"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        return transition(id, PERMISSION_GRADE_SUBMISSION, false, request.getEtag(), Set.of("submitted", "accepted"),
                "grade", List.of("status", "marksObtained", "gradedByUserId", "gradedOn", "feedback"), null,
                (row, now, actor) -> {
                    BigDecimal maximum = repository.assignmentMaxMarks(row.getAssignmentId())
                            .orElseThrow(() -> new ResourceNotFoundException("assessment.assignment",
                                    row.getAssignmentId()));
                    if (marks.compareTo(maximum) > 0) {
                        throw new ValidationFailedException(List.of(new FieldError("marks",
                                "must not exceed the assignment maximum marks", "MAX")));
                    }
                    return repository.grade(id, row.getRowVersion(), marks, actor,
                            LocalDate.now(clock), trimmed(request.getFeedback()), now, actor);
                });
    }

    /**
     * One workflow step: visible under the step's permission (404), the caller's own student's submission when the
     * step is open to students and the caller is not staff there (404 otherwise; sis is asked before the write), the
     * etag (412), the state (409), then the guarded update with its audit record and event.
     */
    private AssignmentSubmission transition(UUID id, String permission, boolean openToStudents, String etag,
            Set<String> allowedStatuses, String action, List<String> changedFields, String reason,
            SubmissionTransition transition) {
        DataScope scope = DataScope.current(permission);
        Optional<Visible> before = Optional.empty();
        if (openToStudents) {
            Visible visible = doInReadTransaction(() -> loadVisible(scope, id));
            requireOwnOrStaff(visible);
            before = Optional.of(visible);
        }
        Optional<Visible> checked = before;
        return doInWriteTransaction(() -> {
            Visible visible = loadVisible(scope, id);
            checked.ifPresent(read -> requireSameInstitution(read.institution(), visible.institution()));
            AssignmentSubmissionEntity entity = visible.row();
            scope.requireWritable(visible.institution());
            OptionalInt expected = Etags.bodyVersion(etag);
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!allowedStatuses.contains(entity.getStatus())) {
                throw new InvalidStateException(
                        "The assignment submission cannot be changed this way in its current state.");
            }
            if (transition.apply(entity, now(), Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, action, changedFields, reason);
            outbox.publish(AGGREGATE + "." + action, AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * Staff (the institution in their reach) pass; anyone else must own the submission's student, or the submission
     * does not exist for them (404). Answers whether the caller acts as the student's owner rather than as staff.
     */
    private boolean requireOwnOrStaff(Visible visible) {
        AuthContext caller = AuthContext.require();
        if (StaffReach.of(caller, REVIEW_PERMISSIONS).canRead(visible.institution())) {
            return false;
        }
        if (ownership.relationTo(caller, visible.row().getStudentId()).isEmpty()) {
            throw new ResourceNotFoundException(AGGREGATE, visible.row().getId());
        }
        return true;
    }

    private static void requireOwnerEditable(boolean owner, AssignmentSubmissionEntity entity) {
        if (owner && !OWNER_EDITABLE.contains(entity.getStatus())) {
            throw new InvalidStateException(
                    "A submission can be changed by its student only while it is a draft or returned for changes.");
        }
    }

    /** The row moved to another institution between the check with sis and the write: the check no longer holds. */
    private static void requireSameInstitution(Optional<UUID> checked, Optional<UUID> current) {
        if (!checked.equals(current)) {
            throw new PreconditionFailedException("The assignment changed meanwhile; try again.");
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    private static String trimmed(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @FunctionalInterface
    private interface SubmissionTransition {
        int apply(AssignmentSubmissionEntity row, OffsetDateTime now, UUID actor);
    }

    /** The live row, if the caller's scope shows it (404 otherwise), with the institution it belongs to. */
    private Visible loadVisible(DataScope scope, UUID id) {
        AssignmentSubmissionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        Optional<UUID> institution = institutionOf(entity);
        if (!scope.canRead(institution)) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return new Visible(entity, institution);
    }

    /** The institution of the assessment.assignment the row belongs to. */
    private Optional<UUID> institutionOf(AssignmentSubmissionEntity entity) {
        return repository.institutionOf(entity.getAssignmentId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(AssignmentSubmissionEntity entity, AssignmentSubmissionUpdate changes) {
        Patches patch = new Patches();
        patch.apply("assignmentId", changes.getAssignmentId(), entity::setAssignmentId);
        patch.apply("content", changes.getContent(), entity::setContent);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(AssignmentSubmissionEntity entity) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getAssignmentId() == null) {
            errors.add(new FieldError("assignmentId", "must not be null", "NotNull"));
        }
        if (entity.getStudentId() == null) {
            errors.add(new FieldError("studentId", "must not be null", "NotNull"));
        }
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
