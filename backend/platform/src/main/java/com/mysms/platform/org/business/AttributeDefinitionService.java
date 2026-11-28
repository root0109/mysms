package com.mysms.platform.org.business;

import com.mysms.authclient.DataScope;
import com.mysms.platform.org.api.model.AttributeDefinition;
import com.mysms.platform.org.api.model.AttributeDefinitionUpdate;
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
import com.mysms.platform.org.config.OrgDatabaseConfig;
import com.mysms.platform.org.config.OrgEventsConfig;
import com.mysms.platform.org.domain.AttributeDefinitionEntity;
import com.mysms.platform.org.events.OrgEvents;
import com.mysms.platform.org.mapper.AttributeDefinitionMapper;
import com.mysms.platform.org.repository.AttributeDefinitionRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of org.attribute_definition: audit and domain event in the same transaction as every change; rows
 * outside the caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class AttributeDefinitionService extends AppTransactionManager {

    public static final String AGGREGATE = AttributeDefinitionEntity.TABLE;
    static final String PERMISSION_LIST = "org.attribute_definition.read";
    static final String PERMISSION_GET = "org.attribute_definition.read";
    static final String PERMISSION_CREATE = "org.attribute_definition.create";
    static final String PERMISSION_UPDATE = "org.attribute_definition.update";
    static final String PERMISSION_DELETE = "org.attribute_definition.delete";


    private final AttributeDefinitionRepository repository;
    private final AttributeDefinitionMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public AttributeDefinitionService(AttributeDefinitionRepository repository, AttributeDefinitionMapper mapper,
            @Qualifier(OrgDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(OrgDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(OrgEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(OrgEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public AttributeDefinition getAttributeDefinition(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<AttributeDefinition> listAttributeDefinitions(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, AttributeDefinitionRepository.SORT_FIELDS,
                        AttributeDefinitionRepository.DEFAULT_ORDER,
                AttributeDefinitionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, AttributeDefinitionRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<AttributeDefinitionEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public AttributeDefinition createAttributeDefinition(AttributeDefinition request, boolean validateOnly) {
        validator.validate(request);
        AttributeDefinitionEntity entity = mapper.toEntity(request);
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
            AttributeDefinitionEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + OrgEvents.CREATED, AGGREGATE, stored.getId(), OrgEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. A field the request leaves out is kept, a null one is cleared and a
     * value is set (§7.2). The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public AttributeDefinition updateAttributeDefinition(UUID id, String ifMatch, AttributeDefinitionUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            AttributeDefinitionEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + OrgEvents.UPDATED, AGGREGATE, id, OrgEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteAttributeDefinition(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            AttributeDefinitionEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByInstitutionAttributeAttributeDefinitionId(id)) {
                throw new InvalidStateException(
                        "The attribute definition is still referenced by org.institution_attribute rows; remove "
                                + "those first.");
            }
            if (repository.referencedByInstitutionTypeAttributeAttributeDefinitionId(id)) {
                throw new InvalidStateException(
                        "The attribute definition is still referenced by org.institution_type_attribute rows; remove "
                                + "those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + OrgEvents.DELETED, AGGREGATE, id, OrgEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private AttributeDefinitionEntity loadVisible(DataScope scope, UUID id) {
        AttributeDefinitionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOf(AttributeDefinitionEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(AttributeDefinitionEntity entity, AttributeDefinitionUpdate changes) {
        Patches patch = new Patches();
        patch.apply("name", changes.getName(), entity::setName);
        patch.apply("dataType", changes.getDataType(), v -> entity.setDataType(v == null ? null : v.getValue()));
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(AttributeDefinitionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (!creating && entity.getDataType() == null) {
            errors.add(new FieldError("dataType", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(AttributeDefinitionEntity entity) {
        if (repository.takenName1(entity)) {
            throw new DuplicateResourceException("A attribute definition with the same name already exists.");
        }
    }
}
