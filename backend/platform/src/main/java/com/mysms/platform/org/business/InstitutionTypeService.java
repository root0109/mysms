package com.mysms.platform.org.business;

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
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.Patches;
import com.mysms.platform.org.api.model.InstitutionType;
import com.mysms.platform.org.api.model.InstitutionTypeAttribute;
import com.mysms.platform.org.api.model.InstitutionTypeAttributeUpdate;
import com.mysms.platform.org.api.model.InstitutionTypeUpdate;
import com.mysms.platform.org.config.OrgDatabaseConfig;
import com.mysms.platform.org.config.OrgEventsConfig;
import com.mysms.platform.org.domain.InstitutionTypeAttributeEntity;
import com.mysms.platform.org.domain.InstitutionTypeEntity;
import com.mysms.platform.org.events.OrgEvents;
import com.mysms.platform.org.mapper.InstitutionTypeAttributeMapper;
import com.mysms.platform.org.mapper.InstitutionTypeMapper;
import com.mysms.platform.org.repository.InstitutionTypeAttributeRepository;
import com.mysms.platform.org.repository.InstitutionTypeRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of org.institution_type: audit and domain event in the same transaction as every change; rows
 * outside the caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 *
 * <p>Also serves the records that exist only as part of the InstitutionType aggregate: InstitutionTypeAttribute.
 */
@Service
public class InstitutionTypeService extends AppTransactionManager {

    public static final String AGGREGATE = InstitutionTypeEntity.TABLE;
    static final String PERMISSION_LIST = "org.institution_type.read";
    static final String PERMISSION_GET = "org.institution_type.read";
    static final String PERMISSION_CREATE = "org.institution_type.create";
    static final String PERMISSION_UPDATE = "org.institution_type.update";
    static final String PERMISSION_DELETE = "org.institution_type.delete";

    public static final String INSTITUTION_TYPE_ATTRIBUTE_AGGREGATE = InstitutionTypeAttributeEntity.TABLE;
    static final String INSTITUTION_TYPE_ATTRIBUTE_PERMISSION_LIST = "org.institution_type_attribute.read";
    static final String INSTITUTION_TYPE_ATTRIBUTE_PERMISSION_GET = "org.institution_type_attribute.read";
    static final String INSTITUTION_TYPE_ATTRIBUTE_PERMISSION_CREATE = "org.institution_type_attribute.create";
    static final String INSTITUTION_TYPE_ATTRIBUTE_PERMISSION_UPDATE = "org.institution_type_attribute.update";
    static final String INSTITUTION_TYPE_ATTRIBUTE_PERMISSION_DELETE = "org.institution_type_attribute.delete";

    private final InstitutionTypeRepository repository;
    private final InstitutionTypeMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final InstitutionTypeAttributeRepository institutionTypeAttributeRepository;
    private final InstitutionTypeAttributeMapper institutionTypeAttributeMapper;

    public InstitutionTypeService(InstitutionTypeRepository repository,
            InstitutionTypeMapper mapper,
            @Qualifier(OrgDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(OrgDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(OrgEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(OrgEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            InstitutionTypeAttributeRepository institutionTypeAttributeRepository,
            InstitutionTypeAttributeMapper institutionTypeAttributeMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.institutionTypeAttributeRepository = institutionTypeAttributeRepository;
        this.institutionTypeAttributeMapper = institutionTypeAttributeMapper;
    }

    public InstitutionType getInstitutionType(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<InstitutionType> listInstitutionTypes(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, InstitutionTypeRepository.SORT_FIELDS, InstitutionTypeRepository.DEFAULT_ORDER,
                InstitutionTypeRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, InstitutionTypeRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<InstitutionTypeEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public InstitutionType createInstitutionType(InstitutionType request, boolean validateOnly) {
        validator.validate(request);
        InstitutionTypeEntity entity = mapper.toEntity(request);
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
            InstitutionTypeEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + OrgEvents.CREATED, AGGREGATE, stored.getId(), OrgEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. A field the request leaves out is kept, a null one is cleared and a
     * value is set (§7.2). The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public InstitutionType updateInstitutionType(UUID id, String ifMatch, InstitutionTypeUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            InstitutionTypeEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = apply(entity, changes);
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
            outbox.publish(AGGREGATE + OrgEvents.UPDATED, AGGREGATE, id, OrgEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteInstitutionType(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            InstitutionTypeEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByInstitutionInstitutionTypeId(id)) {
                throw new InvalidStateException(
                        "The institution type is still referenced by org.institution rows; remove those first.");
            }
            if (repository.referencedByInstitutionTypeAttributeInstitutionTypeId(id)) {
                throw new InvalidStateException(
                        "The institution type is still referenced by org.institution_type_attribute rows; remove "
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
    private InstitutionTypeEntity loadVisible(DataScope scope, UUID id) {
        InstitutionTypeEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOf(InstitutionTypeEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(InstitutionTypeEntity entity, InstitutionTypeUpdate changes) {
        Patches patch = new Patches();
        patch.apply("code", changes.getCode(), entity::setCode);
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        patch.apply("isActive", changes.getIsActive(), entity::setIsActive);
        patch.apply("name", changes.getName(), entity::setName);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(InstitutionTypeEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(InstitutionTypeEntity entity) {
        if (repository.takenName1(entity)) {
            throw new DuplicateResourceException("A institution type with the same name already exists.");
        }
        if (repository.takenCodeCode(entity)) {
            throw new DuplicateResourceException("A institution type with the same code already exists.");
        }
    }

    // ---- InstitutionTypeAttribute: a record of the InstitutionType aggregate ----

    public InstitutionTypeAttribute getInstitutionTypeAttribute(UUID institutionTypeId, UUID id) {
        return doInReadTransaction(() -> {
            return institutionTypeAttributeMapper.toDto(loadVisibleInstitutionTypeAttribute(DataScope.current(
                    INSTITUTION_TYPE_ATTRIBUTE_PERMISSION_GET), institutionTypeId, id));
        });
    }

    public Page<InstitutionTypeAttribute> listInstitutionTypeAttributes(UUID institutionTypeId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, InstitutionTypeAttributeRepository.SORT_FIELDS,
                        InstitutionTypeAttributeRepository.DEFAULT_ORDER,
                InstitutionTypeAttributeRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, InstitutionTypeAttributeRepository.FILTER_FIELDS)
                .and(InstitutionTypeAttributeRepository.ofParent(institutionTypeId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + institutionTypeId, clock);
        return doInReadTransaction(() -> {
            Page<InstitutionTypeAttributeEntity> rows = institutionTypeAttributeRepository.list(page, where);
            return rows.map(institutionTypeAttributeMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public InstitutionTypeAttribute createInstitutionTypeAttribute(UUID institutionTypeId,
            InstitutionTypeAttribute request, boolean validateOnly) {
        validator.validate(request);
        InstitutionTypeAttributeEntity entity = institutionTypeAttributeMapper.toEntity(request);
        entity.setInstitutionTypeId(institutionTypeId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(INSTITUTION_TYPE_ATTRIBUTE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!institutionTypeAttributeRepository.existsInstitutionTypeId(entity.getInstitutionTypeId())) {
                throw new ResourceNotFoundException("org.institution_type", entity.getInstitutionTypeId());
            }
            assertValidInstitutionTypeAttribute(entity, true);
            scope.requireWritable(institutionOfInstitutionTypeAttribute(entity));
            assertUniqueInstitutionTypeAttribute(entity);
            if (validateOnly) {
                return institutionTypeAttributeMapper.toDto(entity);
            }
            InstitutionTypeAttributeEntity stored = institutionTypeAttributeRepository.insert(entity);
            audit.recordCreate(INSTITUTION_TYPE_ATTRIBUTE_AGGREGATE, stored.getId());
            outbox.publish(INSTITUTION_TYPE_ATTRIBUTE_AGGREGATE + OrgEvents.CREATED, INSTITUTION_TYPE_ATTRIBUTE_AGGREGATE,
                    stored.getId(), OrgEvents.payload(stored.getId()));
            return institutionTypeAttributeMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. A field the request leaves out is kept, a null one is cleared and a
     * value is set (§7.2). The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public InstitutionTypeAttribute updateInstitutionTypeAttribute(UUID institutionTypeId, UUID id, String ifMatch,
            InstitutionTypeAttributeUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(INSTITUTION_TYPE_ATTRIBUTE_PERMISSION_UPDATE);
            InstitutionTypeAttributeEntity entity = loadVisibleInstitutionTypeAttribute(scope, institutionTypeId, id);
            scope.requireWritable(institutionOfInstitutionTypeAttribute(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyInstitutionTypeAttribute(entity, changes);
            validator.validate(institutionTypeAttributeMapper.toDto(entity));
            assertValidInstitutionTypeAttribute(entity, false);
            scope.requireWritable(institutionOfInstitutionTypeAttribute(entity));
            assertUniqueInstitutionTypeAttribute(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return institutionTypeAttributeMapper.toDto(entity);
            }
            if (institutionTypeAttributeRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(INSTITUTION_TYPE_ATTRIBUTE_AGGREGATE, id, fields);
            outbox.publish(INSTITUTION_TYPE_ATTRIBUTE_AGGREGATE + OrgEvents.UPDATED, INSTITUTION_TYPE_ATTRIBUTE_AGGREGATE,
                    id, OrgEvents.payload(id));
            return institutionTypeAttributeMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteInstitutionTypeAttribute(UUID institutionTypeId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(INSTITUTION_TYPE_ATTRIBUTE_PERMISSION_DELETE);
            InstitutionTypeAttributeEntity entity = loadVisibleInstitutionTypeAttribute(scope, institutionTypeId, id);
            scope.requireWritable(institutionOfInstitutionTypeAttribute(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (institutionTypeAttributeRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(INSTITUTION_TYPE_ATTRIBUTE_AGGREGATE, id);
            outbox.publish(INSTITUTION_TYPE_ATTRIBUTE_AGGREGATE + OrgEvents.DELETED, INSTITUTION_TYPE_ATTRIBUTE_AGGREGATE,
                    id, OrgEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private InstitutionTypeAttributeEntity loadVisibleInstitutionTypeAttribute(DataScope scope, UUID institutionTypeId, UUID id) {
        InstitutionTypeAttributeEntity entity = institutionTypeAttributeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(INSTITUTION_TYPE_ATTRIBUTE_AGGREGATE, id));
        if (!scope.canRead(institutionOfInstitutionTypeAttribute(entity))
                || !institutionTypeId.equals(entity.getInstitutionTypeId())) {
            throw new ResourceNotFoundException(INSTITUTION_TYPE_ATTRIBUTE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOfInstitutionTypeAttribute(InstitutionTypeAttributeEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyInstitutionTypeAttribute(InstitutionTypeAttributeEntity entity,
            InstitutionTypeAttributeUpdate changes) {
        Patches patch = new Patches();
        patch.apply("attributeDefinitionId", changes.getAttributeDefinitionId(), entity::setAttributeDefinitionId);
        patch.apply("isRequired", changes.getIsRequired(), entity::setIsRequired);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidInstitutionTypeAttribute(InstitutionTypeAttributeEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionTypeId() == null) {
            errors.add(new FieldError("institutionTypeId", "must not be null", "NotNull"));
        }
        if (entity.getAttributeDefinitionId() == null) {
            errors.add(new FieldError("attributeDefinitionId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsRequired() == null) {
            errors.add(new FieldError("isRequired", "must not be null", "NotNull"));
        }
        if (entity.getAttributeDefinitionId() != null
                && !institutionTypeAttributeRepository.existsAttributeDefinitionId(entity.getAttributeDefinitionId())) {
            errors.add(new FieldError("attributeDefinitionId", "attribute definition does not exist", "NOT_FOUND"));
        }
        if (entity.getInstitutionTypeId() != null
                && !institutionTypeAttributeRepository.existsInstitutionTypeId(entity.getInstitutionTypeId())) {
            errors.add(new FieldError("institutionTypeId", "institution type does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueInstitutionTypeAttribute(InstitutionTypeAttributeEntity entity) {
        if (institutionTypeAttributeRepository.takenInstitutionTypeIdAttributeDefinitionId1(entity)) {
            throw new DuplicateResourceException(
                    "A institution type attribute with the same institution type id and attribute definition id "
                            + "already exists.");
        }
    }
}
