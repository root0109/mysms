package com.mysms.campus.facility.business;

import com.mysms.authclient.DataScope;
import com.mysms.campus.facility.api.model.AssetItem;
import com.mysms.campus.facility.api.model.AssetItemUpdate;
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
import com.mysms.campus.facility.config.FacilityDatabaseConfig;
import com.mysms.campus.facility.config.FacilityEventsConfig;
import com.mysms.campus.facility.domain.AssetItemEntity;
import com.mysms.campus.facility.events.FacilityEvents;
import com.mysms.campus.facility.mapper.AssetItemMapper;
import com.mysms.campus.facility.repository.AssetItemRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of facility.asset_item: audit and domain event commit in the same transaction as every change. Rows
 * outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class AssetItemService extends AppTransactionManager {

    public static final String AGGREGATE = AssetItemEntity.TABLE;
    static final String PERMISSION_LIST = "facility.asset_item.read";
    static final String PERMISSION_GET = "facility.asset_item.read";
    static final String PERMISSION_CREATE = "facility.asset_item.create";
    static final String PERMISSION_UPDATE = "facility.asset_item.update";
    static final String PERMISSION_DELETE = "facility.asset_item.delete";


    private final AssetItemRepository repository;
    private final AssetItemMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public AssetItemService(AssetItemRepository repository, AssetItemMapper mapper,
            @Qualifier(FacilityDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(FacilityDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(FacilityEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(FacilityEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public AssetItem getAssetItem(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<AssetItem> listAssetItems(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, AssetItemRepository.SORT_FIELDS, AssetItemRepository.DEFAULT_ORDER,
                AssetItemRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, AssetItemRepository.FILTER_FIELDS)
                .and(AssetItemRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<AssetItemEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public AssetItem createAssetItem(AssetItem request, boolean validateOnly) {
        validator.validate(request);
        AssetItemEntity entity = mapper.toEntity(request);
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
            AssetItemEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + FacilityEvents.CREATED, AGGREGATE, stored.getId(),
                    FacilityEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public AssetItem updateAssetItem(UUID id, String ifMatch, AssetItemUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            AssetItemEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + FacilityEvents.UPDATED, AGGREGATE, id, FacilityEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteAssetItem(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            AssetItemEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByRoomAssetAssetItemId(id)) {
                throw new InvalidStateException(
                        "The asset item is still referenced by facility.room_asset rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + FacilityEvents.DELETED, AGGREGATE, id, FacilityEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private AssetItemEntity loadVisible(DataScope scope, UUID id) {
        AssetItemEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(AssetItemEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(AssetItemEntity entity, AssetItemUpdate changes) {
        Patches patch = new Patches();
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        patch.apply("isActive", changes.getIsActive(), entity::setIsActive);
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("code", changes.getCode(), entity::setCode);
        patch.apply("name", changes.getName(), entity::setName);
        patch.apply("unitOfMeasure", changes.getUnitOfMeasure(), entity::setUnitOfMeasure);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(AssetItemEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (!creating && entity.getUnitOfMeasure() == null) {
            errors.add(new FieldError("unitOfMeasure", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(AssetItemEntity entity) {
        if (entity.getCode() != null && repository.takenInstitutionIdCodeCode(entity)) {
            throw new DuplicateResourceException("A asset item with the same institution id and code already exists.");
        }
    }
}
