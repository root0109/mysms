package com.mysms.hrms.hr.business;

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
import com.mysms.core.filter.FilterParser;
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.Patches;
import com.mysms.hrms.hr.api.model.ApproveLeaveRequest;
import com.mysms.hrms.hr.api.model.CancelLeaveRequest;
import com.mysms.hrms.hr.api.model.LeaveApplication;
import com.mysms.hrms.hr.api.model.LeaveApplicationUpdate;
import com.mysms.hrms.hr.api.model.RejectLeaveRequest;
import com.mysms.hrms.hr.config.HrDatabaseConfig;
import com.mysms.hrms.hr.config.HrEventsConfig;
import com.mysms.hrms.hr.domain.LeaveApplicationEntity;
import com.mysms.hrms.hr.events.HrEvents;
import com.mysms.hrms.hr.mapper.LeaveApplicationMapper;
import com.mysms.hrms.hr.repository.LeaveApplicationRepository;
import com.mysms.hrms.hr.repository.LeaveLedgerRepository.LockedBalance;
import com.mysms.hrms.hr.repository.LeaveLedgerRepository;
import java.math.BigDecimal;
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
 * Business service of hr.leave_application: audit and domain event commit in the same transaction as every change. Rows
 * outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class LeaveApplicationService extends AppTransactionManager {

    public static final String AGGREGATE = LeaveApplicationEntity.TABLE;
    static final String PERMISSION_LIST = "hr.leave_application.read";
    static final String PERMISSION_GET = "hr.leave_application.read";
    static final String PERMISSION_CREATE = "hr.leave_application.create";
    static final String PERMISSION_UPDATE = "hr.leave_application.update";
    static final String PERMISSION_DELETE = "hr.leave_application.delete";
    static final String PERMISSION_REJECT_LEAVE = "hr.leave_application.reject_leave";
    static final String PERMISSION_CANCEL_LEAVE = "hr.leave_application.cancel_leave";
    static final String PERMISSION_APPROVE_LEAVE = "hr.leave_application.approve_leave";
    /** The longest comment a decision keeps (422 beyond). */
    static final int MAX_COMMENT = 4000;


    private final LeaveApplicationRepository repository;
    private final LeaveApplicationMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final LeaveLedgerRepository ledger;

    public LeaveApplicationService(LeaveApplicationRepository repository, LeaveApplicationMapper mapper,
            LeaveLedgerRepository ledger,
            @Qualifier(HrDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(HrDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(HrEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(HrEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.ledger = ledger;
    }

    public LeaveApplication getLeaveApplication(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<LeaveApplication> listLeaveApplications(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, LeaveApplicationRepository.SORT_FIELDS,
                        LeaveApplicationRepository.DEFAULT_ORDER,
                LeaveApplicationRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, LeaveApplicationRepository.FILTER_FIELDS)
                .and(LeaveApplicationRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<LeaveApplicationEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public LeaveApplication createLeaveApplication(LeaveApplication request, boolean validateOnly) {
        validator.validate(request);
        LeaveApplicationEntity entity = mapper.toEntity(request);
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
            LeaveApplicationEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + HrEvents.CREATED, AGGREGATE, stored.getId(), HrEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public LeaveApplication updateLeaveApplication(UUID id, String ifMatch, LeaveApplicationUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            LeaveApplicationEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (!"pending".equals(entity.getStatus())) {
                // a decided application is what was decided on; an approved one is counted in the leave balance
                throw new InvalidStateException("A leave application is changed only while it is pending.");
            }
            List<String> fields = apply(entity, changes);
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
            outbox.publish(AGGREGATE + HrEvents.UPDATED, AGGREGATE, id, HrEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteLeaveApplication(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            LeaveApplicationEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if ("approved".equals(entity.getStatus()) || "completed".equals(entity.getStatus())) {
                throw new InvalidStateException(
                        "Approved leave is counted in the leave balance and is not deleted.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + HrEvents.DELETED, AGGREGATE, id, HrEvents.payload(id));
        });
    }

    /**
     * hr.ApproveLeave: a pending application becomes approved and its days are counted against the staff member's
     * balance for its leave type and year (opened with the type's yearly allowance if missing). The application and the
     * balance are locked, so two approvals cannot spend the same days; pending applications hold nothing back. Too few
     * days left, any state but pending, or no leave type where the school keeps leave types answer 409. Nobody approves
     * their own leave (403), by account, by another staff record of theirs, or acting as someone else.
     */
    public LeaveApplication approveLeave(UUID id, ApproveLeaveRequest request) {
        String comment = decisionComment(request == null ? null : request.getReason());
        DataScope scope = DataScope.current(PERMISSION_APPROVE_LEAVE);
        AuthContext caller = AuthContext.require();
        return doInWriteTransaction(() -> {
            if (!ledger.lockApplication(id)) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            LeaveApplicationEntity entity = repository.findById(id)
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
                throw new InvalidStateException(
                        "The leave application cannot be changed this way in its current state.");
            }
            LeaveLedgerRepository.Applicant applicant = ledger.applicant(entity.getStaffId());
            if (isOneOf(applicant, caller.userId(), caller.personId())
                    || isOneOf(applicant, caller.impersonatorId().orElse(null), Optional.empty())) {
                throw new PermissionDeniedException("Nobody approves their own leave.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            UUID actor = Actor.current().orElse(null);
            countAgainstBalance(entity, now, actor);
            if (repository.approveLeave(id, entity.getRowVersion(), now, actor, now.toLocalDate(), comment) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "approveLeave",
                    List.of("status", "decidedByUserId", "decidedOn"), comment);
            outbox.publish(AGGREGATE + ".approveLeave", AGGREGATE, id, HrEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** Whether the applicant is this account or this person. */
    private static boolean isOneOf(LeaveLedgerRepository.Applicant applicant, UUID userId, Optional<UUID> personId) {
        return userId != null && applicant.userId().filter(userId::equals).isPresent()
                || personId.isPresent() && personId.equals(applicant.personId());
    }

    /**
     * Counts the application's days against the balance of its leave type and year, with the audit record of the
     * balance it opens or changes; 409 when too few days are left.
     */
    private void countAgainstBalance(LeaveApplicationEntity application, OffsetDateTime now, UUID actor) {
        if (application.getLeaveTypeId() == null) {
            if (ledger.hasLeaveTypes(application.getInstitutionId(), application.getAcademicYearId())) {
                throw new InvalidStateException(
                        "The application names no leave type, so its days cannot be counted; it is rejected, or "
                                + "changed to name one.");
            }
            return; // a school that keeps no leave types keeps no balances
        }
        LockedBalance balance = ledger.lockBalance(UuidV7.next(clock), application.getStaffId(),
                        application.getLeaveTypeId(), application.getInstitutionId(),
                        application.getAcademicYearId(), now, actor)
                .orElseThrow(() -> new InvalidStateException(
                        "The leave type of the application is not one the school offers for this year."));
        if (balance.opened()) {
            audit.recordCreate(LeaveBalanceService.AGGREGATE, balance.id());
            outbox.publish(LeaveBalanceService.AGGREGATE + HrEvents.CREATED, LeaveBalanceService.AGGREGATE,
                    balance.id(), HrEvents.payload(balance.id()));
        }
        BigDecimal days = ledger.daysOf(application.getId());
        if (days.compareTo(balance.available()) > 0) {
            throw new InvalidStateException("The application asks for " + LeaveDays.plain(days) + " days and "
                    + LeaveDays.plain(balance.available().max(BigDecimal.ZERO)) + " are left for this leave type.");
        }
        if (ledger.useDays(balance, days, now, actor) == 0) {
            throw new PreconditionFailedException("The leave balance changed since it was read.");
        }
        audit.recordUpdate(LeaveBalanceService.AGGREGATE, balance.id(), List.of("usedDays"));
        outbox.publish(LeaveBalanceService.AGGREGATE + HrEvents.UPDATED, LeaveBalanceService.AGGREGATE, balance.id(),
                HrEvents.payload(balance.id()));
    }

    /** A decision's comment as it is kept: trimmed, null when none was given, at most MAX_COMMENT characters. */
    private static String decisionComment(String given) {
        String comment = given == null ? "" : given.trim();
        if (comment.length() > MAX_COMMENT) {
            throw new ValidationFailedException(List.of(new FieldError("reason",
                    "size must be between 0 and " + MAX_COMMENT, "Size")));
        }
        return comment.isEmpty() ? null : comment;
    }

    /**
     * hr.RejectLeave: Reject. Allowed while status is pending, on a row the caller may see and write; otherwise 409 and
     * nothing changes.
     */
    public LeaveApplication rejectLeave(UUID id, RejectLeaveRequest request) {
        String comment = decisionComment(request == null ? null : request.getReason());
        DataScope scope = DataScope.current(PERMISSION_REJECT_LEAVE);
        return doInWriteTransaction(() -> {
            LeaveApplicationEntity entity = repository.findById(id)
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
                throw new InvalidStateException(
                        "The leave application cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.rejectLeave(id, entity.getRowVersion(), now, Actor.current().orElse(null),
                    now.toLocalDate(), comment) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "rejectLeave", List.of("status", "decidedByUserId", "decidedOn"),
                    comment);
            outbox.publish(AGGREGATE + ".rejectLeave", AGGREGATE, id, HrEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * hr.CancelLeave: withdraws a pending application, or cancels an approved leave before it starts and gives its days
     * back to the balance in the same transaction (both locked, as for an approval). A started leave stays approved;
     * that and any other state answer 409. Only the applicant or someone who may decide on leave in the institution may
     * cancel (403 otherwise).
     */
    public LeaveApplication cancelLeave(UUID id, CancelLeaveRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CANCEL_LEAVE);
        AuthContext caller = AuthContext.require();
        return doInWriteTransaction(() -> {
            if (!ledger.lockApplication(id)) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            LeaveApplicationEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            boolean decider = caller.hasPermission(PERMISSION_APPROVE_LEAVE)
                    && DataScope.current(PERMISSION_APPROVE_LEAVE).canRead(institutionOf(entity));
            if (!decider && !isOneOf(ledger.applicant(entity.getStaffId()), caller.userId(), caller.personId())) {
                throw new PermissionDeniedException(
                        "Only the applicant, or someone who decides on leave, calls an application off.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            UUID actor = Actor.current().orElse(null);
            if ("approved".equals(entity.getStatus())) {
                if (!now.isBefore(entity.getFromAt())) {
                    throw new InvalidStateException("The leave has started, so it can no longer be cancelled.");
                }
                returnToBalance(entity, now, actor);
            } else if (!"pending".equals(entity.getStatus())) {
                throw new InvalidStateException(
                        "The leave application cannot be changed this way in its current state.");
            }
            if (repository.cancelLeave(id, entity.getRowVersion(), now, actor) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "cancelLeave", List.of("status"), request == null ? null
                    : request.getReason());
            outbox.publish(AGGREGATE + ".cancelLeave", AGGREGATE, id, HrEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** Gives a cancelled approved leave's days back to the balance its approval counted them against. */
    private void returnToBalance(LeaveApplicationEntity application, OffsetDateTime now, UUID actor) {
        if (application.getLeaveTypeId() == null) {
            return; // approved where the school keeps no leave types: nothing was counted
        }
        Optional<LockedBalance> counted = ledger.lockCountedBalance(application.getStaffId(),
                application.getLeaveTypeId(), application.getAcademicYearId());
        if (counted.isEmpty()) {
            return; // the balance was deleted since: there is nothing to give back to
        }
        if (ledger.returnDays(counted.get(), ledger.daysOf(application.getId()), now, actor) == 0) {
            throw new PreconditionFailedException("The leave balance changed since it was read.");
        }
        audit.recordUpdate(LeaveBalanceService.AGGREGATE, counted.get().id(), List.of("usedDays"));
        outbox.publish(LeaveBalanceService.AGGREGATE + HrEvents.UPDATED, LeaveBalanceService.AGGREGATE,
                counted.get().id(), HrEvents.payload(counted.get().id()));
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private LeaveApplicationEntity loadVisible(DataScope scope, UUID id) {
        LeaveApplicationEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(LeaveApplicationEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(LeaveApplicationEntity entity, LeaveApplicationUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("academicYearId", changes.getAcademicYearId(), entity::setAcademicYearId);
        patch.apply("staffId", changes.getStaffId(), entity::setStaffId);
        patch.apply("leaveTypeId", changes.getLeaveTypeId(), entity::setLeaveTypeId);
        patch.apply("absenceReasonId", changes.getAbsenceReasonId(), entity::setAbsenceReasonId);
        patch.apply("classSectionId", changes.getClassSectionId(), entity::setClassSectionId);
        patch.apply("fromAt", changes.getFromAt(), entity::setFromAt);
        patch.apply("toAt", changes.getToAt(), entity::setToAt);
        patch.apply("dayPart", changes.getDayPart(), v -> entity.setDayPart(v == null ? null : v.getValue()));
        patch.apply("remarks", changes.getRemarks(), entity::setRemarks);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(LeaveApplicationEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getAcademicYearId() == null) {
            errors.add(new FieldError("academicYearId", "must not be null", "NotNull"));
        }
        if (entity.getStaffId() == null) {
            errors.add(new FieldError("staffId", "must not be null", "NotNull"));
        }
        if (entity.getFromAt() == null) {
            errors.add(new FieldError("fromAt", "must not be null", "NotNull"));
        }
        if (entity.getToAt() == null) {
            errors.add(new FieldError("toAt", "must not be null", "NotNull"));
        }
        if (!creating && entity.getDayPart() == null) {
            errors.add(new FieldError("dayPart", "must not be null", "NotNull"));
        }
        if (entity.getLeaveTypeId() != null && !repository.existsLeaveTypeId(entity.getLeaveTypeId())) {
            errors.add(new FieldError("leaveTypeId", "leave type does not exist", "NOT_FOUND"));
        }
        if (entity.getStaffId() != null && !repository.existsStaffId(entity.getStaffId())) {
            errors.add(new FieldError("staffId", "staff does not exist", "NOT_FOUND"));
        }
        if (errors.isEmpty()) {
            // leave is counted in the staff member's own school, against a type that school offers for the year
            if (!ledger.staffOfInstitution(entity.getStaffId(), entity.getInstitutionId())) {
                errors.add(new FieldError("staffId", "is not a staff member of the institution", "OtherInstitution"));
            }
            if (entity.getLeaveTypeId() != null && !ledger.offersLeaveType(entity.getLeaveTypeId(),
                    entity.getInstitutionId(), entity.getAcademicYearId())) {
                errors.add(new FieldError("leaveTypeId",
                        "is not an active leave type of the institution for this academic year", "NotOffered"));
            }
            boolean oneDay = entity.getFromAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate()
                    .equals(entity.getToAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate());
            if (entity.getDayPart() != null && !"full_day".equals(entity.getDayPart()) && !oneDay) {
                errors.add(new FieldError("dayPart", "a half day is a leave of one day", "HalfDay"));
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(LeaveApplicationEntity entity) {

    }
}
