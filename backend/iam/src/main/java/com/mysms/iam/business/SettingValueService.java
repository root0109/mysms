package com.mysms.iam.business;

import com.mysms.iam.api.model.SettingValue;
import com.mysms.iam.api.model.SettingValueUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
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
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.config.IamEventsConfig;
import com.mysms.iam.domain.SettingDefinitionEntity;
import com.mysms.iam.domain.SettingValueEntity;
import com.mysms.iam.events.IamEvents;
import com.mysms.iam.mapper.SettingValueMapper;
import com.mysms.iam.repository.SettingDefinitionRepository;
import com.mysms.iam.repository.SettingValueRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Administrators' setting values at tenant, institution, department, class-section or user scope (iam.*SettingValue).
 * Every value is checked against its catalogue definition (type, allowed values, range, allowed scopes); the scope id
 * must be absent exactly for the tenant scope; a user-level value can never be locked; one live value per setting
 * and scope. Callers act only on scopes their grants cover ({@link SettingScopes}); a value outside them is 404 when
 * read and 403 when written.
 */
@Service
public class SettingValueService extends AppTransactionManager {

    public static final String AGGREGATE = SettingValueEntity.TABLE;
    static final List<String> WRITABLE = List.of("value", "isLocked");
    private static final String READ = "iam.setting_value.read";
    private static final String CREATE = "iam.setting_value.create";
    private static final String UPDATE = "iam.setting_value.update";
    private static final String DELETE = "iam.setting_value.delete";
    private static final String STALE = "If-Match does not match the current ETag.";

    private final SettingValueRepository repository;
    private final SettingDefinitionRepository definitions;
    private final SettingValueMapper mapper;
    private final SettingScopes scopes;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public SettingValueService(SettingValueRepository repository, SettingDefinitionRepository definitions,
                               SettingValueMapper mapper, SettingScopes scopes,
                               @Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(IamDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
                               @Qualifier(IamEventsConfig.OUTBOX) OutboxWriter outbox,
                               @Qualifier(IamEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator,
                               Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.definitions = definitions;
        this.mapper = mapper;
        this.scopes = scopes;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public SettingValue get(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(id, READ));
        });
    }

    public Page<SettingValue> list(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, SettingValueRepository.SORT_FIELDS, SettingValueRepository.DEFAULT_ORDER,
                SettingValueRepository.ID_COLUMN);
        SqlFragment parsed = FilterParser.parse(filter, SettingValueRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            SqlFragment where = parsed.and(scopes.readableFilter(READ));
            Page<SettingValueEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the value; with validateOnly every check runs and nothing is written. */
    public SettingValue create(SettingValue request, boolean validateOnly) {
        validator.validate(request);
        SettingValueEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        return doInWriteTransaction(() -> {
            assertValid(entity);
            if (!scopes.permitted(CREATE, entity.getScopeType(), entity.getScopeId())) {
                throw new PermissionDeniedException("The scope is outside the caller's grants.");
            }
            if (repository.findAtScope(entity.getScopeType(), entity.getScopeId(), entity.getSettingKey()).isPresent()) {
                throw new DuplicateResourceException("This setting already has a value at this scope.");
            }
            if (!validateOnly) {
                repository.insert(entity);
                audit.recordCreate(AGGREGATE, entity.getId());
                outbox.publish(AGGREGATE + IamEvents.CREATED, AGGREGATE, entity.getId(), eventPayload(entity));
            }
            return mapper.toDto(entity);
        });
    }

    /** Partial update of value and lock under If-Match (scope and key are fixed once created). */
    public SettingValue update(UUID id, String ifMatch, SettingValueUpdate changes, String updateMask,
                               boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        validator.validate(changes);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            SettingValueEntity entity = loadVisible(id, READ);
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException(STALE);
            }
            if (!scopes.permitted(UPDATE, entity.getScopeType(), entity.getScopeId())) {
                throw new PermissionDeniedException("The scope is outside the caller's grants.");
            }
            for (String field : fields) {
                switch (field) {
                    case "value" -> entity.setValue(changes.getValue());
                    case "isLocked" -> entity.setLocked(Boolean.TRUE.equals(changes.getIsLocked()));
                    default -> throw new IllegalStateException("unhandled writable field " + field);
                }
            }
            assertValid(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (!validateOnly) {
                if (repository.update(entity, expected) == 0) {
                    throw new PreconditionFailedException(STALE);
                }
                entity.setRowVersion(expected + 1);
                audit.recordUpdate(AGGREGATE, id, fields);
                outbox.publish(AGGREGATE + IamEvents.UPDATED, AGGREGATE, id, eventPayload(entity));
            }
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match: the broader scopes apply again. */
    public void delete(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            SettingValueEntity entity = loadVisible(id, READ);
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException(STALE);
            }
            if (!scopes.permitted(DELETE, entity.getScopeType(), entity.getScopeId())) {
                throw new PermissionDeniedException("The scope is outside the caller's grants.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException(STALE);
            }
            entity.setRowVersion(expected + 1);
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + IamEvents.DELETED, AGGREGATE, id, eventPayload(entity));
        });
    }

    private static Map<String, Object> eventPayload(SettingValueEntity entity) {
        return IamEvents.settingValue(entity.getId(), entity.getSettingKey(), entity.getScopeType(),
                entity.getScopeId(), entity.getValue(), entity.getRowVersion());
    }

    /** The row, or 404 when it does not exist or lies outside the caller's readable scopes. */
    private SettingValueEntity loadVisible(UUID id, String permission) {
        SettingValueEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scopes.permitted(permission, entity.getScopeType(), entity.getScopeId())) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** Catalogue and scope rules; 422 with every failing field. */
    private void assertValid(SettingValueEntity entity) {
        List<FieldError> errors = new ArrayList<>();
        Optional<SettingDefinitionEntity> definition = definitions.findByKey(entity.getSettingKey());
        boolean tenantScope = SettingRules.TENANT.equals(entity.getScopeType());
        if (tenantScope && entity.getScopeId() != null) {
            errors.add(new FieldError("scopeId", "must be absent for the tenant scope", "Null"));
        }
        if (!tenantScope && entity.getScopeId() == null) {
            errors.add(new FieldError("scopeId", "is required for this scope", "NotNull"));
        }
        if (SettingRules.USER.equals(entity.getScopeType()) && entity.isLocked()) {
            errors.add(new FieldError("isLocked", "a user-level value cannot be locked", "AssertFalse"));
        }
        if (definition.isEmpty()) {
            errors.add(new FieldError("settingKey", "is not a setting of the catalogue", "Exists"));
        } else {
            if (!SettingRules.scopeAllowed(definition.get(), entity.getScopeType())) {
                errors.add(new FieldError("scopeType", "this setting cannot be set at " + entity.getScopeType()
                        + " scope", "Scope"));
            }
            errors.addAll(SettingRules.valueErrors(definition.get(), entity.getValue(), "value"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }
}
