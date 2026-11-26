package com.mysms.academics.assessment.business;

import com.mysms.academics.assessment.api.model.HomeworkSubmission;
import com.mysms.academics.assessment.api.model.HomeworkSubmissionUpdate;
import com.mysms.academics.assessment.config.AssessmentDatabaseConfig;
import com.mysms.academics.assessment.config.AssessmentEventsConfig;
import com.mysms.academics.assessment.domain.HomeworkSubmissionEntity;
import com.mysms.academics.assessment.events.AssessmentEvents;
import com.mysms.academics.assessment.mapper.HomeworkSubmissionMapper;
import com.mysms.academics.assessment.repository.HomeworkSubmissionRepository;
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
 * Business service of assessment.homework_submission: audit and domain event commit in the same transaction as every
 * change. <p>Staff (holding {@link #PERMISSION_CHECK} in the homework's institution) work on every submission there;
 * anyone else reaches only their own students' submissions, as sis confirms with the caller's token ({@link
 * StudentOwnershipLookup}): another student's is 404, creating one in another student's name 403, and they change their
 * own only until it is checked (409). sis is asked outside any transaction and the write re-checks its row. The check
 * and the reply are written only by {@link HomeworkWorkflowService}.
 */
@Service
public class HomeworkSubmissionService extends AppTransactionManager {

    public static final String AGGREGATE = HomeworkSubmissionEntity.TABLE;
    static final String PERMISSION_LIST = "assessment.homework_submission.read";
    static final String PERMISSION_GET = "assessment.homework_submission.read";
    static final String PERMISSION_CREATE = "assessment.homework_submission.create";
    static final String PERMISSION_UPDATE = "assessment.homework_submission.update";
    static final String PERMISSION_DELETE = "assessment.homework_submission.delete";
    /** The permission only staff hold: where the caller holds it, every submission of the institution is theirs. */
    static final String PERMISSION_CHECK = "assessment.homework_submission.check_homework_submission";
    /** The state in which a student may still change their own submission. */
    static final String OWNER_EDITABLE = "unchecked";
    /**
     * The contract's HomeworkSubmissionUpdate fields, in apply order. The student is not among them (a submission
     * stays the work of the student it was made for), nor the check and the reply: only the workflow writes those.
     */

    /** A row as read before sis is asked: the submission and the institution it belongs to. */
    private record Visible(HomeworkSubmissionEntity row, Optional<UUID> institution) {
    }

    private final HomeworkSubmissionRepository repository;
    private final HomeworkSubmissionMapper mapper;
    private final StudentOwnershipLookup ownership;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public HomeworkSubmissionService(HomeworkSubmissionRepository repository, HomeworkSubmissionMapper mapper,
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

    public HomeworkSubmission getHomeworkSubmission(UUID id) {
        DataScope scope = DataScope.current(PERMISSION_GET);
        Visible visible = doInReadTransaction(() -> loadVisible(scope, id));
        requireOwnOrStaff(visible);
        return mapper.toDto(visible.row());
    }

    /** Staff read every submission in their reach; anyone else the submissions of their own students only. */
    public Page<HomeworkSubmission> listHomeworkSubmissions(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        AuthContext caller = AuthContext.require();
        SortSpec sort = SortSpec.parse(sortBy, HomeworkSubmissionRepository.SORT_FIELDS,
                HomeworkSubmissionRepository.DEFAULT_ORDER, HomeworkSubmissionRepository.ID_COLUMN);
        DataScope scope = DataScope.current(PERMISSION_LIST);
        DataScope reach = StaffReach.of(caller, PERMISSION_CHECK);
        SqlFragment where = FilterParser.parse(filter, HomeworkSubmissionRepository.FILTER_FIELDS)
                .and(HomeworkSubmissionRepository.scope(scope));
        if (!StaffReach.covers(reach, scope)) {
            where = where.and(HomeworkSubmissionRepository.reachableOrOwn(reach, ownership.ownStudents(caller)));
        }
        SqlFragment filtered = where;
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> repository.list(page, filtered).map(mapper::toDto));
    }

    /**
     * Creates the row; with validateOnly every check runs and nothing is written. Outside the caller's staff reach
     * the student must be the caller's own (403).
     */
    public HomeworkSubmission createHomeworkSubmission(HomeworkSubmission request, boolean validateOnly) {
        validator.validate(request);
        HomeworkSubmissionEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        AuthContext caller = AuthContext.require();
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        Optional<UUID> institution = doInReadTransaction(() -> institutionOf(entity));
        if (institution.isPresent()) {
            scope.requireWritable(institution);
            if (!StaffReach.of(caller, PERMISSION_CHECK).canRead(institution) && entity.getStudentId() != null
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
            HomeworkSubmissionEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + AssessmentEvents.CREATED, AGGREGATE, stored.getId(),
                    AssessmentEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public HomeworkSubmission updateHomeworkSubmission(UUID id, String ifMatch, HomeworkSubmissionUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        DataScope scope = DataScope.current(PERMISSION_UPDATE);
        Visible before = doInReadTransaction(() -> loadVisible(scope, id));
        boolean owner = requireOwnOrStaff(before);
        return doInWriteTransaction(() -> {
            Visible visible = loadVisible(scope, id);
            requireSameInstitution(before.institution(), visible.institution());
            HomeworkSubmissionEntity entity = visible.row();
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

    /** Soft delete under If-Match; a student deletes their own submission only until it is checked. */
    public void deleteHomeworkSubmission(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        DataScope scope = DataScope.current(PERMISSION_DELETE);
        Visible before = doInReadTransaction(() -> loadVisible(scope, id));
        boolean owner = requireOwnOrStaff(before);
        runInWriteTransaction(() -> {
            Visible visible = loadVisible(scope, id);
            requireSameInstitution(before.institution(), visible.institution());
            HomeworkSubmissionEntity entity = visible.row();
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

    /**
     * Staff (the institution in their reach) pass; anyone else must own the submission's student, or the submission
     * does not exist for them (404). Answers whether the caller acts as the student's owner rather than as staff.
     */
    private boolean requireOwnOrStaff(Visible visible) {
        AuthContext caller = AuthContext.require();
        if (StaffReach.of(caller, PERMISSION_CHECK).canRead(visible.institution())) {
            return false;
        }
        if (ownership.relationTo(caller, visible.row().getStudentId()).isEmpty()) {
            throw new ResourceNotFoundException(AGGREGATE, visible.row().getId());
        }
        return true;
    }

    private static void requireOwnerEditable(boolean owner, HomeworkSubmissionEntity entity) {
        if (owner && !OWNER_EDITABLE.equals(entity.getStatus())) {
            throw new InvalidStateException("A submission can be changed by its student only until it is checked.");
        }
    }

    /** The row moved to another institution between the check with sis and the write: the check no longer holds. */
    private static void requireSameInstitution(Optional<UUID> checked, Optional<UUID> current) {
        if (!checked.equals(current)) {
            throw new PreconditionFailedException("The homework changed meanwhile; try again.");
        }
    }

    /** The live row, if the caller's scope shows it (404 otherwise), with the institution it belongs to. */
    private Visible loadVisible(DataScope scope, UUID id) {
        HomeworkSubmissionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        Optional<UUID> institution = institutionOf(entity);
        if (!scope.canRead(institution)) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return new Visible(entity, institution);
    }

    /** The institution of the assessment.homework the row belongs to. */
    private Optional<UUID> institutionOf(HomeworkSubmissionEntity entity) {
        return repository.institutionOf(entity.getHomeworkId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(HomeworkSubmissionEntity entity, HomeworkSubmissionUpdate changes) {
        Patches patch = new Patches();
        patch.apply("homeworkId", changes.getHomeworkId(), entity::setHomeworkId);
        patch.apply("submittedOn", changes.getSubmittedOn(), entity::setSubmittedOn);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(HomeworkSubmissionEntity entity) {
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
