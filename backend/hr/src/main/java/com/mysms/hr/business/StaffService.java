package com.mysms.hr.business;

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
import com.mysms.hr.api.model.LeaveBalanceSummary;
import com.mysms.hr.api.model.Staff;
import com.mysms.hr.api.model.StaffDepartment;
import com.mysms.hr.api.model.StaffDepartmentUpdate;
import com.mysms.hr.api.model.StaffSubject;
import com.mysms.hr.api.model.StaffSubjectUpdate;
import com.mysms.hr.api.model.StaffUpdate;
import com.mysms.hr.config.HrDatabaseConfig;
import com.mysms.hr.config.HrEventsConfig;
import com.mysms.hr.domain.StaffDepartmentEntity;
import com.mysms.hr.domain.StaffEntity;
import com.mysms.hr.domain.StaffSubjectEntity;
import com.mysms.hr.events.HrEvents;
import com.mysms.hr.mapper.StaffDepartmentMapper;
import com.mysms.hr.mapper.StaffMapper;
import com.mysms.hr.mapper.StaffSubjectMapper;
import com.mysms.hr.repository.LeaveLedgerRepository;
import com.mysms.hr.repository.StaffDepartmentRepository;
import com.mysms.hr.repository.StaffRepository;
import com.mysms.hr.repository.StaffSubjectRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of hr.staff (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a Staff
 * are served here too — StaffDepartment, StaffSubject.
 */
@Service
public class StaffService extends AppTransactionManager {

    public static final String AGGREGATE = StaffEntity.TABLE;
    static final String PERMISSION_LIST = "hr.staff.read";
    static final String PERMISSION_GET = "hr.staff.read";
    static final String PERMISSION_CREATE = "hr.staff.create";
    static final String PERMISSION_UPDATE = "hr.staff.update";
    static final String PERMISSION_DELETE = "hr.staff.delete";
    static final String PERMISSION_GET_LEAVE_BALANCE = "hr.staff.get_leave_balance";
    static final String PERMISSION_READ_LEAVE_BALANCES = "hr.leave_balance.read";
    /** The contract's StaffUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("personId", "userId", "institutionId", "employeeCode", "designationId", "subDesignation",
                    "employmentType",
            "joiningDate", "reportingToStaffId", "primaryDepartmentId", "highestQualificationId", "qualificationDetail",
            "specialization", "interests", "maxPeriodsPerWeek");

    public static final String STAFF_DEPARTMENT_AGGREGATE = StaffDepartmentEntity.TABLE;
    static final String STAFF_DEPARTMENT_PERMISSION_LIST = "hr.staff_department.read";
    static final String STAFF_DEPARTMENT_PERMISSION_GET = "hr.staff_department.read";
    static final String STAFF_DEPARTMENT_PERMISSION_CREATE = "hr.staff_department.create";
    static final String STAFF_DEPARTMENT_PERMISSION_UPDATE = "hr.staff_department.update";
    static final String STAFF_DEPARTMENT_PERMISSION_DELETE = "hr.staff_department.delete";
    /** The contract's StaffDepartmentUpdate fields, in apply order. */
    static final List<String> STAFF_DEPARTMENT_WRITABLE = List.of("departmentId");

    public static final String STAFF_SUBJECT_AGGREGATE = StaffSubjectEntity.TABLE;
    static final String STAFF_SUBJECT_PERMISSION_LIST = "hr.staff_subject.read";
    static final String STAFF_SUBJECT_PERMISSION_GET = "hr.staff_subject.read";
    static final String STAFF_SUBJECT_PERMISSION_CREATE = "hr.staff_subject.create";
    static final String STAFF_SUBJECT_PERMISSION_UPDATE = "hr.staff_subject.update";
    static final String STAFF_SUBJECT_PERMISSION_DELETE = "hr.staff_subject.delete";
    /** The contract's StaffSubjectUpdate fields, in apply order. */
    static final List<String> STAFF_SUBJECT_WRITABLE = List.of("subjectId");

    private final StaffRepository repository;
    private final StaffMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final LeaveLedgerRepository leaveLedger;
    private final StaffDepartmentRepository staffDepartmentRepository;
    private final StaffDepartmentMapper staffDepartmentMapper;
    private final StaffSubjectRepository staffSubjectRepository;
    private final StaffSubjectMapper staffSubjectMapper;

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
            StaffSubjectRepository staffSubjectRepository,
            StaffSubjectMapper staffSubjectMapper,
            LeaveLedgerRepository leaveLedger) {
        super(database, transactions);
        this.leaveLedger = leaveLedger;
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.staffDepartmentRepository = staffDepartmentRepository;
        this.staffDepartmentMapper = staffDepartmentMapper;
        this.staffSubjectRepository = staffSubjectRepository;
        this.staffSubjectMapper = staffSubjectMapper;
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
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Staff updateStaff(UUID id, String ifMatch, StaffUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            StaffEntity entity = loadVisible(scope, id);
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
            if (repository.referencedByStaffSubjectStaffId(id)) {
                throw new InvalidStateException(
                        "The staff is still referenced by hr.staff_subject rows; remove those first.");
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

    /** The event payload of a staff record as stored: whose it is, where, and its row version. */
    private static Map<String, Object> memberPayload(StaffEntity staff) {
        return HrEvents.member(staff.getId(), staff.getUserId(), staff.getPersonId(), staff.getInstitutionId(),
                staff.getRowVersion());
    }

    /**
     * hr.GetStaffLeaveBalance: the staff member's position per active leave type of their institution for one
     * academic year — entitled, taken (approved), pending (asked for) and available (what is left after both, never
     * below zero; an approval itself looks only at what is taken, so of several pending applications the first
     * approved gets the days). The staff member reads their own; anyone else also needs the permission to read
     * leave balances in the staff member's institution. hr does not know which academic year is the current one
     * (org owns that), so the caller names the year.
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

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(StaffEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected StaffUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(StaffEntity entity, StaffUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "personId" -> entity.setPersonId(changes.getPersonId());
                case "userId" -> entity.setUserId(changes.getUserId());
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "employeeCode" -> entity.setEmployeeCode(changes.getEmployeeCode());
                case "designationId" -> entity.setDesignationId(changes.getDesignationId());
                case "subDesignation" -> entity.setSubDesignation(changes.getSubDesignation());
                case "employmentType" -> entity.setEmploymentType(Optional.ofNullable(changes.getEmploymentType())
                        .map(v -> v.getValue()).orElse(null));
                case "joiningDate" -> entity.setJoiningDate(changes.getJoiningDate());
                case "reportingToStaffId" -> entity.setReportingToStaffId(changes.getReportingToStaffId());
                case "primaryDepartmentId" -> entity.setPrimaryDepartmentId(changes.getPrimaryDepartmentId());
                case "highestQualificationId" -> entity.setHighestQualificationId(changes.getHighestQualificationId());
                case "qualificationDetail" -> entity.setQualificationDetail(changes.getQualificationDetail());
                case "specialization" -> entity.setSpecialization(changes.getSpecialization());
                case "interests" -> entity.setInterests(changes.getInterests());
                case "maxPeriodsPerWeek" -> entity.setMaxPeriodsPerWeek(changes.getMaxPeriodsPerWeek());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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

    // ---- StaffDepartment: a record of the Staff aggregate ----

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
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public StaffDepartment updateStaffDepartment(UUID staffId, UUID id, String ifMatch, StaffDepartmentUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, STAFF_DEPARTMENT_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(STAFF_DEPARTMENT_PERMISSION_UPDATE);
            StaffDepartmentEntity entity = loadVisibleStaffDepartment(scope, staffId, id);
            scope.requireWritable(institutionOfStaffDepartment(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyStaffDepartment(entity, changes, fields);
            validator.validate(staffDepartmentMapper.toDto(entity));
            assertValidStaffDepartment(entity, false);
            scope.requireWritable(institutionOfStaffDepartment(entity));
            assertUniqueStaffDepartment(entity);
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

    /** The institution that owns the row (the institution of the hr.staff it belongs to). */
    private Optional<UUID> institutionOfStaffDepartment(StaffDepartmentEntity entity) {
        return staffDepartmentRepository.institutionOf(entity.getStaffId());
    }

    /**
     * Applies the selected StaffDepartmentUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyStaffDepartment(StaffDepartmentEntity entity, StaffDepartmentUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "departmentId" -> entity.setDepartmentId(changes.getDepartmentId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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

    // ---- StaffSubject: a record of the Staff aggregate ----

    public StaffSubject getStaffSubject(UUID staffId, UUID id) {
        return doInReadTransaction(() -> {
            return staffSubjectMapper.toDto(loadVisibleStaffSubject(DataScope.current(STAFF_SUBJECT_PERMISSION_GET), staffId,
                    id));
        });
    }

    public Page<StaffSubject> listStaffSubjects(UUID staffId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, StaffSubjectRepository.SORT_FIELDS, StaffSubjectRepository.DEFAULT_ORDER,
                StaffSubjectRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, StaffSubjectRepository.FILTER_FIELDS)
                .and(StaffSubjectRepository.ofParent(staffId))
                        .and(StaffSubjectRepository.scope(DataScope.current(STAFF_SUBJECT_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + staffId, clock);
        return doInReadTransaction(() -> {
            Page<StaffSubjectEntity> rows = staffSubjectRepository.list(page, where);
            return rows.map(staffSubjectMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public StaffSubject createStaffSubject(UUID staffId, StaffSubject request, boolean validateOnly) {
        validator.validate(request);
        StaffSubjectEntity entity = staffSubjectMapper.toEntity(request);
        entity.setStaffId(staffId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(STAFF_SUBJECT_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!staffSubjectRepository.existsStaffId(entity.getStaffId())) {
                throw new ResourceNotFoundException("hr.staff", entity.getStaffId());
            }
            if (!scope.canRead(institutionOfStaffSubject(entity))) {
                throw new ResourceNotFoundException("hr.staff", entity.getStaffId());
            }
            assertValidStaffSubject(entity, true);
            scope.requireWritable(institutionOfStaffSubject(entity));
            assertUniqueStaffSubject(entity);
            if (validateOnly) {
                return staffSubjectMapper.toDto(entity);
            }
            StaffSubjectEntity stored = staffSubjectRepository.insert(entity);
            audit.recordCreate(STAFF_SUBJECT_AGGREGATE, stored.getId());
            outbox.publish(STAFF_SUBJECT_AGGREGATE + HrEvents.CREATED, STAFF_SUBJECT_AGGREGATE, stored.getId(),
                    HrEvents.payload(stored.getId()));
            return staffSubjectMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public StaffSubject updateStaffSubject(UUID staffId, UUID id, String ifMatch, StaffSubjectUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, STAFF_SUBJECT_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(STAFF_SUBJECT_PERMISSION_UPDATE);
            StaffSubjectEntity entity = loadVisibleStaffSubject(scope, staffId, id);
            scope.requireWritable(institutionOfStaffSubject(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyStaffSubject(entity, changes, fields);
            validator.validate(staffSubjectMapper.toDto(entity));
            assertValidStaffSubject(entity, false);
            scope.requireWritable(institutionOfStaffSubject(entity));
            assertUniqueStaffSubject(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return staffSubjectMapper.toDto(entity);
            }
            if (staffSubjectRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(STAFF_SUBJECT_AGGREGATE, id, fields);
            outbox.publish(STAFF_SUBJECT_AGGREGATE + HrEvents.UPDATED, STAFF_SUBJECT_AGGREGATE, id, HrEvents.payload(id));
            return staffSubjectMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteStaffSubject(UUID staffId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(STAFF_SUBJECT_PERMISSION_DELETE);
            StaffSubjectEntity entity = loadVisibleStaffSubject(scope, staffId, id);
            scope.requireWritable(institutionOfStaffSubject(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (staffSubjectRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(STAFF_SUBJECT_AGGREGATE, id);
            outbox.publish(STAFF_SUBJECT_AGGREGATE + HrEvents.DELETED, STAFF_SUBJECT_AGGREGATE, id, HrEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private StaffSubjectEntity loadVisibleStaffSubject(DataScope scope, UUID staffId, UUID id) {
        StaffSubjectEntity entity = staffSubjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(STAFF_SUBJECT_AGGREGATE, id));
        if (!scope.canRead(institutionOfStaffSubject(entity)) || !staffId.equals(entity.getStaffId())) {
            throw new ResourceNotFoundException(STAFF_SUBJECT_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the hr.staff it belongs to). */
    private Optional<UUID> institutionOfStaffSubject(StaffSubjectEntity entity) {
        return staffSubjectRepository.institutionOf(entity.getStaffId());
    }

    /**
     * Applies the selected StaffSubjectUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyStaffSubject(StaffSubjectEntity entity, StaffSubjectUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "subjectId" -> entity.setSubjectId(changes.getSubjectId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidStaffSubject(StaffSubjectEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getStaffId() == null) {
            errors.add(new FieldError("staffId", "must not be null", "NotNull"));
        }
        if (entity.getSubjectId() == null) {
            errors.add(new FieldError("subjectId", "must not be null", "NotNull"));
        }
        if (entity.getStaffId() != null && !staffSubjectRepository.existsStaffId(entity.getStaffId())) {
            errors.add(new FieldError("staffId", "staff does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueStaffSubject(StaffSubjectEntity entity) {
        if (staffSubjectRepository.takenStaffIdSubjectId1(entity)) {
            throw new DuplicateResourceException(
                    "A staff subject with the same staff id and subject id already exists.");
        }
    }
}
