package com.mysms.sis.business;

import com.mysms.authclient.DataScope;
import com.mysms.sis.api.model.StudentLeaveDecisionRequest;
import com.mysms.sis.api.model.StudentLeaveRequest;
import com.mysms.sis.api.model.StudentLeaveRequestUpdate;
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
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import com.mysms.sis.config.SisDatabaseConfig;
import com.mysms.sis.config.SisEventsConfig;
import com.mysms.sis.domain.StudentLeaveRequestEntity;
import com.mysms.sis.events.SisEvents;
import com.mysms.sis.mapper.StudentLeaveRequestMapper;
import com.mysms.sis.repository.StudentLeaveRequestRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of sis.student_leave_request (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 */
@Service
public class StudentLeaveRequestService extends AppTransactionManager {

    public static final String AGGREGATE = StudentLeaveRequestEntity.TABLE;
    static final String PERMISSION_LIST = "sis.student_leave_request.read";
    static final String PERMISSION_GET = "sis.student_leave_request.read";
    static final String PERMISSION_CREATE = "sis.student_leave_request.create";
    static final String PERMISSION_UPDATE = "sis.student_leave_request.update";
    static final String PERMISSION_DELETE = "sis.student_leave_request.delete";
    static final String PERMISSION_APPROVE_STUDENT_LEAVE_REQUEST = "sis.student_leave_request.approve";
    static final String PERMISSION_REJECT_STUDENT_LEAVE_REQUEST = "sis.student_leave_request.reject";
    /** The contract's StudentLeaveRequestUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("studentId", "academicYearId", "fromDate", "toDate", "dayPart", "reason");


    private final StudentLeaveRequestRepository repository;
    private final StudentLeaveRequestMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public StudentLeaveRequestService(StudentLeaveRequestRepository repository, StudentLeaveRequestMapper mapper,
            @Qualifier(SisDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(SisDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(SisEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(SisEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public StudentLeaveRequest getStudentLeaveRequest(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<StudentLeaveRequest> listStudentLeaveRequests(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, StudentLeaveRequestRepository.SORT_FIELDS,
                        StudentLeaveRequestRepository.DEFAULT_ORDER,
                StudentLeaveRequestRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, StudentLeaveRequestRepository.FILTER_FIELDS)
                .and(StudentLeaveRequestRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<StudentLeaveRequestEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public StudentLeaveRequest createStudentLeaveRequest(StudentLeaveRequest request, boolean validateOnly) {
        validator.validate(request);
        StudentLeaveRequestEntity entity = mapper.toEntity(request);
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
            StudentLeaveRequestEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + SisEvents.CREATED, AGGREGATE, stored.getId(), SisEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public StudentLeaveRequest updateStudentLeaveRequest(UUID id, String ifMatch, StudentLeaveRequestUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            StudentLeaveRequestEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + SisEvents.UPDATED, AGGREGATE, id, SisEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteStudentLeaveRequest(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            StudentLeaveRequestEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + SisEvents.DELETED, AGGREGATE, id, SisEvents.payload(id));
        });
    }

    /**
     * sis.ApproveStudentLeaveRequest: Approves the request and records who decided and why. Approved leave is linked
     * from attendance records. The row must be visible and writable for the caller, and the step is allowed while
     * status is pending; in any other state
     * the answer is 409 and nothing changes.
     */
    public StudentLeaveRequest approveStudentLeaveRequest(UUID id, String ifMatch,
            StudentLeaveDecisionRequest request) {
        DataScope scope = DataScope.current(PERMISSION_APPROVE_STUDENT_LEAVE_REQUEST);
        return doInWriteTransaction(() -> {
            StudentLeaveRequestEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != Etags.expectedVersion(ifMatch)) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (!(Set.of("pending").contains(entity.getStatus()))) {
                throw new InvalidStateException(
                        "The student leave request cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.approveStudentLeaveRequest(id, entity.getRowVersion(), now, Actor.current().orElse(null),
                    request == null ? null : request.getDecisionNote()) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "approve",
                    List.of("status", "decidedByUserId", "decidedAt", "decisionNote"), null);
            outbox.publish(AGGREGATE + ".approve", AGGREGATE, id, SisEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * sis.RejectStudentLeaveRequest: Rejects the request and records who decided and why. Approved leave is linked from
     * attendance records. The row must be visible and writable for the caller, and the step is allowed while status is
     * pending; in any other state
     * the answer is 409 and nothing changes.
     */
    public StudentLeaveRequest rejectStudentLeaveRequest(UUID id, String ifMatch, StudentLeaveDecisionRequest request) {
        DataScope scope = DataScope.current(PERMISSION_REJECT_STUDENT_LEAVE_REQUEST);
        return doInWriteTransaction(() -> {
            StudentLeaveRequestEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != Etags.expectedVersion(ifMatch)) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (!(Set.of("pending").contains(entity.getStatus()))) {
                throw new InvalidStateException(
                        "The student leave request cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.rejectStudentLeaveRequest(id, entity.getRowVersion(), now, Actor.current().orElse(null),
                    request == null ? null : request.getDecisionNote()) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "reject",
                    List.of("status", "decidedByUserId", "decidedAt", "decisionNote"), null);
            outbox.publish(AGGREGATE + ".reject", AGGREGATE, id, SisEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private StudentLeaveRequestEntity loadVisible(DataScope scope, UUID id) {
        StudentLeaveRequestEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the sis.student it belongs to). */
    private Optional<UUID> institutionOf(StudentLeaveRequestEntity entity) {
        return repository.institutionOf(entity.getStudentId());
    }

    /**
     * Applies the selected StudentLeaveRequestUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void apply(StudentLeaveRequestEntity entity, StudentLeaveRequestUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "studentId" -> entity.setStudentId(changes.getStudentId());
                case "academicYearId" -> entity.setAcademicYearId(changes.getAcademicYearId());
                case "fromDate" -> entity.setFromDate(changes.getFromDate());
                case "toDate" -> entity.setToDate(changes.getToDate());
                case "dayPart" -> entity.setDayPart(Optional.ofNullable(changes.getDayPart()).map(v -> v.getValue())
                        .orElse(null));
                case "reason" -> entity.setReason(changes.getReason());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(StudentLeaveRequestEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getStudentId() == null) {
            errors.add(new FieldError("studentId", "must not be null", "NotNull"));
        }
        if (entity.getFromDate() == null) {
            errors.add(new FieldError("fromDate", "must not be null", "NotNull"));
        }
        if (entity.getToDate() == null) {
            errors.add(new FieldError("toDate", "must not be null", "NotNull"));
        }
        if (!creating && entity.getDayPart() == null) {
            errors.add(new FieldError("dayPart", "must not be null", "NotNull"));
        }
        if (entity.getStudentId() != null && !repository.existsStudentId(entity.getStudentId())) {
            errors.add(new FieldError("studentId", "student does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(StudentLeaveRequestEntity entity) {

    }
}
