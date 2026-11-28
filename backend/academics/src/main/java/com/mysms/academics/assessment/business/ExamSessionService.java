package com.mysms.academics.assessment.business;

import com.mysms.academics.assessment.config.AssessmentDatabaseConfig;
import com.mysms.academics.assessment.config.AssessmentEventsConfig;
import com.mysms.academics.assessment.domain.ExamSessionEntity;
import com.mysms.academics.assessment.events.AssessmentEvents;
import com.mysms.academics.assessment.mapper.ExamSessionMapper;
import com.mysms.academics.assessment.repository.ExamSessionRepository;
import com.mysms.authclient.DataScope;
import com.mysms.academics.assessment.api.model.CancelExamSessionRequest;
import com.mysms.academics.assessment.api.model.CompleteExamSessionRequest;
import com.mysms.academics.assessment.api.model.ExamSession;
import com.mysms.academics.assessment.api.model.ExamSessionUpdate;
import com.mysms.academics.assessment.api.model.MarkExamSessionHeldRequest;
import com.mysms.academics.assessment.api.model.ResetExamSessionRequest;
import com.mysms.academics.assessment.api.model.ScheduleExamSessionRequest;
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
import com.mysms.core.web.Patches;
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
 * Business service of assessment.exam_session: audit and domain event commit in the same transaction as every change.
 * Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public ExamSession updateExamSession(UUID id, String ifMatch, ExamSessionUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ExamSessionEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = apply(entity, changes);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
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
     * assessment.ScheduleExamSession: Schedule. Allowed while status is draft, on a row the caller may see and write;
     * otherwise 409 and nothing changes.
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
     * assessment.MarkExamSessionHeld: Mark held. Allowed while status is scheduled, on a row the caller may see and
     * write; otherwise 409 and nothing changes.
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
     * assessment.CompleteExamSession: Done. Allowed while status is held, on a row the caller may see and write;
     * otherwise 409 and nothing changes.
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
     * assessment.CancelExamSession: Cancel. Allowed while status is draft or scheduled, on a row the caller may see and
     * write; otherwise 409 and nothing changes.
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
     * assessment.ResetExamSession: Back to draft. Allowed while status is cancelled, on a row the caller may see and
     * write; otherwise 409 and nothing changes.
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

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(ExamSessionEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(ExamSessionEntity entity, ExamSessionUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("academicYearId", changes.getAcademicYearId(), entity::setAcademicYearId);
        patch.apply("academicTermId", changes.getAcademicTermId(), entity::setAcademicTermId);
        patch.apply("examTypeId", changes.getExamTypeId(), entity::setExamTypeId);
        patch.apply("code", changes.getCode(), entity::setCode);
        patch.apply("name", changes.getName(), entity::setName);
        patch.apply("gradeLevelId", changes.getGradeLevelId(), entity::setGradeLevelId);
        patch.apply("courseId", changes.getCourseId(), entity::setCourseId);
        patch.apply("batchId", changes.getBatchId(), entity::setBatchId);
        patch.apply("evaluationType", changes.getEvaluationType(),
                v -> entity.setEvaluationType(v == null ? null : v.getValue()));
        patch.apply("startDate", changes.getStartDate(), entity::setStartDate);
        patch.apply("endDate", changes.getEndDate(), entity::setEndDate);
        patch.apply("venue", changes.getVenue(), entity::setVenue);
        return patch.applied();
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
