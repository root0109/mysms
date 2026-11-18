package com.mysms.attendance.business;

import com.mysms.attendance.api.model.AttendanceRecord;
import com.mysms.attendance.api.model.AttendanceSheet;
import com.mysms.attendance.api.model.AttendanceSheetUpdate;
import com.mysms.attendance.api.model.CancelAttendanceSheetRequest;
import com.mysms.attendance.api.model.ResetAttendanceSheetRequest;
import com.mysms.attendance.api.model.StartAttendanceSheetRequest;
import com.mysms.attendance.config.AttendanceDatabaseConfig;
import com.mysms.attendance.config.AttendanceEventsConfig;
import com.mysms.attendance.domain.AttendanceRecordEntity;
import com.mysms.attendance.domain.AttendanceSheetEntity;
import com.mysms.attendance.events.AttendanceEvents;
import com.mysms.attendance.mapper.AttendanceRecordMapper;
import com.mysms.attendance.mapper.AttendanceSheetMapper;
import com.mysms.attendance.repository.AttendanceRecordRepository;
import com.mysms.attendance.repository.AttendanceSheetRepository;
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
 * Business service of attendance.attendance_sheet and of the records that exist only as part of a AttendanceSheet
 * (AttendanceRecord): audit and domain event commit in the same transaction as every change. Rows outside the caller's
 * institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class AttendanceSheetService extends AppTransactionManager {

    public static final String AGGREGATE = AttendanceSheetEntity.TABLE;
    static final String PERMISSION_LIST = "attendance.attendance_sheet.read";
    static final String PERMISSION_GET = "attendance.attendance_sheet.read";
    static final String PERMISSION_CREATE = "attendance.attendance_sheet.create";
    static final String PERMISSION_UPDATE = "attendance.attendance_sheet.update";
    static final String PERMISSION_DELETE = "attendance.attendance_sheet.delete";
    static final String PERMISSION_START_ATTENDANCE_SHEET = "attendance.attendance_sheet.start_attendance_sheet";
    static final String PERMISSION_CANCEL_ATTENDANCE_SHEET = "attendance.attendance_sheet.cancel_attendance_sheet";
    static final String PERMISSION_RESET_ATTENDANCE_SHEET = "attendance.attendance_sheet.reset_attendance_sheet";
    /** The contract's AttendanceSheetUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("registerId", "academicYearId", "attendanceDate", "timetableSessionId", "dayPart", "reference",
            "takenByStaffId");

    public static final String ATTENDANCE_RECORD_AGGREGATE = AttendanceRecordEntity.TABLE;
    static final String ATTENDANCE_RECORD_PERMISSION_LIST = "attendance.attendance_record.read";
    static final String ATTENDANCE_RECORD_PERMISSION_GET = "attendance.attendance_record.read";

    private final AttendanceSheetRepository repository;
    private final AttendanceSheetMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceRecordMapper attendanceRecordMapper;

    public AttendanceSheetService(AttendanceSheetRepository repository,
            AttendanceSheetMapper mapper,
            @Qualifier(AttendanceDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AttendanceDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AttendanceEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AttendanceEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            AttendanceRecordRepository attendanceRecordRepository,
            AttendanceRecordMapper attendanceRecordMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.attendanceRecordMapper = attendanceRecordMapper;
    }

    public AttendanceSheet getAttendanceSheet(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<AttendanceSheet> listAttendanceSheets(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, AttendanceSheetRepository.SORT_FIELDS, AttendanceSheetRepository.DEFAULT_ORDER,
                AttendanceSheetRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, AttendanceSheetRepository.FILTER_FIELDS)
                .and(AttendanceSheetRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<AttendanceSheetEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public AttendanceSheet createAttendanceSheet(AttendanceSheet request, boolean validateOnly) {
        validator.validate(request);
        AttendanceSheetEntity entity = mapper.toEntity(request);
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
            AttendanceSheetEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + AttendanceEvents.CREATED, AGGREGATE, stored.getId(),
                    AttendanceEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public AttendanceSheet updateAttendanceSheet(UUID id, String ifMatch, AttendanceSheetUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            AttendanceSheetEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + AttendanceEvents.UPDATED, AGGREGATE, id, AttendanceEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteAttendanceSheet(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            AttendanceSheetEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByAttendanceRecordSheetId(id)) {
                throw new InvalidStateException(
                        "The attendance sheet is still referenced by attendance.attendance_record rows; remove those "
                                + "first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + AttendanceEvents.DELETED, AGGREGATE, id, AttendanceEvents.payload(id));
        });
    }

    /**
     * attendance.StartAttendanceSheet: Start taking attendance. Allowed while status is draft, on a row the caller may
     * see and write; otherwise 409 and nothing changes.
     */
    public AttendanceSheet startAttendanceSheet(UUID id, StartAttendanceSheetRequest request) {
        DataScope scope = DataScope.current(PERMISSION_START_ATTENDANCE_SHEET);
        return doInWriteTransaction(() -> {
            AttendanceSheetEntity entity = repository.findById(id)
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
                throw new InvalidStateException(
                        "The attendance sheet cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.startAttendanceSheet(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "start", List.of("status"), null);
            outbox.publish(AGGREGATE + ".start", AGGREGATE, id, AttendanceEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * attendance.CancelAttendanceSheet: Cancel. Allowed while status is draft or in_progress, on a row the caller may
     * see and write; otherwise 409 and nothing changes.
     */
    public AttendanceSheet cancelAttendanceSheet(UUID id, CancelAttendanceSheetRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CANCEL_ATTENDANCE_SHEET);
        return doInWriteTransaction(() -> {
            AttendanceSheetEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("draft", "in_progress").contains(entity.getStatus()))) {
                throw new InvalidStateException(
                        "The attendance sheet cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.cancelAttendanceSheet(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "cancel", List.of("status"), request == null ? null
                    : request.getReason());
            outbox.publish(AGGREGATE + ".cancel", AGGREGATE, id, AttendanceEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * attendance.ResetAttendanceSheet: Back to draft. Allowed while status is cancelled or done, on a row the caller may
     * see and write; otherwise 409 and nothing changes. Reopening a completed sheet to correct it keeps its marks.
     */
    public AttendanceSheet resetAttendanceSheet(UUID id, ResetAttendanceSheetRequest request) {
        DataScope scope = DataScope.current(PERMISSION_RESET_ATTENDANCE_SHEET);
        return doInWriteTransaction(() -> {
            AttendanceSheetEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("cancelled", "done").contains(entity.getStatus()))) {
                throw new InvalidStateException(
                        "The attendance sheet cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.resetAttendanceSheet(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "reset", List.of("status"), null);
            outbox.publish(AGGREGATE + ".reset", AGGREGATE, id, AttendanceEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private AttendanceSheetEntity loadVisible(DataScope scope, UUID id) {
        AttendanceSheetEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the attendance.attendance_register the row belongs to. */
    private Optional<UUID> institutionOf(AttendanceSheetEntity entity) {
        return repository.institutionOf(entity.getRegisterId());
    }

    /** Applies the selected AttendanceSheetUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(AttendanceSheetEntity entity, AttendanceSheetUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "registerId" -> entity.setRegisterId(changes.getRegisterId());
                case "academicYearId" -> entity.setAcademicYearId(changes.getAcademicYearId());
                case "attendanceDate" -> entity.setAttendanceDate(changes.getAttendanceDate());
                case "timetableSessionId" -> entity.setTimetableSessionId(changes.getTimetableSessionId());
                case "dayPart" -> entity.setDayPart(Optional.ofNullable(changes.getDayPart()).map(v -> v.getValue())
                        .orElse(null));
                case "reference" -> entity.setReference(changes.getReference());
                case "takenByStaffId" -> entity.setTakenByStaffId(changes.getTakenByStaffId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(AttendanceSheetEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getRegisterId() == null) {
            errors.add(new FieldError("registerId", "must not be null", "NotNull"));
        }
        if (entity.getAttendanceDate() == null) {
            errors.add(new FieldError("attendanceDate", "must not be null", "NotNull"));
        }
        if (!creating && entity.getDayPart() == null) {
            errors.add(new FieldError("dayPart", "must not be null", "NotNull"));
        }
        if (entity.getRegisterId() != null && !repository.existsRegisterId(entity.getRegisterId())) {
            errors.add(new FieldError("registerId", "attendance register does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(AttendanceSheetEntity entity) {

    }

    // ---- AttendanceRecord ----

    public AttendanceRecord getAttendanceRecord(UUID sheetId, UUID id) {
        return doInReadTransaction(() -> {
            return attendanceRecordMapper.toDto(loadVisibleAttendanceRecord(DataScope.current(
                    ATTENDANCE_RECORD_PERMISSION_GET), sheetId, id));
        });
    }

    public Page<AttendanceRecord> listAttendanceRecords(UUID sheetId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, AttendanceRecordRepository.SORT_FIELDS,
                        AttendanceRecordRepository.DEFAULT_ORDER,
                AttendanceRecordRepository.ID_COLUMN);
        // a partitioned table: the list reads only the partitions of the range the client names
        FilterParser.requireRange(filter, AttendanceRecordRepository.FILTER_FIELDS, "attendanceDate");
        SqlFragment where = FilterParser.parse(filter, AttendanceRecordRepository.FILTER_FIELDS)
                .and(AttendanceRecordRepository.ofParent(sheetId))
                        .and(AttendanceRecordRepository.scope(DataScope.current(ATTENDANCE_RECORD_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + sheetId, clock);
        return doInReadTransaction(() -> {
            Page<AttendanceRecordEntity> rows = attendanceRecordRepository.list(page, where);
            return rows.map(attendanceRecordMapper::toDto);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private AttendanceRecordEntity loadVisibleAttendanceRecord(DataScope scope, UUID sheetId, UUID id) {
        AttendanceRecordEntity entity = attendanceRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ATTENDANCE_RECORD_AGGREGATE, id));
        if (!scope.canRead(institutionOfAttendanceRecord(entity)) || !sheetId.equals(entity.getSheetId())) {
            throw new ResourceNotFoundException(ATTENDANCE_RECORD_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the attendance.attendance_sheet → attendance.attendance_register the row belongs to. */
    private Optional<UUID> institutionOfAttendanceRecord(AttendanceRecordEntity entity) {
        return attendanceRecordRepository.institutionOf(entity.getSheetId());
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidAttendanceRecord(AttendanceRecordEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getSheetId() == null) {
            errors.add(new FieldError("sheetId", "must not be null", "NotNull"));
        }
        if (entity.getStudentId() == null) {
            errors.add(new FieldError("studentId", "must not be null", "NotNull"));
        }
        if (entity.getStatus() == null) {
            errors.add(new FieldError("status", "must not be null", "NotNull"));
        }
        if (entity.getAbsenceReasonId() != null
                && !attendanceRecordRepository.existsAbsenceReasonId(entity.getAbsenceReasonId())) {
            errors.add(new FieldError("absenceReasonId", "absence reason does not exist", "NOT_FOUND"));
        }
        if (entity.getAttendanceTypeId() != null
                && !attendanceRecordRepository.existsAttendanceTypeId(entity.getAttendanceTypeId())) {
            errors.add(new FieldError("attendanceTypeId", "attendance type does not exist", "NOT_FOUND"));
        }
        if (entity.getSheetId() != null && !attendanceRecordRepository.existsSheetId(entity.getSheetId())) {
            errors.add(new FieldError("sheetId", "attendance sheet does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueAttendanceRecord(AttendanceRecordEntity entity) {
        if (attendanceRecordRepository.takenSheetIdStudentIdAttendanceDate1(entity)) {
            throw new DuplicateResourceException(
                    "A attendance record with the same sheet id and student id and attendance date already exists.");
        }
    }
}
