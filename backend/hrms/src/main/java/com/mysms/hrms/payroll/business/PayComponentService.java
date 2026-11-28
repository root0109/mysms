package com.mysms.hrms.payroll.business;

import com.mysms.authclient.DataScope;
import com.mysms.hrms.payroll.api.model.PayComponent;
import com.mysms.hrms.payroll.api.model.PayComponentUpdate;
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
import com.mysms.core.web.Patches;
import com.mysms.hrms.payroll.config.PayrollDatabaseConfig;
import com.mysms.hrms.payroll.config.PayrollEventsConfig;
import com.mysms.hrms.payroll.domain.PayComponentEntity;
import com.mysms.hrms.payroll.events.PayrollEvents;
import com.mysms.hrms.payroll.mapper.PayComponentMapper;
import com.mysms.hrms.payroll.repository.PayComponentRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of payroll.pay_component: audit and domain event commit in the same transaction as every change.
 * Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class PayComponentService extends AppTransactionManager {

    public static final String AGGREGATE = PayComponentEntity.TABLE;
    static final String PERMISSION_LIST = "payroll.pay_component.read";
    static final String PERMISSION_GET = "payroll.pay_component.read";
    static final String PERMISSION_CREATE = "payroll.pay_component.create";
    static final String PERMISSION_UPDATE = "payroll.pay_component.update";
    static final String PERMISSION_DELETE = "payroll.pay_component.delete";


    private final PayComponentRepository repository;
    private final PayComponentMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public PayComponentService(PayComponentRepository repository, PayComponentMapper mapper,
            @Qualifier(PayrollDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PayrollDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PayrollEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(PayrollEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public PayComponent getPayComponent(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<PayComponent> listPayComponents(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, PayComponentRepository.SORT_FIELDS, PayComponentRepository.DEFAULT_ORDER,
                PayComponentRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PayComponentRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<PayComponentEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public PayComponent createPayComponent(PayComponent request, boolean validateOnly) {
        validator.validate(request);
        PayComponentEntity entity = mapper.toEntity(request);
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
            PayComponentEntity stored = repository.insert(entity);
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
    public PayComponent updatePayComponent(UUID id, String ifMatch, PayComponentUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            PayComponentEntity entity = loadVisible(scope, id);
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
    public void deletePayComponent(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            PayComponentEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedBySalaryTemplateComponentPayComponentId(id)) {
                throw new InvalidStateException(
                        "The pay component is still referenced by payroll.salary_template_component rows; remove "
                                + "those first.");
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
    private PayComponentEntity loadVisible(DataScope scope, UUID id) {
        PayComponentEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOf(PayComponentEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(PayComponentEntity entity, PayComponentUpdate changes) {
        Patches patch = new Patches();
        patch.apply("code", changes.getCode(), entity::setCode);
        patch.apply("name", changes.getName(), entity::setName);
        patch.apply("componentType", changes.getComponentType(), v -> entity.setComponentType(v == null ? null : v.getValue()));
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(PayComponentEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (entity.getComponentType() == null) {
            errors.add(new FieldError("componentType", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(PayComponentEntity entity) {
        if (repository.takenCodeCode(entity)) {
            throw new DuplicateResourceException("A pay component with the same code already exists.");
        }
    }
}
