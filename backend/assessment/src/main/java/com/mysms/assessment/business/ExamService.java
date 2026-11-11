package com.mysms.assessment.business;

import com.mysms.assessment.api.model.CancelExamRequest;
import com.mysms.assessment.api.model.AllocateExamRoomsRequest;
import com.mysms.assessment.api.model.CompleteExamRequest;
import com.mysms.assessment.api.model.Exam;
import com.mysms.assessment.api.model.ExamInvigilator;
import com.mysms.assessment.api.model.ExamResultList;
import com.mysms.assessment.api.model.ExamInvigilatorUpdate;
import com.mysms.assessment.api.model.ExamUpdate;
import com.mysms.assessment.api.model.MarkEntry;
import com.mysms.assessment.api.model.MarkExamHeldRequest;
import com.mysms.assessment.api.model.MarkExamResultsUpdatedRequest;
import com.mysms.assessment.api.model.RecordExamMarksRequest;
import com.mysms.assessment.api.model.ResetExamRequest;
import com.mysms.assessment.api.model.ScheduleExamRequest;
import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.config.AssessmentEventsConfig;
import com.mysms.assessment.domain.ExamEntity;
import com.mysms.assessment.domain.ExamInvigilatorEntity;
import com.mysms.assessment.domain.ExamResultEntity;
import com.mysms.assessment.domain.ExamSessionEntity;
import com.mysms.assessment.events.AssessmentEvents;
import com.mysms.assessment.mapper.ExamResultMapper;
import com.mysms.assessment.mapper.ExamInvigilatorMapper;
import com.mysms.assessment.mapper.ExamMapper;
import com.mysms.assessment.repository.ExamInvigilatorRepository;
import com.mysms.assessment.repository.ExamRepository;
import com.mysms.assessment.repository.ExamResultRepository;
import com.mysms.assessment.repository.ExamSessionRepository;
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
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private final ExamResultRepository resultRepository;
    private final ExamResultMapper resultMapper;
    private final ExamSessionRepository sessionRepository;
    private final ExamRosterLookup rosterLookup;
    private final ExamRoomLookup roomLookup;
    private final ExamCoursePlanLookup coursePlanLookup;

    public ExamService(ExamRepository repository,
            ExamMapper mapper,
            @Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AssessmentDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AssessmentEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AssessmentEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            ExamInvigilatorRepository examInvigilatorRepository,
            ExamInvigilatorMapper examInvigilatorMapper,
            ExamResultRepository resultRepository,
            ExamResultMapper resultMapper,
            ExamSessionRepository sessionRepository,
            ExamRosterLookup rosterLookup,
            ExamRoomLookup roomLookup,
            ExamCoursePlanLookup coursePlanLookup) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.examInvigilatorRepository = examInvigilatorRepository;
        this.examInvigilatorMapper = examInvigilatorMapper;
        this.resultRepository = resultRepository;
        this.resultMapper = resultMapper;
        this.sessionRepository = sessionRepository;
        this.rosterLookup = rosterLookup;
        this.roomLookup = roomLookup;
        this.coursePlanLookup = coursePlanLookup;
    }

    /** Register the SIS roster once; repeated calls add only students not already registered. */
    public ExamResultList populateExamResults(UUID id) {
        DataScope scope = DataScope.current("assessment.exam.populate_exam_results");
        return doInWriteTransaction(() -> {
            ExamEntity exam = visibleExam(scope, id);
            scope.requireWritable(institutionOf(exam));
            if (!"scheduled".equals(exam.getStatus())) {
                throw new InvalidStateException("Exam attendees can only be populated after scheduling.");
            }
            ExamSessionEntity session = sessionRepository.findById(exam.getExamSessionId())
                    .orElseThrow(() -> new ResourceNotFoundException("assessment.exam_session", exam.getExamSessionId()));
            List<UUID> students = rosterLookup.students(exam, session);
            BigDecimal credits = coursePlanLookup.credits(exam, session);
            if (students.isEmpty()) {
                throw new InvalidStateException("The SIS roster has no students for this exam.");
            }
            Map<UUID, ExamResultEntity> existing = new HashMap<>();
            for (ExamResultEntity result : resultRepository.listForExam(id)) {
                existing.put(result.getStudentId(), result);
            }
            UUID tenantId = CallContextHolder.require().tenantId().orElseThrow();
            for (UUID studentId : students) {
                if (existing.containsKey(studentId)) {
                    ExamResultEntity row = existing.get(studentId);
                    if (credits != null && row.getCredits() == null) {
                        if (resultRepository.setCredits(row.getId(), row.getRowVersion(), credits, now(),
                                Actor.current().orElse(null)) == 0) {
                            throw new PreconditionFailedException(
                                    "An exam attendee changed while credits were being populated.");
                        }
                        audit.recordTransition(ExamResultService.AGGREGATE, row.getId(), "snapshotCredits",
                                List.of("credits"), null);
                        outbox.publish(ExamResultService.AGGREGATE + ".creditsSnapshotted",
                                ExamResultService.AGGREGATE, row.getId(), AssessmentEvents.payload(row.getId()));
                    }
                    continue;
                }
                ExamResultEntity result = ExamResultEntity.builder().tenantId(tenantId).examId(id)
                        .studentId(studentId).attendanceStatus("expected").maxMarks(exam.getTotalMarks())
                        .credits(credits).build();
                result.onCreatingData(Actor.current(), clock);
                ExamResultEntity stored = resultRepository.insert(result);
                existing.put(studentId, stored);
                audit.recordCreate(ExamResultService.AGGREGATE, stored.getId());
                outbox.publish(ExamResultService.AGGREGATE + AssessmentEvents.CREATED,
                        ExamResultService.AGGREGATE, stored.getId(), AssessmentEvents.payload(stored.getId()));
            }
            return resultList(resultRepository.listForExam(id));
        });
    }

    /** Allocate the ordered exam roster across active rooms without exceeding their published capacities. */
    public ExamResultList allocateExamRooms(UUID id, AllocateExamRoomsRequest request) {
        DataScope scope = DataScope.current("assessment.exam.allocate_exam_rooms");
        return doInWriteTransaction(() -> {
            ExamEntity exam = visibleExam(scope, id);
            Optional<UUID> institution = institutionOf(exam);
            scope.requireWritable(institution);
            if (!"scheduled".equals(exam.getStatus())) {
                throw new InvalidStateException("Rooms can only be allocated to a scheduled exam.");
            }
            if (request == null || request.getRoomIds() == null || request.getRoomIds().isEmpty()) {
                throw new ValidationFailedException(List.of(new FieldError("roomIds", "must not be empty", "NotEmpty")));
            }
            List<ExamResultEntity> attendees = resultRepository.listForExam(id).stream()
                    .filter(row -> !Set.of("absent", "exempted").contains(row.getAttendanceStatus())).toList();
            if (attendees.isEmpty()) {
                throw new InvalidStateException("Populate the attendee list before allocating rooms.");
            }
            Set<UUID> uniqueRoomIds = new HashSet<>();
            List<UUID> roomIds = request.getRoomIds();
            List<Integer> capacities = new ArrayList<>();
            for (int i = 0; i < roomIds.size(); i++) {
                UUID roomId = roomIds.get(i);
                if (roomId == null || !uniqueRoomIds.add(roomId)) {
                    throw new ValidationFailedException(List.of(new FieldError("roomIds[" + i + "]",
                            "room ids must be unique and non-null", "DUPLICATE")));
                }
                com.mysms.sdk.facility.model.Room room = roomLookup.requireRoom(roomId);
                if (institution.isEmpty() || !institution.get().equals(room.getInstitutionId())) {
                    throw new ValidationFailedException(List.of(new FieldError("roomIds[" + i + "]",
                            "room belongs to another institution", "OUT_OF_SCOPE")));
                }
                if (room.getCapacity() == null || room.getCapacity() <= 0) {
                    throw new ValidationFailedException(List.of(new FieldError("roomIds[" + i + "]",
                            "room must have positive capacity", "MIN")));
                }
                capacities.add(room.getCapacity());
            }
            long totalCapacity = capacities.stream().mapToLong(Integer::longValue).sum();
            if (totalCapacity < attendees.size()) {
                throw new ValidationFailedException(List.of(new FieldError("roomIds",
                        "room capacity is smaller than the attendee count", "CAPACITY")));
            }
            OffsetDateTime now = now();
            UUID actor = Actor.current().orElse(null);
            List<ExamResultEntity> changed = new ArrayList<>();
            int roomIndex = 0;
            int assigned = 0;
            for (ExamResultEntity attendee : attendees) {
                while (assigned >= capacities.get(roomIndex)) {
                    roomIndex++;
                    assigned = 0;
                }
                UUID roomId = roomIds.get(roomIndex);
                if (!roomId.equals(attendee.getRoomId())) {
                    if (resultRepository.assignRoom(attendee.getId(), attendee.getRowVersion(), roomId, now, actor) == 0) {
                        throw new PreconditionFailedException("An attendee changed during room allocation.");
                    }
                    audit.recordTransition(ExamResultService.AGGREGATE, attendee.getId(), "allocateRoom",
                            List.of("roomId"), null);
                    outbox.publish(ExamResultService.AGGREGATE + ".roomAllocated", ExamResultService.AGGREGATE,
                            attendee.getId(), AssessmentEvents.payload(attendee.getId()));
                }
                assigned++;
            }
            changed.addAll(resultRepository.listForExam(id));
            return resultList(changed);
        });
    }

    /** Mark a scheduled exam held once at least one attendee row exists. */
    public Exam markExamHeld(UUID id, MarkExamHeldRequest request) {
        return transitionExam(id, "assessment.exam.mark_exam_held", request == null ? null : request.getEtag(),
                Set.of("scheduled"), "markHeld", List.of("status"), () -> {
                    if (resultRepository.countForExam(id) == 0) {
                        throw new InvalidStateException("Populate exam attendees before marking the exam held.");
                    }
                }, repository::markExamHeld);
    }

    /** Close marks entry only when each present attendee has a mark and attendance is no longer expected. */
    public Exam markExamResultsUpdated(UUID id, MarkExamResultsUpdatedRequest request) {
        return transitionExam(id, "assessment.exam.mark_exam_results_updated",
                request == null ? null : request.getEtag(), Set.of("held"), "markResultsUpdated",
                List.of("status", "resultsEntered"), () -> {
                    if (!resultRepository.allMarksEntered(id)) {
                        throw new InvalidStateException("Every attendee must have attendance and marks before results are updated.");
                    }
                }, repository::markExamResultsUpdated);
    }

    /** Record marks through the immutable audit log; batch validation precedes every write. */
    public ExamResultList recordExamMarks(UUID id, RecordExamMarksRequest request) {
        DataScope scope = DataScope.current("assessment.exam.record_exam_marks");
        return doInWriteTransaction(() -> {
            ExamEntity exam = visibleExam(scope, id);
            scope.requireWritable(institutionOf(exam));
            if (!Set.of("held", "result_updated").contains(exam.getStatus())) {
                throw new InvalidStateException("Marks can only be recorded after the exam is held.");
            }
            if (request == null || request.getEntries() == null || request.getEntries().isEmpty()) {
                throw new ValidationFailedException(List.of(new FieldError("entries", "must not be empty", "NotEmpty")));
            }
            Map<UUID, ExamResultEntity> byStudent = new HashMap<>();
            for (ExamResultEntity row : resultRepository.listForExam(id)) {
                byStudent.put(row.getStudentId(), row);
            }
            List<ValidatedMark> marks = new ArrayList<>();
            Set<UUID> seen = new HashSet<>();
            List<FieldError> errors = new ArrayList<>();
            int index = 0;
            for (MarkEntry entry : request.getEntries()) {
                String prefix = "entries[" + index++ + "]";
                if (entry == null || entry.getStudentId() == null || entry.getAttendanceStatus() == null) {
                    errors.add(new FieldError(prefix, "studentId and attendanceStatus are required", "NotNull"));
                    continue;
                }
                UUID studentId = entry.getStudentId();
                if (!seen.add(studentId)) {
                    errors.add(new FieldError(prefix + ".studentId", "student appears more than once", "DUPLICATE"));
                    continue;
                }
                ExamResultEntity current = byStudent.get(studentId);
                if (current == null) {
                    errors.add(new FieldError(prefix + ".studentId", "student is not registered for this exam", "NOT_FOUND"));
                    continue;
                }
                boolean absent = entry.getAttendanceStatus() == MarkEntry.AttendanceStatusEnum.ABSENT;
                BigDecimal value = absent ? null : WireValues.decimal(entry.getMarksObtained());
                if (!absent && value == null) {
                    errors.add(new FieldError(prefix + ".marksObtained", "marks are required for a present student", "NotNull"));
                    continue;
                }
                if (absent && entry.getMarksObtained() != null) {
                    errors.add(new FieldError(prefix + ".marksObtained", "absent students cannot have marks", "INVALID"));
                    continue;
                }
                if (value != null && (value.signum() < 0 || value.compareTo(exam.getTotalMarks()) > 0
                        || value.scale() > 2)) {
                    errors.add(new FieldError(prefix + ".marksObtained", "marks must be from 0 through the exam maximum with at most 2 decimals", "RANGE"));
                    continue;
                }
                String note = entry.getNote() == null || entry.getNote().isBlank() ? null : entry.getNote().trim();
                if (note != null && note.length() > 300) {
                    errors.add(new FieldError(prefix + ".note", "must be at most 300 characters", "Size"));
                    continue;
                }
                String status = absent ? "absent" : (value.compareTo(exam.getPassMarks()) >= 0 ? "pass" : "fail");
                marks.add(new ValidatedMark(current, absent ? "absent" : "present", value, status, note));
            }
            if (!errors.isEmpty()) {
                throw new ValidationFailedException(errors);
            }
            UUID actor = Actor.current().orElseThrow(() -> new IllegalStateException("Authenticated actor is missing."));
            OffsetDateTime now = now();
            boolean anyChanged = false;
            for (ValidatedMark mark : marks) {
                ExamResultEntity current = mark.current();
                if (java.util.Objects.equals(current.getAttendanceStatus(), mark.attendanceStatus())
                        && java.util.Objects.equals(current.getResultStatus(), mark.resultStatus())
                        && equalDecimal(current.getMarks(), mark.marks())
                        && java.util.Objects.equals(current.getNote(), mark.note())) {
                    continue;
                }
                requireUnaggregated(current);
                String reason = mark.note() == null ? "Marks entered through RecordExamMarks" : mark.note();
                if (resultRepository.recordMarks(current, current.getRowVersion(), mark.attendanceStatus(),
                        mark.marks(), exam.getTotalMarks(), mark.resultStatus(), mark.note(), actor, now, reason) == 0) {
                    throw new PreconditionFailedException("An exam result changed while marks were being recorded.");
                }
                anyChanged = true;
                audit.recordTransition(ExamResultService.AGGREGATE, current.getId(), "recordMarks",
                        List.of("attendanceStatus", "marks", "maxMarks", "resultStatus", "note"), reason);
                outbox.publish(ExamResultService.AGGREGATE + ".marksRecorded", ExamResultService.AGGREGATE,
                        current.getId(), AssessmentEvents.payload(current.getId()));
            }
            if (anyChanged && "result_updated".equals(exam.getStatus())
                    && repository.reopenResults(id, exam.getRowVersion(), now, actor) == 0) {
                throw new PreconditionFailedException("The exam changed while marks were being recorded.");
            }
            if (anyChanged && "result_updated".equals(exam.getStatus())) {
                audit.recordTransition(AGGREGATE, id, "reopenResults", List.of("status", "resultsEntered"), null);
                outbox.publish(AGGREGATE + ".resultsReopened", AGGREGATE, id, AssessmentEvents.payload(id));
            }
            return resultList(resultRepository.listForExam(id));
        });
    }

    private Exam transitionExam(UUID id, String permission, String etag, Set<String> states, String action,
            List<String> fields, Runnable before, ExamTransition transition) {
        DataScope scope = DataScope.current(permission);
        return doInWriteTransaction(() -> {
            ExamEntity entity = visibleExam(scope, id);
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(etag);
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!states.contains(entity.getStatus())) {
                throw new InvalidStateException("The exam cannot be changed this way in its current state.");
            }
            before.run();
            OffsetDateTime now = now();
            UUID actor = Actor.current().orElse(null);
            if (transition.apply(id, entity.getRowVersion(), now, actor) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, action, fields, null);
            outbox.publish(AGGREGATE + "." + action, AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    private ExamEntity visibleExam(DataScope scope, UUID id) {
        ExamEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    private ExamResultList resultList(List<ExamResultEntity> entities) {
        ExamResultList response = new ExamResultList();
        response.setItems(entities.stream().map(resultMapper::toDto).toList());
        return response;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    private static boolean equalDecimal(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    static void requireUnaggregated(ExamResultEntity current) {
        if (current.getMarksheetLineId() != null) {
            throw new InvalidStateException("Marks cannot be corrected after the result has been added to a marksheet.");
        }
    }

    private record ValidatedMark(ExamResultEntity current, String attendanceStatus, BigDecimal marks,
            String resultStatus, String note) { }

    @FunctionalInterface
    private interface ExamTransition {
        int apply(UUID id, int expected, OffsetDateTime now, UUID actor);
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
