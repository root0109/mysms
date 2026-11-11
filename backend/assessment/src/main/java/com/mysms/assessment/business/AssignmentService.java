package com.mysms.assessment.business;

import com.mysms.assessment.api.model.Assignment;
import com.mysms.assessment.api.model.AssignmentAllocation;
import com.mysms.assessment.api.model.AssignmentAllocationUpdate;
import com.mysms.assessment.api.model.AssignmentApproval;
import com.mysms.assessment.api.model.AssignmentApprovalUpdate;
import com.mysms.assessment.api.model.AssignmentSubject;
import com.mysms.assessment.api.model.AssignmentSubjectUpdate;
import com.mysms.assessment.api.model.AssignmentUpdate;
import com.mysms.assessment.api.model.ApproveAssignmentRequest;
import com.mysms.assessment.api.model.CancelAssignmentRequest;
import com.mysms.assessment.api.model.FinishAssignmentRequest;
import com.mysms.assessment.api.model.PublishAssignmentRequest;
import com.mysms.assessment.api.model.RejectAssignmentRequest;
import com.mysms.assessment.api.model.ResetAssignmentRequest;
import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.config.AssessmentEventsConfig;
import com.mysms.assessment.domain.AssignmentAllocationEntity;
import com.mysms.assessment.domain.AssignmentApprovalEntity;
import com.mysms.assessment.domain.AssignmentEntity;
import com.mysms.assessment.domain.AssignmentSubjectEntity;
import com.mysms.assessment.events.AssessmentEvents;
import com.mysms.assessment.mapper.AssignmentAllocationMapper;
import com.mysms.assessment.mapper.AssignmentApprovalMapper;
import com.mysms.assessment.mapper.AssignmentMapper;
import com.mysms.assessment.mapper.AssignmentSubjectMapper;
import com.mysms.assessment.repository.AssignmentAllocationRepository;
import com.mysms.assessment.repository.AssignmentApprovalRepository;
import com.mysms.assessment.repository.AssignmentRepository;
import com.mysms.assessment.repository.AssignmentSubjectRepository;
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
import com.mysms.assessment.domain.AssignmentApprovalEntity;
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
 * Business service of assessment.assignment (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a Assignment
 * are served here too — AssignmentAllocation, AssignmentApproval, AssignmentSubject.
 */
@Service
public class AssignmentService extends AppTransactionManager {

    public static final String AGGREGATE = AssignmentEntity.TABLE;
    static final String PERMISSION_LIST = "assessment.assignment.read";
    static final String PERMISSION_GET = "assessment.assignment.read";
    static final String PERMISSION_CREATE = "assessment.assignment.create";
    static final String PERMISSION_UPDATE = "assessment.assignment.update";
    static final String PERMISSION_DELETE = "assessment.assignment.delete";
    static final String PERMISSION_PUBLISH_ASSIGNMENT = "assessment.assignment.publish_assignment";
    static final String PERMISSION_CANCEL_ASSIGNMENT = "assessment.assignment.cancel_assignment";
    static final String PERMISSION_RESET_ASSIGNMENT = "assessment.assignment.reset_assignment";
    static final String PERMISSION_APPROVE_ASSIGNMENT = "assessment.assignment.approve_assignment";
    static final String PERMISSION_REJECT_ASSIGNMENT = "assessment.assignment.reject_assignment";
    static final String PERMISSION_FINISH_ASSIGNMENT = "assessment.assignment.finish_assignment";
    /** The contract's AssignmentUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "academicYearId", "title", "description", "assignmentTypeId", "classSectionId",
                    "courseId",
            "batchId", "assignedByStaffId", "reviewerStaffId", "maxMarks", "issuedOn", "dueAt", "attachmentFileId");

    public static final String ASSIGNMENT_ALLOCATION_AGGREGATE = AssignmentAllocationEntity.TABLE;
    static final String ASSIGNMENT_ALLOCATION_PERMISSION_LIST = "assessment.assignment_allocation.read";
    static final String ASSIGNMENT_ALLOCATION_PERMISSION_GET = "assessment.assignment_allocation.read";
    static final String ASSIGNMENT_ALLOCATION_PERMISSION_CREATE = "assessment.assignment_allocation.create";
    static final String ASSIGNMENT_ALLOCATION_PERMISSION_UPDATE = "assessment.assignment_allocation.update";
    static final String ASSIGNMENT_ALLOCATION_PERMISSION_DELETE = "assessment.assignment_allocation.delete";
    /** The contract's AssignmentAllocationUpdate fields, in apply order. */
    static final List<String> ASSIGNMENT_ALLOCATION_WRITABLE = List.of("studentId");

    public static final String ASSIGNMENT_APPROVAL_AGGREGATE = AssignmentApprovalEntity.TABLE;
    static final String ASSIGNMENT_APPROVAL_PERMISSION_LIST = "assessment.assignment_approval.read";
    static final String ASSIGNMENT_APPROVAL_PERMISSION_GET = "assessment.assignment_approval.read";
    static final String ASSIGNMENT_APPROVAL_PERMISSION_CREATE = "assessment.assignment_approval.create";
    static final String ASSIGNMENT_APPROVAL_PERMISSION_UPDATE = "assessment.assignment_approval.update";
    static final String ASSIGNMENT_APPROVAL_PERMISSION_DELETE = "assessment.assignment_approval.delete";
    /** The contract's AssignmentApprovalUpdate fields, in apply order. */
    static final List<String> ASSIGNMENT_APPROVAL_WRITABLE = List.of("decision", "comments", "decidedByUserId", "decidedAt");

    public static final String ASSIGNMENT_SUBJECT_AGGREGATE = AssignmentSubjectEntity.TABLE;
    static final String ASSIGNMENT_SUBJECT_PERMISSION_LIST = "assessment.assignment_subject.read";
    static final String ASSIGNMENT_SUBJECT_PERMISSION_GET = "assessment.assignment_subject.read";
    static final String ASSIGNMENT_SUBJECT_PERMISSION_CREATE = "assessment.assignment_subject.create";
    static final String ASSIGNMENT_SUBJECT_PERMISSION_UPDATE = "assessment.assignment_subject.update";
    static final String ASSIGNMENT_SUBJECT_PERMISSION_DELETE = "assessment.assignment_subject.delete";
    /** The contract's AssignmentSubjectUpdate fields, in apply order. */
    static final List<String> ASSIGNMENT_SUBJECT_WRITABLE = List.of("subjectId");

    private final AssignmentRepository repository;
    private final AssignmentMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final AssignmentAllocationRepository assignmentAllocationRepository;
    private final AssignmentAllocationMapper assignmentAllocationMapper;
    private final AssignmentApprovalRepository assignmentApprovalRepository;
    private final AssignmentApprovalMapper assignmentApprovalMapper;
    private final AssignmentSubjectRepository assignmentSubjectRepository;
    private final AssignmentSubjectMapper assignmentSubjectMapper;

    public AssignmentService(AssignmentRepository repository,
            AssignmentMapper mapper,
            @Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AssessmentDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AssessmentEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AssessmentEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            AssignmentAllocationRepository assignmentAllocationRepository,
            AssignmentAllocationMapper assignmentAllocationMapper,
            AssignmentApprovalRepository assignmentApprovalRepository,
            AssignmentApprovalMapper assignmentApprovalMapper,
            AssignmentSubjectRepository assignmentSubjectRepository,
            AssignmentSubjectMapper assignmentSubjectMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.assignmentAllocationRepository = assignmentAllocationRepository;
        this.assignmentAllocationMapper = assignmentAllocationMapper;
        this.assignmentApprovalRepository = assignmentApprovalRepository;
        this.assignmentApprovalMapper = assignmentApprovalMapper;
        this.assignmentSubjectRepository = assignmentSubjectRepository;
        this.assignmentSubjectMapper = assignmentSubjectMapper;
    }

    public Assignment getAssignment(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Assignment> listAssignments(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, AssignmentRepository.SORT_FIELDS, AssignmentRepository.DEFAULT_ORDER,
                AssignmentRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, AssignmentRepository.FILTER_FIELDS)
                .and(AssignmentRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<AssignmentEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Assignment createAssignment(Assignment request, boolean validateOnly) {
        validator.validate(request);
        AssignmentEntity entity = mapper.toEntity(request);
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
            AssignmentEntity stored = repository.insert(entity);
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
    public Assignment updateAssignment(UUID id, String ifMatch, AssignmentUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            AssignmentEntity entity = loadVisible(scope, id);
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
    public void deleteAssignment(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            AssignmentEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByAssignmentAllocationAssignmentId(id)) {
                throw new InvalidStateException(
                        "The assignment is still referenced by assessment.assignment_allocation rows; remove those "
                                + "first.");
            }
            if (repository.referencedByAssignmentApprovalAssignmentId(id)) {
                throw new InvalidStateException(
                        "The assignment is still referenced by assessment.assignment_approval rows; remove those "
                                + "first.");
            }
            if (repository.referencedByAssignmentSubjectAssignmentId(id)) {
                throw new InvalidStateException(
                        "The assignment is still referenced by assessment.assignment_subject rows; remove those "
                                + "first.");
            }
            if (repository.referencedByAssignmentSubmissionAssignmentId(id)) {
                throw new InvalidStateException(
                        "The assignment is still referenced by assessment.assignment_submission rows; remove those "
                                + "first.");
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
     * assessment.PublishAssignment: Publish (status published; becomes ongoing automatically at issued_on). The row
     * must be visible and writable for the caller, and the step is allowed while status is draft; in any other state
     * the answer is 409 and nothing changes.
     */
    public Assignment publishAssignment(UUID id, PublishAssignmentRequest request) {
        DataScope scope = DataScope.current(PERMISSION_PUBLISH_ASSIGNMENT);
        return doInWriteTransaction(() -> {
            AssignmentEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("draft").contains(entity.getStatus()))) {
                throw new InvalidStateException("The assignment cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.publishAssignment(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "publish", List.of("status"), null);
            outbox.publish(AGGREGATE + ".publish", AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * assessment.ApproveAssignment: approve the newest pending review and publish its draft in one transaction.
     * The assignment's own scope and version protect both records; an absent pending review is a state conflict.
     */
    public Assignment approveAssignment(UUID id, ApproveAssignmentRequest request) {
        DataScope scope = DataScope.current(PERMISSION_APPROVE_ASSIGNMENT);
        return doInWriteTransaction(() -> {
            AssignmentEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            Optional<UUID> institution = institutionOf(entity);
            if (!scope.canRead(institution)) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institution);
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!"draft".equals(entity.getStatus())) {
                throw new InvalidStateException("The assignment cannot be approved in its current state.");
            }
            AssignmentApprovalEntity approval = assignmentApprovalRepository.findPendingForAssignment(id)
                    .orElseThrow(() -> new InvalidStateException("The assignment has no pending approval."));
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            approval.setDecision("approved");
            approval.setComments(request == null ? null : trimmed(request.getReason()));
            approval.setDecidedByUserId(Actor.current().orElse(null));
            approval.setDecidedAt(now);
            approval.onUpdatingData(Actor.current(), clock);
            if (assignmentApprovalRepository.update(approval, approval.getRowVersion()) == 0) {
                throw new PreconditionFailedException("The pending approval changed meanwhile; read it again.");
            }
            if (repository.publishAssignment(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The assignment changed meanwhile; read it again.");
            }
            audit.recordTransition(ASSIGNMENT_APPROVAL_AGGREGATE, approval.getId(), "approve",
                    List.of("decision", "comments", "decidedByUserId", "decidedAt"), request == null ? null
                            : request.getReason());
            audit.recordTransition(AGGREGATE, id, "approve", List.of("status"), request == null ? null
                    : request.getReason());
            outbox.publish(ASSIGNMENT_APPROVAL_AGGREGATE + ".approve", ASSIGNMENT_APPROVAL_AGGREGATE,
                    approval.getId(), AssessmentEvents.payload(approval.getId()));
            outbox.publish(AGGREGATE + ".approve", AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * assessment.RejectAssignment: reject the newest pending review while keeping the draft available to its author.
     */
    public Assignment rejectAssignment(UUID id, RejectAssignmentRequest request) {
        DataScope scope = DataScope.current(PERMISSION_REJECT_ASSIGNMENT);
        return doInWriteTransaction(() -> {
            AssignmentEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            Optional<UUID> institution = institutionOf(entity);
            if (!scope.canRead(institution)) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institution);
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!"draft".equals(entity.getStatus())) {
                throw new InvalidStateException("The assignment cannot be rejected in its current state.");
            }
            AssignmentApprovalEntity approval = assignmentApprovalRepository.findPendingForAssignment(id)
                    .orElseThrow(() -> new InvalidStateException("The assignment has no pending approval."));
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            approval.setDecision("rejected");
            approval.setComments(request == null ? null : trimmed(request.getReason()));
            approval.setDecidedByUserId(Actor.current().orElse(null));
            approval.setDecidedAt(now);
            approval.onUpdatingData(Actor.current(), clock);
            if (assignmentApprovalRepository.update(approval, approval.getRowVersion()) == 0) {
                throw new PreconditionFailedException("The pending approval changed meanwhile; read it again.");
            }
            audit.recordTransition(ASSIGNMENT_APPROVAL_AGGREGATE, approval.getId(), "reject",
                    List.of("decision", "comments", "decidedByUserId", "decidedAt"), request == null ? null
                            : request.getReason());
            outbox.publish(ASSIGNMENT_APPROVAL_AGGREGATE + ".reject", ASSIGNMENT_APPROVAL_AGGREGATE,
                    approval.getId(), AssessmentEvents.payload(approval.getId()));
            outbox.publish(AGGREGATE + ".reject", AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** assessment.FinishAssignment: close a published or ongoing assignment to further submissions. */
    public Assignment finishAssignment(UUID id, FinishAssignmentRequest request) {
        DataScope scope = DataScope.current(PERMISSION_FINISH_ASSIGNMENT);
        return doInWriteTransaction(() -> {
            AssignmentEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            Optional<UUID> institution = institutionOf(entity);
            if (!scope.canRead(institution)) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institution);
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!Set.of("published", "ongoing").contains(entity.getStatus())) {
                throw new InvalidStateException("The assignment cannot be finished in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.finishAssignment(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "finish", List.of("status"), null);
            outbox.publish(AGGREGATE + ".finish", AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    private static String trimmed(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * assessment.CancelAssignment: Cancel. The row must be visible and writable for the caller, and the step is allowed
     * while status is draft or published or ongoing; in any other state
     * the answer is 409 and nothing changes.
     */
    public Assignment cancelAssignment(UUID id, CancelAssignmentRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CANCEL_ASSIGNMENT);
        return doInWriteTransaction(() -> {
            AssignmentEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("draft", "published", "ongoing").contains(entity.getStatus()))) {
                throw new InvalidStateException("The assignment cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.cancelAssignment(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "cancel", List.of("status"), request == null ? null
                    : request.getReason());
            outbox.publish(AGGREGATE + ".cancel", AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * assessment.ResetAssignment: Back to draft. The row must be visible and writable for the caller, and the step is
     * allowed while status is cancelled; in any other state
     * the answer is 409 and nothing changes.
     */
    public Assignment resetAssignment(UUID id, ResetAssignmentRequest request) {
        DataScope scope = DataScope.current(PERMISSION_RESET_ASSIGNMENT);
        return doInWriteTransaction(() -> {
            AssignmentEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("cancelled").contains(entity.getStatus()))) {
                throw new InvalidStateException("The assignment cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.resetAssignment(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "reset", List.of("status"), null);
            outbox.publish(AGGREGATE + ".reset", AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private AssignmentEntity loadVisible(DataScope scope, UUID id) {
        AssignmentEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(AssignmentEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected AssignmentUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(AssignmentEntity entity, AssignmentUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "academicYearId" -> entity.setAcademicYearId(changes.getAcademicYearId());
                case "title" -> entity.setTitle(changes.getTitle());
                case "description" -> entity.setDescription(changes.getDescription());
                case "assignmentTypeId" -> entity.setAssignmentTypeId(changes.getAssignmentTypeId());
                case "classSectionId" -> entity.setClassSectionId(changes.getClassSectionId());
                case "courseId" -> entity.setCourseId(changes.getCourseId());
                case "batchId" -> entity.setBatchId(changes.getBatchId());
                case "assignedByStaffId" -> entity.setAssignedByStaffId(changes.getAssignedByStaffId());
                case "reviewerStaffId" -> entity.setReviewerStaffId(changes.getReviewerStaffId());
                case "maxMarks" -> entity.setMaxMarks(WireValues.decimal(changes.getMaxMarks()));
                case "issuedOn" -> entity.setIssuedOn(changes.getIssuedOn());
                case "dueAt" -> entity.setDueAt(changes.getDueAt());
                case "attachmentFileId" -> entity.setAttachmentFileId(changes.getAttachmentFileId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(AssignmentEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("maxMarks", WireValues.decimal(entity.getMaxMarks()), 8, 2, errors);
        if (entity.getAssignmentTypeId() != null && !repository.existsAssignmentTypeId(entity.getAssignmentTypeId())) {
            errors.add(new FieldError("assignmentTypeId", "assignment type does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(AssignmentEntity entity) {

    }

    // ---- AssignmentAllocation: a record of the Assignment aggregate ----

    public AssignmentAllocation getAssignmentAllocation(UUID assignmentId, UUID id) {
        return doInReadTransaction(() -> {
            return assignmentAllocationMapper.toDto(loadVisibleAssignmentAllocation(DataScope.current(
                    ASSIGNMENT_ALLOCATION_PERMISSION_GET), assignmentId, id));
        });
    }

    public Page<AssignmentAllocation> listAssignmentAllocations(UUID assignmentId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, AssignmentAllocationRepository.SORT_FIELDS,
                        AssignmentAllocationRepository.DEFAULT_ORDER,
                AssignmentAllocationRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, AssignmentAllocationRepository.FILTER_FIELDS)
                .and(AssignmentAllocationRepository.ofParent(assignmentId))
                        .and(AssignmentAllocationRepository.scope(DataScope.current(ASSIGNMENT_ALLOCATION_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + assignmentId, clock);
        return doInReadTransaction(() -> {
            Page<AssignmentAllocationEntity> rows = assignmentAllocationRepository.list(page, where);
            return rows.map(assignmentAllocationMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public AssignmentAllocation createAssignmentAllocation(UUID assignmentId, AssignmentAllocation request,
            boolean validateOnly) {
        validator.validate(request);
        AssignmentAllocationEntity entity = assignmentAllocationMapper.toEntity(request);
        entity.setAssignmentId(assignmentId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(ASSIGNMENT_ALLOCATION_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!assignmentAllocationRepository.existsAssignmentId(entity.getAssignmentId())) {
                throw new ResourceNotFoundException("assessment.assignment", entity.getAssignmentId());
            }
            if (!scope.canRead(institutionOfAssignmentAllocation(entity))) {
                throw new ResourceNotFoundException("assessment.assignment", entity.getAssignmentId());
            }
            assertValidAssignmentAllocation(entity, true);
            scope.requireWritable(institutionOfAssignmentAllocation(entity));
            assertUniqueAssignmentAllocation(entity);
            if (validateOnly) {
                return assignmentAllocationMapper.toDto(entity);
            }
            AssignmentAllocationEntity stored = assignmentAllocationRepository.insert(entity);
            audit.recordCreate(ASSIGNMENT_ALLOCATION_AGGREGATE, stored.getId());
            outbox.publish(ASSIGNMENT_ALLOCATION_AGGREGATE + AssessmentEvents.CREATED, ASSIGNMENT_ALLOCATION_AGGREGATE,
                    stored.getId(),
                    AssessmentEvents.payload(stored.getId()));
            return assignmentAllocationMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public AssignmentAllocation updateAssignmentAllocation(UUID assignmentId, UUID id, String ifMatch,
            AssignmentAllocationUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, ASSIGNMENT_ALLOCATION_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ASSIGNMENT_ALLOCATION_PERMISSION_UPDATE);
            AssignmentAllocationEntity entity = loadVisibleAssignmentAllocation(scope, assignmentId, id);
            scope.requireWritable(institutionOfAssignmentAllocation(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyAssignmentAllocation(entity, changes, fields);
            validator.validate(assignmentAllocationMapper.toDto(entity));
            assertValidAssignmentAllocation(entity, false);
            scope.requireWritable(institutionOfAssignmentAllocation(entity));
            assertUniqueAssignmentAllocation(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return assignmentAllocationMapper.toDto(entity);
            }
            if (assignmentAllocationRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(ASSIGNMENT_ALLOCATION_AGGREGATE, id, fields);
            outbox.publish(ASSIGNMENT_ALLOCATION_AGGREGATE + AssessmentEvents.UPDATED, ASSIGNMENT_ALLOCATION_AGGREGATE, id,
                    AssessmentEvents.payload(id));
            return assignmentAllocationMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteAssignmentAllocation(UUID assignmentId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ASSIGNMENT_ALLOCATION_PERMISSION_DELETE);
            AssignmentAllocationEntity entity = loadVisibleAssignmentAllocation(scope, assignmentId, id);
            scope.requireWritable(institutionOfAssignmentAllocation(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (assignmentAllocationRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(ASSIGNMENT_ALLOCATION_AGGREGATE, id);
            outbox.publish(ASSIGNMENT_ALLOCATION_AGGREGATE + AssessmentEvents.DELETED, ASSIGNMENT_ALLOCATION_AGGREGATE, id,
                    AssessmentEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private AssignmentAllocationEntity loadVisibleAssignmentAllocation(DataScope scope, UUID assignmentId, UUID id) {
        AssignmentAllocationEntity entity = assignmentAllocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ASSIGNMENT_ALLOCATION_AGGREGATE, id));
        if (!scope.canRead(institutionOfAssignmentAllocation(entity)) || !assignmentId.equals(entity.getAssignmentId())) {
            throw new ResourceNotFoundException(ASSIGNMENT_ALLOCATION_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the assessment.assignment it belongs to). */
    private Optional<UUID> institutionOfAssignmentAllocation(AssignmentAllocationEntity entity) {
        return assignmentAllocationRepository.institutionOf(entity.getAssignmentId());
    }

    /**
     * Applies the selected AssignmentAllocationUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyAssignmentAllocation(AssignmentAllocationEntity entity, AssignmentAllocationUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "studentId" -> entity.setStudentId(changes.getStudentId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidAssignmentAllocation(AssignmentAllocationEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getAssignmentId() == null) {
            errors.add(new FieldError("assignmentId", "must not be null", "NotNull"));
        }
        if (entity.getStudentId() == null) {
            errors.add(new FieldError("studentId", "must not be null", "NotNull"));
        }
        if (entity.getAssignmentId() != null && !assignmentAllocationRepository.existsAssignmentId(entity.getAssignmentId())) {
            errors.add(new FieldError("assignmentId", "assignment does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueAssignmentAllocation(AssignmentAllocationEntity entity) {
        if (assignmentAllocationRepository.takenAssignmentIdStudentId1(entity)) {
            throw new DuplicateResourceException(
                    "A assignment allocation with the same assignment id and student id already exists.");
        }
    }

    // ---- AssignmentApproval: a record of the Assignment aggregate ----

    public AssignmentApproval getAssignmentApproval(UUID assignmentId, UUID id) {
        return doInReadTransaction(() -> {
            return assignmentApprovalMapper.toDto(loadVisibleAssignmentApproval(DataScope.current(
                    ASSIGNMENT_APPROVAL_PERMISSION_GET), assignmentId, id));
        });
    }

    public Page<AssignmentApproval> listAssignmentApprovals(UUID assignmentId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, AssignmentApprovalRepository.SORT_FIELDS,
                        AssignmentApprovalRepository.DEFAULT_ORDER,
                AssignmentApprovalRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, AssignmentApprovalRepository.FILTER_FIELDS)
                .and(AssignmentApprovalRepository.ofParent(assignmentId))
                        .and(AssignmentApprovalRepository.scope(DataScope.current(ASSIGNMENT_APPROVAL_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + assignmentId, clock);
        return doInReadTransaction(() -> {
            Page<AssignmentApprovalEntity> rows = assignmentApprovalRepository.list(page, where);
            return rows.map(assignmentApprovalMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public AssignmentApproval createAssignmentApproval(UUID assignmentId, AssignmentApproval request,
            boolean validateOnly) {
        validator.validate(request);
        AssignmentApprovalEntity entity = assignmentApprovalMapper.toEntity(request);
        entity.setAssignmentId(assignmentId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(ASSIGNMENT_APPROVAL_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!assignmentApprovalRepository.existsAssignmentId(entity.getAssignmentId())) {
                throw new ResourceNotFoundException("assessment.assignment", entity.getAssignmentId());
            }
            if (!scope.canRead(institutionOfAssignmentApproval(entity))) {
                throw new ResourceNotFoundException("assessment.assignment", entity.getAssignmentId());
            }
            assertValidAssignmentApproval(entity, true);
            scope.requireWritable(institutionOfAssignmentApproval(entity));
            assertUniqueAssignmentApproval(entity);
            if (validateOnly) {
                return assignmentApprovalMapper.toDto(entity);
            }
            AssignmentApprovalEntity stored = assignmentApprovalRepository.insert(entity);
            audit.recordCreate(ASSIGNMENT_APPROVAL_AGGREGATE, stored.getId());
            outbox.publish(ASSIGNMENT_APPROVAL_AGGREGATE + AssessmentEvents.CREATED, ASSIGNMENT_APPROVAL_AGGREGATE,
                    stored.getId(),
                    AssessmentEvents.payload(stored.getId()));
            return assignmentApprovalMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public AssignmentApproval updateAssignmentApproval(UUID assignmentId, UUID id, String ifMatch,
            AssignmentApprovalUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, ASSIGNMENT_APPROVAL_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ASSIGNMENT_APPROVAL_PERMISSION_UPDATE);
            AssignmentApprovalEntity entity = loadVisibleAssignmentApproval(scope, assignmentId, id);
            scope.requireWritable(institutionOfAssignmentApproval(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyAssignmentApproval(entity, changes, fields);
            validator.validate(assignmentApprovalMapper.toDto(entity));
            assertValidAssignmentApproval(entity, false);
            scope.requireWritable(institutionOfAssignmentApproval(entity));
            assertUniqueAssignmentApproval(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return assignmentApprovalMapper.toDto(entity);
            }
            if (assignmentApprovalRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(ASSIGNMENT_APPROVAL_AGGREGATE, id, fields);
            outbox.publish(ASSIGNMENT_APPROVAL_AGGREGATE + AssessmentEvents.UPDATED, ASSIGNMENT_APPROVAL_AGGREGATE, id,
                    AssessmentEvents.payload(id));
            return assignmentApprovalMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteAssignmentApproval(UUID assignmentId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ASSIGNMENT_APPROVAL_PERMISSION_DELETE);
            AssignmentApprovalEntity entity = loadVisibleAssignmentApproval(scope, assignmentId, id);
            scope.requireWritable(institutionOfAssignmentApproval(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (assignmentApprovalRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(ASSIGNMENT_APPROVAL_AGGREGATE, id);
            outbox.publish(ASSIGNMENT_APPROVAL_AGGREGATE + AssessmentEvents.DELETED, ASSIGNMENT_APPROVAL_AGGREGATE, id,
                    AssessmentEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private AssignmentApprovalEntity loadVisibleAssignmentApproval(DataScope scope, UUID assignmentId, UUID id) {
        AssignmentApprovalEntity entity = assignmentApprovalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ASSIGNMENT_APPROVAL_AGGREGATE, id));
        if (!scope.canRead(institutionOfAssignmentApproval(entity)) || !assignmentId.equals(entity.getAssignmentId())) {
            throw new ResourceNotFoundException(ASSIGNMENT_APPROVAL_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the assessment.assignment it belongs to). */
    private Optional<UUID> institutionOfAssignmentApproval(AssignmentApprovalEntity entity) {
        return assignmentApprovalRepository.institutionOf(entity.getAssignmentId());
    }

    /**
     * Applies the selected AssignmentApprovalUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyAssignmentApproval(AssignmentApprovalEntity entity, AssignmentApprovalUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "decision" -> entity.setDecision(Optional.ofNullable(changes.getDecision()).map(v -> v.getValue())
                        .orElse(null));
                case "comments" -> entity.setComments(changes.getComments());
                case "decidedByUserId" -> entity.setDecidedByUserId(changes.getDecidedByUserId());
                case "decidedAt" -> entity.setDecidedAt(changes.getDecidedAt());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidAssignmentApproval(AssignmentApprovalEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getAssignmentId() == null) {
            errors.add(new FieldError("assignmentId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getDecision() == null) {
            errors.add(new FieldError("decision", "must not be null", "NotNull"));
        }
        if (entity.getAssignmentId() != null && !assignmentApprovalRepository.existsAssignmentId(entity.getAssignmentId())) {
            errors.add(new FieldError("assignmentId", "assignment does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueAssignmentApproval(AssignmentApprovalEntity entity) {

    }

    // ---- AssignmentSubject: a record of the Assignment aggregate ----

    public AssignmentSubject getAssignmentSubject(UUID assignmentId, UUID id) {
        return doInReadTransaction(() -> {
            return assignmentSubjectMapper.toDto(loadVisibleAssignmentSubject(DataScope.current(
                    ASSIGNMENT_SUBJECT_PERMISSION_GET), assignmentId, id));
        });
    }

    public Page<AssignmentSubject> listAssignmentSubjects(UUID assignmentId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, AssignmentSubjectRepository.SORT_FIELDS,
                        AssignmentSubjectRepository.DEFAULT_ORDER,
                AssignmentSubjectRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, AssignmentSubjectRepository.FILTER_FIELDS)
                .and(AssignmentSubjectRepository.ofParent(assignmentId))
                        .and(AssignmentSubjectRepository.scope(DataScope.current(ASSIGNMENT_SUBJECT_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + assignmentId, clock);
        return doInReadTransaction(() -> {
            Page<AssignmentSubjectEntity> rows = assignmentSubjectRepository.list(page, where);
            return rows.map(assignmentSubjectMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public AssignmentSubject createAssignmentSubject(UUID assignmentId, AssignmentSubject request,
            boolean validateOnly) {
        validator.validate(request);
        AssignmentSubjectEntity entity = assignmentSubjectMapper.toEntity(request);
        entity.setAssignmentId(assignmentId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(ASSIGNMENT_SUBJECT_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!assignmentSubjectRepository.existsAssignmentId(entity.getAssignmentId())) {
                throw new ResourceNotFoundException("assessment.assignment", entity.getAssignmentId());
            }
            if (!scope.canRead(institutionOfAssignmentSubject(entity))) {
                throw new ResourceNotFoundException("assessment.assignment", entity.getAssignmentId());
            }
            assertValidAssignmentSubject(entity, true);
            scope.requireWritable(institutionOfAssignmentSubject(entity));
            assertUniqueAssignmentSubject(entity);
            if (validateOnly) {
                return assignmentSubjectMapper.toDto(entity);
            }
            AssignmentSubjectEntity stored = assignmentSubjectRepository.insert(entity);
            audit.recordCreate(ASSIGNMENT_SUBJECT_AGGREGATE, stored.getId());
            outbox.publish(ASSIGNMENT_SUBJECT_AGGREGATE + AssessmentEvents.CREATED, ASSIGNMENT_SUBJECT_AGGREGATE, stored.getId(),
                    AssessmentEvents.payload(stored.getId()));
            return assignmentSubjectMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public AssignmentSubject updateAssignmentSubject(UUID assignmentId, UUID id, String ifMatch,
            AssignmentSubjectUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, ASSIGNMENT_SUBJECT_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ASSIGNMENT_SUBJECT_PERMISSION_UPDATE);
            AssignmentSubjectEntity entity = loadVisibleAssignmentSubject(scope, assignmentId, id);
            scope.requireWritable(institutionOfAssignmentSubject(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyAssignmentSubject(entity, changes, fields);
            validator.validate(assignmentSubjectMapper.toDto(entity));
            assertValidAssignmentSubject(entity, false);
            scope.requireWritable(institutionOfAssignmentSubject(entity));
            assertUniqueAssignmentSubject(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return assignmentSubjectMapper.toDto(entity);
            }
            if (assignmentSubjectRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(ASSIGNMENT_SUBJECT_AGGREGATE, id, fields);
            outbox.publish(ASSIGNMENT_SUBJECT_AGGREGATE + AssessmentEvents.UPDATED, ASSIGNMENT_SUBJECT_AGGREGATE, id,
                    AssessmentEvents.payload(id));
            return assignmentSubjectMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteAssignmentSubject(UUID assignmentId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ASSIGNMENT_SUBJECT_PERMISSION_DELETE);
            AssignmentSubjectEntity entity = loadVisibleAssignmentSubject(scope, assignmentId, id);
            scope.requireWritable(institutionOfAssignmentSubject(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (assignmentSubjectRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(ASSIGNMENT_SUBJECT_AGGREGATE, id);
            outbox.publish(ASSIGNMENT_SUBJECT_AGGREGATE + AssessmentEvents.DELETED, ASSIGNMENT_SUBJECT_AGGREGATE, id,
                    AssessmentEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private AssignmentSubjectEntity loadVisibleAssignmentSubject(DataScope scope, UUID assignmentId, UUID id) {
        AssignmentSubjectEntity entity = assignmentSubjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ASSIGNMENT_SUBJECT_AGGREGATE, id));
        if (!scope.canRead(institutionOfAssignmentSubject(entity)) || !assignmentId.equals(entity.getAssignmentId())) {
            throw new ResourceNotFoundException(ASSIGNMENT_SUBJECT_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the assessment.assignment it belongs to). */
    private Optional<UUID> institutionOfAssignmentSubject(AssignmentSubjectEntity entity) {
        return assignmentSubjectRepository.institutionOf(entity.getAssignmentId());
    }

    /**
     * Applies the selected AssignmentSubjectUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyAssignmentSubject(AssignmentSubjectEntity entity, AssignmentSubjectUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "subjectId" -> entity.setSubjectId(changes.getSubjectId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidAssignmentSubject(AssignmentSubjectEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getAssignmentId() == null) {
            errors.add(new FieldError("assignmentId", "must not be null", "NotNull"));
        }
        if (entity.getSubjectId() == null) {
            errors.add(new FieldError("subjectId", "must not be null", "NotNull"));
        }
        if (entity.getAssignmentId() != null && !assignmentSubjectRepository.existsAssignmentId(entity.getAssignmentId())) {
            errors.add(new FieldError("assignmentId", "assignment does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueAssignmentSubject(AssignmentSubjectEntity entity) {
        if (assignmentSubjectRepository.takenAssignmentIdSubjectId1(entity)) {
            throw new DuplicateResourceException(
                    "A assignment subject with the same assignment id and subject id already exists.");
        }
    }
}
