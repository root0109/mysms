package com.mysms.facility.business;

import com.mysms.authclient.DataScope;
import com.mysms.facility.api.model.Facility;
import com.mysms.facility.api.model.FacilityUpdate;
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
import com.mysms.facility.config.FacilityDatabaseConfig;
import com.mysms.facility.config.FacilityEventsConfig;
import com.mysms.facility.domain.FacilityEntity;
import com.mysms.facility.events.FacilityEvents;
import com.mysms.facility.mapper.FacilityMapper;
import com.mysms.facility.repository.FacilityRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of facility.facility: audit and domain event commit in the same transaction as every change. Rows
 * outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class FacilityService extends AppTransactionManager {

    public static final String AGGREGATE = FacilityEntity.TABLE;
    static final String PERMISSION_LIST = "facility.facility.read";
    static final String PERMISSION_GET = "facility.facility.read";
    static final String PERMISSION_CREATE = "facility.facility.create";
    static final String PERMISSION_UPDATE = "facility.facility.update";
    static final String PERMISSION_DELETE = "facility.facility.delete";
    /** The contract's FacilityUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("sortOrder", "institutionId", "code", "name", "isActive");


    private final FacilityRepository repository;
    private final FacilityMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public FacilityService(FacilityRepository repository, FacilityMapper mapper,
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

    public Facility getFacility(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Facility> listFacilitys(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, FacilityRepository.SORT_FIELDS, FacilityRepository.DEFAULT_ORDER,
                FacilityRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, FacilityRepository.FILTER_FIELDS)
                .and(FacilityRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<FacilityEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Facility createFacility(Facility request, boolean validateOnly) {
        validator.validate(request);
        FacilityEntity entity = mapper.toEntity(request);
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
            FacilityEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + FacilityEvents.CREATED, AGGREGATE, stored.getId(),
                    FacilityEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public Facility updateFacility(UUID id, String ifMatch, FacilityUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            FacilityEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + FacilityEvents.UPDATED, AGGREGATE, id, FacilityEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteFacility(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            FacilityEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByRoomFacilityFacilityId(id)) {
                throw new InvalidStateException(
                        "The facility is still referenced by facility.room_facility rows; remove those first.");
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
    private FacilityEntity loadVisible(DataScope scope, UUID id) {
        FacilityEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(FacilityEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected FacilityUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(FacilityEntity entity, FacilityUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "code" -> entity.setCode(changes.getCode());
                case "name" -> entity.setName(changes.getName());
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(FacilityEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
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

    private void assertUnique(FacilityEntity entity) {
        if (repository.takenInstitutionIdCodeCode(entity)) {
            throw new DuplicateResourceException("A facility with the same institution id and code already exists.");
        }
    }
}
