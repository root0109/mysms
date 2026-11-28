package com.mysms.hrms.hr.business;

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
import com.mysms.core.web.Patches;
import com.mysms.hrms.hr.api.model.ChangeStaffStatusRequest;
import com.mysms.hrms.hr.api.model.CreateStaffPortalAccountRequest;
import com.mysms.hrms.hr.api.model.LeaveBalanceSummary;
import com.mysms.hrms.hr.api.model.Staff;
import com.mysms.hrms.hr.api.model.StaffDepartment;
import com.mysms.hrms.hr.api.model.StaffDepartmentUpdate;
import com.mysms.hrms.hr.api.model.StaffUpdate;
import com.mysms.hrms.hr.config.HrDatabaseConfig;
import com.mysms.hrms.hr.config.HrEventsConfig;
import com.mysms.hrms.hr.domain.StaffDepartmentEntity;
import com.mysms.hrms.hr.domain.StaffEntity;
import com.mysms.hrms.hr.events.HrEvents;
import com.mysms.hrms.hr.mapper.StaffDepartmentMapper;
import com.mysms.hrms.hr.mapper.StaffMapper;
import com.mysms.hrms.hr.repository.LeaveLedgerRepository;
import com.mysms.hrms.hr.repository.StaffDepartmentRepository;
import com.mysms.hrms.hr.repository.StaffRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
 * Business service of hr.staff and of the records that exist only as part of a Staff (StaffDepartment):
 * audit and domain event commit in the same transaction as every change. Rows outside the caller's institutions are
 * invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class StaffService extends AppTransactionManager {

    public static final String AGGREGATE = StaffEntity.TABLE;
    static final String PERMISSION_LIST = "hr.staff.read";
    static final String PERMISSION_GET = "hr.staff.read";
    public static final String PERMISSION_CREATE = "hr.staff.create";
    static final String PERMISSION_UPDATE = "hr.staff.update";
    static final String PERMISSION_DELETE = "hr.staff.delete";
    static final String PERMISSION_CHANGE_STATUS = "hr.staff.change_staff_status";
    static final String PERMISSION_CREATE_PORTAL_ACCOUNT = "hr.staff.create_staff_portal_account";
    static final String PERMISSION_GET_LEAVE_BALANCE = "hr.staff.get_leave_balance";
    static final String PERMISSION_READ_LEAVE_BALANCES = "hr.leave_balance.read";

    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_INACTIVE = "inactive";
    private static final String STATUS_EXITED = "exited";
    /**
     * hr.ChangeStaffStatus, from each status to the statuses it may become: a staff member is suspended and taken back,
     * leaves from either state, and one who left comes back as active — the same record, because an institution keeps
     * one staff record per person.
     */
    static final Map<String, Set<String>> STATUS_MOVES = Map.of(
            STATUS_ACTIVE, Set.of(STATUS_INACTIVE, STATUS_EXITED),
            STATUS_INACTIVE, Set.of(STATUS_ACTIVE, STATUS_EXITED),
            STATUS_EXITED, Set.of(STATUS_ACTIVE));

    public static final String STAFF_DEPARTMENT_AGGREGATE = StaffDepartmentEntity.TABLE;
    static final String STAFF_DEPARTMENT_PERMISSION_LIST = "hr.staff_department.read";
    static final String STAFF_DEPARTMENT_PERMISSION_GET = "hr.staff_department.read";
    static final String STAFF_DEPARTMENT_PERMISSION_CREATE = "hr.staff_department.create";
    static final String STAFF_DEPARTMENT_PERMISSION_UPDATE = "hr.staff_department.update";
    static final String STAFF_DEPARTMENT_PERMISSION_DELETE = "hr.staff_department.delete";


    private final StaffRepository repository;
    private final StaffMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final LeaveLedgerRepository leaveLedger;
    private final StaffDepartmentRepository staffDepartmentRepository;
    private final StaffDepartmentMapper staffDepartmentMapper;
    private final StaffAccountLookup accounts;

    public StaffService(StaffRepository repository,
            StaffMapper mapper,
            @Qualifier(HrDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(HrDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(HrEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(HrEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            StaffDepartmentRepository staffDepartmentRepository,
            StaffDepartmentMapper staffDepartmentMapper,
            LeaveLedgerRepository leaveLedger,
            StaffAccountLookup accounts) {
        super(database, transactions);
        this.accounts = accounts;
        this.leaveLedger = leaveLedger;
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.staffDepartmentRepository = staffDepartmentRepository;
        this.staffDepartmentMapper = staffDepartmentMapper;
    }

    public Staff getStaff(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Staff> listStaffs(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, StaffRepository.SORT_FIELDS, StaffRepository.DEFAULT_ORDER,
                StaffRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, StaffRepository.FILTER_FIELDS)
                .and(StaffRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<StaffEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Staff createStaff(Staff request, boolean validateOnly) {
        validator.validate(request);
        if (request.getUserId() != null) {
            throw accountGivenByItsOperation();
        }
        StaffEntity entity = mapper.toEntity(request);
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
            StaffEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + HrEvents.CREATED, AGGREGATE, stored.getId(), memberPayload(stored));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public Staff updateStaff(UUID id, String ifMatch, StaffUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            StaffEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            UUID account = Patches.value(changes.getUserId());
            if (account != null && !account.equals(entity.getUserId())) {
                throw accountGivenByItsOperation();
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
            outbox.publish(AGGREGATE + HrEvents.UPDATED, AGGREGATE, id, memberPayload(entity));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteStaff(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            StaffEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByLeaveApplicationStaffId(id)) {
                throw new InvalidStateException(
                        "The staff is still referenced by hr.leave_application rows; remove those first.");
            }
            if (repository.referencedByLeaveBalanceStaffId(id)) {
                throw new InvalidStateException(
                        "The staff is still referenced by hr.leave_balance rows; remove those first.");
            }
            if (repository.referencedByStaffReportingToStaffId(id)) {
                throw new InvalidStateException("The staff is still referenced by hr.staff rows; remove those first.");
            }
            if (repository.referencedByStaffDepartmentStaffId(id)) {
                throw new InvalidStateException(
                        "The staff is still referenced by hr.staff_department rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + HrEvents.DELETED, AGGREGATE, id, memberPayload(entity));
        });
    }

    /**
     * hr.ChangeStaffStatus: activates, deactivates or exits a staff member, along {@link #STATUS_MOVES}. The row must
     * be visible and writable for the caller; a move the current status does not allow — the status it already has
     * included — answers 409 and nothing changes; a stale body etag is 412.
     */
    public Staff changeStaffStatus(UUID id, ChangeStaffStatusRequest request) {
        validator.validate(request);
        String status = request.getStatus().getValue();
        DataScope scope = DataScope.current(PERMISSION_CHANGE_STATUS);
        return doInWriteTransaction(() -> {
            StaffEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!STATUS_MOVES.getOrDefault(entity.getStatus(), Set.of()).contains(status)) {
                throw new InvalidStateException("A staff member who is " + entity.getStatus() + " cannot become "
                        + status + ".");
            }
            if (repository.changeStatus(id, entity.getRowVersion(), status, now(), Actor.current().orElse(null))
                    == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            StaffEntity changed = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            audit.recordTransition(AGGREGATE, id, "changeStaffStatus", List.of("status"), null);
            outbox.publish(AGGREGATE + HrEvents.UPDATED, AGGREGATE, id, memberPayload(changed));
            return mapper.toDto(changed);
        });
    }

    /**
     * hr.CreateStaffPortalAccount: gives a staff member without a login account one, created in iam with the caller's
     * own token ({@link StaffAccountLookup}) and signing in with the employee number, then stored on the staff row. No
     * transaction is held while iam is asked; a repeated call after a half-way failure finds and stores the account. An
     * existing account or a staff member who left is 409, no employee number 422. iam sends no invitations yet, so
     * asking for one is 422 rather than silently dropped.
     */
    public Staff createStaffPortalAccount(UUID id, CreateStaffPortalAccountRequest request) {
        if (request != null && Boolean.TRUE.equals(request.getSendInvite())) {
            throw new ValidationFailedException(List.of(new FieldError("sendInvite",
                    "invitations cannot be sent yet; set the first password in user administration", "NOT_SUPPORTED")));
        }
        DataScope scope = DataScope.current(PERMISSION_CREATE_PORTAL_ACCOUNT);
        StaffEntity member = doInPrimaryReadTransaction(() -> {
            StaffEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getUserId() != null) {
                throw new InvalidStateException("The staff member already has a login account.");
            }
            if (STATUS_EXITED.equals(entity.getStatus())) {
                throw new InvalidStateException("A staff member who has left is not given a login account.");
            }
            if (entity.getEmployeeCode() == null || entity.getEmployeeCode().isBlank()) {
                throw new ValidationFailedException(List.of(new FieldError("employeeCode",
                        "the staff member needs an employee number: it becomes the login name", "NotNull")));
            }
            return entity;
        });
        UUID account = accounts.accountFor(member.getPersonId(), member.getEmployeeCode().trim(),
                member.getJoiningDate());
        return doInWriteTransaction(() -> {
            // iam was asked outside any transaction: what was checked before is checked again on the row as it is now
            StaffEntity current = loadVisible(scope, id);
            scope.requireWritable(institutionOf(current));
            if (current.getUserId() != null) {
                return sameAccount(current, account);
            }
            if (STATUS_EXITED.equals(current.getStatus())) {
                throw new InvalidStateException("A staff member who has left is not given a login account.");
            }
            if (!current.getPersonId().equals(member.getPersonId())) {
                throw new InvalidStateException("The staff record names another person since the account was asked for.");
            }
            if (repository.attachAccount(id, current.getRowVersion(), account, now(), Actor.current().orElse(null))
                    == 0) {
                // a concurrent call or change came between the read and the write
                StaffEntity changed = loadVisible(scope, id);
                if (changed.getUserId() != null) {
                    return sameAccount(changed, account);
                }
                throw new PreconditionFailedException("The staff record changed while the account was created.");
            }
            StaffEntity stored = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (repository.takenInstitutionIdUserId2(stored)) {
                throw new DuplicateResourceException(
                        "Another staff record of this institution already has this person's login account.");
            }
            audit.recordUpdate(AGGREGATE, id, List.of("userId"));
            outbox.publish(AGGREGATE + HrEvents.UPDATED, AGGREGATE, id, memberPayload(stored));
            return mapper.toDto(stored);
        });
    }

    /** The answer when the staff member has an account by now: this call's account is success, any other a conflict. */
    private Staff sameAccount(StaffEntity current, UUID account) {
        if (!account.equals(current.getUserId())) {
            throw new InvalidStateException("The staff member already has a login account.");
        }
        return mapper.toDto(current);
    }

    /**
     * The login account is what iam.GetMe names a staff record by, so only hr.CreateStaffPortalAccount gives it: the
     * standard create and update refuse a value (422), though an update may clear a wrong one.
     */
    private static ValidationFailedException accountGivenByItsOperation() {
        return new ValidationFailedException(List.of(new FieldError("userId",
                "the login account is given by creating the staff member's portal account", "READ_ONLY")));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    /** The event payload of a staff record as stored: whose it is, where, and its row version. */
    private static Map<String, Object> memberPayload(StaffEntity staff) {
        return HrEvents.member(staff.getId(), staff.getUserId(), staff.getPersonId(), staff.getInstitutionId(),
                staff.getJoiningDate(), staff.getRowVersion());
    }

    /**
     * hr.GetStaffLeaveBalance: the staff member's position per active leave type for one academic year: entitled,
     * taken, pending and available (after both, never below zero). The staff member reads their own; anyone else needs
     * the permission to read leave balances in that institution. hr does not know the current academic year, so the
     * caller names it.
     */
    public Page<LeaveBalanceSummary> getStaffLeaveBalance(UUID staffId, UUID academicYearId, Integer pageNumber,
            Integer pageSize, String cursor) {
        DataScope scope = DataScope.current(PERMISSION_GET_LEAVE_BALANCE);
        AuthContext caller = AuthContext.require();
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, LeaveLedgerRepository.POSITION_ORDER,
                staffId + "|" + academicYearId, clock);
        return doInReadTransaction(() -> {
            StaffEntity member = loadVisible(scope, staffId);
            boolean own = Objects.equals(caller.userId(), member.getUserId())
                    || caller.personId().filter(person -> person.equals(member.getPersonId())).isPresent();
            if (!own && !(caller.hasPermission(PERMISSION_READ_LEAVE_BALANCES)
                    && DataScope.current(PERMISSION_READ_LEAVE_BALANCES).canRead(institutionOf(member)))) {
                throw new PermissionDeniedException(
                        "Another staff member's leave position also needs " + PERMISSION_READ_LEAVE_BALANCES + ".");
            }
            return leaveLedger.positions(staffId, member.getInstitutionId(), academicYearId, page)
                    .map(position -> new LeaveBalanceSummary(position.leaveTypeId(),
                            LeaveDays.plain(position.entitled()), LeaveDays.plain(position.taken()),
                            LeaveDays.plain(position.pending()), LeaveDays.plain(position.entitled()
                            .subtract(position.taken()).subtract(position.pending()).max(BigDecimal.ZERO))));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private StaffEntity loadVisible(DataScope scope, UUID id) {
        StaffEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(StaffEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(StaffEntity entity, StaffUpdate changes) {
        Patches patch = new Patches();
        patch.apply("personId", changes.getPersonId(), entity::setPersonId);
        patch.apply("userId", changes.getUserId(), entity::setUserId);
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("employeeCode", changes.getEmployeeCode(), entity::setEmployeeCode);
        patch.apply("designationId", changes.getDesignationId(), entity::setDesignationId);
        patch.apply("subDesignation", changes.getSubDesignation(), entity::setSubDesignation);
        patch.apply("employmentType", changes.getEmploymentType(),
                v -> entity.setEmploymentType(v == null ? null : v.getValue()));
        patch.apply("joiningDate", changes.getJoiningDate(), entity::setJoiningDate);
        patch.apply("reportingToStaffId", changes.getReportingToStaffId(), entity::setReportingToStaffId);
        patch.apply("primaryDepartmentId", changes.getPrimaryDepartmentId(), entity::setPrimaryDepartmentId);
        patch.apply("highestQualificationId", changes.getHighestQualificationId(), entity::setHighestQualificationId);
        patch.apply("qualificationDetail", changes.getQualificationDetail(), entity::setQualificationDetail);
        patch.apply("specialization", changes.getSpecialization(), entity::setSpecialization);
        patch.apply("interests", changes.getInterests(), entity::setInterests);
        patch.apply("maxPeriodsPerWeek", changes.getMaxPeriodsPerWeek(), entity::setMaxPeriodsPerWeek);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(StaffEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getPersonId() == null) {
            errors.add(new FieldError("personId", "must not be null", "NotNull"));
        }
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getEmploymentType() == null) {
            errors.add(new FieldError("employmentType", "must not be null", "NotNull"));
        }
        if (entity.getDesignationId() != null && !repository.existsDesignationId(entity.getDesignationId())) {
            errors.add(new FieldError("designationId", "designation does not exist", "NOT_FOUND"));
        }
        if (entity.getReportingToStaffId() != null
                && !repository.existsReportingToStaffId(entity.getReportingToStaffId())) {
            errors.add(new FieldError("reportingToStaffId", "staff does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(StaffEntity entity) {
        if (repository.takenInstitutionIdPersonId1(entity)) {
            throw new DuplicateResourceException("A staff with the same institution id and person id already exists.");
        }
        if (entity.getUserId() != null && repository.takenInstitutionIdUserId2(entity)) {
            throw new DuplicateResourceException("A staff with the same institution id and user id already exists.");
        }
    }

    // ---- StaffDepartment ----

    public StaffDepartment getStaffDepartment(UUID staffId, UUID id) {
        return doInReadTransaction(() -> {
            return staffDepartmentMapper.toDto(loadVisibleStaffDepartment(DataScope.current(STAFF_DEPARTMENT_PERMISSION_GET),
                    staffId, id));
        });
    }

    public Page<StaffDepartment> listStaffDepartments(UUID staffId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, StaffDepartmentRepository.SORT_FIELDS, StaffDepartmentRepository.DEFAULT_ORDER,
                StaffDepartmentRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, StaffDepartmentRepository.FILTER_FIELDS)
                .and(StaffDepartmentRepository.ofParent(staffId))
                        .and(StaffDepartmentRepository.scope(DataScope.current(STAFF_DEPARTMENT_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + staffId, clock);
        return doInReadTransaction(() -> {
            Page<StaffDepartmentEntity> rows = staffDepartmentRepository.list(page, where);
            return rows.map(staffDepartmentMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public StaffDepartment createStaffDepartment(UUID staffId, StaffDepartment request, boolean validateOnly) {
        validator.validate(request);
        StaffDepartmentEntity entity = staffDepartmentMapper.toEntity(request);
        entity.setStaffId(staffId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(STAFF_DEPARTMENT_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!staffDepartmentRepository.existsStaffId(entity.getStaffId())) {
                throw new ResourceNotFoundException("hr.staff", entity.getStaffId());
            }
            if (!scope.canRead(institutionOfStaffDepartment(entity))) {
                throw new ResourceNotFoundException("hr.staff", entity.getStaffId());
            }
            assertValidStaffDepartment(entity, true);
            scope.requireWritable(institutionOfStaffDepartment(entity));
            assertUniqueStaffDepartment(entity);
            if (validateOnly) {
                return staffDepartmentMapper.toDto(entity);
            }
            StaffDepartmentEntity stored = staffDepartmentRepository.insert(entity);
            audit.recordCreate(STAFF_DEPARTMENT_AGGREGATE, stored.getId());
            outbox.publish(STAFF_DEPARTMENT_AGGREGATE + HrEvents.CREATED, STAFF_DEPARTMENT_AGGREGATE, stored.getId(),
                    HrEvents.payload(stored.getId()));
            return staffDepartmentMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public StaffDepartment updateStaffDepartment(UUID staffId, UUID id, String ifMatch, StaffDepartmentUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(STAFF_DEPARTMENT_PERMISSION_UPDATE);
            StaffDepartmentEntity entity = loadVisibleStaffDepartment(scope, staffId, id);
            scope.requireWritable(institutionOfStaffDepartment(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyStaffDepartment(entity, changes);
            validator.validate(staffDepartmentMapper.toDto(entity));
            assertValidStaffDepartment(entity, false);
            scope.requireWritable(institutionOfStaffDepartment(entity));
            assertUniqueStaffDepartment(entity);
            if (fields.isEmpty()) {
                return staffDepartmentMapper.toDto(entity);
            }
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return staffDepartmentMapper.toDto(entity);
            }
            if (staffDepartmentRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(STAFF_DEPARTMENT_AGGREGATE, id, fields);
            outbox.publish(STAFF_DEPARTMENT_AGGREGATE + HrEvents.UPDATED, STAFF_DEPARTMENT_AGGREGATE, id, HrEvents.payload(id));
            return staffDepartmentMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteStaffDepartment(UUID staffId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(STAFF_DEPARTMENT_PERMISSION_DELETE);
            StaffDepartmentEntity entity = loadVisibleStaffDepartment(scope, staffId, id);
            scope.requireWritable(institutionOfStaffDepartment(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (staffDepartmentRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(STAFF_DEPARTMENT_AGGREGATE, id);
            outbox.publish(STAFF_DEPARTMENT_AGGREGATE + HrEvents.DELETED, STAFF_DEPARTMENT_AGGREGATE, id, HrEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private StaffDepartmentEntity loadVisibleStaffDepartment(DataScope scope, UUID staffId, UUID id) {
        StaffDepartmentEntity entity = staffDepartmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(STAFF_DEPARTMENT_AGGREGATE, id));
        if (!scope.canRead(institutionOfStaffDepartment(entity)) || !staffId.equals(entity.getStaffId())) {
            throw new ResourceNotFoundException(STAFF_DEPARTMENT_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the hr.staff the row belongs to. */
    private Optional<UUID> institutionOfStaffDepartment(StaffDepartmentEntity entity) {
        return staffDepartmentRepository.institutionOf(entity.getStaffId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyStaffDepartment(StaffDepartmentEntity entity, StaffDepartmentUpdate changes) {
        Patches patch = new Patches();
        patch.apply("departmentId", changes.getDepartmentId(), entity::setDepartmentId);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidStaffDepartment(StaffDepartmentEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getStaffId() == null) {
            errors.add(new FieldError("staffId", "must not be null", "NotNull"));
        }
        if (entity.getDepartmentId() == null) {
            errors.add(new FieldError("departmentId", "must not be null", "NotNull"));
        }
        if (entity.getStaffId() != null && !staffDepartmentRepository.existsStaffId(entity.getStaffId())) {
            errors.add(new FieldError("staffId", "staff does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueStaffDepartment(StaffDepartmentEntity entity) {
        if (staffDepartmentRepository.takenStaffIdDepartmentId1(entity)) {
            throw new DuplicateResourceException(
                    "A staff department with the same staff id and department id already exists.");
        }
    }
}
