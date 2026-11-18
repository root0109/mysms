package com.mysms.attendance.business;

import com.mysms.attendance.config.AttendanceDatabaseConfig;
import com.mysms.attendance.config.AttendanceEventsConfig;
import com.mysms.attendance.domain.StaffAttendanceEntity;
import com.mysms.attendance.events.AttendanceEvents;
import com.mysms.attendance.mapper.StaffAttendanceMapper;
import com.mysms.attendance.repository.StaffAttendanceRepository;
import com.mysms.authclient.DataScope;
import com.mysms.attendance.api.model.StaffAttendance;
import com.mysms.attendance.api.model.StaffAttendanceUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of attendance.staff_attendance: audit and domain event commit in the same transaction as every
 * change. Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide
 * grant.
 */
@Service
public class StaffAttendanceService extends AppTransactionManager {

    public static final String AGGREGATE = StaffAttendanceEntity.TABLE;
    static final String PERMISSION_LIST = "attendance.staff_attendance.read";
    static final String PERMISSION_GET = "attendance.staff_attendance.read";
    static final String PERMISSION_CREATE = "attendance.staff_attendance.create";
    static final String PERMISSION_UPDATE = "attendance.staff_attendance.update";
    /** The contract's StaffAttendanceUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("status", "checkInAt", "checkOutAt", "leaveApplicationId", "remark");


    private final StaffAttendanceRepository repository;
    private final StaffAttendanceMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public StaffAttendanceService(StaffAttendanceRepository repository, StaffAttendanceMapper mapper,
            @Qualifier(AttendanceDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AttendanceDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AttendanceEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(AttendanceEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public StaffAttendance getStaffAttendance(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<StaffAttendance> listStaffAttendances(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, StaffAttendanceRepository.SORT_FIELDS, StaffAttendanceRepository.DEFAULT_ORDER,
                StaffAttendanceRepository.ID_COLUMN);
        // a partitioned table: the list reads only the partitions of the range the client names
        FilterParser.requireRange(filter, StaffAttendanceRepository.FILTER_FIELDS, "attendanceDate");
        SqlFragment where = FilterParser.parse(filter, StaffAttendanceRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<StaffAttendanceEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public StaffAttendance createStaffAttendance(StaffAttendance request, boolean validateOnly) {
        validator.validate(request);
        StaffAttendanceEntity entity = mapper.toEntity(request);
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
            StaffAttendanceEntity stored = repository.insert(entity);
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
    public StaffAttendance updateStaffAttendance(UUID id, String ifMatch, StaffAttendanceUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            StaffAttendanceEntity entity = loadVisible(scope, id);
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

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private StaffAttendanceEntity loadVisible(DataScope scope, UUID id) {
        StaffAttendanceEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOf(StaffAttendanceEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected StaffAttendanceUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(StaffAttendanceEntity entity, StaffAttendanceUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "status" -> entity.setStatus(Optional.ofNullable(changes.getStatus()).map(v -> v.getValue())
                        .orElse(null));
                case "checkInAt" -> entity.setCheckInAt(changes.getCheckInAt());
                case "checkOutAt" -> entity.setCheckOutAt(changes.getCheckOutAt());
                case "leaveApplicationId" -> entity.setLeaveApplicationId(changes.getLeaveApplicationId());
                case "remark" -> entity.setRemark(changes.getRemark());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(StaffAttendanceEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getStaffId() == null) {
            errors.add(new FieldError("staffId", "must not be null", "NotNull"));
        }
        if (entity.getAttendanceDate() == null) {
            errors.add(new FieldError("attendanceDate", "must not be null", "NotNull"));
        }
        if (entity.getStatus() == null) {
            errors.add(new FieldError("status", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(StaffAttendanceEntity entity) {
        if (repository.takenStaffIdAttendanceDate1(entity)) {
            throw new DuplicateResourceException(
                    "A staff attendance with the same staff id and attendance date already exists.");
        }
    }
}
