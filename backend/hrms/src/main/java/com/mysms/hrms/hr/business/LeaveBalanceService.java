package com.mysms.hrms.hr.business;

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
import com.mysms.core.web.Patches;
import com.mysms.hrms.hr.api.model.LeaveBalance;
import com.mysms.hrms.hr.api.model.LeaveBalanceUpdate;
import com.mysms.hrms.hr.config.HrDatabaseConfig;
import com.mysms.hrms.hr.config.HrEventsConfig;
import com.mysms.hrms.hr.domain.LeaveBalanceEntity;
import com.mysms.hrms.hr.events.HrEvents;
import com.mysms.hrms.hr.mapper.LeaveBalanceMapper;
import com.mysms.hrms.hr.repository.LeaveBalanceRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of hr.leave_balance: audit and domain event commit in the same transaction as every change. Rows
 * outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class LeaveBalanceService extends AppTransactionManager {

    public static final String AGGREGATE = LeaveBalanceEntity.TABLE;
    static final String PERMISSION_LIST = "hr.leave_balance.read";
    static final String PERMISSION_GET = "hr.leave_balance.read";
    static final String PERMISSION_CREATE = "hr.leave_balance.create";
    static final String PERMISSION_UPDATE = "hr.leave_balance.update";


    private final LeaveBalanceRepository repository;
    private final LeaveBalanceMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public LeaveBalanceService(LeaveBalanceRepository repository, LeaveBalanceMapper mapper,
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
    }

    public LeaveBalance getLeaveBalance(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<LeaveBalance> listLeaveBalances(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, LeaveBalanceRepository.SORT_FIELDS, LeaveBalanceRepository.DEFAULT_ORDER,
                LeaveBalanceRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, LeaveBalanceRepository.FILTER_FIELDS)
                .and(LeaveBalanceRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<LeaveBalanceEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public LeaveBalance createLeaveBalance(LeaveBalance request, boolean validateOnly) {
        validator.validate(request);
        LeaveBalanceEntity entity = mapper.toEntity(request);
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
            LeaveBalanceEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + HrEvents.CREATED, AGGREGATE, stored.getId(), HrEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public LeaveBalance updateLeaveBalance(UUID id, String ifMatch, LeaveBalanceUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            LeaveBalanceEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            UUID staffBefore = entity.getStaffId();
            UUID typeBefore = entity.getLeaveTypeId();
            UUID yearBefore = entity.getAcademicYearId();
            List<String> fields = apply(entity, changes);
            if (!Objects.equals(staffBefore, entity.getStaffId()) || !Objects.equals(typeBefore, entity.getLeaveTypeId())
                    || !Objects.equals(yearBefore, entity.getAcademicYearId())) {
                // the days used are this staff member's, of this type, in this year: a balance does not move
                throw new InvalidStateException(
                        "A leave balance keeps its staff member, leave type and academic year.");
            }
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

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private LeaveBalanceEntity loadVisible(DataScope scope, UUID id) {
        LeaveBalanceEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the hr.staff the row belongs to. */
    private Optional<UUID> institutionOf(LeaveBalanceEntity entity) {
        return repository.institutionOf(entity.getStaffId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(LeaveBalanceEntity entity, LeaveBalanceUpdate changes) {
        Patches patch = new Patches();
        patch.apply("staffId", changes.getStaffId(), entity::setStaffId);
        patch.apply("leaveTypeId", changes.getLeaveTypeId(), entity::setLeaveTypeId);
        patch.apply("academicYearId", changes.getAcademicYearId(), entity::setAcademicYearId);
        patch.apply("entitledDays", changes.getEntitledDays(), v -> entity.setEntitledDays(WireValues.decimal(v)));
        patch.apply("carriedForwardDays", changes.getCarriedForwardDays(),
                v -> entity.setCarriedForwardDays(WireValues.decimal(v)));
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(LeaveBalanceEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getStaffId() == null) {
            errors.add(new FieldError("staffId", "must not be null", "NotNull"));
        }
        if (entity.getAcademicYearId() == null) {
            // approved leave is counted against the balance of its academic year: one without a year counts nothing
            errors.add(new FieldError("academicYearId", "must not be null", "NotNull"));
        }
        if (entity.getEntitledDays() != null && entity.getUsedDays() != null
                && entity.getEntitledDays().add(entity.getCarriedForwardDays() == null ? BigDecimal.ZERO
                : entity.getCarriedForwardDays()).compareTo(entity.getUsedDays()) < 0) {
            errors.add(new FieldError("entitledDays", "must cover the days already used", "BelowUsed"));
        }
        if (entity.getLeaveTypeId() == null) {
            errors.add(new FieldError("leaveTypeId", "must not be null", "NotNull"));
        }
        if (entity.getEntitledDays() == null) {
            errors.add(new FieldError("entitledDays", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("entitledDays", WireValues.decimal(entity.getEntitledDays()), 5, 1, errors);
        if (!creating && entity.getCarriedForwardDays() == null) {
            errors.add(new FieldError("carriedForwardDays", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("carriedForwardDays", WireValues.decimal(entity.getCarriedForwardDays()), 5, 1, errors);
        if (entity.getLeaveTypeId() != null && !repository.existsLeaveTypeId(entity.getLeaveTypeId())) {
            errors.add(new FieldError("leaveTypeId", "leave type does not exist", "NOT_FOUND"));
        }
        if (entity.getStaffId() != null && !repository.existsStaffId(entity.getStaffId())) {
            errors.add(new FieldError("staffId", "staff does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(LeaveBalanceEntity entity) {
        if (entity.getAcademicYearId() != null && repository.takenStaffIdLeaveTypeIdAcademicYearId1(entity)) {
            throw new DuplicateResourceException(
                    "A leave balance with the same staff id and leave type id and academic year id already exists.");
        }
    }
}
