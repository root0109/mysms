package com.mysms.academics.business;

import com.mysms.academics.api.model.ApproveLessonPlanRequest;
import com.mysms.academics.api.model.ArchiveLessonPlanRequest;
import com.mysms.academics.api.model.LessonPlan;
import com.mysms.academics.api.model.LessonPlanApproval;
import com.mysms.academics.api.model.LessonPlanApprovalUpdate;
import com.mysms.academics.api.model.LessonPlanUpdate;
import com.mysms.academics.api.model.PublishLessonPlanRequest;
import com.mysms.academics.api.model.RejectLessonPlanRequest;
import com.mysms.academics.api.model.SubmitLessonPlanRequest;
import com.mysms.academics.config.AcademicsDatabaseConfig;
import com.mysms.academics.config.AcademicsEventsConfig;
import com.mysms.academics.domain.LessonPlanApprovalEntity;
import com.mysms.academics.domain.LessonPlanEntity;
import com.mysms.academics.events.AcademicsEvents;
import com.mysms.academics.mapper.LessonPlanApprovalMapper;
import com.mysms.academics.mapper.LessonPlanMapper;
import com.mysms.academics.repository.LessonPlanApprovalRepository;
import com.mysms.academics.repository.LessonPlanRepository;
import com.mysms.authclient.DataScope;
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
 * Business service of academics.lesson_plan: audit and domain event in the same transaction as every change; rows
 * outside the caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 *
 * <p>Also serves the records that exist only as part of the LessonPlan aggregate: LessonPlanApproval, the review
 * records, which approve and reject write and nothing else changes.
 */
@Service
public class LessonPlanService extends AppTransactionManager {

    public static final String AGGREGATE = LessonPlanEntity.TABLE;
    static final String PERMISSION_LIST = "academics.lesson_plan.read";
    static final String PERMISSION_GET = "academics.lesson_plan.read";
    static final String PERMISSION_CREATE = "academics.lesson_plan.create";
    static final String PERMISSION_UPDATE = "academics.lesson_plan.update";
    static final String PERMISSION_DELETE = "academics.lesson_plan.delete";
    static final String PERMISSION_SUBMIT_LESSON_PLAN = "academics.lesson_plan.submit_lesson_plan";
    static final String PERMISSION_APPROVE_LESSON_PLAN = "academics.lesson_plan.approve_lesson_plan";
    static final String PERMISSION_REJECT_LESSON_PLAN = "academics.lesson_plan.reject_lesson_plan";
    /** The longest reason a review keeps (422 beyond). */
    static final int MAX_REASON = 4000;
    static final String PERMISSION_PUBLISH_LESSON_PLAN = "academics.lesson_plan.publish_lesson_plan";
    static final String PERMISSION_ARCHIVE_LESSON_PLAN = "academics.lesson_plan.archive_lesson_plan";
    /** The contract's LessonPlanUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("teachingAssignmentId", "curriculumUnitId", "unitNo", "unitName", "title", "plannedDurationMinutes",
            "description", "objective", "materialsRequired", "introduction", "procedureText", "conclusion",
            "assessmentText", "modification", "notes", "startDate", "endDate");

    public static final String LESSON_PLAN_APPROVAL_AGGREGATE = LessonPlanApprovalEntity.TABLE;
    static final String LESSON_PLAN_APPROVAL_PERMISSION_LIST = "academics.lesson_plan_approval.read";
    static final String LESSON_PLAN_APPROVAL_PERMISSION_GET = "academics.lesson_plan_approval.read";

    private final LessonPlanRepository repository;
    private final LessonPlanMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final LessonPlanApprovalRepository lessonPlanApprovalRepository;
    private final LessonPlanApprovalMapper lessonPlanApprovalMapper;

    public LessonPlanService(LessonPlanRepository repository,
            LessonPlanMapper mapper,
            @Qualifier(AcademicsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AcademicsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AcademicsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AcademicsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            LessonPlanApprovalRepository lessonPlanApprovalRepository,
            LessonPlanApprovalMapper lessonPlanApprovalMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.lessonPlanApprovalRepository = lessonPlanApprovalRepository;
        this.lessonPlanApprovalMapper = lessonPlanApprovalMapper;
    }

    public LessonPlan getLessonPlan(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<LessonPlan> listLessonPlans(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, LessonPlanRepository.SORT_FIELDS, LessonPlanRepository.DEFAULT_ORDER,
                LessonPlanRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, LessonPlanRepository.FILTER_FIELDS)
                .and(LessonPlanRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<LessonPlanEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public LessonPlan createLessonPlan(LessonPlan request, boolean validateOnly) {
        validator.validate(request);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkDuration("plannedDurationMinutes", request.getPlannedDurationMinutes(), 60, wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        LessonPlanEntity entity = mapper.toEntity(request);
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
            LessonPlanEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + AcademicsEvents.CREATED, AGGREGATE, stored.getId(),
                    AcademicsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public LessonPlan updateLessonPlan(UUID id, String ifMatch, LessonPlanUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkDuration("plannedDurationMinutes", changes.getPlannedDurationMinutes(), 60, wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            LessonPlanEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + AcademicsEvents.UPDATED, AGGREGATE, id, AcademicsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteLessonPlan(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            LessonPlanEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByLessonPlanApprovalLessonPlanId(id)) {
                throw new InvalidStateException(
                        "The lesson plan has been reviewed; a reviewed plan is archived, not deleted.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + AcademicsEvents.DELETED, AGGREGATE, id, AcademicsEvents.payload(id));
        });
    }

    /**
     * academics.SubmitLessonPlan: Submit for approval. The row must be visible and writable for the caller, and the
     * step is allowed while status is draft; in any other state
     * the answer is 409 and nothing changes.
     */
    public LessonPlan submitLessonPlan(UUID id, SubmitLessonPlanRequest request) {
        DataScope scope = DataScope.current(PERMISSION_SUBMIT_LESSON_PLAN);
        return doInWriteTransaction(() -> {
            LessonPlanEntity entity = repository.findById(id)
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
                throw new InvalidStateException("The lesson plan cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.submitLessonPlan(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "submit", List.of("status"), null);
            outbox.publish(AGGREGATE + ".submit", AGGREGATE, id, AcademicsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * academics.ApproveLessonPlan: a plan waiting for review (status pending) becomes approved, and the decision is
     * kept as a review record — who decided, when, and the reason when one was given. The row must be visible and
     * writable for the caller; in any other state the answer is 409 and nothing changes. An approved plan can then
     * be published to students and guardians (academics.PublishLessonPlan).
     */
    public LessonPlan approveLessonPlan(UUID id, ApproveLessonPlanRequest request) {
        String reason = reviewReason(request == null ? null : request.getReason());
        DataScope scope = DataScope.current(PERMISSION_APPROVE_LESSON_PLAN);
        return doInWriteTransaction(() -> {
            LessonPlanEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!"pending".equals(entity.getStatus())) {
                throw new InvalidStateException("The lesson plan cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.approveLessonPlan(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            recordReview(entity, "approved", reason, now);
            audit.recordTransition(AGGREGATE, id, "approve", List.of("status"), reason.isEmpty() ? null : reason);
            outbox.publish(AGGREGATE + ".approve", AGGREGATE, id, AcademicsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The reason of a review as it is kept: trimmed, empty when none was given, at most MAX_REASON characters. */
    private static String reviewReason(String given) {
        String reason = given == null ? "" : given.trim();
        if (reason.length() > MAX_REASON) {
            throw new ValidationFailedException(List.of(new FieldError("reason",
                    "size must be between 0 and " + MAX_REASON, "Size")));
        }
        return reason;
    }

    /** The review record of a decision, written in the transaction of the step it belongs to. */
    private void recordReview(LessonPlanEntity plan, String decision, String reason, OffsetDateTime now) {
        LessonPlanApprovalEntity review = LessonPlanApprovalEntity.builder()
                .lessonPlanId(plan.getId())
                .decision(decision)
                .comments(reason)
                .decidedByUserId(Actor.current().orElse(null))
                .decidedAt(now)
                .build();
        review.onCreatingData(Actor.current(), clock);
        review.setTenantId(plan.getTenantId());
        audit.recordCreate(LESSON_PLAN_APPROVAL_AGGREGATE, lessonPlanApprovalRepository.insert(review).getId());
    }

    /**
     * academics.RejectLessonPlan: Reject. The row must be visible and writable for the caller, and the step is allowed
     * while status is pending; in any other state
     * the answer is 409 and nothing changes. The decision is kept as a review record, like an approval.
     */
    public LessonPlan rejectLessonPlan(UUID id, RejectLessonPlanRequest request) {
        String reason = reviewReason(request == null ? null : request.getReason());
        DataScope scope = DataScope.current(PERMISSION_REJECT_LESSON_PLAN);
        return doInWriteTransaction(() -> {
            LessonPlanEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("pending").contains(entity.getStatus()))) {
                throw new InvalidStateException("The lesson plan cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.rejectLessonPlan(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            recordReview(entity, "rejected", reason, now);
            audit.recordTransition(AGGREGATE, id, "reject", List.of("status"), reason.isEmpty() ? null : reason);
            outbox.publish(AGGREGATE + ".reject", AGGREGATE, id, AcademicsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * academics.PublishLessonPlan: Publish to students / guardians. The row must be visible and writable for the
     * caller, and the step is allowed while status is approved and is_published is false; in any other state
     * the answer is 409 and nothing changes.
     */
    public LessonPlan publishLessonPlan(UUID id, PublishLessonPlanRequest request) {
        DataScope scope = DataScope.current(PERMISSION_PUBLISH_LESSON_PLAN);
        return doInWriteTransaction(() -> {
            LessonPlanEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("approved").contains(entity.getStatus()) && !Boolean.TRUE.equals(entity.getIsPublished()))) {
                throw new InvalidStateException("The lesson plan cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.publishLessonPlan(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "publish", List.of("isPublished", "publishedAt"), null);
            outbox.publish(AGGREGATE + ".publish", AGGREGATE, id, AcademicsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * academics.ArchiveLessonPlan: Archive. The row must be visible and writable for the caller, and the step is
     * allowed while status is approved or rejected or cancelled; in any other state
     * the answer is 409 and nothing changes.
     */
    public LessonPlan archiveLessonPlan(UUID id, ArchiveLessonPlanRequest request) {
        DataScope scope = DataScope.current(PERMISSION_ARCHIVE_LESSON_PLAN);
        return doInWriteTransaction(() -> {
            LessonPlanEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("approved", "rejected", "cancelled").contains(entity.getStatus()))) {
                throw new InvalidStateException("The lesson plan cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.archiveLessonPlan(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "archive", List.of("status"), null);
            outbox.publish(AGGREGATE + ".archive", AGGREGATE, id, AcademicsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private LessonPlanEntity loadVisible(DataScope scope, UUID id) {
        LessonPlanEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /**
     * The institution that owns the row (the institution of the academics.teaching_assignment → academics.class_subject
     * → academics.class_section it belongs to).
     */
    private Optional<UUID> institutionOf(LessonPlanEntity entity) {
        return repository.institutionOf(entity.getTeachingAssignmentId());
    }

    /** Applies the selected LessonPlanUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(LessonPlanEntity entity, LessonPlanUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "teachingAssignmentId" -> entity.setTeachingAssignmentId(changes.getTeachingAssignmentId());
                case "curriculumUnitId" -> entity.setCurriculumUnitId(changes.getCurriculumUnitId());
                case "unitNo" -> entity.setUnitNo(changes.getUnitNo());
                case "unitName" -> entity.setUnitName(changes.getUnitName());
                case "title" -> entity.setTitle(changes.getTitle());
                case "plannedDurationMinutes" -> entity.setPlannedDurationMinutes(
                        WireValues.duration(changes.getPlannedDurationMinutes(), 60));
                case "description" -> entity.setDescription(changes.getDescription());
                case "objective" -> entity.setObjective(changes.getObjective());
                case "materialsRequired" -> entity.setMaterialsRequired(changes.getMaterialsRequired());
                case "introduction" -> entity.setIntroduction(changes.getIntroduction());
                case "procedureText" -> entity.setProcedureText(changes.getProcedureText());
                case "conclusion" -> entity.setConclusion(changes.getConclusion());
                case "assessmentText" -> entity.setAssessmentText(changes.getAssessmentText());
                case "modification" -> entity.setModification(changes.getModification());
                case "notes" -> entity.setNotes(changes.getNotes());
                case "startDate" -> entity.setStartDate(changes.getStartDate());
                case "endDate" -> entity.setEndDate(changes.getEndDate());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(LessonPlanEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getTeachingAssignmentId() == null) {
            errors.add(new FieldError("teachingAssignmentId", "must not be null", "NotNull"));
        }
        if (entity.getUnitNo() == null) {
            errors.add(new FieldError("unitNo", "must not be null", "NotNull"));
        }
        if (entity.getUnitName() == null) {
            errors.add(new FieldError("unitName", "must not be null", "NotNull"));
        }
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (entity.getPlannedDurationMinutes() == null) {
            errors.add(new FieldError("plannedDurationMinutes", "must not be null", "NotNull"));
        }
        if (entity.getDescription() == null) {
            errors.add(new FieldError("description", "must not be null", "NotNull"));
        }
        if (entity.getCurriculumUnitId() != null && !repository.existsCurriculumUnitId(entity.getCurriculumUnitId())) {
            errors.add(new FieldError("curriculumUnitId", "curriculum unit does not exist", "NOT_FOUND"));
        }
        if (entity.getTeachingAssignmentId() != null
                && !repository.existsTeachingAssignmentId(entity.getTeachingAssignmentId())) {
            errors.add(new FieldError("teachingAssignmentId", "teaching assignment does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(LessonPlanEntity entity) {

    }

    // ---- LessonPlanApproval: a record of the LessonPlan aggregate ----

    public LessonPlanApproval getLessonPlanApproval(UUID lessonPlanId, UUID id) {
        return doInReadTransaction(() -> {
            requireVisiblePlan(DataScope.current(LESSON_PLAN_APPROVAL_PERMISSION_GET), lessonPlanId);
            LessonPlanApprovalEntity entity = lessonPlanApprovalRepository.findById(id)
                    .filter(found -> lessonPlanId.equals(found.getLessonPlanId()))
                    .orElseThrow(() -> new ResourceNotFoundException(LESSON_PLAN_APPROVAL_AGGREGATE, id));
            return lessonPlanApprovalMapper.toDto(entity);
        });
    }

    public Page<LessonPlanApproval> listLessonPlanApprovals(UUID lessonPlanId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, LessonPlanApprovalRepository.SORT_FIELDS,
                LessonPlanApprovalRepository.DEFAULT_ORDER, LessonPlanApprovalRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, LessonPlanApprovalRepository.FILTER_FIELDS)
                .and(LessonPlanApprovalRepository.ofParent(lessonPlanId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + lessonPlanId, clock);
        DataScope scope = DataScope.current(LESSON_PLAN_APPROVAL_PERMISSION_LIST);
        return doInReadTransaction(() -> {
            requireVisiblePlan(scope, lessonPlanId);
            Page<LessonPlanApprovalEntity> rows = lessonPlanApprovalRepository.list(page, where);
            return rows.map(lessonPlanApprovalMapper::toDto);
        });
    }

    /**
     * Refused: a review record is written by approving or rejecting the plan, in the name of who decided. One made
     * by hand could stand in for a decision nobody took.
     */
    public LessonPlanApproval createLessonPlanApproval(UUID lessonPlanId, LessonPlanApproval request,
            boolean validateOnly) {
        throw writtenByTheReview();
    }

    /** Refused for the same reason as create: the decision, who took it and when are set by approve and reject. */
    public LessonPlanApproval updateLessonPlanApproval(UUID lessonPlanId, UUID id, String ifMatch,
            LessonPlanApprovalUpdate changes, String updateMask, boolean validateOnly) {
        throw writtenByTheReview();
    }

    /** Refused: the record of who approved or rejected a plan is kept for as long as the plan. */
    public void deleteLessonPlanApproval(UUID lessonPlanId, UUID id, String ifMatch) {
        throw writtenByTheReview();
    }

    private static InvalidStateException writtenByTheReview() {
        return new InvalidStateException(
                "Reviews of a lesson plan are written by approving and rejecting the plan, not directly.");
    }

    /**
     * A review is seen where its plan is: the plan must exist and lie inside the caller's institutions, or the answer
     * is 404 — the reviews of another school's plan are not readable by knowing its id.
     */
    private void requireVisiblePlan(DataScope scope, UUID lessonPlanId) {
        LessonPlanEntity plan = repository.findById(lessonPlanId)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, lessonPlanId));
        if (!scope.canRead(institutionOf(plan))) {
            throw new ResourceNotFoundException(AGGREGATE, lessonPlanId);
        }
    }
}
