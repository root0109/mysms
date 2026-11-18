package com.mysms.org.business;

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
import com.mysms.core.web.UpdateMask;
import com.mysms.org.api.model.Institution;
import com.mysms.org.api.model.InstitutionAttribute;
import com.mysms.org.api.model.InstitutionAttributeUpdate;
import com.mysms.org.api.model.InstitutionUpdate;
import com.mysms.org.config.OrgDatabaseConfig;
import com.mysms.org.config.OrgEventsConfig;
import com.mysms.org.domain.InstitutionAttributeEntity;
import com.mysms.org.domain.InstitutionEntity;
import com.mysms.org.events.OrgEvents;
import com.mysms.org.mapper.InstitutionAttributeMapper;
import com.mysms.org.mapper.InstitutionMapper;
import com.mysms.org.repository.InstitutionAttributeRepository;
import com.mysms.org.repository.InstitutionRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of org.institution: audit and domain event in the same transaction as every change; rows outside the
 * caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 *
 * <p>Also serves the records that exist only as part of the Institution aggregate: InstitutionAttribute.
 */
@Service
public class InstitutionService extends AppTransactionManager {

    public static final String AGGREGATE = InstitutionEntity.TABLE;
    static final String PERMISSION_LIST = "org.institution.read";
    static final String PERMISSION_GET = "org.institution.read";
    static final String PERMISSION_CREATE = "org.institution.create";
    static final String PERMISSION_UPDATE = "org.institution.update";
    static final String PERMISSION_DELETE = "org.institution.delete";
    static final String PERMISSION_LIST_ACTIVE = "org.institution.list_active";
    /** The contract's InstitutionUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("parentInstitutionId", "code", "name", "slug", "institutionType", "email", "phone", "addressLine1",
            "addressLine2", "cityId", "stateProvinceId", "countryId", "postalCode", "timezone", "currencyId",
            "logoFileId", "signatureFileId", "accreditation", "approvalAuthority", "status", "institutionTypeId",
            "themeId");

    public static final String INSTITUTION_ATTRIBUTE_AGGREGATE = InstitutionAttributeEntity.TABLE;
    static final String INSTITUTION_ATTRIBUTE_PERMISSION_LIST = "org.institution_attribute.read";
    static final String INSTITUTION_ATTRIBUTE_PERMISSION_GET = "org.institution_attribute.read";
    static final String INSTITUTION_ATTRIBUTE_PERMISSION_CREATE = "org.institution_attribute.create";
    static final String INSTITUTION_ATTRIBUTE_PERMISSION_UPDATE = "org.institution_attribute.update";
    static final String INSTITUTION_ATTRIBUTE_PERMISSION_DELETE = "org.institution_attribute.delete";
    /** The contract's InstitutionAttributeUpdate fields, in apply order. */
    static final List<String> INSTITUTION_ATTRIBUTE_WRITABLE = List.of("attrKey", "attrValue", "attributeDefinitionId");

    private final InstitutionRepository repository;
    private final InstitutionMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final InstitutionAttributeRepository institutionAttributeRepository;
    private final InstitutionAttributeMapper institutionAttributeMapper;

    public InstitutionService(InstitutionRepository repository,
            InstitutionMapper mapper,
            @Qualifier(OrgDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(OrgDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(OrgEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(OrgEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            InstitutionAttributeRepository institutionAttributeRepository,
            InstitutionAttributeMapper institutionAttributeMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.institutionAttributeRepository = institutionAttributeRepository;
        this.institutionAttributeMapper = institutionAttributeMapper;
    }

    public Institution getInstitution(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Institution> listInstitutions(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, InstitutionRepository.SORT_FIELDS, InstitutionRepository.DEFAULT_ORDER,
                InstitutionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, InstitutionRepository.FILTER_FIELDS)
                .and(InstitutionRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<InstitutionEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** org.ListActiveInstitutions: the tenant's active institutions, by name, for a service that offers them. */
    public Page<Institution> listActiveInstitutions(Integer pageNumber, Integer pageSize, String cursor,
            String filter) {
        SortSpec sort = SortSpec.parse(List.of("name"), InstitutionRepository.SORT_FIELDS,
                InstitutionRepository.DEFAULT_ORDER, InstitutionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, InstitutionRepository.FILTER_FIELDS)
                .and(InstitutionRepository.ACTIVE)
                .and(InstitutionRepository.scope(DataScope.current(PERMISSION_LIST_ACTIVE)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|active", clock);
        return doInReadTransaction(() -> repository.list(page, where).map(mapper::toDto));
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Institution createInstitution(Institution request, boolean validateOnly) {
        validator.validate(request);
        InstitutionEntity entity = mapper.toEntity(request);
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
            InstitutionEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + OrgEvents.CREATED, AGGREGATE, stored.getId(), OrgEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Institution updateInstitution(UUID id, String ifMatch, InstitutionUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            InstitutionEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + OrgEvents.UPDATED, AGGREGATE, id, OrgEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteInstitution(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            InstitutionEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByAcademicYearInstitutionId(id)) {
                throw new InvalidStateException(
                        "The institution is still referenced by org.academic_year rows; remove those first.");
            }
            if (repository.referencedByDepartmentInstitutionId(id)) {
                throw new InvalidStateException(
                        "The institution is still referenced by org.department rows; remove those first.");
            }
            if (repository.referencedByHolidayInstitutionId(id)) {
                throw new InvalidStateException(
                        "The institution is still referenced by org.holiday rows; remove those first.");
            }
            if (repository.referencedByInstitutionParentInstitutionId(id)) {
                throw new InvalidStateException(
                        "The institution is still referenced by org.institution rows; remove those first.");
            }
            if (repository.referencedByInstitutionAttributeInstitutionId(id)) {
                throw new InvalidStateException(
                        "The institution is still referenced by org.institution_attribute rows; remove those first.");
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
    private InstitutionEntity loadVisible(DataScope scope, UUID id) {
        InstitutionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution is its own row; a new one needs a tenant-wide grant). */
    private Optional<UUID> institutionOf(InstitutionEntity entity) {
        return Optional.ofNullable(entity.getId());
    }

    /** Applies the selected InstitutionUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(InstitutionEntity entity, InstitutionUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "parentInstitutionId" -> entity.setParentInstitutionId(changes.getParentInstitutionId());
                case "code" -> entity.setCode(changes.getCode());
                case "name" -> entity.setName(changes.getName());
                case "slug" -> entity.setSlug(changes.getSlug());
                case "institutionType" -> entity.setInstitutionType(Optional.ofNullable(changes.getInstitutionType())
                        .map(v -> v.getValue()).orElse(null));
                case "email" -> entity.setEmail(changes.getEmail());
                case "phone" -> entity.setPhone(changes.getPhone());
                case "addressLine1" -> entity.setAddressLine1(changes.getAddressLine1());
                case "addressLine2" -> entity.setAddressLine2(changes.getAddressLine2());
                case "cityId" -> entity.setCityId(changes.getCityId());
                case "stateProvinceId" -> entity.setStateProvinceId(changes.getStateProvinceId());
                case "countryId" -> entity.setCountryId(changes.getCountryId());
                case "postalCode" -> entity.setPostalCode(changes.getPostalCode());
                case "timezone" -> entity.setTimezone(changes.getTimezone());
                case "currencyId" -> entity.setCurrencyId(changes.getCurrencyId());
                case "logoFileId" -> entity.setLogoFileId(changes.getLogoFileId());
                case "signatureFileId" -> entity.setSignatureFileId(changes.getSignatureFileId());
                case "accreditation" -> entity.setAccreditation(changes.getAccreditation());
                case "approvalAuthority" -> entity.setApprovalAuthority(changes.getApprovalAuthority());
                case "status" -> entity.setStatus(Optional.ofNullable(changes.getStatus()).map(v -> v.getValue())
                        .orElse(null));
                case "institutionTypeId" -> entity.setInstitutionTypeId(changes.getInstitutionTypeId());
                case "themeId" -> entity.setThemeId(changes.getThemeId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(InstitutionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (!creating && entity.getInstitutionType() == null) {
            errors.add(new FieldError("institutionType", "must not be null", "NotNull"));
        }
        if (!creating && entity.getStatus() == null) {
            errors.add(new FieldError("status", "must not be null", "NotNull"));
        }
        if (entity.getInstitutionTypeId() != null
                && !repository.existsInstitutionTypeId(entity.getInstitutionTypeId())) {
            errors.add(new FieldError("institutionTypeId", "institution type does not exist", "NOT_FOUND"));
        }
        if (entity.getParentInstitutionId() != null
                && !repository.existsParentInstitutionId(entity.getParentInstitutionId())) {
            errors.add(new FieldError("parentInstitutionId", "institution does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(InstitutionEntity entity) {
        if (repository.takenCode1(entity)) {
            throw new DuplicateResourceException("A institution with the same code already exists.");
        }
        if (entity.getSlug() != null && repository.takenSlug2(entity)) {
            throw new DuplicateResourceException("A institution with the same slug already exists.");
        }
    }

    // ---- InstitutionAttribute: a record of the Institution aggregate ----

    public InstitutionAttribute getInstitutionAttribute(UUID institutionId, UUID id) {
        return doInReadTransaction(() -> {
            return institutionAttributeMapper.toDto(loadVisibleInstitutionAttribute(DataScope.current(
                    INSTITUTION_ATTRIBUTE_PERMISSION_GET), institutionId, id));
        });
    }

    public Page<InstitutionAttribute> listInstitutionAttributes(UUID institutionId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, InstitutionAttributeRepository.SORT_FIELDS,
                        InstitutionAttributeRepository.DEFAULT_ORDER,
                InstitutionAttributeRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, InstitutionAttributeRepository.FILTER_FIELDS)
                .and(InstitutionAttributeRepository.ofParent(institutionId))
                        .and(InstitutionAttributeRepository.scope(DataScope.current(INSTITUTION_ATTRIBUTE_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + institutionId, clock);
        return doInReadTransaction(() -> {
            Page<InstitutionAttributeEntity> rows = institutionAttributeRepository.list(page, where);
            return rows.map(institutionAttributeMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public InstitutionAttribute createInstitutionAttribute(UUID institutionId, InstitutionAttribute request,
            boolean validateOnly) {
        validator.validate(request);
        InstitutionAttributeEntity entity = institutionAttributeMapper.toEntity(request);
        entity.setInstitutionId(institutionId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(INSTITUTION_ATTRIBUTE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!institutionAttributeRepository.existsInstitutionId(entity.getInstitutionId())) {
                throw new ResourceNotFoundException("org.institution", entity.getInstitutionId());
            }
            assertValidInstitutionAttribute(entity, true);
            scope.requireWritable(institutionOfInstitutionAttribute(entity));
            assertUniqueInstitutionAttribute(entity);
            if (validateOnly) {
                return institutionAttributeMapper.toDto(entity);
            }
            InstitutionAttributeEntity stored = institutionAttributeRepository.insert(entity);
            audit.recordCreate(INSTITUTION_ATTRIBUTE_AGGREGATE, stored.getId());
            outbox.publish(INSTITUTION_ATTRIBUTE_AGGREGATE + OrgEvents.CREATED, INSTITUTION_ATTRIBUTE_AGGREGATE,
                    stored.getId(), OrgEvents.payload(stored.getId()));
            return institutionAttributeMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public InstitutionAttribute updateInstitutionAttribute(UUID institutionId, UUID id, String ifMatch,
            InstitutionAttributeUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, INSTITUTION_ATTRIBUTE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(INSTITUTION_ATTRIBUTE_PERMISSION_UPDATE);
            InstitutionAttributeEntity entity = loadVisibleInstitutionAttribute(scope, institutionId, id);
            scope.requireWritable(institutionOfInstitutionAttribute(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyInstitutionAttribute(entity, changes, fields);
            validator.validate(institutionAttributeMapper.toDto(entity));
            assertValidInstitutionAttribute(entity, false);
            scope.requireWritable(institutionOfInstitutionAttribute(entity));
            assertUniqueInstitutionAttribute(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return institutionAttributeMapper.toDto(entity);
            }
            if (institutionAttributeRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(INSTITUTION_ATTRIBUTE_AGGREGATE, id, fields);
            outbox.publish(INSTITUTION_ATTRIBUTE_AGGREGATE + OrgEvents.UPDATED, INSTITUTION_ATTRIBUTE_AGGREGATE, id,
                    OrgEvents.payload(id));
            return institutionAttributeMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteInstitutionAttribute(UUID institutionId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(INSTITUTION_ATTRIBUTE_PERMISSION_DELETE);
            InstitutionAttributeEntity entity = loadVisibleInstitutionAttribute(scope, institutionId, id);
            scope.requireWritable(institutionOfInstitutionAttribute(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (institutionAttributeRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(INSTITUTION_ATTRIBUTE_AGGREGATE, id);
            outbox.publish(INSTITUTION_ATTRIBUTE_AGGREGATE + OrgEvents.DELETED, INSTITUTION_ATTRIBUTE_AGGREGATE, id,
                    OrgEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private InstitutionAttributeEntity loadVisibleInstitutionAttribute(DataScope scope, UUID institutionId, UUID id) {
        InstitutionAttributeEntity entity = institutionAttributeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(INSTITUTION_ATTRIBUTE_AGGREGATE, id));
        if (!scope.canRead(institutionOfInstitutionAttribute(entity)) || !institutionId.equals(entity.getInstitutionId())) {
            throw new ResourceNotFoundException(INSTITUTION_ATTRIBUTE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOfInstitutionAttribute(InstitutionAttributeEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /**
     * Applies the selected InstitutionAttributeUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyInstitutionAttribute(InstitutionAttributeEntity entity, InstitutionAttributeUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "attrKey" -> entity.setAttrKey(changes.getAttrKey());
                case "attrValue" -> entity.setAttrValue(changes.getAttrValue());
                case "attributeDefinitionId" -> entity.setAttributeDefinitionId(changes.getAttributeDefinitionId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidInstitutionAttribute(InstitutionAttributeEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getAttrKey() == null) {
            errors.add(new FieldError("attrKey", "must not be null", "NotNull"));
        }
        if (entity.getAttributeDefinitionId() != null
                && !institutionAttributeRepository.existsAttributeDefinitionId(entity.getAttributeDefinitionId())) {
            errors.add(new FieldError("attributeDefinitionId", "attribute definition does not exist", "NOT_FOUND"));
        }
        if (entity.getInstitutionId() != null && !institutionAttributeRepository.existsInstitutionId(entity.getInstitutionId())) {
            errors.add(new FieldError("institutionId", "institution does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueInstitutionAttribute(InstitutionAttributeEntity entity) {
        if (institutionAttributeRepository.takenInstitutionIdAttrKey1(entity)) {
            throw new DuplicateResourceException(
                    "A institution attribute with the same institution id and attr key already exists.");
        }
    }
}
