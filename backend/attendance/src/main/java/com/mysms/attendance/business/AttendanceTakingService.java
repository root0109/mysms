package com.mysms.attendance.business;

import com.mysms.attendance.api.model.AttendanceMark;
import com.mysms.attendance.api.model.AttendanceRecordList;
import com.mysms.attendance.api.model.AttendanceSheet;
import com.mysms.attendance.api.model.CompleteAttendanceSheetRequest;
import com.mysms.attendance.api.model.RecordAttendanceRequest;
import com.mysms.attendance.api.model.TakeAttendanceForSessionRequest;
import com.mysms.attendance.config.AttendanceDatabaseConfig;
import com.mysms.attendance.config.AttendanceEventsConfig;
import com.mysms.attendance.domain.AttendanceRecordEntity;
import com.mysms.attendance.domain.AttendanceRegisterEntity;
import com.mysms.attendance.domain.AttendanceSheetEntity;
import com.mysms.attendance.events.AttendanceEvents;
import com.mysms.attendance.mapper.AttendanceRecordMapper;
import com.mysms.attendance.mapper.AttendanceSheetMapper;
import com.mysms.attendance.repository.AttendanceRecordRepository;
import com.mysms.attendance.repository.AttendanceRecordRepository.MarkType;
import com.mysms.attendance.repository.AttendanceRegisterRepository;
import com.mysms.attendance.repository.AttendanceSheetRepository;
import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Taking attendance on a sheet: open the sheet of a timetable session with its expected students marked present, record
 * or correct marks while it is in progress, and complete it. A sheet and its marks are one aggregate: every write locks
 * the sheet row first, so recording and completing never interleave, and every recording gives the sheet a new version,
 * so a stale etag is told to reload (412). A sheet outside the caller's institutions is 404; a change needs the
 * institution in scope.
 */
@Service
public class AttendanceTakingService extends AppTransactionManager {

    static final String AGGREGATE = AttendanceSheetEntity.TABLE;
    static final String PERMISSION_RECORD = "attendance.attendance_sheet.record_attendance";
    static final String PERMISSION_COMPLETE = "attendance.attendance_sheet.complete_attendance_sheet";
    static final String PERMISSION_TAKE_FOR_SESSION = "attendance.attendance_sheet.take_attendance_for_session";
    /** Marks one call may carry: a page of the roster, the list page size limit. */
    static final int MAX_MARKS = 500;
    /** Width of attendance.attendance_record.remark. */
    static final int MAX_REMARK = 255;
    private static final String IN_PROGRESS = "in_progress";
    private static final Set<String> REASONED = Set.of("absent", "excused");

    private final AttendanceSheetRepository sheets;
    private final AttendanceRecordRepository records;
    private final AttendanceRegisterRepository registers;
    private final AttendanceSheetMapper sheetMapper;
    private final AttendanceRecordMapper recordMapper;
    private final SessionLookup sessions;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public AttendanceTakingService(AttendanceSheetRepository sheets, AttendanceRecordRepository records,
            AttendanceRegisterRepository registers, AttendanceSheetMapper sheetMapper,
            AttendanceRecordMapper recordMapper, SessionLookup sessions,
            @Qualifier(AttendanceDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AttendanceDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AttendanceEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AttendanceEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.sheets = sheets;
        this.records = records;
        this.registers = registers;
        this.sheetMapper = sheetMapper;
        this.recordMapper = recordMapper;
        this.sessions = sessions;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    /**
     * attendance.RecordAttendance: sets one mark per student on a sheet in progress (a first mark or a correction; an
     * unchanged mark is not written). All-or-nothing: one invalid mark refuses every mark, with a field error per
     * problem ({@code marks[i].field}). <p>A first mark for a student the sheet does not know is checked with sis as a
     * student of the sheet's institution the caller can see (422 otherwise), between a read-only look at the sheet and
     * the writing transaction, so no lock is held across it. <p>The optional etag is the sheet's (412 when stale). With
     * {@code complete} the sheet is completed in the same transaction, which needs the completing permission too (403,
     * nothing stored). Answers the marked students' records in request order.
     */
    public AttendanceRecordList recordAttendance(UUID sheetId, RecordAttendanceRequest request) {
        validator.validate(request);
        List<AttendanceMark> marks = request.getMarks();
        assertShape(marks);
        DataScope scope = DataScope.current(PERMISSION_RECORD);
        boolean completing = Boolean.TRUE.equals(request.getComplete());
        if (completing && !AuthContext.require().hasPermission(PERMISSION_COMPLETE)) {
            throw new PermissionDeniedException("Completing the sheet also needs " + PERMISSION_COMPLETE + ".");
        }
        OptionalInt expected = Etags.bodyVersion(request.getEtag());
        Set<UUID> checked = checkedNewStudents(scope, sheetId, marks);
        Optional<UUID> actor = Actor.current();
        return doInWriteTransaction(() -> {
            AttendanceSheetEntity sheet = lockWritable(scope, sheetId);
            Optional<UUID> institution = sheets.institutionOf(sheet.getRegisterId());
            if (completing) {
                DataScope.current(PERMISSION_COMPLETE).requireWritable(institution);
            }
            if (expected.isPresent() && expected.getAsInt() != sheet.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!IN_PROGRESS.equals(sheet.getStatus())) {
                throw new InvalidStateException("Attendance can be recorded only while the sheet is in progress.");
            }
            assertReferences(marks, institution);
            Map<UUID, AttendanceRecordEntity> existing = records.findBySheet(sheetId, sheet.getAttendanceDate())
                    .stream().collect(Collectors.toMap(AttendanceRecordEntity::getStudentId, record -> record));
            Map<UUID, AttendanceRecordEntity> stored = new HashMap<>();
            List<AttendanceRecordEntity> first = new ArrayList<>();
            int corrected = 0;
            for (AttendanceMark mark : marks) {
                AttendanceRecordEntity record = existing.get(mark.getStudentId());
                if (record == null) {
                    if (!checked.contains(mark.getStudentId())) {
                        // the student's record vanished between the look and the lock: the caller's view is stale
                        throw new PreconditionFailedException("The sheet changed since it was read.");
                    }
                    first.add(newRecord(sheet, mark.getStudentId(), mark, actor));
                } else if (sameMark(record, mark)) {
                    stored.put(mark.getStudentId(), record);
                } else {
                    stored.put(mark.getStudentId(), correct(record, mark, actor));
                    corrected++;
                }
            }
            for (AttendanceRecordEntity inserted : records.insertMarks(first)) {
                stored.put(inserted.getStudentId(), inserted);
                audit.recordCreate(AttendanceRecordEntity.TABLE, inserted.getId());
            }
            int version = sheet.getRowVersion();
            if (!first.isEmpty() || corrected > 0) {
                sheets.touch(sheetId, now(), actor.orElse(null));
                version++;
                audit.recordTransition(AGGREGATE, sheetId, "recordAttendance", List.of("marks"),
                        first.size() + " marks recorded, " + corrected + " corrected");
                outbox.publish(AGGREGATE + AttendanceEvents.UPDATED, AGGREGATE, sheetId,
                        AttendanceEvents.payload(sheetId));
            }
            if (completing) {
                complete(sheet.getId(), sheet.getAttendanceDate(), version, actor);
            }
            return new AttendanceRecordList(marks.stream().map(mark -> stored.get(mark.getStudentId()))
                    .map(recordMapper::toDto).toList());
        });
    }

    /** What the reference check needs of a sheet, read before sis is asked: where it belongs and who is on it. */
    private record SheetAtAGlance(UUID institutionId, UUID classSectionId, LocalDate date, Set<UUID> students) {
    }

    /**
     * The students among the marks that the sheet has no record of, each confirmed by sis as a student of the
     * sheet's institution (422 names the others). The sheet is only read here — in its own short transaction, closed
     * before sis is asked.
     */
    private Set<UUID> checkedNewStudents(DataScope scope, UUID sheetId, List<AttendanceMark> marks) {
        SheetAtAGlance seen = doInReadTransaction(() -> {
            AttendanceSheetEntity sheet = sheets.findById(sheetId)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, sheetId));
            AttendanceRegisterEntity register = registers.findById(sheet.getRegisterId())
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, sheetId));
            if (!scope.canRead(Optional.of(register.getInstitutionId()))) {
                throw new ResourceNotFoundException(AGGREGATE, sheetId);
            }
            scope.requireWritable(Optional.of(register.getInstitutionId()));
            if (!IN_PROGRESS.equals(sheet.getStatus())) {
                throw new InvalidStateException("Attendance can be recorded only while the sheet is in progress.");
            }
            return new SheetAtAGlance(register.getInstitutionId(), register.getClassSectionId(),
                    sheet.getAttendanceDate(), records.findBySheet(sheetId, sheet.getAttendanceDate()).stream()
                            .map(AttendanceRecordEntity::getStudentId).collect(Collectors.toSet()));
        });
        Set<UUID> known = seen.students();
        Set<UUID> fresh = marks.stream().map(AttendanceMark::getStudentId).filter(student -> !known.contains(student))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (fresh.isEmpty()) {
            return fresh;
        }
        Set<UUID> unknown = sessions.unknownStudents(seen.institutionId(), seen.classSectionId(), seen.date(), fresh);
        if (!unknown.isEmpty()) {
            List<FieldError> errors = new ArrayList<>();
            for (int i = 0; i < marks.size(); i++) {
                if (unknown.contains(marks.get(i).getStudentId())) {
                    errors.add(new FieldError("marks[" + i + "].studentId", "student does not exist", "NOT_FOUND"));
                }
            }
            throw new ValidationFailedException(errors);
        }
        return fresh;
    }

    /**
     * attendance.CompleteAttendanceSheet: attendance is taken — the sheet becomes done and its marks count in the
     * summaries. Allowed while the sheet is in progress and has at least one mark (409 otherwise); the optional etag
     * must be the sheet's current one (412). The event names the absent students, for whoever notifies guardians.
     */
    public AttendanceSheet completeAttendanceSheet(UUID sheetId, CompleteAttendanceSheetRequest request) {
        DataScope scope = DataScope.current(PERMISSION_COMPLETE);
        Optional<UUID> actor = Actor.current();
        return doInWriteTransaction(() -> {
            AttendanceSheetEntity sheet = lockWritable(scope, sheetId);
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != sheet.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!IN_PROGRESS.equals(sheet.getStatus())) {
                throw new InvalidStateException(
                        "The attendance sheet cannot be changed this way in its current state.");
            }
            complete(sheetId, sheet.getAttendanceDate(), sheet.getRowVersion(), actor);
            return sheetMapper.toDto(sheets.findById(sheetId)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, sheetId)));
        });
    }

    /**
     * attendance.TakeAttendanceForSession: the sheet of a timetable session — the existing one, or a new sheet in
     * progress on the session's date with every expected student marked present, ready for the teacher to change the
     * exceptions. The session, its students and the date come from the modules that own them (see
     * {@link SessionLookup}), read before the transaction starts; the register is the active one that covers the
     * session (409 when the institution has none). Two callers opening the same session get the same sheet.
     */
    public AttendanceSheet takeAttendanceForSession(TakeAttendanceForSessionRequest request) {
        validator.validate(request);
        DataScope scope = DataScope.current(PERMISSION_TAKE_FOR_SESSION);
        SessionLookup.SessionPlan plan = sessions.plan(request.getTimetableSessionId());
        scope.requireWritable(Optional.of(plan.institutionId()));
        try {
            return openSheet(scope, plan);
        } catch (DuplicateKeyException openedMeanwhile) {
            // another caller created the session's sheet first (uq_attendance_sheet_session): theirs is the sheet
            return openSheet(scope, plan);
        }
    }

    /** One attempt, in one transaction: the session's sheet if it exists, else a new one with its students. */
    private AttendanceSheet openSheet(DataScope scope, SessionLookup.SessionPlan plan) {
        Optional<UUID> actor = Actor.current();
        return doInWriteTransaction(() -> {
            // the register is locked before the sheet is looked up, so concurrent openers of a session queue here;
            // callers who resolve different registers are told apart by the unique index instead
            Optional<UUID> register = registers.findForSession(plan.institutionId(), plan.subjectId(),
                    plan.courseId(), plan.batchId(), plan.classSectionId());
            Optional<AttendanceSheetEntity> opened = sheets.findBySession(plan.sessionId());
            if (opened.isPresent()) {
                // a sheet in a register outside the caller's institutions does not exist for them
                Optional<UUID> institution = sheets.institutionOf(opened.get().getRegisterId());
                if (!scope.canRead(institution)) {
                    throw new ValidationFailedException(List.of(new FieldError("timetableSessionId",
                            "timetable session does not exist", "NOT_FOUND")));
                }
                scope.requireWritable(institution);
                return sheetMapper.toDto(opened.get());
            }
            UUID registerId = register.orElseThrow(() -> new InvalidStateException(
                    "No active attendance register covers this session."));
            AttendanceSheetEntity sheet = AttendanceSheetEntity.builder()
                    .registerId(registerId)
                    .attendanceDate(plan.date())
                    .timetableSessionId(plan.sessionId())
                    .takenByStaffId(plan.staffId())
                    .build();
            sheet.onCreatingData(actor, clock);
            sheet.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
            AttendanceSheetEntity stored = sheets.insert(sheet);
            UUID sheetId = stored.getId();
            audit.recordCreate(AGGREGATE, sheetId);
            outbox.publish(AGGREGATE + AttendanceEvents.CREATED, AGGREGATE, sheetId,
                    AttendanceEvents.payload(sheetId));
            if (sheets.startAttendanceSheet(sheetId, stored.getRowVersion(), now(), actor.orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            records.insertMarks(plan.studentIds().stream().map(studentId -> newRecord(stored, studentId,
                    new AttendanceMark(studentId, AttendanceMark.StatusEnum.PRESENT), actor)).toList());
            audit.recordTransition(AGGREGATE, sheetId, "start", List.of("status", "marks"),
                    plan.studentIds().size() + " expected students marked present");
            outbox.publish(AGGREGATE + ".start", AGGREGATE, sheetId, AttendanceEvents.payload(sheetId));
            return sheetMapper.toDto(sheets.findById(sheetId)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, sheetId)));
        });
    }

    /** The sheet, locked for this transaction, if the caller may see it (else 404) and change it (else 403). */
    private AttendanceSheetEntity lockWritable(DataScope scope, UUID sheetId) {
        AttendanceSheetEntity sheet = sheets.findByIdForUpdate(sheetId)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, sheetId));
        Optional<UUID> institution = sheets.institutionOf(sheet.getRegisterId());
        if (!scope.canRead(institution)) {
            throw new ResourceNotFoundException(AGGREGATE, sheetId);
        }
        scope.requireWritable(institution);
        return sheet;
    }

    /** Moves the locked sheet from in progress to done and announces it with its absentees. */
    private void complete(UUID sheetId, LocalDate attendanceDate, int version, Optional<UUID> actor) {
        List<AttendanceRecordEntity> marks = records.findBySheet(sheetId, attendanceDate);
        if (marks.isEmpty()) {
            throw new InvalidStateException("The attendance sheet has no marks to complete.");
        }
        if (sheets.completeAttendanceSheet(sheetId, version, now(), actor.orElse(null)) == 0) {
            throw new PreconditionFailedException("The row changed since it was read.");
        }
        List<UUID> absentees = marks.stream().filter(mark -> "absent".equals(mark.getStatus()))
                .map(AttendanceRecordEntity::getStudentId).toList();
        audit.recordTransition(AGGREGATE, sheetId, "complete", List.of("status"), null);
        outbox.publish(AGGREGATE + AttendanceEvents.COMPLETE, AGGREGATE, sheetId,
                AttendanceEvents.completed(sheetId, attendanceDate, absentees));
    }

    private AttendanceRecordEntity newRecord(AttendanceSheetEntity sheet, UUID studentId, AttendanceMark mark,
            Optional<UUID> actor) {
        AttendanceRecordEntity record = AttendanceRecordEntity.builder()
                .sheetId(sheet.getId())
                .studentId(studentId)
                .attendanceDate(sheet.getAttendanceDate())
                .build();
        apply(record, mark, actor);
        record.onCreatingData(actor, clock);
        record.setTenantId(sheet.getTenantId());
        return record;
    }

    /**
     * Stores the correction of one mark under the version it was read with, with its audit record. The sheet is
     * locked, so only a change through the record's own operations can have moved it on (412).
     */
    private AttendanceRecordEntity correct(AttendanceRecordEntity record, AttendanceMark mark, Optional<UUID> actor) {
        List<String> changed = changedFields(record, mark);
        int version = record.getRowVersion();
        apply(record, mark, actor);
        record.onUpdatingData(actor, clock);
        AttendanceRecordEntity updated = records.updateMark(record, version)
                .orElseThrow(() -> new PreconditionFailedException("A mark changed since it was read."));
        audit.recordUpdate(AttendanceRecordEntity.TABLE, updated.getId(), changed);
        return updated;
    }

    private static boolean sameMark(AttendanceRecordEntity record, AttendanceMark mark) {
        return changedFields(record, mark).isEmpty();
    }

    /** The columns a mark would change on a record (their names, for the audit record). */
    private static List<String> changedFields(AttendanceRecordEntity record, AttendanceMark mark) {
        List<String> changed = new ArrayList<>();
        if (!mark.getStatus().getValue().equals(record.getStatus())) {
            changed.add("status");
        }
        if (!Objects.equals(mark.getAttendanceTypeId(), record.getAttendanceTypeId())) {
            changed.add("attendanceTypeId");
        }
        if (!Objects.equals(mark.getAbsenceReasonId(), record.getAbsenceReasonId())) {
            changed.add("absenceReasonId");
        }
        if (!Objects.equals(remarkOf(mark), record.getRemark())) {
            changed.add("remark");
        }
        return changed;
    }

    private static String remarkOf(AttendanceMark mark) {
        String remark = mark.getRemark();
        return remark == null || remark.isBlank() ? null : remark.strip();
    }

    private static void apply(AttendanceRecordEntity record, AttendanceMark mark, Optional<UUID> actor) {
        record.setStatus(mark.getStatus().getValue());
        record.setAttendanceTypeId(mark.getAttendanceTypeId());
        record.setAbsenceReasonId(mark.getAbsenceReasonId());
        record.setRemark(remarkOf(mark));
        record.setRecordedByUserId(actor.orElse(null));
    }

    /** What can be judged from the request alone: size, one mark per student, remark width, reason only on absence. */
    private static void assertShape(List<AttendanceMark> marks) {
        if (marks.isEmpty() || marks.size() > MAX_MARKS) {
            throw new ValidationFailedException(List.of(
                    new FieldError("marks", "must contain 1 to " + MAX_MARKS + " marks", "Size")));
        }
        List<FieldError> errors = new ArrayList<>();
        Set<UUID> students = new HashSet<>();
        for (int i = 0; i < marks.size(); i++) {
            AttendanceMark mark = marks.get(i);
            if (mark == null) {
                errors.add(new FieldError("marks[" + i + "]", "must not be null", "NotNull"));
                continue;
            }
            String at = "marks[" + i + "].";
            if (!students.add(mark.getStudentId())) {
                errors.add(new FieldError(at + "studentId", "is marked more than once", "DUPLICATE"));
            }
            String remark = mark.getRemark();
            if (remark != null && remark.strip().length() > MAX_REMARK) {
                errors.add(new FieldError(at + "remark", "must be at most " + MAX_REMARK + " characters", "Size"));
            }
            if (mark.getAbsenceReasonId() != null && !REASONED.contains(mark.getStatus().getValue())) {
                errors.add(new FieldError(at + "absenceReasonId", "applies only to an absent or excused mark",
                        "INVALID"));
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    /**
     * The in-module references of the marks: a configured mark must be active, belong to the sheet's institution and
     * stand for the mark's status; an absence reason must be active.
     */
    private void assertReferences(List<AttendanceMark> marks, Optional<UUID> institution) {
        Map<UUID, MarkType> types = records.findMarkTypes(marks.stream().map(AttendanceMark::getAttendanceTypeId)
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        Set<UUID> reasons = records.findActiveAbsenceReasons(marks.stream().map(AttendanceMark::getAbsenceReasonId)
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        List<FieldError> errors = new ArrayList<>();
        for (int i = 0; i < marks.size(); i++) {
            AttendanceMark mark = marks.get(i);
            String at = "marks[" + i + "].";
            if (mark.getAttendanceTypeId() != null) {
                MarkType type = types.get(mark.getAttendanceTypeId());
                // a mark configured for another institution does not exist for this sheet
                if (type == null || !institution.map(type.institutionId()::equals).orElse(false)) {
                    errors.add(new FieldError(at + "attendanceTypeId", "attendance type does not exist",
                            "NOT_FOUND"));
                } else if (!type.standsFor(mark.getStatus().getValue())) {
                    errors.add(new FieldError(at + "attendanceTypeId", "does not stand for the mark's status",
                            "INVALID"));
                }
            }
            if (mark.getAbsenceReasonId() != null && !reasons.contains(mark.getAbsenceReasonId())) {
                errors.add(new FieldError(at + "absenceReasonId", "absence reason does not exist", "NOT_FOUND"));
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
