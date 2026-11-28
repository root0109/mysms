package com.mysms.hrms.payroll.business;

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
import com.mysms.hrms.payroll.api.model.StaffSalary;
import com.mysms.hrms.payroll.api.model.StaffSalaryComponent;
import com.mysms.hrms.payroll.api.model.StaffSalaryComponentUpdate;
import com.mysms.hrms.payroll.api.model.StaffSalaryUpdate;
import com.mysms.hrms.payroll.config.PayrollDatabaseConfig;
import com.mysms.hrms.payroll.config.PayrollEventsConfig;
import com.mysms.hrms.payroll.domain.StaffSalaryComponentEntity;
import com.mysms.hrms.payroll.domain.StaffSalaryEntity;
import com.mysms.hrms.payroll.events.PayrollEvents;
import com.mysms.hrms.payroll.mapper.StaffSalaryComponentMapper;
import com.mysms.hrms.payroll.mapper.StaffSalaryMapper;
import com.mysms.hrms.payroll.repository.StaffSalaryComponentRepository;
import com.mysms.hrms.payroll.repository.StaffSalaryRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of payroll.staff_salary and of the records that exist only as part of a StaffSalary
 * (StaffSalaryComponent): audit and domain event commit in the same transaction as every change. Rows outside the
 * caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class StaffSalaryService extends AppTransactionManager {

    public static final String AGGREGATE = StaffSalaryEntity.TABLE;
    static final String PERMISSION_LIST = "payroll.staff_salary.read";
    static final String PERMISSION_GET = "payroll.staff_salary.read";
    static final String PERMISSION_CREATE = "payroll.staff_salary.create";
    static final String PERMISSION_UPDATE = "payroll.staff_salary.update";
    static final String PERMISSION_DELETE = "payroll.staff_salary.delete";

    public static final String STAFF_SALARY_COMPONENT_AGGREGATE = StaffSalaryComponentEntity.TABLE;
    static final String STAFF_SALARY_COMPONENT_PERMISSION_LIST = "payroll.staff_salary_component.read";
    static final String STAFF_SALARY_COMPONENT_PERMISSION_GET = "payroll.staff_salary_component.read";
    static final String STAFF_SALARY_COMPONENT_PERMISSION_CREATE = "payroll.staff_salary_component.create";
    static final String STAFF_SALARY_COMPONENT_PERMISSION_UPDATE = "payroll.staff_salary_component.update";
    static final String STAFF_SALARY_COMPONENT_PERMISSION_DELETE = "payroll.staff_salary_component.delete";

    private final StaffSalaryRepository repository;
    private final StaffSalaryMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final StaffSalaryComponentRepository staffSalaryComponentRepository;
    private final StaffSalaryComponentMapper staffSalaryComponentMapper;

    public StaffSalaryService(StaffSalaryRepository repository,
            StaffSalaryMapper mapper,
            @Qualifier(PayrollDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PayrollDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PayrollEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PayrollEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            StaffSalaryComponentRepository staffSalaryComponentRepository,
            StaffSalaryComponentMapper staffSalaryComponentMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.staffSalaryComponentRepository = staffSalaryComponentRepository;
        this.staffSalaryComponentMapper = staffSalaryComponentMapper;
    }

    public StaffSalary getStaffSalary(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<StaffSalary> listStaffSalarys(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, StaffSalaryRepository.SORT_FIELDS, StaffSalaryRepository.DEFAULT_ORDER,
                StaffSalaryRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, StaffSalaryRepository.FILTER_FIELDS)
                .and(StaffSalaryRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<StaffSalaryEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public StaffSalary createStaffSalary(StaffSalary request, boolean validateOnly) {
        validator.validate(request);
        StaffSalaryEntity entity = mapper.toEntity(request);
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
            StaffSalaryEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + PayrollEvents.CREATED, AGGREGATE, stored.getId(),
                    PayrollEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public StaffSalary updateStaffSalary(UUID id, String ifMatch, StaffSalaryUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            StaffSalaryEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
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
            outbox.publish(AGGREGATE + PayrollEvents.UPDATED, AGGREGATE, id, PayrollEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteStaffSalary(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            StaffSalaryEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByPayslipStaffSalaryId(id)) {
                throw new InvalidStateException(
                        "The staff salary is still referenced by payroll.payslip rows; remove those first.");
            }
            if (repository.referencedByStaffSalaryComponentStaffSalaryId(id)) {
                throw new InvalidStateException(
                        "The staff salary is still referenced by payroll.staff_salary_component rows; remove those "
                                + "first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + PayrollEvents.DELETED, AGGREGATE, id, PayrollEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private StaffSalaryEntity loadVisible(DataScope scope, UUID id) {
        StaffSalaryEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the payroll.salary_template the row belongs to. */
    private Optional<UUID> institutionOf(StaffSalaryEntity entity) {
        return repository.institutionOf(entity.getSalaryTemplateId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(StaffSalaryEntity entity, StaffSalaryUpdate changes) {
        Patches patch = new Patches();
        patch.apply("staffId", changes.getStaffId(), entity::setStaffId);
        patch.apply("salaryTemplateId", changes.getSalaryTemplateId(), entity::setSalaryTemplateId);
        patch.apply("grossSalary", changes.getGrossSalary(), v -> entity.setGrossSalary(WireValues.decimal(v)));
        patch.apply("effectiveDate", changes.getEffectiveDate(), entity::setEffectiveDate);
        patch.apply("remarks", changes.getRemarks(), entity::setRemarks);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(StaffSalaryEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getStaffId() == null) {
            errors.add(new FieldError("staffId", "must not be null", "NotNull"));
        }
        if (entity.getSalaryTemplateId() == null) {
            errors.add(new FieldError("salaryTemplateId", "must not be null", "NotNull"));
        }
        if (entity.getGrossSalary() == null) {
            errors.add(new FieldError("grossSalary", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("grossSalary", WireValues.decimal(entity.getGrossSalary()), 14, 2, errors);
        if (entity.getEffectiveDate() == null) {
            errors.add(new FieldError("effectiveDate", "must not be null", "NotNull"));
        }
        if (entity.getSalaryTemplateId() != null && !repository.existsSalaryTemplateId(entity.getSalaryTemplateId())) {
            errors.add(new FieldError("salaryTemplateId", "salary template does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(StaffSalaryEntity entity) {
        if (repository.takenStaffIdEffectiveDate1(entity)) {
            throw new DuplicateResourceException(
                    "A staff salary with the same staff id and effective date already exists.");
        }
    }

    // ---- StaffSalaryComponent ----

    public StaffSalaryComponent getStaffSalaryComponent(UUID staffSalaryId, UUID id) {
        return doInReadTransaction(() -> {
            return staffSalaryComponentMapper.toDto(loadVisibleStaffSalaryComponent(DataScope.current(
                    STAFF_SALARY_COMPONENT_PERMISSION_GET), staffSalaryId, id));
        });
    }

    public Page<StaffSalaryComponent> listStaffSalaryComponents(UUID staffSalaryId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, StaffSalaryComponentRepository.SORT_FIELDS,
                        StaffSalaryComponentRepository.DEFAULT_ORDER,
                StaffSalaryComponentRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, StaffSalaryComponentRepository.FILTER_FIELDS)
                .and(StaffSalaryComponentRepository.ofParent(staffSalaryId))
                        .and(StaffSalaryComponentRepository.scope(DataScope.current(STAFF_SALARY_COMPONENT_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + staffSalaryId, clock);
        return doInReadTransaction(() -> {
            Page<StaffSalaryComponentEntity> rows = staffSalaryComponentRepository.list(page, where);
            return rows.map(staffSalaryComponentMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public StaffSalaryComponent createStaffSalaryComponent(UUID staffSalaryId, StaffSalaryComponent request,
            boolean validateOnly) {
        validator.validate(request);
        StaffSalaryComponentEntity entity = staffSalaryComponentMapper.toEntity(request);
        entity.setStaffSalaryId(staffSalaryId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(STAFF_SALARY_COMPONENT_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!staffSalaryComponentRepository.existsStaffSalaryId(entity.getStaffSalaryId())) {
                throw new ResourceNotFoundException("payroll.staff_salary", entity.getStaffSalaryId());
            }
            if (!scope.canRead(institutionOfStaffSalaryComponent(entity))) {
                throw new ResourceNotFoundException("payroll.staff_salary", entity.getStaffSalaryId());
            }
            assertValidStaffSalaryComponent(entity, true);
            scope.requireWritable(institutionOfStaffSalaryComponent(entity));
            assertUniqueStaffSalaryComponent(entity);
            if (validateOnly) {
                return staffSalaryComponentMapper.toDto(entity);
            }
            StaffSalaryComponentEntity stored = staffSalaryComponentRepository.insert(entity);
            audit.recordCreate(STAFF_SALARY_COMPONENT_AGGREGATE, stored.getId());
            outbox.publish(STAFF_SALARY_COMPONENT_AGGREGATE + PayrollEvents.CREATED, STAFF_SALARY_COMPONENT_AGGREGATE,
                    stored.getId(),
                    PayrollEvents.payload(stored.getId()));
            return staffSalaryComponentMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public StaffSalaryComponent updateStaffSalaryComponent(UUID staffSalaryId, UUID id, String ifMatch,
            StaffSalaryComponentUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(STAFF_SALARY_COMPONENT_PERMISSION_UPDATE);
            StaffSalaryComponentEntity entity = loadVisibleStaffSalaryComponent(scope, staffSalaryId, id);
            scope.requireWritable(institutionOfStaffSalaryComponent(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyStaffSalaryComponent(entity, changes);
            validator.validate(staffSalaryComponentMapper.toDto(entity));
            assertValidStaffSalaryComponent(entity, false);
            scope.requireWritable(institutionOfStaffSalaryComponent(entity));
            assertUniqueStaffSalaryComponent(entity);
            if (fields.isEmpty()) {
                return staffSalaryComponentMapper.toDto(entity);
            }
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return staffSalaryComponentMapper.toDto(entity);
            }
            if (staffSalaryComponentRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(STAFF_SALARY_COMPONENT_AGGREGATE, id, fields);
            outbox.publish(STAFF_SALARY_COMPONENT_AGGREGATE + PayrollEvents.UPDATED, STAFF_SALARY_COMPONENT_AGGREGATE, id,
                    PayrollEvents.payload(id));
            return staffSalaryComponentMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteStaffSalaryComponent(UUID staffSalaryId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(STAFF_SALARY_COMPONENT_PERMISSION_DELETE);
            StaffSalaryComponentEntity entity = loadVisibleStaffSalaryComponent(scope, staffSalaryId, id);
            scope.requireWritable(institutionOfStaffSalaryComponent(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (staffSalaryComponentRepository.referencedByPayslipLineStaffSalaryComponentId(id)) {
                throw new InvalidStateException(
                        "The staff salary component is still referenced by payroll.payslip_line rows; remove those "
                                + "first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (staffSalaryComponentRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(STAFF_SALARY_COMPONENT_AGGREGATE, id);
            outbox.publish(STAFF_SALARY_COMPONENT_AGGREGATE + PayrollEvents.DELETED, STAFF_SALARY_COMPONENT_AGGREGATE, id,
                    PayrollEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private StaffSalaryComponentEntity loadVisibleStaffSalaryComponent(DataScope scope, UUID staffSalaryId, UUID id) {
        StaffSalaryComponentEntity entity = staffSalaryComponentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(STAFF_SALARY_COMPONENT_AGGREGATE, id));
        if (!scope.canRead(institutionOfStaffSalaryComponent(entity)) || !staffSalaryId.equals(entity.getStaffSalaryId())) {
            throw new ResourceNotFoundException(STAFF_SALARY_COMPONENT_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the payroll.staff_salary → payroll.salary_template the row belongs to. */
    private Optional<UUID> institutionOfStaffSalaryComponent(StaffSalaryComponentEntity entity) {
        return staffSalaryComponentRepository.institutionOf(entity.getStaffSalaryId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyStaffSalaryComponent(StaffSalaryComponentEntity entity, StaffSalaryComponentUpdate changes) {
        Patches patch = new Patches();
        patch.apply("salaryTemplateComponentId", changes.getSalaryTemplateComponentId(), entity::setSalaryTemplateComponentId);
        patch.apply("amount", changes.getAmount(), v -> entity.setAmount(WireValues.decimal(v)));
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidStaffSalaryComponent(StaffSalaryComponentEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getStaffSalaryId() == null) {
            errors.add(new FieldError("staffSalaryId", "must not be null", "NotNull"));
        }
        if (entity.getSalaryTemplateComponentId() == null) {
            errors.add(new FieldError("salaryTemplateComponentId", "must not be null", "NotNull"));
        }
        if (entity.getAmount() == null) {
            errors.add(new FieldError("amount", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("amount", WireValues.decimal(entity.getAmount()), 14, 2, errors);
        if (entity.getSalaryTemplateComponentId() != null
                && !staffSalaryComponentRepository.existsSalaryTemplateComponentId(entity.getSalaryTemplateComponentId())) {
            errors.add(
                    new FieldError("salaryTemplateComponentId", "salary template component does not exist",
                            "NOT_FOUND"));
        }
        if (entity.getStaffSalaryId() != null && !staffSalaryComponentRepository.existsStaffSalaryId(entity.getStaffSalaryId())) {
            errors.add(new FieldError("staffSalaryId", "staff salary does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueStaffSalaryComponent(StaffSalaryComponentEntity entity) {
        if (staffSalaryComponentRepository.takenStaffSalaryIdSalaryTemplateComponentId1(entity)) {
            throw new DuplicateResourceException(
                    "A staff salary component with the same staff salary id and salary template component id already "
                            + "exists.");
        }
    }
}
