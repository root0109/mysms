package com.mysms.assessment.business;

import com.mysms.assessment.api.model.CancelExamRequest;
import com.mysms.assessment.api.model.CompleteExamRequest;
import com.mysms.assessment.api.model.Exam;
import com.mysms.assessment.api.model.ExamInvigilator;
import com.mysms.assessment.api.model.ExamInvigilatorUpdate;
import com.mysms.assessment.api.model.ExamUpdate;
import com.mysms.assessment.api.model.ResetExamRequest;
import com.mysms.assessment.api.model.ScheduleExamRequest;
import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.config.AssessmentEventsConfig;
import com.mysms.assessment.domain.ExamEntity;
import com.mysms.assessment.domain.ExamInvigilatorEntity;
import com.mysms.assessment.events.AssessmentEvents;
import com.mysms.assessment.mapper.ExamInvigilatorMapper;
import com.mysms.assessment.mapper.ExamMapper;
import com.mysms.assessment.repository.ExamInvigilatorRepository;
import com.mysms.assessment.repository.ExamRepository;
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
 * Business service of assessment.exam (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a Exam
 * are served here too — ExamInvigilator.
 */
@Service
public class ExamService extends AppTransactionManager {

    public static final String AGGREGATE = ExamEntity.TABLE;
    static final String PERMISSION_LIST = "assessment.exam.read";
    static final String PERMISSION_GET = "assessment.exam.read";
    static final String PERMISSION_CREATE = "assessment.exam.create";
    static final String PERMISSION_UPDATE = "assessment.exam.update";
    static final String PERMISSION_DELETE = "assessment.exam.delete";
    static final String PERMISSION_SCHEDULE_EXAM = "assessment.exam.schedule_exam";
    static final String PERMISSION_COMPLETE_EXAM = "assessment.exam.complete_exam";
    static final String PERMISSION_CANCEL_EXAM = "assessment.exam.cancel_exam";
    static final String PERMISSION_RESET_EXAM = "assessment.exam.reset_exam";
    /** The contract's ExamUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("examSessionId", "subjectId", "classSectionId", "code", "name", "startAt", "endAt", "totalMarks",
            "passMarks", "resultsEntered", "notes");

    public static final String EXAM_INVIGILATOR_AGGREGATE = ExamInvigilatorEntity.TABLE;
    static final String EXAM_INVIGILATOR_PERMISSION_LIST = "assessment.exam_invigilator.read";
    static final String EXAM_INVIGILATOR_PERMISSION_GET = "assessment.exam_invigilator.read";
    static final String EXAM_INVIGILATOR_PERMISSION_CREATE = "assessment.exam_invigilator.create";
    static final String EXAM_INVIGILATOR_PERMISSION_UPDATE = "assessment.exam_invigilator.update";
    static final String EXAM_INVIGILATOR_PERMISSION_DELETE = "assessment.exam_invigilator.delete";
    /** The contract's ExamInvigilatorUpdate fields, in apply order. */
    static final List<String> EXAM_INVIGILATOR_WRITABLE = List.of("staffId");

    private final ExamRepository repository;
    private final ExamMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final ExamInvigilatorRepository examInvigilatorRepository;
    private final ExamInvigilatorMapper examInvigilatorMapper;

    public ExamService(ExamRepository repository,
            ExamMapper mapper,
            @Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AssessmentDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AssessmentEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AssessmentEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            ExamInvigilatorRepository examInvigilatorRepository,
            ExamInvigilatorMapper examInvigilatorMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.examInvigilatorRepository = examInvigilatorRepository;
        this.examInvigilatorMapper = examInvigilatorMapper;
    }

    public Exam getExam(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Exam> listExams(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, ExamRepository.SORT_FIELDS, ExamRepository.DEFAULT_ORDER,
                ExamRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ExamRepository.FILTER_FIELDS)
                .and(ExamRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ExamEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Exam createExam(Exam request, boolean validateOnly) {
        validator.validate(request);
        ExamEntity entity = mapper.toEntity(request);
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
            ExamEntity stored = repository.insert(entity);
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
    public Exam updateExam(UUID id, String ifMatch, ExamUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ExamEntity entity = loadVisible(scope, id);
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
    public void deleteExam(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            ExamEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByExamInvigilatorExamId(id)) {
                throw new InvalidStateException(
                        "The exam is still referenced by assessment.exam_invigilator rows; remove those first.");
            }
            if (repository.referencedByExamResultExamId(id)) {
                throw new InvalidStateException(
                        "The exam is still referenced by assessment.exam_result rows; remove those first.");
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
     * assessment.ScheduleExam: Schedule. The row must be visible and writable for the caller, and the step is allowed
     * while status is draft; in any other state
     * the answer is 409 and nothing changes.
     */
    public Exam scheduleExam(UUID id, ScheduleExamRequest request) {
        DataScope scope = DataScope.current(PERMISSION_SCHEDULE_EXAM);
        return doInWriteTransaction(() -> {
            ExamEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("draft").contains(entity.getStatus()))) {
                throw new InvalidStateException("The exam cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.scheduleExam(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "schedule", List.of("status"), null);
            outbox.publish(AGGREGATE + ".schedule", AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * assessment.CompleteExam: Done. The row must be visible and writable for the caller, and the step is allowed while
     * status is held or result_updated; in any other state
     * the answer is 409 and nothing changes.
     */
    public Exam completeExam(UUID id, CompleteExamRequest request) {
        DataScope scope = DataScope.current(PERMISSION_COMPLETE_EXAM);
        return doInWriteTransaction(() -> {
            ExamEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("held", "result_updated").contains(entity.getStatus()))) {
                throw new InvalidStateException("The exam cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.completeExam(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "complete", List.of("status"), null);
            outbox.publish(AGGREGATE + ".complete", AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * assessment.CancelExam: Cancel. The row must be visible and writable for the caller, and the step is allowed while
     * status is draft or scheduled; in any other state
     * the answer is 409 and nothing changes.
     */
    public Exam cancelExam(UUID id, CancelExamRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CANCEL_EXAM);
        return doInWriteTransaction(() -> {
            ExamEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("draft", "scheduled").contains(entity.getStatus()))) {
                throw new InvalidStateException("The exam cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.cancelExam(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
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
     * assessment.ResetExam: Back to draft. The row must be visible and writable for the caller, and the step is allowed
     * while status is cancelled; in any other state
     * the answer is 409 and nothing changes.
     */
    public Exam resetExam(UUID id, ResetExamRequest request) {
        DataScope scope = DataScope.current(PERMISSION_RESET_EXAM);
        return doInWriteTransaction(() -> {
            ExamEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("cancelled").contains(entity.getStatus()))) {
                throw new InvalidStateException("The exam cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.resetExam(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "reset", List.of("status"), null);
            outbox.publish(AGGREGATE + ".reset", AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ExamEntity loadVisible(DataScope scope, UUID id) {
        ExamEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the assessment.exam_session it belongs to). */
    private Optional<UUID> institutionOf(ExamEntity entity) {
        return repository.institutionOf(entity.getExamSessionId());
    }

    /** Applies the selected ExamUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(ExamEntity entity, ExamUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "examSessionId" -> entity.setExamSessionId(changes.getExamSessionId());
                case "subjectId" -> entity.setSubjectId(changes.getSubjectId());
                case "classSectionId" -> entity.setClassSectionId(changes.getClassSectionId());
                case "code" -> entity.setCode(changes.getCode());
                case "name" -> entity.setName(changes.getName());
                case "startAt" -> entity.setStartAt(changes.getStartAt());
                case "endAt" -> entity.setEndAt(changes.getEndAt());
                case "totalMarks" -> entity.setTotalMarks(WireValues.decimal(changes.getTotalMarks()));
                case "passMarks" -> entity.setPassMarks(WireValues.decimal(changes.getPassMarks()));
                case "resultsEntered" -> entity.setResultsEntered(changes.getResultsEntered());
                case "notes" -> entity.setNotes(changes.getNotes());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ExamEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getExamSessionId() == null) {
            errors.add(new FieldError("examSessionId", "must not be null", "NotNull"));
        }
        if (entity.getSubjectId() == null) {
            errors.add(new FieldError("subjectId", "must not be null", "NotNull"));
        }
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (entity.getStartAt() == null) {
            errors.add(new FieldError("startAt", "must not be null", "NotNull"));
        }
        if (entity.getEndAt() == null) {
            errors.add(new FieldError("endAt", "must not be null", "NotNull"));
        }
        if (entity.getTotalMarks() == null) {
            errors.add(new FieldError("totalMarks", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("totalMarks", WireValues.decimal(entity.getTotalMarks()), 8, 2, errors);
        if (entity.getPassMarks() == null) {
            errors.add(new FieldError("passMarks", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("passMarks", WireValues.decimal(entity.getPassMarks()), 8, 2, errors);
        if (!creating && entity.getResultsEntered() == null) {
            errors.add(new FieldError("resultsEntered", "must not be null", "NotNull"));
        }
        if (entity.getExamSessionId() != null && !repository.existsExamSessionId(entity.getExamSessionId())) {
            errors.add(new FieldError("examSessionId", "exam session does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ExamEntity entity) {
        if (repository.takenExamSessionIdCode1(entity)) {
            throw new DuplicateResourceException("A exam with the same exam session id and code already exists.");
        }
    }

    // ---- ExamInvigilator: a record of the Exam aggregate ----

    public ExamInvigilator getExamInvigilator(UUID examId, UUID id) {
        return doInReadTransaction(() -> {
            return examInvigilatorMapper.toDto(loadVisibleExamInvigilator(DataScope.current(EXAM_INVIGILATOR_PERMISSION_GET),
                    examId, id));
        });
    }

    public Page<ExamInvigilator> listExamInvigilators(UUID examId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ExamInvigilatorRepository.SORT_FIELDS, ExamInvigilatorRepository.DEFAULT_ORDER,
                ExamInvigilatorRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ExamInvigilatorRepository.FILTER_FIELDS)
                .and(ExamInvigilatorRepository.ofParent(examId))
                        .and(ExamInvigilatorRepository.scope(DataScope.current(EXAM_INVIGILATOR_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + examId, clock);
        return doInReadTransaction(() -> {
            Page<ExamInvigilatorEntity> rows = examInvigilatorRepository.list(page, where);
            return rows.map(examInvigilatorMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ExamInvigilator createExamInvigilator(UUID examId, ExamInvigilator request, boolean validateOnly) {
        validator.validate(request);
        ExamInvigilatorEntity entity = examInvigilatorMapper.toEntity(request);
        entity.setExamId(examId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(EXAM_INVIGILATOR_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!examInvigilatorRepository.existsExamId(entity.getExamId())) {
                throw new ResourceNotFoundException("assessment.exam", entity.getExamId());
            }
            if (!scope.canRead(institutionOfExamInvigilator(entity))) {
                throw new ResourceNotFoundException("assessment.exam", entity.getExamId());
            }
            assertValidExamInvigilator(entity, true);
            scope.requireWritable(institutionOfExamInvigilator(entity));
            assertUniqueExamInvigilator(entity);
            if (validateOnly) {
                return examInvigilatorMapper.toDto(entity);
            }
            ExamInvigilatorEntity stored = examInvigilatorRepository.insert(entity);
            audit.recordCreate(EXAM_INVIGILATOR_AGGREGATE, stored.getId());
            outbox.publish(EXAM_INVIGILATOR_AGGREGATE + AssessmentEvents.CREATED, EXAM_INVIGILATOR_AGGREGATE, stored.getId(),
                    AssessmentEvents.payload(stored.getId()));
            return examInvigilatorMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public ExamInvigilator updateExamInvigilator(UUID examId, UUID id, String ifMatch, ExamInvigilatorUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, EXAM_INVIGILATOR_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EXAM_INVIGILATOR_PERMISSION_UPDATE);
            ExamInvigilatorEntity entity = loadVisibleExamInvigilator(scope, examId, id);
            scope.requireWritable(institutionOfExamInvigilator(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyExamInvigilator(entity, changes, fields);
            validator.validate(examInvigilatorMapper.toDto(entity));
            assertValidExamInvigilator(entity, false);
            scope.requireWritable(institutionOfExamInvigilator(entity));
            assertUniqueExamInvigilator(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return examInvigilatorMapper.toDto(entity);
            }
            if (examInvigilatorRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(EXAM_INVIGILATOR_AGGREGATE, id, fields);
            outbox.publish(EXAM_INVIGILATOR_AGGREGATE + AssessmentEvents.UPDATED, EXAM_INVIGILATOR_AGGREGATE, id,
                    AssessmentEvents.payload(id));
            return examInvigilatorMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteExamInvigilator(UUID examId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EXAM_INVIGILATOR_PERMISSION_DELETE);
            ExamInvigilatorEntity entity = loadVisibleExamInvigilator(scope, examId, id);
            scope.requireWritable(institutionOfExamInvigilator(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (examInvigilatorRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(EXAM_INVIGILATOR_AGGREGATE, id);
            outbox.publish(EXAM_INVIGILATOR_AGGREGATE + AssessmentEvents.DELETED, EXAM_INVIGILATOR_AGGREGATE, id,
                    AssessmentEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ExamInvigilatorEntity loadVisibleExamInvigilator(DataScope scope, UUID examId, UUID id) {
        ExamInvigilatorEntity entity = examInvigilatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(EXAM_INVIGILATOR_AGGREGATE, id));
        if (!scope.canRead(institutionOfExamInvigilator(entity)) || !examId.equals(entity.getExamId())) {
            throw new ResourceNotFoundException(EXAM_INVIGILATOR_AGGREGATE, id);
        }
        return entity;
    }

    /**
     * The institution that owns the row (the institution of the assessment.exam → assessment.exam_session it belongs
     * to).
     */
    private Optional<UUID> institutionOfExamInvigilator(ExamInvigilatorEntity entity) {
        return examInvigilatorRepository.institutionOf(entity.getExamId());
    }

    /**
     * Applies the selected ExamInvigilatorUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyExamInvigilator(ExamInvigilatorEntity entity, ExamInvigilatorUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "staffId" -> entity.setStaffId(changes.getStaffId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidExamInvigilator(ExamInvigilatorEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getExamId() == null) {
            errors.add(new FieldError("examId", "must not be null", "NotNull"));
        }
        if (entity.getStaffId() == null) {
            errors.add(new FieldError("staffId", "must not be null", "NotNull"));
        }
        if (entity.getExamId() != null && !examInvigilatorRepository.existsExamId(entity.getExamId())) {
            errors.add(new FieldError("examId", "exam does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueExamInvigilator(ExamInvigilatorEntity entity) {
        if (examInvigilatorRepository.takenExamIdStaffId1(entity)) {
            throw new DuplicateResourceException(
                    "A exam invigilator with the same exam id and staff id already exists.");
        }
    }
}
