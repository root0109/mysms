package com.mysms.timetable.business;

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
import com.mysms.core.web.UpdateMask;
import com.mysms.timetable.api.model.BatchConfirmSessionsRequest;
import com.mysms.timetable.api.model.CancelSessionRequest;
import com.mysms.timetable.api.model.CompleteSessionRequest;
import com.mysms.timetable.api.model.ConfirmSessionRequest;
import com.mysms.timetable.api.model.GenerateSessionsRequest;
import com.mysms.timetable.api.model.Operation;
import com.mysms.timetable.api.model.ResetSessionRequest;
import com.mysms.timetable.api.model.Session;
import com.mysms.timetable.api.model.SessionList;
import com.mysms.timetable.api.model.SessionStudent;
import com.mysms.timetable.api.model.SessionStudentUpdate;
import com.mysms.timetable.api.model.SessionUpdate;
import com.mysms.timetable.api.model.TimetableView;
import com.mysms.timetable.api.model.WeeklySlot;
import com.mysms.timetable.config.TimetableDatabaseConfig;
import com.mysms.timetable.config.TimetableEventsConfig;
import com.mysms.timetable.domain.SessionEntity;
import com.mysms.timetable.domain.SessionStudentEntity;
import com.mysms.timetable.events.TimetableEvents;
import com.mysms.timetable.mapper.SessionMapper;
import com.mysms.timetable.mapper.SessionStudentMapper;
import com.mysms.timetable.repository.InstitutionZoneRepository;
import com.mysms.timetable.repository.PeriodRow;
import com.mysms.timetable.repository.SessionRepository;
import com.mysms.timetable.repository.SessionStudentRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of timetable.session and of the records that exist only as part of a Session (SessionStudent): audit
 * and domain event commit in the same transaction as every change. Rows outside the caller's institutions are
 * invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class SessionService extends AppTransactionManager {

    public static final String AGGREGATE = SessionEntity.TABLE;
    static final String PERMISSION_LIST = "timetable.session.read";
    static final String PERMISSION_GET = "timetable.session.read";
    static final String PERMISSION_CREATE = "timetable.session.create";
    static final String PERMISSION_UPDATE = "timetable.session.update";
    static final String PERMISSION_DELETE = "timetable.session.delete";
    static final String PERMISSION_GET_TIMETABLE = "timetable.session.get_timetable";
    /** The longest stretch of days one timetable view covers (422 beyond): a term is read month by month. */
    static final int MAX_TIMETABLE_DAYS = 62;
    static final String PERMISSION_CONFIRM_SESSION = "timetable.session.confirm_session";
    static final String PERMISSION_COMPLETE_SESSION = "timetable.session.complete_session";
    static final String PERMISSION_CANCEL_SESSION = "timetable.session.cancel_session";
    static final String PERMISSION_RESET_SESSION = "timetable.session.reset_session";
    static final String PERMISSION_GENERATE_SESSIONS = "timetable.session.generate_sessions";
    static final String PERMISSION_BATCH_CONFIRM = "timetable.session.batch_confirm_sessions";
    /** The operation kind a generation run is recorded under. */
    static final String KIND_GENERATE = "timetable.GenerateSessions";
    /** Generation limits (422 beyond): a school year at a time, a week of slots, and the meetings they make. */
    static final int MAX_GENERATE_DAYS = 366;
    static final int MAX_SLOTS = 100;
    static final int MAX_OCCURRENCES = 5000;
    /** The clashes a run reports one by one; the count covers the rest. */
    static final int MAX_REPORTED_CLASHES = 200;
    static final int MAX_BATCH_CONFIRM = 500;
    /**
     * How far apart a generated meeting and a live session of the same class, period and weekday may start and still
     * be the same meeting: well under the week between two meetings of a slot, well over any gap between time zones.
     */
    static final Duration SAME_MEETING_WINDOW = Duration.ofDays(3);
    /** The contract's SessionUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "periodId", "startAt", "endAt", "weekday", "classSectionId", "courseId", "batchId",
            "subjectId", "staffId", "roomId", "title", "sessionType", "color");

    public static final String SESSION_STUDENT_AGGREGATE = SessionStudentEntity.TABLE;
    static final String SESSION_STUDENT_PERMISSION_LIST = "timetable.session_student.read";
    static final String SESSION_STUDENT_PERMISSION_GET = "timetable.session_student.read";
    static final String SESSION_STUDENT_PERMISSION_CREATE = "timetable.session_student.create";
    static final String SESSION_STUDENT_PERMISSION_UPDATE = "timetable.session_student.update";
    static final String SESSION_STUDENT_PERMISSION_DELETE = "timetable.session_student.delete";
    /** The contract's SessionStudentUpdate fields, in apply order. */
    static final List<String> SESSION_STUDENT_WRITABLE = List.of("studentId");

    private final SessionRepository repository;
    private final SessionMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final SessionStudentRepository sessionStudentRepository;
    private final SessionStudentMapper sessionStudentMapper;
    private final OperationService operations;
    private final InstitutionZoneRepository zones;

    public SessionService(SessionRepository repository,
            SessionMapper mapper,
            @Qualifier(TimetableDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(TimetableDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(TimetableEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(TimetableEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            SessionStudentRepository sessionStudentRepository,
            SessionStudentMapper sessionStudentMapper,
            OperationService operations,
            InstitutionZoneRepository zones) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.sessionStudentRepository = sessionStudentRepository;
        this.sessionStudentMapper = sessionStudentMapper;
        this.operations = operations;
        this.zones = zones;
    }

    public Session getSession(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Session> listSessions(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, SessionRepository.SORT_FIELDS, SessionRepository.DEFAULT_ORDER,
                SessionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, SessionRepository.FILTER_FIELDS)
                .and(SessionRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<SessionEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /**
     * timetable.GetTimetable: the published timetable of a class, batch, teacher, student or room (at least one) from
     * fromDate to toDate, in start order. Days are UTC days, since this module does not know an institution's time
     * zone: a client showing local days asks for one more day on each side. Only the caller's institutions are
     * answered; drafts are left out, and other modules' names are not filled. The student filter also needs the
     * permission to read session students (403 without it), because which sessions list a student is that student's own
     * data. Until the view knows the caller's own student or staff record it is a grant for staff roles.
     */
    public Page<TimetableView> getTimetable(LocalDate fromDate, LocalDate toDate, UUID classSectionId, UUID batchId,
            UUID staffId, UUID studentId, UUID roomId, Integer pageNumber, Integer pageSize, String cursor) {
        List<FieldError> errors = new ArrayList<>();
        if (outOfCalendar(fromDate)) {
            errors.add(new FieldError("fromDate", "must be a year between 1900 and 9999", "DateRange"));
        }
        if (outOfCalendar(toDate)) {
            errors.add(new FieldError("toDate", "must be a year between 1900 and 9999", "DateRange"));
        } else if (toDate.isBefore(fromDate)) {
            errors.add(new FieldError("toDate", "must not be before fromDate", "DateRange"));
        } else if (ChronoUnit.DAYS.between(fromDate, toDate) >= MAX_TIMETABLE_DAYS) {
            errors.add(new FieldError("toDate", "the days fromDate to toDate may be at most " + MAX_TIMETABLE_DAYS,
                    "DateRange"));
        }
        if (classSectionId == null && batchId == null && staffId == null && studentId == null && roomId == null) {
            errors.add(new FieldError("classSectionId",
                    "one of classSectionId, batchId, staffId, studentId or roomId is required", "Required"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        if (studentId != null && !AuthContext.require().hasPermission(SESSION_STUDENT_PERMISSION_LIST)) {
            throw new PermissionDeniedException(
                    "A student's own sessions also need " + SESSION_STUDENT_PERMISSION_LIST + ".");
        }
        SqlFragment where = SessionRepository.timetableBetween(fromDate.atStartOfDay().atOffset(ZoneOffset.UTC),
                        toDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC))
                .and(SessionRepository.scope(DataScope.current(PERMISSION_GET_TIMETABLE)));
        if (classSectionId != null) {
            where = where.and(SessionRepository.ofClassSection(classSectionId));
        }
        if (batchId != null) {
            where = where.and(SessionRepository.ofBatch(batchId));
        }
        if (staffId != null) {
            where = where.and(SessionRepository.ofStaff(staffId));
        }
        if (roomId != null) {
            where = where.and(SessionRepository.ofRoom(roomId));
        }
        if (studentId != null) {
            where = where.and(SessionRepository.listingStudent(studentId));
        }
        SortSpec sort = SortSpec.parse(null, SessionRepository.TIMETABLE_SORT_FIELDS, SessionRepository.TIMETABLE_ORDER,
                SessionRepository.ID_COLUMN);
        // the page token is bound to the view it was issued for
        String view = String.join("|", fromDate.toString(), toDate.toString(), String.valueOf(classSectionId),
                String.valueOf(batchId), String.valueOf(staffId), String.valueOf(studentId), String.valueOf(roomId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, view, clock);
        SqlFragment filter = where;
        return doInReadTransaction(() -> {
            Page<SessionEntity> rows = repository.list(page, filter);
            return rows.map(row -> new TimetableView(mapper.toDto(row)));
        });
    }

    /** Whether a day lies outside the years a timetable can be asked for (and a timestamp can hold). */
    private static boolean outOfCalendar(LocalDate day) {
        return day.getYear() < 1900 || day.getYear() > 9999;
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Session createSession(Session request, boolean validateOnly) {
        validator.validate(request);
        SessionEntity entity = mapper.toEntity(request);
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
            SessionEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + TimetableEvents.CREATED, AGGREGATE, stored.getId(),
                    TimetableEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public Session updateSession(UUID id, String ifMatch, SessionUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            SessionEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + TimetableEvents.UPDATED, AGGREGATE, id, TimetableEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteSession(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            SessionEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedBySessionStudentSessionId(id)) {
                throw new InvalidStateException(
                        "The session is still referenced by timetable.session_student rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + TimetableEvents.DELETED, AGGREGATE, id, TimetableEvents.payload(id));
        });
    }

    /**
     * timetable.ConfirmSession: Confirm. Allowed while status is draft, on a row the caller may see and write;
     * otherwise 409 and nothing changes.
     */
    public Session confirmSession(UUID id, ConfirmSessionRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CONFIRM_SESSION);
        return doInWriteTransaction(() -> {
            SessionEntity entity = repository.findById(id)
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
                throw new InvalidStateException("The session cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.confirmSession(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "confirm", List.of("status"), null);
            outbox.publish(AGGREGATE + ".confirm", AGGREGATE, id, TimetableEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * timetable.CompleteSession: Mark done. Allowed while status is confirmed, on a row the caller may see and write;
     * otherwise 409 and nothing changes.
     */
    public Session completeSession(UUID id, CompleteSessionRequest request) {
        DataScope scope = DataScope.current(PERMISSION_COMPLETE_SESSION);
        return doInWriteTransaction(() -> {
            SessionEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("confirmed").contains(entity.getStatus()))) {
                throw new InvalidStateException("The session cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.completeSession(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "complete", List.of("status"), null);
            outbox.publish(AGGREGATE + ".complete", AGGREGATE, id, TimetableEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * timetable.CancelSession: Cancel. Allowed while status is draft or confirmed, on a row the caller may see and
     * write; otherwise 409 and nothing changes.
     */
    public Session cancelSession(UUID id, CancelSessionRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CANCEL_SESSION);
        return doInWriteTransaction(() -> {
            SessionEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("draft", "confirmed").contains(entity.getStatus()))) {
                throw new InvalidStateException("The session cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.cancelSession(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "cancel", List.of("status"), request == null ? null
                    : request.getReason());
            outbox.publish(AGGREGATE + ".cancel", AGGREGATE, id, TimetableEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * timetable.ResetSession: Back to draft. Allowed while status is cancelled, on a row the caller may see and write;
     * otherwise 409 and nothing changes.
     */
    public Session resetSession(UUID id, ResetSessionRequest request) {
        DataScope scope = DataScope.current(PERMISSION_RESET_SESSION);
        return doInWriteTransaction(() -> {
            SessionEntity entity = repository.findById(id)
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
                throw new InvalidStateException("The session cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.resetSession(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "reset", List.of("status"), null);
            outbox.publish(AGGREGATE + ".reset", AGGREGATE, id, TimetableEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private SessionEntity loadVisible(DataScope scope, UUID id) {
        SessionEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    /**
     * timetable.GenerateSessions: writes the draft sessions a weekly pattern means between two dates, each slot at its
     * period's local start time in the school's own time zone (from org); the request names the zone only for a school
     * without one, and naming another zone than the school's is 422. Idempotent per class, period and day: a meeting
     * that already exists is kept, a draft of it at another time (a run in another zone) is moved, never duplicated, so
     * a re-run writes nothing new. A meeting that would overlap another live session of the institution with the same
     * teacher, room or class is not written but reported (the operation's {@code result}: created, kept, replaced,
     * clashes). Runs as a long-running operation recorded in timetable.operation, answered finished.
     */
    public Operation generateSessions(GenerateSessionsRequest request) {
        validator.validate(request);
        checkGeneration(request);
        UUID institution = request.getInstitutionId();
        DataScope scope = DataScope.current(PERMISSION_GENERATE_SESSIONS);
        scope.requireWritable(Optional.of(institution));
        List<UUID> periodIds = request.getSlots().stream().map(WeeklySlot::getPeriodId).distinct().toList();
        ZoneId zone = doInReadTransaction(() -> {
            slotsOf(request, periodRows(repository.periods(periodIds), institution));
            return zoneOf(institution, request.getTimeZone());
        });
        UUID operation = operations.start(KIND_GENERATE, "org.institution", institution);
        try {
            Map<String, Object> outcome = doInWriteTransaction(() -> {
                Map<UUID, PeriodRow> periods = periodRows(repository.lockPeriods(periodIds), institution);
                List<WeeklyPattern.Occurrence> meetings = WeeklyPattern.expand(slotsOf(request, periods),
                        request.getStartDate(), request.getEndDate(), zone);
                return writeMeetings(request, meetings);
            });
            return operations.succeed(operation, outcome);
        } catch (RuntimeException e) {
            operations.fail(operation, "INTERNAL", "The sessions could not be generated; nothing was written. Check "
                    + "the pattern and run it again.");
            throw e;
        }
    }

    /**
     * timetable.BatchConfirmSessions: confirms up to 500 sessions at once, all or nothing. Each must be a live session
     * the caller may write (422 {@code sessionIds[i]} for one they cannot see); a confirmed one stays as it is, and a
     * done or cancelled one refuses the whole batch (409). Answers the sessions in the order asked.
     */
    public SessionList batchConfirmSessions(BatchConfirmSessionsRequest request) {
        validator.validate(request);
        List<UUID> ids = request.getSessionIds();
        List<FieldError> shape = new ArrayList<>();
        if (ids.isEmpty() || ids.size() > MAX_BATCH_CONFIRM) {
            shape.add(new FieldError("sessionIds", "name between 1 and " + MAX_BATCH_CONFIRM + " sessions", "Size"));
        } else if (ids.stream().distinct().count() != ids.size()) {
            shape.add(new FieldError("sessionIds", "names a session more than once", "Unique"));
        }
        if (!shape.isEmpty()) {
            throw new ValidationFailedException(shape);
        }
        DataScope scope = DataScope.current(PERMISSION_BATCH_CONFIRM);
        return doInWriteTransaction(() -> {
            Map<UUID, SessionEntity> found = new HashMap<>();
            repository.lockByIds(ids).forEach(session -> found.put(session.getId(), session));
            List<FieldError> missing = new ArrayList<>();
            for (int i = 0; i < ids.size(); i++) {
                SessionEntity session = found.get(ids.get(i));
                if (session == null || !scope.canRead(institutionOf(session))) {
                    missing.add(new FieldError("sessionIds[" + i + "]", "session does not exist", "NOT_FOUND"));
                }
            }
            if (!missing.isEmpty()) {
                throw new ValidationFailedException(missing);
            }
            List<UUID> drafts = new ArrayList<>();
            for (UUID id : ids) {
                SessionEntity session = found.get(id);
                scope.requireWritable(institutionOf(session));
                if ("draft".equals(session.getStatus())) {
                    drafts.add(id);
                } else if (!"confirmed".equals(session.getStatus())) {
                    throw new InvalidStateException("A session that is done or cancelled cannot be confirmed; "
                            + "leave it out of the batch.");
                }
            }
            if (!drafts.isEmpty()) {
                OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                        .truncatedTo(ChronoUnit.MICROS);
                if (repository.confirmDrafts(drafts, now, Actor.current().orElse(null)) != drafts.size()) {
                    throw new PreconditionFailedException("A session changed while it was confirmed; try again.");
                }
                for (UUID id : drafts) {
                    audit.recordTransition(AGGREGATE, id, "confirm", List.of("status"), null);
                    outbox.publish(AGGREGATE + ".confirm", AGGREGATE, id, TimetableEvents.payload(id));
                }
            }
            Map<UUID, SessionEntity> confirmed = new HashMap<>();
            repository.lockByIds(ids).forEach(session -> confirmed.put(session.getId(), session));
            SessionList list = new SessionList();
            ids.forEach(id -> list.addItemsItem(mapper.toDto(confirmed.get(id))));
            return list;
        });
    }

    /** The request's shape beyond the contract's: dates, limits, the class, the slots, a named time zone. */
    private static void checkGeneration(GenerateSessionsRequest request) {
        List<FieldError> errors = new ArrayList<>();
        if (request.getTimeZone() != null) {
            try {
                ZoneId.of(request.getTimeZone());
            } catch (DateTimeException e) {
                errors.add(new FieldError("timeZone", "is not a known time zone", "TIME_ZONE"));
            }
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            errors.add(new FieldError("endDate", "must not be before startDate", "DATE_RANGE"));
        } else if (ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) >= MAX_GENERATE_DAYS) {
            errors.add(new FieldError("endDate", "at most " + MAX_GENERATE_DAYS + " days at a time", "DATE_RANGE"));
        }
        if (request.getClassSectionId() == null && (request.getCourseId() == null || request.getBatchId() == null)) {
            errors.add(new FieldError("classSectionId", "name a class section, or a course and a batch", "REQUIRED"));
        }
        List<WeeklySlot> slots = request.getSlots();
        if (slots.isEmpty() || slots.size() > MAX_SLOTS) {
            errors.add(new FieldError("slots", "name between 1 and " + MAX_SLOTS + " slots", "Size"));
        }
        for (int i = 0; i < slots.size(); i++) {
            Integer weekday = slots.get(i).getWeekday();
            if (weekday < 1 || weekday > 7) {
                errors.add(new FieldError("slots[" + i + "].weekday", "must be an ISO weekday from 1 to 7", "Range"));
            }
        }
        if (errors.isEmpty() && WeeklyPattern.count(slots.stream().map(WeeklySlot::getWeekday).toList(),
                request.getStartDate(), request.getEndDate()) > MAX_OCCURRENCES) {
            errors.add(new FieldError("slots", "the pattern makes more than " + MAX_OCCURRENCES
                    + " sessions; generate a shorter stretch", "Size"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    /**
     * The zone the school's periods are in: its own from org; the requested one only while org gave it none (422 when
     * neither names one, or when the request names another zone than the school's).
     */
    private ZoneId zoneOf(UUID institution, String requested) {
        Optional<ZoneId> asked = Optional.ofNullable(requested).map(ZoneId::of);
        Optional<ZoneId> own = zones.zoneOf(institution).map(ZoneId::of);
        if (own.isPresent()) {
            if (asked.isPresent() && !asked.get().getRules().equals(own.get().getRules())) {
                throw new ValidationFailedException(List.of(new FieldError("timeZone",
                        "differs from the school's time zone " + own.get().getId() + "; leave it out", "TIME_ZONE")));
            }
            return own.get();
        }
        return asked.orElseThrow(() -> new ValidationFailedException(List.of(new FieldError("timeZone",
                "the school has no time zone of its own; name the one its periods are in", "REQUIRED"))));
    }

    private static Map<UUID, PeriodRow> periodRows(List<PeriodRow> rows, UUID institution) {
        Map<UUID, PeriodRow> periods = new HashMap<>();
        rows.stream().filter(row -> institution.equals(row.institutionId())).forEach(row -> periods.put(row.id(), row));
        return periods;
    }

    /** The request's slots with their periods; 422 for a period that is not the institution's or has no length. */
    private static List<WeeklyPattern.Slot> slotsOf(GenerateSessionsRequest request, Map<UUID, PeriodRow> periods) {
        List<FieldError> errors = new ArrayList<>();
        List<WeeklyPattern.Slot> slots = new ArrayList<>();
        for (int i = 0; i < request.getSlots().size(); i++) {
            WeeklySlot slot = request.getSlots().get(i);
            PeriodRow period = periods.get(slot.getPeriodId());
            if (period == null) {
                errors.add(new FieldError("slots[" + i + "].periodId", "period does not exist", "NOT_FOUND"));
            } else if (period.durationMinutes() == null) {
                errors.add(new FieldError("slots[" + i + "].periodId", "the period has no duration", "REQUIRED"));
            } else {
                long seconds = period.durationMinutes().multiply(BigDecimal.valueOf(60)).longValue();
                slots.add(new WeeklyPattern.Slot(slot.getWeekday(), period.id(), period.startTime(),
                        Duration.ofSeconds(seconds), slot.getSubjectId(), slot.getStaffId(), slot.getRoomId()));
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        return slots;
    }

    /** Writes the meetings that are neither there already nor clashing; answers the run's outcome. */
    private Map<String, Object> writeMeetings(GenerateSessionsRequest request,
            List<WeeklyPattern.Occurrence> meetings) {
        List<SessionEntity> live = meetings.isEmpty() ? new ArrayList<>()
                : new ArrayList<>(repository.liveOverlapping(request.getInstitutionId(),
                        meetings.get(0).startAt().minus(SAME_MEETING_WINDOW),
                        meetings.get(meetings.size() - 1).endAt().plus(SAME_MEETING_WINDOW)));
        int created = 0;
        int kept = 0;
        int replaced = 0;
        int clashCount = 0;
        List<Map<String, Object>> clashes = new ArrayList<>();
        Set<UUID> matched = new HashSet<>();
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        for (WeeklyPattern.Occurrence meeting : meetings) {
            SessionEntity candidate = sessionOf(request, meeting, tenant);
            Optional<SessionEntity> prior = live.stream()
                    .filter(other -> !matched.contains(other.getId()) && sameSlot(other, candidate)).findFirst();
            prior.ifPresent(found -> matched.add(found.getId()));
            if (prior.isPresent() && sameMeeting(prior.get(), candidate)) {
                kept++;
                continue;
            }
            List<SessionEntity> others = prior.isEmpty() ? live
                    : live.stream().filter(other -> !other.getId().equals(prior.get().getId())).toList();
            Optional<Map<String, Object>> clash = prior.isPresent() && !"draft".equals(prior.get().getStatus())
                    ? Optional.of(clash(meeting, "class", prior.get()))
                    : clashOf(candidate, meeting, others);
            if (clash.isPresent()) {
                clashCount++;
                if (clashes.size() < MAX_REPORTED_CLASHES) {
                    clashes.add(clash.get());
                }
                continue;
            }
            if (prior.isPresent()) {
                live.set(live.indexOf(prior.get()), moved(prior.get(), candidate));
                replaced++;
                continue;
            }
            candidate.onCreatingData(Actor.current(), clock);
            SessionEntity stored = repository.insert(candidate);
            live.add(stored);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + TimetableEvents.CREATED, AGGREGATE, stored.getId(),
                    TimetableEvents.payload(stored.getId()));
            created++;
        }
        Map<String, Object> outcome = new LinkedHashMap<>();
        outcome.put("created", created);
        outcome.put("kept", kept);
        outcome.put("replaced", replaced);
        outcome.put("clashCount", clashCount);
        outcome.put("clashes", clashes);
        return outcome;
    }

    private static SessionEntity sessionOf(GenerateSessionsRequest request, WeeklyPattern.Occurrence meeting,
            UUID tenant) {
        SessionEntity session = new SessionEntity();
        session.setTenantId(tenant);
        session.setInstitutionId(request.getInstitutionId());
        session.setPeriodId(meeting.slot().periodId());
        session.setStartAt(meeting.startAt());
        session.setEndAt(meeting.endAt());
        session.setWeekday(meeting.slot().weekday());
        session.setClassSectionId(request.getClassSectionId());
        session.setCourseId(request.getCourseId());
        session.setBatchId(request.getBatchId());
        session.setSubjectId(meeting.slot().subjectId());
        session.setStaffId(meeting.slot().staffId());
        session.setRoomId(meeting.slot().roomId());
        return session;
    }

    /** A draft session takes the generated meeting's time (412 when it changed meanwhile). */
    private SessionEntity moved(SessionEntity draft, SessionEntity meeting) {
        int expected = draft.getRowVersion();
        draft.setStartAt(meeting.getStartAt());
        draft.setEndAt(meeting.getEndAt());
        draft.onUpdatingData(Actor.current(), clock);
        if (repository.update(draft, expected) == 0) {
            throw new PreconditionFailedException("A session changed while the pattern was generated; run it again.");
        }
        draft.setRowVersion(expected + 1);
        audit.recordUpdate(AGGREGATE, draft.getId(), List.of("startAt", "endAt"));
        outbox.publish(AGGREGATE + TimetableEvents.UPDATED, AGGREGATE, draft.getId(),
                TimetableEvents.payload(draft.getId()));
        return draft;
    }

    /**
     * The class's meeting of the same subject, teacher and room in the same period on the same weekday of the same
     * week, whatever time zone placed it.
     */
    private static boolean sameSlot(SessionEntity live, SessionEntity meeting) {
        return sameClass(meeting, live) && Objects.equals(live.getPeriodId(), meeting.getPeriodId())
                && Objects.equals(live.getWeekday(), meeting.getWeekday())
                && Objects.equals(live.getSubjectId(), meeting.getSubjectId())
                && Objects.equals(live.getStaffId(), meeting.getStaffId())
                && Objects.equals(live.getRoomId(), meeting.getRoomId())
                && Duration.between(live.getStartAt(), meeting.getStartAt()).abs().compareTo(SAME_MEETING_WINDOW) < 0;
    }

    /** The same class meeting the same subject with the same teacher in the same room at the same time. */
    private static boolean sameMeeting(SessionEntity a, SessionEntity b) {
        return a.getStartAt().isEqual(b.getStartAt()) && a.getEndAt().isEqual(b.getEndAt())
                && Objects.equals(a.getClassSectionId(), b.getClassSectionId())
                && Objects.equals(a.getCourseId(), b.getCourseId()) && Objects.equals(a.getBatchId(), b.getBatchId())
                && Objects.equals(a.getSubjectId(), b.getSubjectId()) && Objects.equals(a.getStaffId(), b.getStaffId())
                && Objects.equals(a.getRoomId(), b.getRoomId());
    }

    /** The first live session the meeting would overlap with its teacher, room or class, as reported. */
    private static Optional<Map<String, Object>> clashOf(SessionEntity candidate, WeeklyPattern.Occurrence meeting,
            List<SessionEntity> live) {
        for (SessionEntity other : live) {
            boolean overlaps = other.getStartAt().isBefore(candidate.getEndAt())
                    && other.getEndAt().isAfter(candidate.getStartAt());
            if (!overlaps) {
                continue;
            }
            String on = candidate.getStaffId() != null && candidate.getStaffId().equals(other.getStaffId()) ? "staff"
                    : candidate.getRoomId() != null && candidate.getRoomId().equals(other.getRoomId()) ? "room"
                    : sameClass(other, candidate) ? "class" : null;
            if (on != null) {
                return Optional.of(clash(meeting, on, other));
            }
        }
        return Optional.empty();
    }

    private static Map<String, Object> clash(WeeklyPattern.Occurrence meeting, String on, SessionEntity other) {
        Map<String, Object> clash = new LinkedHashMap<>();
        clash.put("date", meeting.date().toString());
        clash.put("weekday", meeting.slot().weekday());
        clash.put("periodId", meeting.slot().periodId().toString());
        clash.put("on", on);
        clash.put("sessionId", other.getId().toString());
        return clash;
    }

    private static boolean sameClass(SessionEntity a, SessionEntity b) {
        return a.getClassSectionId() != null ? a.getClassSectionId().equals(b.getClassSectionId())
                : a.getCourseId() != null && a.getCourseId().equals(b.getCourseId())
                        && Objects.equals(a.getBatchId(), b.getBatchId());
    }

    private Optional<UUID> institutionOf(SessionEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected SessionUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(SessionEntity entity, SessionUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "periodId" -> entity.setPeriodId(changes.getPeriodId());
                case "startAt" -> entity.setStartAt(changes.getStartAt());
                case "endAt" -> entity.setEndAt(changes.getEndAt());
                case "weekday" -> entity.setWeekday(changes.getWeekday());
                case "classSectionId" -> entity.setClassSectionId(changes.getClassSectionId());
                case "courseId" -> entity.setCourseId(changes.getCourseId());
                case "batchId" -> entity.setBatchId(changes.getBatchId());
                case "subjectId" -> entity.setSubjectId(changes.getSubjectId());
                case "staffId" -> entity.setStaffId(changes.getStaffId());
                case "roomId" -> entity.setRoomId(changes.getRoomId());
                case "title" -> entity.setTitle(changes.getTitle());
                case "sessionType" -> entity.setSessionType(changes.getSessionType());
                case "color" -> entity.setColor(changes.getColor());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(SessionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getStartAt() == null) {
            errors.add(new FieldError("startAt", "must not be null", "NotNull"));
        }
        if (entity.getEndAt() == null) {
            errors.add(new FieldError("endAt", "must not be null", "NotNull"));
        }
        if (entity.getPeriodId() != null && !repository.existsPeriodId(entity.getPeriodId())) {
            errors.add(new FieldError("periodId", "period does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(SessionEntity entity) {

    }

    // ---- SessionStudent ----

    public SessionStudent getSessionStudent(UUID sessionId, UUID id) {
        return doInReadTransaction(() -> {
            return sessionStudentMapper.toDto(loadVisibleSessionStudent(DataScope.current(SESSION_STUDENT_PERMISSION_GET),
                    sessionId, id));
        });
    }

    public Page<SessionStudent> listSessionStudents(UUID sessionId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, SessionStudentRepository.SORT_FIELDS, SessionStudentRepository.DEFAULT_ORDER,
                SessionStudentRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, SessionStudentRepository.FILTER_FIELDS)
                .and(SessionStudentRepository.ofParent(sessionId))
                        .and(SessionStudentRepository.scope(DataScope.current(SESSION_STUDENT_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + sessionId, clock);
        return doInReadTransaction(() -> {
            Page<SessionStudentEntity> rows = sessionStudentRepository.list(page, where);
            return rows.map(sessionStudentMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public SessionStudent createSessionStudent(UUID sessionId, SessionStudent request, boolean validateOnly) {
        validator.validate(request);
        SessionStudentEntity entity = sessionStudentMapper.toEntity(request);
        entity.setSessionId(sessionId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(SESSION_STUDENT_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!sessionStudentRepository.existsSessionId(entity.getSessionId())) {
                throw new ResourceNotFoundException("timetable.session", entity.getSessionId());
            }
            if (!scope.canRead(institutionOfSessionStudent(entity))) {
                throw new ResourceNotFoundException("timetable.session", entity.getSessionId());
            }
            assertValidSessionStudent(entity, true);
            scope.requireWritable(institutionOfSessionStudent(entity));
            assertUniqueSessionStudent(entity);
            if (validateOnly) {
                return sessionStudentMapper.toDto(entity);
            }
            SessionStudentEntity stored = sessionStudentRepository.insert(entity);
            audit.recordCreate(SESSION_STUDENT_AGGREGATE, stored.getId());
            outbox.publish(SESSION_STUDENT_AGGREGATE + TimetableEvents.CREATED, SESSION_STUDENT_AGGREGATE, stored.getId(),
                    TimetableEvents.payload(stored.getId()));
            return sessionStudentMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public SessionStudent updateSessionStudent(UUID sessionId, UUID id, String ifMatch, SessionStudentUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, SESSION_STUDENT_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(SESSION_STUDENT_PERMISSION_UPDATE);
            SessionStudentEntity entity = loadVisibleSessionStudent(scope, sessionId, id);
            scope.requireWritable(institutionOfSessionStudent(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applySessionStudent(entity, changes, fields);
            validator.validate(sessionStudentMapper.toDto(entity));
            assertValidSessionStudent(entity, false);
            scope.requireWritable(institutionOfSessionStudent(entity));
            assertUniqueSessionStudent(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return sessionStudentMapper.toDto(entity);
            }
            if (sessionStudentRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(SESSION_STUDENT_AGGREGATE, id, fields);
            outbox.publish(SESSION_STUDENT_AGGREGATE + TimetableEvents.UPDATED, SESSION_STUDENT_AGGREGATE, id,
                    TimetableEvents.payload(id));
            return sessionStudentMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteSessionStudent(UUID sessionId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(SESSION_STUDENT_PERMISSION_DELETE);
            SessionStudentEntity entity = loadVisibleSessionStudent(scope, sessionId, id);
            scope.requireWritable(institutionOfSessionStudent(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (sessionStudentRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(SESSION_STUDENT_AGGREGATE, id);
            outbox.publish(SESSION_STUDENT_AGGREGATE + TimetableEvents.DELETED, SESSION_STUDENT_AGGREGATE, id,
                    TimetableEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private SessionStudentEntity loadVisibleSessionStudent(DataScope scope, UUID sessionId, UUID id) {
        SessionStudentEntity entity = sessionStudentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(SESSION_STUDENT_AGGREGATE, id));
        if (!scope.canRead(institutionOfSessionStudent(entity)) || !sessionId.equals(entity.getSessionId())) {
            throw new ResourceNotFoundException(SESSION_STUDENT_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the timetable.session the row belongs to. */
    private Optional<UUID> institutionOfSessionStudent(SessionStudentEntity entity) {
        return sessionStudentRepository.institutionOf(entity.getSessionId());
    }

    /** Applies the selected SessionStudentUpdate fields; explicit per field so a mask can clear values. */
    private static void applySessionStudent(SessionStudentEntity entity, SessionStudentUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "studentId" -> entity.setStudentId(changes.getStudentId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidSessionStudent(SessionStudentEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getSessionId() == null) {
            errors.add(new FieldError("sessionId", "must not be null", "NotNull"));
        }
        if (entity.getStudentId() == null) {
            errors.add(new FieldError("studentId", "must not be null", "NotNull"));
        }
        if (entity.getSessionId() != null && !sessionStudentRepository.existsSessionId(entity.getSessionId())) {
            errors.add(new FieldError("sessionId", "session does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueSessionStudent(SessionStudentEntity entity) {
        if (sessionStudentRepository.takenSessionIdStudentId1(entity)) {
            throw new DuplicateResourceException(
                    "A session student with the same session id and student id already exists.");
        }
    }
}
