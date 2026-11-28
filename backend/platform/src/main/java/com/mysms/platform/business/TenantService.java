package com.mysms.platform.business;

import com.mysms.authclient.DataScope;
import com.mysms.platform.api.model.Tenant;
import com.mysms.platform.api.model.TenantUpdate;
import com.mysms.platform.api.model.Operation;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContext;
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
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.config.PlatformEventsConfig;
import com.mysms.platform.domain.TenantEntity;
import com.mysms.platform.events.PlatformEvents;
import com.mysms.platform.mapper.TenantMapper;
import com.mysms.platform.repository.TenantRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of platform.tenant: audit and domain event in the same transaction as every change; rows outside the
 * caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class TenantService extends AppTransactionManager {

    public static final String AGGREGATE = TenantEntity.TABLE;


    private final TenantRepository repository;
    private final TenantMapper mapper;
    private final OperationService operations;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public TenantService(TenantRepository repository, TenantMapper mapper, OperationService operations,
            @Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PlatformDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PlatformEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(PlatformEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.operations = operations;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public Tenant getTenant(UUID id) {
        return doInGlobalReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.global(), id));
        });
    }

    public Page<Tenant> listTenants(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, TenantRepository.SORT_FIELDS, TenantRepository.DEFAULT_ORDER,
                TenantRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, TenantRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInGlobalReadTransaction(() -> {
            Page<TenantEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Tenant createTenant(Tenant request, boolean validateOnly) {
        validator.validate(request);
        TenantEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        DataScope scope = DataScope.global();
        return doInGlobalWriteTransaction(() -> {
            try (CallContextHolder.Scope global = CallContextHolder.withoutTenant()) {
                assertValid(entity, true);
                scope.requireWritable(institutionOf(entity));
                assertUnique(entity);
                if (validateOnly) {
                    return mapper.toDto(entity);
                }
                TenantEntity stored = repository.insert(entity);
                audit.recordCreate(AGGREGATE, stored.getId());
                outbox.publish(AGGREGATE + PlatformEvents.CREATED, AGGREGATE, stored.getId(),
                        eventPayload(stored, stored.getRowVersion()));
                return mapper.toDto(stored);

            }
        });
    }

    /**
     * Partial update under If-Match. A field the request leaves out is kept, a null one is cleared and a
     * value is set (§7.2). The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Tenant updateTenant(UUID id, String ifMatch, TenantUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInGlobalWriteTransaction(() -> {
            try (CallContextHolder.Scope global = CallContextHolder.withoutTenant()) {
                DataScope scope = DataScope.global();
                TenantEntity entity = loadVisible(scope, id);
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
                outbox.publish(AGGREGATE + PlatformEvents.UPDATED, AGGREGATE, id, eventPayload(entity, expected + 1));
                return mapper.toDto(entity);

            }
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteTenant(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInGlobalWriteTransaction(() -> {
            try (CallContextHolder.Scope global = CallContextHolder.withoutTenant()) {
                DataScope scope = DataScope.global();
                TenantEntity entity = loadVisible(scope, id);
                scope.requireWritable(institutionOf(entity));
                if (entity.getRowVersion() != expected) {
                    throw new PreconditionFailedException("If-Match does not match the current ETag.");
                }
                entity.onDeletingData(Actor.current(), clock);
                if (repository.softDelete(entity, expected) == 0) {
                    throw new PreconditionFailedException("If-Match does not match the current ETag.");
                }
                audit.recordDelete(AGGREGATE, id);
                outbox.publish(AGGREGATE + PlatformEvents.DELETED, AGGREGATE, id, eventPayload(entity, expected + 1));
            }
        });
    }

    /**
     * Registers the tenant iam's installation bootstrap made, named by its code until platform staff rename it; one that
     * exists already (live or deleted) is left as it is. A code another tenant holds is a
     * {@link DuplicateResourceException}.
     */
    public void registerBootstrapTenant(UUID id, String code) {
        runInWriteTransaction(() -> {
            if (repository.exists(id)) {
                return;
            }
            TenantEntity entity = TenantEntity.builder().code(code).name(code).build();
            entity.onCreatingData(Actor.current(), clock);
            entity.setId(id);
            assertUnique(entity);
            TenantEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, id);
            outbox.publish(AGGREGATE + PlatformEvents.CREATED, AGGREGATE, id, eventPayload(stored,
                    stored.getRowVersion()));
        });
    }

    /**
     * platform.ClearDemoData (platform staff): demo data lives in the modules that loaded it, which the platform may
     * not touch, so the platform announces the request in the tenant — the {@code platform.tenant.clearDemoData} event,
     * consumed by each module that loads demo data — and answers the finished operation that records the announcement.
     * A closed tenant is 409.
     */
    public Operation clearDemoData(UUID id) {
        TenantEntity tenant = doInGlobalReadTransaction(() -> loadVisible(DataScope.global(), id));
        if ("closed".equals(tenant.getStatus())) {
            throw new InvalidStateException("A closed tenant has no data to clear.");
        }
        CallContext staff = CallContextHolder.require();
        return CallContextHolder.callAs(staff.withIdentity(id, staff.userId().orElse(null)), () ->
                doInWriteTransaction(() -> {
                    Operation operation = operations.recordFinished("platform.ClearDemoData", AGGREGATE, id, null,
                            null);
                    audit.recordTransition(AGGREGATE, id, "clearDemoData", List.of(), null);
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("id", id.toString());
                    payload.put("operationId", operation.getOperationId().toString());
                    outbox.publish(AGGREGATE + ".clearDemoData", AGGREGATE, id, payload);
                    return operation;
                }));
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private TenantEntity loadVisible(DataScope scope, UUID id) {
        TenantEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOf(TenantEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(TenantEntity entity, TenantUpdate changes) {
        Patches patch = new Patches();
        patch.apply("code", changes.getCode(), entity::setCode);
        patch.apply("name", changes.getName(), entity::setName);
        patch.apply("legalName", changes.getLegalName(), entity::setLegalName);
        patch.apply("status", changes.getStatus(), v -> entity.setStatus(v == null ? null : v.getValue()));
        patch.apply("primaryEmail", changes.getPrimaryEmail(), entity::setPrimaryEmail);
        patch.apply("primaryPhone", changes.getPrimaryPhone(), entity::setPrimaryPhone);
        patch.apply("countryId", changes.getCountryId(), entity::setCountryId);
        patch.apply("defaultCurrencyId", changes.getDefaultCurrencyId(), entity::setDefaultCurrencyId);
        patch.apply("timezone", changes.getTimezone(), entity::setTimezone);
        patch.apply("locale", changes.getLocale(), entity::setLocale);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(TenantEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (!creating && entity.getStatus() == null) {
            errors.add(new FieldError("status", "must not be null", "NotNull"));
        }
        if (!creating && entity.getTimezone() == null) {
            errors.add(new FieldError("timezone", "must not be null", "NotNull"));
        }
        if (!creating && entity.getLocale() == null) {
            errors.add(new FieldError("locale", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(TenantEntity entity) {
        if (repository.takenCodeCode(entity)) {
            throw new DuplicateResourceException("A tenant with the same code already exists.");
        }
    }

    /**
     * The event of a change to a global row: its id and the row version the change produced, and code and status for
     * the copies other modules keep.
     */
    private static Map<String, Object> eventPayload(TenantEntity e, int version) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", e.getId().toString());
        payload.put("rowVersion", version);
        payload.put("code", e.getCode());
        payload.put("status", e.getStatus());
        return payload;
    }
}
