package com.mysms.campus.facility.business;

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
import com.mysms.core.web.Patches;
import com.mysms.campus.facility.api.model.Room;
import com.mysms.campus.facility.api.model.RoomAsset;
import com.mysms.campus.facility.api.model.RoomAssetUpdate;
import com.mysms.campus.facility.api.model.RoomFacility;
import com.mysms.campus.facility.api.model.RoomFacilityUpdate;
import com.mysms.campus.facility.api.model.RoomUpdate;
import com.mysms.campus.facility.config.FacilityDatabaseConfig;
import com.mysms.campus.facility.config.FacilityEventsConfig;
import com.mysms.campus.facility.domain.RoomAssetEntity;
import com.mysms.campus.facility.domain.RoomEntity;
import com.mysms.campus.facility.domain.RoomFacilityEntity;
import com.mysms.campus.facility.events.FacilityEvents;
import com.mysms.campus.facility.mapper.RoomAssetMapper;
import com.mysms.campus.facility.mapper.RoomFacilityMapper;
import com.mysms.campus.facility.mapper.RoomMapper;
import com.mysms.campus.facility.repository.RoomAssetRepository;
import com.mysms.campus.facility.repository.RoomFacilityRepository;
import com.mysms.campus.facility.repository.RoomRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of facility.room and of the records that exist only as part of a Room (RoomAsset, RoomFacility):
 * audit and domain event commit in the same transaction as every change. Rows outside the caller's institutions are
 * invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class RoomService extends AppTransactionManager {

    public static final String AGGREGATE = RoomEntity.TABLE;
    static final String PERMISSION_LIST = "facility.room.read";
    static final String PERMISSION_GET = "facility.room.read";
    static final String PERMISSION_CREATE = "facility.room.create";
    static final String PERMISSION_UPDATE = "facility.room.update";
    static final String PERMISSION_DELETE = "facility.room.delete";

    public static final String ROOM_ASSET_AGGREGATE = RoomAssetEntity.TABLE;
    static final String ROOM_ASSET_PERMISSION_LIST = "facility.room_asset.read";
    static final String ROOM_ASSET_PERMISSION_GET = "facility.room_asset.read";
    static final String ROOM_ASSET_PERMISSION_CREATE = "facility.room_asset.create";
    static final String ROOM_ASSET_PERMISSION_UPDATE = "facility.room_asset.update";
    static final String ROOM_ASSET_PERMISSION_DELETE = "facility.room_asset.delete";

    public static final String ROOM_FACILITY_AGGREGATE = RoomFacilityEntity.TABLE;
    static final String ROOM_FACILITY_PERMISSION_LIST = "facility.room_facility.read";
    static final String ROOM_FACILITY_PERMISSION_GET = "facility.room_facility.read";
    static final String ROOM_FACILITY_PERMISSION_CREATE = "facility.room_facility.create";
    static final String ROOM_FACILITY_PERMISSION_UPDATE = "facility.room_facility.update";
    static final String ROOM_FACILITY_PERMISSION_DELETE = "facility.room_facility.delete";

    private final RoomRepository repository;
    private final RoomMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final RoomAssetRepository roomAssetRepository;
    private final RoomAssetMapper roomAssetMapper;
    private final RoomFacilityRepository roomFacilityRepository;
    private final RoomFacilityMapper roomFacilityMapper;

    public RoomService(RoomRepository repository,
            RoomMapper mapper,
            @Qualifier(FacilityDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(FacilityDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(FacilityEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(FacilityEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            RoomAssetRepository roomAssetRepository,
            RoomAssetMapper roomAssetMapper,
            RoomFacilityRepository roomFacilityRepository,
            RoomFacilityMapper roomFacilityMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.roomAssetRepository = roomAssetRepository;
        this.roomAssetMapper = roomAssetMapper;
        this.roomFacilityRepository = roomFacilityRepository;
        this.roomFacilityMapper = roomFacilityMapper;
    }

    public Room getRoom(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Room> listRooms(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, RoomRepository.SORT_FIELDS, RoomRepository.DEFAULT_ORDER,
                RoomRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, RoomRepository.FILTER_FIELDS)
                .and(RoomRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<RoomEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Room createRoom(Room request, boolean validateOnly) {
        validator.validate(request);
        RoomEntity entity = mapper.toEntity(request);
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
            RoomEntity stored = repository.insert(entity);
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
    public Room updateRoom(UUID id, String ifMatch, RoomUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            RoomEntity entity = loadVisible(scope, id);
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
    public void deleteRoom(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            RoomEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByRoomAssetRoomId(id)) {
                throw new InvalidStateException(
                        "The room is still referenced by facility.room_asset rows; remove those first.");
            }
            if (repository.referencedByRoomFacilityRoomId(id)) {
                throw new InvalidStateException(
                        "The room is still referenced by facility.room_facility rows; remove those first.");
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
    private RoomEntity loadVisible(DataScope scope, UUID id) {
        RoomEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(RoomEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(RoomEntity entity, RoomUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("code", changes.getCode(), entity::setCode);
        patch.apply("name", changes.getName(), entity::setName);
        patch.apply("roomType", changes.getRoomType(), v -> entity.setRoomType(v == null ? null : v.getValue()));
        patch.apply("capacity", changes.getCapacity(), entity::setCapacity);
        patch.apply("defaultCourseId", changes.getDefaultCourseId(), entity::setDefaultCourseId);
        patch.apply("defaultBatchId", changes.getDefaultBatchId(), entity::setDefaultBatchId);
        patch.apply("defaultClassSectionId", changes.getDefaultClassSectionId(), entity::setDefaultClassSectionId);
        patch.apply("isActive", changes.getIsActive(), entity::setIsActive);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(RoomEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (!creating && entity.getRoomType() == null) {
            errors.add(new FieldError("roomType", "must not be null", "NotNull"));
        }
        if (entity.getCapacity() == null) {
            errors.add(new FieldError("capacity", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(RoomEntity entity) {
        if (repository.takenInstitutionIdCode1(entity)) {
            throw new DuplicateResourceException("A room with the same institution id and code already exists.");
        }
    }

    // ---- RoomAsset ----

    public RoomAsset getRoomAsset(UUID roomId, UUID id) {
        return doInReadTransaction(() -> {
            return roomAssetMapper.toDto(loadVisibleRoomAsset(DataScope.current(ROOM_ASSET_PERMISSION_GET), roomId, id));
        });
    }

    public Page<RoomAsset> listRoomAssets(UUID roomId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, RoomAssetRepository.SORT_FIELDS, RoomAssetRepository.DEFAULT_ORDER,
                RoomAssetRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, RoomAssetRepository.FILTER_FIELDS)
                .and(RoomAssetRepository.ofParent(roomId))
                        .and(RoomAssetRepository.scope(DataScope.current(ROOM_ASSET_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + roomId, clock);
        return doInReadTransaction(() -> {
            Page<RoomAssetEntity> rows = roomAssetRepository.list(page, where);
            return rows.map(roomAssetMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public RoomAsset createRoomAsset(UUID roomId, RoomAsset request, boolean validateOnly) {
        validator.validate(request);
        RoomAssetEntity entity = roomAssetMapper.toEntity(request);
        entity.setRoomId(roomId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(ROOM_ASSET_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!roomAssetRepository.existsRoomId(entity.getRoomId())) {
                throw new ResourceNotFoundException("facility.room", entity.getRoomId());
            }
            if (!scope.canRead(institutionOfRoomAsset(entity))) {
                throw new ResourceNotFoundException("facility.room", entity.getRoomId());
            }
            assertValidRoomAsset(entity, true);
            scope.requireWritable(institutionOfRoomAsset(entity));
            assertUniqueRoomAsset(entity);
            if (validateOnly) {
                return roomAssetMapper.toDto(entity);
            }
            RoomAssetEntity stored = roomAssetRepository.insert(entity);
            audit.recordCreate(ROOM_ASSET_AGGREGATE, stored.getId());
            outbox.publish(ROOM_ASSET_AGGREGATE + FacilityEvents.CREATED, ROOM_ASSET_AGGREGATE, stored.getId(),
                    FacilityEvents.payload(stored.getId()));
            return roomAssetMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public RoomAsset updateRoomAsset(UUID roomId, UUID id, String ifMatch, RoomAssetUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ROOM_ASSET_PERMISSION_UPDATE);
            RoomAssetEntity entity = loadVisibleRoomAsset(scope, roomId, id);
            scope.requireWritable(institutionOfRoomAsset(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyRoomAsset(entity, changes);
            validator.validate(roomAssetMapper.toDto(entity));
            assertValidRoomAsset(entity, false);
            scope.requireWritable(institutionOfRoomAsset(entity));
            assertUniqueRoomAsset(entity);
            if (fields.isEmpty()) {
                return roomAssetMapper.toDto(entity);
            }
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return roomAssetMapper.toDto(entity);
            }
            if (roomAssetRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(ROOM_ASSET_AGGREGATE, id, fields);
            outbox.publish(ROOM_ASSET_AGGREGATE + FacilityEvents.UPDATED, ROOM_ASSET_AGGREGATE, id, FacilityEvents.payload(id));
            return roomAssetMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteRoomAsset(UUID roomId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ROOM_ASSET_PERMISSION_DELETE);
            RoomAssetEntity entity = loadVisibleRoomAsset(scope, roomId, id);
            scope.requireWritable(institutionOfRoomAsset(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (roomAssetRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(ROOM_ASSET_AGGREGATE, id);
            outbox.publish(ROOM_ASSET_AGGREGATE + FacilityEvents.DELETED, ROOM_ASSET_AGGREGATE, id, FacilityEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private RoomAssetEntity loadVisibleRoomAsset(DataScope scope, UUID roomId, UUID id) {
        RoomAssetEntity entity = roomAssetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ROOM_ASSET_AGGREGATE, id));
        if (!scope.canRead(institutionOfRoomAsset(entity)) || !roomId.equals(entity.getRoomId())) {
            throw new ResourceNotFoundException(ROOM_ASSET_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the facility.room the row belongs to. */
    private Optional<UUID> institutionOfRoomAsset(RoomAssetEntity entity) {
        return roomAssetRepository.institutionOf(entity.getRoomId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyRoomAsset(RoomAssetEntity entity, RoomAssetUpdate changes) {
        Patches patch = new Patches();
        patch.apply("assetItemId", changes.getAssetItemId(), entity::setAssetItemId);
        patch.apply("tagCode", changes.getTagCode(), entity::setTagCode);
        patch.apply("quantity", changes.getQuantity(), v -> entity.setQuantity(WireValues.decimal(v)));
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidRoomAsset(RoomAssetEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getRoomId() == null) {
            errors.add(new FieldError("roomId", "must not be null", "NotNull"));
        }
        if (entity.getAssetItemId() == null) {
            errors.add(new FieldError("assetItemId", "must not be null", "NotNull"));
        }
        if (entity.getQuantity() == null) {
            errors.add(new FieldError("quantity", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("quantity", WireValues.decimal(entity.getQuantity()), 10, 2, errors);
        if (entity.getAssetItemId() != null && !roomAssetRepository.existsAssetItemId(entity.getAssetItemId())) {
            errors.add(new FieldError("assetItemId", "asset item does not exist", "NOT_FOUND"));
        }
        if (entity.getRoomId() != null && !roomAssetRepository.existsRoomId(entity.getRoomId())) {
            errors.add(new FieldError("roomId", "room does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueRoomAsset(RoomAssetEntity entity) {

    }

    // ---- RoomFacility ----

    public RoomFacility getRoomFacility(UUID roomId, UUID id) {
        return doInReadTransaction(() -> {
            return roomFacilityMapper.toDto(loadVisibleRoomFacility(DataScope.current(ROOM_FACILITY_PERMISSION_GET), roomId, id));
        });
    }

    public Page<RoomFacility> listRoomFacilitys(UUID roomId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, RoomFacilityRepository.SORT_FIELDS, RoomFacilityRepository.DEFAULT_ORDER,
                RoomFacilityRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, RoomFacilityRepository.FILTER_FIELDS)
                .and(RoomFacilityRepository.ofParent(roomId))
                        .and(RoomFacilityRepository.scope(DataScope.current(ROOM_FACILITY_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + roomId, clock);
        return doInReadTransaction(() -> {
            Page<RoomFacilityEntity> rows = roomFacilityRepository.list(page, where);
            return rows.map(roomFacilityMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public RoomFacility createRoomFacility(UUID roomId, RoomFacility request, boolean validateOnly) {
        validator.validate(request);
        RoomFacilityEntity entity = roomFacilityMapper.toEntity(request);
        entity.setRoomId(roomId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(ROOM_FACILITY_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!roomFacilityRepository.existsRoomId(entity.getRoomId())) {
                throw new ResourceNotFoundException("facility.room", entity.getRoomId());
            }
            if (!scope.canRead(institutionOfRoomFacility(entity))) {
                throw new ResourceNotFoundException("facility.room", entity.getRoomId());
            }
            assertValidRoomFacility(entity, true);
            scope.requireWritable(institutionOfRoomFacility(entity));
            assertUniqueRoomFacility(entity);
            if (validateOnly) {
                return roomFacilityMapper.toDto(entity);
            }
            RoomFacilityEntity stored = roomFacilityRepository.insert(entity);
            audit.recordCreate(ROOM_FACILITY_AGGREGATE, stored.getId());
            outbox.publish(ROOM_FACILITY_AGGREGATE + FacilityEvents.CREATED, ROOM_FACILITY_AGGREGATE, stored.getId(),
                    FacilityEvents.payload(stored.getId()));
            return roomFacilityMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public RoomFacility updateRoomFacility(UUID roomId, UUID id, String ifMatch, RoomFacilityUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ROOM_FACILITY_PERMISSION_UPDATE);
            RoomFacilityEntity entity = loadVisibleRoomFacility(scope, roomId, id);
            scope.requireWritable(institutionOfRoomFacility(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyRoomFacility(entity, changes);
            validator.validate(roomFacilityMapper.toDto(entity));
            assertValidRoomFacility(entity, false);
            scope.requireWritable(institutionOfRoomFacility(entity));
            assertUniqueRoomFacility(entity);
            if (fields.isEmpty()) {
                return roomFacilityMapper.toDto(entity);
            }
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return roomFacilityMapper.toDto(entity);
            }
            if (roomFacilityRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(ROOM_FACILITY_AGGREGATE, id, fields);
            outbox.publish(ROOM_FACILITY_AGGREGATE + FacilityEvents.UPDATED, ROOM_FACILITY_AGGREGATE, id,
                    FacilityEvents.payload(id));
            return roomFacilityMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteRoomFacility(UUID roomId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ROOM_FACILITY_PERMISSION_DELETE);
            RoomFacilityEntity entity = loadVisibleRoomFacility(scope, roomId, id);
            scope.requireWritable(institutionOfRoomFacility(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (roomFacilityRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(ROOM_FACILITY_AGGREGATE, id);
            outbox.publish(ROOM_FACILITY_AGGREGATE + FacilityEvents.DELETED, ROOM_FACILITY_AGGREGATE, id,
                    FacilityEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private RoomFacilityEntity loadVisibleRoomFacility(DataScope scope, UUID roomId, UUID id) {
        RoomFacilityEntity entity = roomFacilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ROOM_FACILITY_AGGREGATE, id));
        if (!scope.canRead(institutionOfRoomFacility(entity)) || !roomId.equals(entity.getRoomId())) {
            throw new ResourceNotFoundException(ROOM_FACILITY_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the facility.room the row belongs to. */
    private Optional<UUID> institutionOfRoomFacility(RoomFacilityEntity entity) {
        return roomFacilityRepository.institutionOf(entity.getRoomId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyRoomFacility(RoomFacilityEntity entity, RoomFacilityUpdate changes) {
        Patches patch = new Patches();
        patch.apply("facilityId", changes.getFacilityId(), entity::setFacilityId);
        patch.apply("quantity", changes.getQuantity(), v -> entity.setQuantity(WireValues.decimal(v)));
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidRoomFacility(RoomFacilityEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getRoomId() == null) {
            errors.add(new FieldError("roomId", "must not be null", "NotNull"));
        }
        if (entity.getFacilityId() == null) {
            errors.add(new FieldError("facilityId", "must not be null", "NotNull"));
        }
        if (entity.getQuantity() == null) {
            errors.add(new FieldError("quantity", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("quantity", WireValues.decimal(entity.getQuantity()), 10, 2, errors);
        if (entity.getFacilityId() != null && !roomFacilityRepository.existsFacilityId(entity.getFacilityId())) {
            errors.add(new FieldError("facilityId", "facility does not exist", "NOT_FOUND"));
        }
        if (entity.getRoomId() != null && !roomFacilityRepository.existsRoomId(entity.getRoomId())) {
            errors.add(new FieldError("roomId", "room does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueRoomFacility(RoomFacilityEntity entity) {
        if (roomFacilityRepository.takenRoomIdFacilityId1(entity)) {
            throw new DuplicateResourceException(
                    "A room facility with the same room id and facility id already exists.");
        }
    }
}
