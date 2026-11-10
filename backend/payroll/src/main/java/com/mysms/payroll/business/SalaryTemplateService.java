package com.mysms.payroll.business;

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
import com.mysms.core.web.UpdateMask;
import com.mysms.payroll.api.model.SalaryTemplate;
import com.mysms.payroll.api.model.SalaryTemplateComponent;
import com.mysms.payroll.api.model.SalaryTemplateComponentUpdate;
import com.mysms.payroll.api.model.SalaryTemplateUpdate;
import com.mysms.payroll.config.PayrollDatabaseConfig;
import com.mysms.payroll.config.PayrollEventsConfig;
import com.mysms.payroll.domain.SalaryTemplateComponentEntity;
import com.mysms.payroll.domain.SalaryTemplateEntity;
import com.mysms.payroll.events.PayrollEvents;
import com.mysms.payroll.mapper.SalaryTemplateComponentMapper;
import com.mysms.payroll.mapper.SalaryTemplateMapper;
import com.mysms.payroll.repository.SalaryTemplateComponentRepository;
import com.mysms.payroll.repository.SalaryTemplateRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of payroll.salary_template (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a SalaryTemplate
 * are served here too — SalaryTemplateComponent.
 */
@Service
public class SalaryTemplateService extends AppTransactionManager {

    public static final String AGGREGATE = SalaryTemplateEntity.TABLE;
    static final String PERMISSION_LIST = "payroll.salary_template.read";
    static final String PERMISSION_GET = "payroll.salary_template.read";
    static final String PERMISSION_CREATE = "payroll.salary_template.create";
    static final String PERMISSION_UPDATE = "payroll.salary_template.update";
    static final String PERMISSION_DELETE = "payroll.salary_template.delete";
    /** The contract's SalaryTemplateUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("institutionId", "name", "isActive");

    public static final String SALARY_TEMPLATE_COMPONENT_AGGREGATE = SalaryTemplateComponentEntity.TABLE;
    static final String SALARY_TEMPLATE_COMPONENT_PERMISSION_LIST = "payroll.salary_template_component.read";
    static final String SALARY_TEMPLATE_COMPONENT_PERMISSION_GET = "payroll.salary_template_component.read";
    static final String SALARY_TEMPLATE_COMPONENT_PERMISSION_CREATE = "payroll.salary_template_component.create";
    static final String SALARY_TEMPLATE_COMPONENT_PERMISSION_UPDATE = "payroll.salary_template_component.update";
    static final String SALARY_TEMPLATE_COMPONENT_PERMISSION_DELETE = "payroll.salary_template_component.delete";
    /** The contract's SalaryTemplateComponentUpdate fields, in apply order. */
    static final List<String> SALARY_TEMPLATE_COMPONENT_WRITABLE = List.of("payComponentId", "payCategoryId", "value");

    private final SalaryTemplateRepository repository;
    private final SalaryTemplateMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final SalaryTemplateComponentRepository salaryTemplateComponentRepository;
    private final SalaryTemplateComponentMapper salaryTemplateComponentMapper;

    public SalaryTemplateService(SalaryTemplateRepository repository,
            SalaryTemplateMapper mapper,
            @Qualifier(PayrollDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PayrollDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PayrollEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PayrollEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            SalaryTemplateComponentRepository salaryTemplateComponentRepository,
            SalaryTemplateComponentMapper salaryTemplateComponentMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.salaryTemplateComponentRepository = salaryTemplateComponentRepository;
        this.salaryTemplateComponentMapper = salaryTemplateComponentMapper;
    }

    public SalaryTemplate getSalaryTemplate(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<SalaryTemplate> listSalaryTemplates(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, SalaryTemplateRepository.SORT_FIELDS, SalaryTemplateRepository.DEFAULT_ORDER,
                SalaryTemplateRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, SalaryTemplateRepository.FILTER_FIELDS)
                .and(SalaryTemplateRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<SalaryTemplateEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public SalaryTemplate createSalaryTemplate(SalaryTemplate request, boolean validateOnly) {
        validator.validate(request);
        SalaryTemplateEntity entity = mapper.toEntity(request);
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
            SalaryTemplateEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + PayrollEvents.CREATED, AGGREGATE, stored.getId(),
                    PayrollEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public SalaryTemplate updateSalaryTemplate(UUID id, String ifMatch, SalaryTemplateUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            SalaryTemplateEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + PayrollEvents.UPDATED, AGGREGATE, id, PayrollEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteSalaryTemplate(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            SalaryTemplateEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedBySalaryTemplateComponentSalaryTemplateId(id)) {
                throw new InvalidStateException(
                        "The salary template is still referenced by payroll.salary_template_component rows; remove "
                                + "those first.");
            }
            if (repository.referencedByStaffSalarySalaryTemplateId(id)) {
                throw new InvalidStateException(
                        "The salary template is still referenced by payroll.staff_salary rows; remove those first.");
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
    private SalaryTemplateEntity loadVisible(DataScope scope, UUID id) {
        SalaryTemplateEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(SalaryTemplateEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /**
     * Applies the selected SalaryTemplateUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void apply(SalaryTemplateEntity entity, SalaryTemplateUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "name" -> entity.setName(changes.getName());
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(SalaryTemplateEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(SalaryTemplateEntity entity) {
        if (repository.takenInstitutionIdName1(entity)) {
            throw new DuplicateResourceException(
                    "A salary template with the same institution id and name already exists.");
        }
    }

    // ---- SalaryTemplateComponent: a record of the SalaryTemplate aggregate ----

    public SalaryTemplateComponent getSalaryTemplateComponent(UUID salaryTemplateId, UUID id) {
        return doInReadTransaction(() -> {
            return salaryTemplateComponentMapper.toDto(loadVisibleSalaryTemplateComponent(DataScope.current(
                    SALARY_TEMPLATE_COMPONENT_PERMISSION_GET), salaryTemplateId, id));
        });
    }

    public Page<SalaryTemplateComponent> listSalaryTemplateComponents(UUID salaryTemplateId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, SalaryTemplateComponentRepository.SORT_FIELDS,
                        SalaryTemplateComponentRepository.DEFAULT_ORDER,
                SalaryTemplateComponentRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, SalaryTemplateComponentRepository.FILTER_FIELDS)
                .and(SalaryTemplateComponentRepository.ofParent(salaryTemplateId))
                        .and(SalaryTemplateComponentRepository.scope(DataScope.current(
                                SALARY_TEMPLATE_COMPONENT_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + salaryTemplateId, clock);
        return doInReadTransaction(() -> {
            Page<SalaryTemplateComponentEntity> rows = salaryTemplateComponentRepository.list(page, where);
            return rows.map(salaryTemplateComponentMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public SalaryTemplateComponent createSalaryTemplateComponent(UUID salaryTemplateId, SalaryTemplateComponent request,
            boolean validateOnly) {
        validator.validate(request);
        SalaryTemplateComponentEntity entity = salaryTemplateComponentMapper.toEntity(request);
        entity.setSalaryTemplateId(salaryTemplateId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(SALARY_TEMPLATE_COMPONENT_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!salaryTemplateComponentRepository.existsSalaryTemplateId(entity.getSalaryTemplateId())) {
                throw new ResourceNotFoundException("payroll.salary_template", entity.getSalaryTemplateId());
            }
            if (!scope.canRead(institutionOfSalaryTemplateComponent(entity))) {
                throw new ResourceNotFoundException("payroll.salary_template", entity.getSalaryTemplateId());
            }
            assertValidSalaryTemplateComponent(entity, true);
            scope.requireWritable(institutionOfSalaryTemplateComponent(entity));
            assertUniqueSalaryTemplateComponent(entity);
            if (validateOnly) {
                return salaryTemplateComponentMapper.toDto(entity);
            }
            SalaryTemplateComponentEntity stored = salaryTemplateComponentRepository.insert(entity);
            audit.recordCreate(SALARY_TEMPLATE_COMPONENT_AGGREGATE, stored.getId());
            outbox.publish(SALARY_TEMPLATE_COMPONENT_AGGREGATE + PayrollEvents.CREATED, SALARY_TEMPLATE_COMPONENT_AGGREGATE,
                    stored.getId(),
                    PayrollEvents.payload(stored.getId()));
            return salaryTemplateComponentMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public SalaryTemplateComponent updateSalaryTemplateComponent(UUID salaryTemplateId, UUID id, String ifMatch,
            SalaryTemplateComponentUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, SALARY_TEMPLATE_COMPONENT_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(SALARY_TEMPLATE_COMPONENT_PERMISSION_UPDATE);
            SalaryTemplateComponentEntity entity = loadVisibleSalaryTemplateComponent(scope, salaryTemplateId, id);
            scope.requireWritable(institutionOfSalaryTemplateComponent(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applySalaryTemplateComponent(entity, changes, fields);
            validator.validate(salaryTemplateComponentMapper.toDto(entity));
            assertValidSalaryTemplateComponent(entity, false);
            scope.requireWritable(institutionOfSalaryTemplateComponent(entity));
            assertUniqueSalaryTemplateComponent(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return salaryTemplateComponentMapper.toDto(entity);
            }
            if (salaryTemplateComponentRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(SALARY_TEMPLATE_COMPONENT_AGGREGATE, id, fields);
            outbox.publish(SALARY_TEMPLATE_COMPONENT_AGGREGATE + PayrollEvents.UPDATED, SALARY_TEMPLATE_COMPONENT_AGGREGATE,
                    id, PayrollEvents.payload(id));
            return salaryTemplateComponentMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteSalaryTemplateComponent(UUID salaryTemplateId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(SALARY_TEMPLATE_COMPONENT_PERMISSION_DELETE);
            SalaryTemplateComponentEntity entity = loadVisibleSalaryTemplateComponent(scope, salaryTemplateId, id);
            scope.requireWritable(institutionOfSalaryTemplateComponent(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (salaryTemplateComponentRepository.referencedByStaffSalaryComponentSalaryTemplateComponentId(id)) {
                throw new InvalidStateException(
                        "The salary template component is still referenced by payroll.staff_salary_component rows; "
                                + "remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (salaryTemplateComponentRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(SALARY_TEMPLATE_COMPONENT_AGGREGATE, id);
            outbox.publish(SALARY_TEMPLATE_COMPONENT_AGGREGATE + PayrollEvents.DELETED, SALARY_TEMPLATE_COMPONENT_AGGREGATE,
                    id, PayrollEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private SalaryTemplateComponentEntity loadVisibleSalaryTemplateComponent(DataScope scope, UUID salaryTemplateId, UUID id) {
        SalaryTemplateComponentEntity entity = salaryTemplateComponentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(SALARY_TEMPLATE_COMPONENT_AGGREGATE, id));
        if (!scope.canRead(institutionOfSalaryTemplateComponent(entity))
                || !salaryTemplateId.equals(entity.getSalaryTemplateId())) {
            throw new ResourceNotFoundException(SALARY_TEMPLATE_COMPONENT_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the payroll.salary_template it belongs to). */
    private Optional<UUID> institutionOfSalaryTemplateComponent(SalaryTemplateComponentEntity entity) {
        return salaryTemplateComponentRepository.institutionOf(entity.getSalaryTemplateId());
    }

    /**
     * Applies the selected SalaryTemplateComponentUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applySalaryTemplateComponent(SalaryTemplateComponentEntity entity, SalaryTemplateComponentUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "payComponentId" -> entity.setPayComponentId(changes.getPayComponentId());
                case "payCategoryId" -> entity.setPayCategoryId(changes.getPayCategoryId());
                case "value" -> entity.setValue(WireValues.decimal(changes.getValue()));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidSalaryTemplateComponent(SalaryTemplateComponentEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getSalaryTemplateId() == null) {
            errors.add(new FieldError("salaryTemplateId", "must not be null", "NotNull"));
        }
        if (entity.getPayComponentId() == null) {
            errors.add(new FieldError("payComponentId", "must not be null", "NotNull"));
        }
        if (entity.getPayCategoryId() == null) {
            errors.add(new FieldError("payCategoryId", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("value", WireValues.decimal(entity.getValue()), 14, 4, errors);
        if (entity.getPayCategoryId() != null
                && !salaryTemplateComponentRepository.existsPayCategoryId(entity.getPayCategoryId())) {
            errors.add(new FieldError("payCategoryId", "pay category does not exist", "NOT_FOUND"));
        }
        if (entity.getPayComponentId() != null
                && !salaryTemplateComponentRepository.existsPayComponentId(entity.getPayComponentId())) {
            errors.add(new FieldError("payComponentId", "pay component does not exist", "NOT_FOUND"));
        }
        if (entity.getSalaryTemplateId() != null
                && !salaryTemplateComponentRepository.existsSalaryTemplateId(entity.getSalaryTemplateId())) {
            errors.add(new FieldError("salaryTemplateId", "salary template does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueSalaryTemplateComponent(SalaryTemplateComponentEntity entity) {
        if (salaryTemplateComponentRepository.takenSalaryTemplateIdPayComponentId1(entity)) {
            throw new DuplicateResourceException(
                    "A salary template component with the same salary template id and pay component id already "
                            + "exists.");
        }
    }
}
