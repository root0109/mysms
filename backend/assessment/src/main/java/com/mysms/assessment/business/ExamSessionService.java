package com.mysms.assessment.business;

import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.config.AssessmentEventsConfig;
import com.mysms.assessment.domain.ExamSessionEntity;
import com.mysms.assessment.events.AssessmentEvents;
import com.mysms.assessment.mapper.ExamSessionMapper;
import com.mysms.assessment.repository.ExamSessionRepository;
import com.mysms.authclient.DataScope;
import com.mysms.assessment.api.model.CancelExamSessionRequest;
import com.mysms.assessment.api.model.CompleteExamSessionRequest;
import com.mysms.assessment.api.model.ExamSession;
import com.mysms.assessment.api.model.ExamSessionUpdate;
import com.mysms.assessment.api.model.MarkExamSessionHeldRequest;
import com.mysms.assessment.api.model.ResetExamSessionRequest;
import com.mysms.assessment.api.model.ScheduleExamSessionRequest;
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
 * Business service of assessment.exam_session (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 */
@Service
public class ExamSessionService extends AppTransactionManager {

    public static final String AGGREGATE = ExamSessionEntity.TABLE;
    static final String PERMISSION_LIST = "assessment.exam_session.read";
    static final String PERMISSION_GET = "assessment.exam_session.read";
    static final String PERMISSION_CREATE = "assessment.exam_session.create";
    static final String PERMISSION_UPDATE = "assessment.exam_session.update";
    static final String PERMISSION_DELETE = "assessment.exam_session.delete";
    static final String PERMISSION_SCHEDULE_EXAM_SESSION = "assessment.exam_session.schedule_exam_session";
    static final String PERMISSION_MARK_EXAM_SESSION_HELD = "assessment.exam_session.mark_exam_session_held";
    static final String PERMISSION_COMPLETE_EXAM_SESSION = "assessment.exam_session.complete_exam_session";
    static final String PERMISSION_CANCEL_EXAM_SESSION = "assessment.exam_session.cancel_exam_session";
    static final String PERMISSION_RESET_EXAM_SESSION = "assessment.exam_session.reset_exam_session";
    /** The contract's ExamSessionUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "academicYearId", "academicTermId", "examTypeId", "code", "name", "gradeLevelId",
            "courseId", "batchId", "evaluationType", "startDate", "endDate", "venue");


    private final ExamSessionRepository repository;
    private final ExamSessionMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public ExamSessionService(ExamSessionRepository repository, ExamSessionMapper mapper,
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

    public ExamSession getExamSession(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<ExamSession> listExamSessions(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, ExamSessionRepository.SORT_FIELDS, ExamSessionRepository.DEFAULT_ORDER,
                ExamSessionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ExamSessionRepository.FILTER_FIELDS)
                .and(ExamSessionRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ExamSessionEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ExamSession createExamSession(ExamSession request, boolean validateOnly) {
        validator.validate(request);
        ExamSessionEntity entity = mapper.toEntity(request);
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
            ExamSessionEntity stored = repository.insert(entity);
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
    public ExamSession updateExamSession(UUID id, String ifMatch, ExamSessionUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ExamSessionEntity entity = loadVisible(scope, id);
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
    public void deleteExamSession(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            ExamSessionEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByExamExamSessionId(id)) {
                throw new InvalidStateException(
                        "The exam session is still referenced by assessment.exam rows; remove those first.");
            }
            if (repository.referencedByMarksheetExamSessionId(id)) {
                throw new InvalidStateException(
                        "The exam session is still referenced by assessment.marksheet rows; remove those first.");
            }
            if (repository.referencedByResultTemplateExamSessionId(id)) {
                throw new InvalidStateException(
                        "The exam session is still referenced by assessment.result_template rows; remove those first.");
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
     * assessment.ScheduleExamSession: Schedule. The row must be visible and writable for the caller, and the step is
     * allowed while status is draft; in any other state
     * the answer is 409 and nothing changes.
     */
    public ExamSession scheduleExamSession(UUID id, ScheduleExamSessionRequest request) {
        DataScope scope = DataScope.current(PERMISSION_SCHEDULE_EXAM_SESSION);
        return doInWriteTransaction(() -> {
            ExamSessionEntity entity = repository.findById(id)
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
                throw new InvalidStateException("The exam session cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.scheduleExamSession(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "schedule", List.of("status"), null);
            outbox.publish(AGGREGATE + ".schedule", AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * assessment.MarkExamSessionHeld: Mark held. The row must be visible and writable for the caller, and the step is
     * allowed while status is scheduled; in any other state
     * the answer is 409 and nothing changes.
     */
    public ExamSession markExamSessionHeld(UUID id, MarkExamSessionHeldRequest request) {
        DataScope scope = DataScope.current(PERMISSION_MARK_EXAM_SESSION_HELD);
        return doInWriteTransaction(() -> {
            ExamSessionEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("scheduled").contains(entity.getStatus()))) {
                throw new InvalidStateException("The exam session cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.markExamSessionHeld(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "markExamSessionHeld", List.of("status"), null);
            outbox.publish(AGGREGATE + ".markExamSessionHeld", AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * assessment.CompleteExamSession: Done. The row must be visible and writable for the caller, and the step is
     * allowed while status is held; in any other state
     * the answer is 409 and nothing changes.
     */
    public ExamSession completeExamSession(UUID id, CompleteExamSessionRequest request) {
        DataScope scope = DataScope.current(PERMISSION_COMPLETE_EXAM_SESSION);
        return doInWriteTransaction(() -> {
            ExamSessionEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("held").contains(entity.getStatus()))) {
                throw new InvalidStateException("The exam session cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.completeExamSession(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "complete", List.of("status"), null);
            outbox.publish(AGGREGATE + ".complete", AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * assessment.CancelExamSession: Cancel. The row must be visible and writable for the caller, and the step is
     * allowed while status is draft or scheduled; in any other state
     * the answer is 409 and nothing changes.
     */
    public ExamSession cancelExamSession(UUID id, CancelExamSessionRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CANCEL_EXAM_SESSION);
        return doInWriteTransaction(() -> {
            ExamSessionEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("draft", "scheduled").contains(entity.getStatus()))) {
                throw new InvalidStateException("The exam session cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.cancelExamSession(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
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
     * assessment.ResetExamSession: Back to draft. The row must be visible and writable for the caller, and the step is
     * allowed while status is cancelled; in any other state
     * the answer is 409 and nothing changes.
     */
    public ExamSession resetExamSession(UUID id, ResetExamSessionRequest request) {
        DataScope scope = DataScope.current(PERMISSION_RESET_EXAM_SESSION);
        return doInWriteTransaction(() -> {
            ExamSessionEntity entity = repository.findById(id)
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
                throw new InvalidStateException("The exam session cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.resetExamSession(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "reset", List.of("status"), null);
            outbox.publish(AGGREGATE + ".reset", AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ExamSessionEntity loadVisible(DataScope scope, UUID id) {
        ExamSessionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(ExamSessionEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected ExamSessionUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(ExamSessionEntity entity, ExamSessionUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "academicYearId" -> entity.setAcademicYearId(changes.getAcademicYearId());
                case "academicTermId" -> entity.setAcademicTermId(changes.getAcademicTermId());
                case "examTypeId" -> entity.setExamTypeId(changes.getExamTypeId());
                case "code" -> entity.setCode(changes.getCode());
                case "name" -> entity.setName(changes.getName());
                case "gradeLevelId" -> entity.setGradeLevelId(changes.getGradeLevelId());
                case "courseId" -> entity.setCourseId(changes.getCourseId());
                case "batchId" -> entity.setBatchId(changes.getBatchId());
                case "evaluationType" -> entity.setEvaluationType(Optional.ofNullable(changes.getEvaluationType())
                        .map(v -> v.getValue()).orElse(null));
                case "startDate" -> entity.setStartDate(changes.getStartDate());
                case "endDate" -> entity.setEndDate(changes.getEndDate());
                case "venue" -> entity.setVenue(changes.getVenue());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ExamSessionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getExamTypeId() == null) {
            errors.add(new FieldError("examTypeId", "must not be null", "NotNull"));
        }
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (!creating && entity.getEvaluationType() == null) {
            errors.add(new FieldError("evaluationType", "must not be null", "NotNull"));
        }
        if (entity.getStartDate() == null) {
            errors.add(new FieldError("startDate", "must not be null", "NotNull"));
        }
        if (entity.getEndDate() == null) {
            errors.add(new FieldError("endDate", "must not be null", "NotNull"));
        }
        if (entity.getExamTypeId() != null && !repository.existsExamTypeId(entity.getExamTypeId())) {
            errors.add(new FieldError("examTypeId", "exam type does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ExamSessionEntity entity) {
        if (repository.takenInstitutionIdCode1(entity)) {
            throw new DuplicateResourceException(
                    "A exam session with the same institution id and code already exists.");
        }
    }
}
