package com.mysms.platform.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.platform.api.model.SaasPlan;
import com.mysms.platform.api.model.SaasPlanUpdate;
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
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.config.PlatformEventsConfig;
import com.mysms.platform.domain.SaasPlanEntity;
import com.mysms.platform.events.PlatformEvents;
import com.mysms.platform.mapper.SaasPlanMapper;
import com.mysms.platform.repository.SaasPlanRepository;
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
 * Business service of platform.saas_plan: audit and domain event in the same transaction as every change; rows outside
 * the caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class SaasPlanService extends AppTransactionManager {

    public static final String AGGREGATE = SaasPlanEntity.TABLE;


    private final SaasPlanRepository repository;
    private final SaasPlanMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public SaasPlanService(SaasPlanRepository repository, SaasPlanMapper mapper,
            @Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PlatformDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PlatformEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(PlatformEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public SaasPlan getSaasPlan(UUID id) {
        return doInGlobalReadTransaction(() -> {
            return projected(mapper.toDto(forCaller(loadVisible(DataScope.global(), id))));
        });
    }

    public Page<SaasPlan> listSaasPlans(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, SaasPlanRepository.SORT_FIELDS, SaasPlanRepository.DEFAULT_ORDER,
                SaasPlanRepository.ID_COLUMN);
        SqlFragment where = projected(FilterParser.parse(filter, SaasPlanRepository.FILTER_FIELDS));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInGlobalReadTransaction(() -> {
            Page<SaasPlanEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto).map(SaasPlanService::projected);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public SaasPlan createSaasPlan(SaasPlan request, boolean validateOnly) {
        validator.validate(request);
        SaasPlanEntity entity = mapper.toEntity(request);
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
                SaasPlanEntity stored = repository.insert(entity);
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
    public SaasPlan updateSaasPlan(UUID id, String ifMatch, SaasPlanUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInGlobalWriteTransaction(() -> {
            try (CallContextHolder.Scope global = CallContextHolder.withoutTenant()) {
                DataScope scope = DataScope.global();
                SaasPlanEntity entity = loadVisible(scope, id);
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
    public void deleteSaasPlan(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInGlobalWriteTransaction(() -> {
            try (CallContextHolder.Scope global = CallContextHolder.withoutTenant()) {
                DataScope scope = DataScope.global();
                SaasPlanEntity entity = loadVisible(scope, id);
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

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private SaasPlanEntity loadVisible(DataScope scope, UUID id) {
        SaasPlanEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOf(SaasPlanEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(SaasPlanEntity entity, SaasPlanUpdate changes) {
        Patches patch = new Patches();
        patch.apply("code", changes.getCode(), entity::setCode);
        patch.apply("displayName", changes.getDisplayName(), entity::setDisplayName);
        patch.apply("billingCycleMonths", changes.getBillingCycleMonths(), entity::setBillingCycleMonths);
        patch.apply("price", changes.getPrice(), v -> entity.setPrice(WireValues.decimal(v)));
        patch.apply("currencyId", changes.getCurrencyId(), entity::setCurrencyId);
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        patch.apply("isActive", changes.getIsActive(), entity::setIsActive);
        patch.apply("maxMembers", changes.getMaxMembers(), entity::setMaxMembers);
        patch.apply("maxEvents", changes.getMaxEvents(), entity::setMaxEvents);
        patch.apply("maxFolders", changes.getMaxFolders(), entity::setMaxFolders);
        patch.apply("maxFiles", changes.getMaxFiles(), entity::setMaxFiles);
        patch.apply("maxVideos", changes.getMaxVideos(), entity::setMaxVideos);
        patch.apply("maxAudios", changes.getMaxAudios(), entity::setMaxAudios);
        patch.apply("maxBulletins", changes.getMaxBulletins(), entity::setMaxBulletins);
        patch.apply("maxGroups", changes.getMaxGroups(), entity::setMaxGroups);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(SaasPlanEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
        }
        if (entity.getDisplayName() == null) {
            errors.add(new FieldError("displayName", "must not be null", "NotNull"));
        }
        if (entity.getBillingCycleMonths() == null) {
            errors.add(new FieldError("billingCycleMonths", "must not be null", "NotNull"));
        }
        if (!creating && entity.getPrice() == null) {
            errors.add(new FieldError("price", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("price", WireValues.decimal(entity.getPrice()), 12, 2, errors);
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(SaasPlanEntity entity) {
        if (repository.takenCodeCode(entity)) {
            throw new DuplicateResourceException("A saas plan with the same code already exists.");
        }
    }

    /** The event of a change to a global row: its id and the row version the change produced. */
    private static Map<String, Object> eventPayload(SaasPlanEntity e, int version) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", e.getId().toString());
        payload.put("rowVersion", version);
        return payload;
    }

    /** Platform staff see every row; guests and tenant users only the published ones (the guest projection). */
    private static boolean staff() {
        return AuthContext.current().map(AuthContext::platformOperator).orElse(false);
    }

    private static SqlFragment projected(SqlFragment where) {
        return staff() ? where : where.and(FilterParser.parse("isActive = true", SaasPlanRepository.FILTER_FIELDS));
    }

    private static SaasPlan projected(SaasPlan dto) {
        if (!staff()) {
            dto.setCreatedBy(null);
            dto.setUpdatedBy(null);
        }
        return dto;
    }

    /** An unpublished row is absent for everyone but platform staff. */
    private SaasPlanEntity forCaller(SaasPlanEntity entity) {
        if (!staff() && !Boolean.TRUE.equals(entity.getIsActive())) {
            throw new ResourceNotFoundException(AGGREGATE, entity.getId());
        }
        return entity;
    }
}
