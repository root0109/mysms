package com.mysms.lms.business;

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
import com.mysms.core.web.UpdateMask;
import com.mysms.lms.api.model.DriveItem;
import com.mysms.lms.api.model.DriveItemShare;
import com.mysms.lms.api.model.DriveItemShareUpdate;
import com.mysms.lms.api.model.DriveItemUpdate;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.DriveItemEntity;
import com.mysms.lms.domain.DriveItemShareEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.DriveItemMapper;
import com.mysms.lms.mapper.DriveItemShareMapper;
import com.mysms.lms.repository.DriveItemRepository;
import com.mysms.lms.repository.DriveItemShareRepository;
import java.time.Clock;
import java.util.Set;
import java.util.LinkedHashSet;
import java.time.temporal.ChronoUnit;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import com.mysms.lms.repository.DriveSharingRepository;
import com.mysms.lms.api.model.ShareDriveItemRequest;
import com.mysms.lms.api.model.DriveItemShareList;
import com.mysms.core.id.UuidV7;
import com.mysms.authclient.AuthContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.drive_item and of the records that exist only as part of a DriveItem (DriveItemShare): audit
 * and domain event commit in the same transaction as every change. Rows outside the caller's institutions are
 * invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class DriveItemService extends AppTransactionManager {

    public static final String AGGREGATE = DriveItemEntity.TABLE;
    static final String PERMISSION_LIST = "lms.drive_item.read";
    static final String PERMISSION_GET = "lms.drive_item.read";
    static final String PERMISSION_CREATE = "lms.drive_item.create";
    static final String PERMISSION_UPDATE = "lms.drive_item.update";
    static final String PERMISSION_DELETE = "lms.drive_item.delete";
    static final String PERMISSION_SHARE = "lms.drive_item.share_drive_item";
    static final int MAX_SHARE = 100;
    /** The contract's DriveItemUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("fileId", "itemType", "icon", "title", "description", "keywords", "sizeBytes",
                    "linkUrl",
            "uploadedAt", "courseId", "isShared");

    public static final String DRIVE_ITEM_SHARE_AGGREGATE = DriveItemShareEntity.TABLE;
    static final String DRIVE_ITEM_SHARE_PERMISSION_LIST = "lms.drive_item_share.read";
    static final String DRIVE_ITEM_SHARE_PERMISSION_CREATE = "lms.drive_item_share.create";
    static final String DRIVE_ITEM_SHARE_PERMISSION_UPDATE = "lms.drive_item_share.update";
    static final String DRIVE_ITEM_SHARE_PERMISSION_DELETE = "lms.drive_item_share.delete";
    /** The contract's DriveItemShareUpdate fields, in apply order. */
    static final List<String> DRIVE_ITEM_SHARE_WRITABLE = List.of("userId", "reaction");

    private final DriveItemRepository repository;
    private final DriveItemMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final DriveItemShareRepository driveItemShareRepository;
    private final DriveItemShareMapper driveItemShareMapper;
    private final DriveSharingRepository sharing;

    public DriveItemService(DriveItemRepository repository,
            DriveItemMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            DriveItemShareRepository driveItemShareRepository,
            DriveItemShareMapper driveItemShareMapper,
            DriveSharingRepository sharing) {
        super(database, transactions);
        this.sharing = sharing;
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.driveItemShareRepository = driveItemShareRepository;
        this.driveItemShareMapper = driveItemShareMapper;
    }

    public DriveItem getDriveItem(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<DriveItem> listDriveItems(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, DriveItemRepository.SORT_FIELDS, DriveItemRepository.DEFAULT_ORDER,
                DriveItemRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, DriveItemRepository.FILTER_FIELDS)
                .and(DriveItemRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<DriveItemEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public DriveItem createDriveItem(DriveItem request, boolean validateOnly) {
        validator.validate(request);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkInt64("sizeBytes", request.getSizeBytes(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        DriveItemEntity entity = mapper.toEntity(request);
        entity.setOwnerUserId(OwnRows.caller());
        entity.setLikeCount(null);
        entity.setDislikeCount(null);
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
            DriveItemEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public DriveItem updateDriveItem(UUID id, String ifMatch, DriveItemUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkInt64("sizeBytes", changes.getSizeBytes(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            DriveItemEntity entity = loadVisible(scope, id);
            OwnRows.require(entity.getOwnerUserId(), AGGREGATE, id);
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
            outbox.publish(AGGREGATE + LmsEvents.UPDATED, AGGREGATE, id, LmsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteDriveItem(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            DriveItemEntity entity = loadVisible(scope, id);
            OwnRows.require(entity.getOwnerUserId(), AGGREGATE, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByDriveItemShareDriveItemId(id)) {
                throw new InvalidStateException(
                        "The drive item is still referenced by lms.drive_item_share rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + LmsEvents.DELETED, AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private DriveItemEntity loadVisible(DataScope scope, UUID id) {
        DriveItemEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.course the row belongs to. */
    private Optional<UUID> institutionOf(DriveItemEntity entity) {
        return repository.institutionOf(entity.getCourseId());
    }

    /** Applies the selected DriveItemUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(DriveItemEntity entity, DriveItemUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "fileId" -> entity.setFileId(changes.getFileId());
                case "itemType" -> entity.setItemType(changes.getItemType());
                case "icon" -> entity.setIcon(changes.getIcon());
                case "title" -> entity.setTitle(changes.getTitle());
                case "description" -> entity.setDescription(changes.getDescription());
                case "keywords" -> entity.setKeywords(changes.getKeywords());
                case "sizeBytes" -> entity.setSizeBytes(WireValues.int64(changes.getSizeBytes()));
                case "linkUrl" -> entity.setLinkUrl(changes.getLinkUrl());
                case "uploadedAt" -> entity.setUploadedAt(changes.getUploadedAt());
                case "courseId" -> entity.setCourseId(changes.getCourseId());
                case "isShared" -> entity.setIsShared(changes.getIsShared());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(DriveItemEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getOwnerUserId() == null) {
            errors.add(new FieldError("ownerUserId", "must not be null", "NotNull"));
        }
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (!creating && entity.getUploadedAt() == null) {
            errors.add(new FieldError("uploadedAt", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsShared() == null) {
            errors.add(new FieldError("isShared", "must not be null", "NotNull"));
        }
        if (!creating && entity.getLikeCount() == null) {
            errors.add(new FieldError("likeCount", "must not be null", "NotNull"));
        }
        if (!creating && entity.getDislikeCount() == null) {
            errors.add(new FieldError("dislikeCount", "must not be null", "NotNull"));
        }
        if (entity.getCourseId() != null && !repository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(DriveItemEntity entity) {

    }

    // ---- DriveItemShare: a record of the DriveItem aggregate ----

    public Page<DriveItemShare> listDriveItemShares(UUID driveItemId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, DriveItemShareRepository.SORT_FIELDS, DriveItemShareRepository.DEFAULT_ORDER,
                DriveItemShareRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, DriveItemShareRepository.FILTER_FIELDS)
                .and(DriveItemShareRepository.ofParent(driveItemId))
                        .and(DriveItemShareRepository.scope(DataScope.current(DRIVE_ITEM_SHARE_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + driveItemId, clock);
        return doInReadTransaction(() -> {
            Page<DriveItemShareEntity> rows = driveItemShareRepository.list(page, where);
            return rows.map(driveItemShareMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public DriveItemShare createDriveItemShare(UUID driveItemId, DriveItemShare request, boolean validateOnly) {
        validator.validate(request);
        DriveItemShareEntity entity = driveItemShareMapper.toEntity(request);
        entity.setDriveItemId(driveItemId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(DRIVE_ITEM_SHARE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!driveItemShareRepository.existsDriveItemId(entity.getDriveItemId())) {
                throw new ResourceNotFoundException("lms.drive_item", entity.getDriveItemId());
            }
            if (!scope.canRead(institutionOfDriveItemShare(entity))) {
                throw new ResourceNotFoundException("lms.drive_item", entity.getDriveItemId());
            }
            assertValidDriveItemShare(entity, true);
            scope.requireWritable(institutionOfDriveItemShare(entity));
            assertUniqueDriveItemShare(entity);
            if (validateOnly) {
                return driveItemShareMapper.toDto(entity);
            }
            DriveItemShareEntity stored = driveItemShareRepository.insert(entity);
            audit.recordCreate(DRIVE_ITEM_SHARE_AGGREGATE, stored.getId());
            outbox.publish(DRIVE_ITEM_SHARE_AGGREGATE + LmsEvents.CREATED, DRIVE_ITEM_SHARE_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return driveItemShareMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public DriveItemShare updateDriveItemShare(UUID driveItemId, UUID id, String ifMatch, DriveItemShareUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, DRIVE_ITEM_SHARE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(DRIVE_ITEM_SHARE_PERMISSION_UPDATE);
            DriveItemShareEntity entity = loadVisibleDriveItemShare(scope, driveItemId, id);
            scope.requireWritable(institutionOfDriveItemShare(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyDriveItemShare(entity, changes, fields);
            validator.validate(driveItemShareMapper.toDto(entity));
            assertValidDriveItemShare(entity, false);
            scope.requireWritable(institutionOfDriveItemShare(entity));
            assertUniqueDriveItemShare(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return driveItemShareMapper.toDto(entity);
            }
            if (driveItemShareRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(DRIVE_ITEM_SHARE_AGGREGATE, id, fields);
            outbox.publish(DRIVE_ITEM_SHARE_AGGREGATE + LmsEvents.UPDATED, DRIVE_ITEM_SHARE_AGGREGATE, id, LmsEvents.payload(id));
            return driveItemShareMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteDriveItemShare(UUID driveItemId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(DRIVE_ITEM_SHARE_PERMISSION_DELETE);
            DriveItemShareEntity entity = loadVisibleDriveItemShare(scope, driveItemId, id);
            scope.requireWritable(institutionOfDriveItemShare(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (driveItemShareRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(DRIVE_ITEM_SHARE_AGGREGATE, id);
            outbox.publish(DRIVE_ITEM_SHARE_AGGREGATE + LmsEvents.DELETED, DRIVE_ITEM_SHARE_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private DriveItemShareEntity loadVisibleDriveItemShare(DataScope scope, UUID driveItemId, UUID id) {
        DriveItemShareEntity entity = driveItemShareRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DRIVE_ITEM_SHARE_AGGREGATE, id));
        if (!scope.canRead(institutionOfDriveItemShare(entity)) || !driveItemId.equals(entity.getDriveItemId())) {
            throw new ResourceNotFoundException(DRIVE_ITEM_SHARE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.drive_item → lms.course it belongs to). */
    private Optional<UUID> institutionOfDriveItemShare(DriveItemShareEntity entity) {
        return driveItemShareRepository.institutionOf(entity.getDriveItemId());
    }

    /** Applies the selected DriveItemShareUpdate fields; explicit per field so a mask can clear values. */
    private static void applyDriveItemShare(DriveItemShareEntity entity, DriveItemShareUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "userId" -> entity.setUserId(changes.getUserId());
                case "reaction" -> entity.setReaction(Optional.ofNullable(changes.getReaction()).map(v -> v.getValue())
                        .orElse(null));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidDriveItemShare(DriveItemShareEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getDriveItemId() == null) {
            errors.add(new FieldError("driveItemId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getDriveItemId() != null && !driveItemShareRepository.existsDriveItemId(entity.getDriveItemId())) {
            errors.add(new FieldError("driveItemId", "drive item does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueDriveItemShare(DriveItemShareEntity entity) {
        if (driveItemShareRepository.takenDriveItemIdUserId1(entity)) {
            throw new DuplicateResourceException(
                    "A drive item share with the same drive item id and user id already exists.");
        }
    }

    // ---- ShareDriveItem ----

    /**
     * Sets who the caller's own item is shared with (at most 100 users, never the owner): the list replaces the
     * current one — a user who stays keeps their share and reaction, a user left out loses it, an empty list unshares
     * the item. Anyone else's item is 404, as a drive is personal. Answers the item's shares as they now stand.
     */
    public DriveItemShareList shareDriveItem(UUID id, ShareDriveItemRequest request) {
        validator.validate(request);
        Set<UUID> users = new LinkedHashSet<>(request.getUserIds());
        UUID owner = AuthContext.require().userId();
        List<FieldError> errors = new ArrayList<>();
        if (users.size() > MAX_SHARE) {
            errors.add(new FieldError("userIds", "must name at most " + MAX_SHARE + " users", "Size"));
        }
        if (users.contains(owner)) {
            errors.add(new FieldError("userIds", "an item is not shared with its owner", "INVALID"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        return doInWriteTransaction(() -> {
            DriveSharingRepository.Owned item = sharing.lock(id).filter(o -> owner.equals(o.ownerUserId()))
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            List<UUID> current = sharing.sharedWith(id);
            for (UUID user : current) {
                if (!users.contains(user)) {
                    for (UUID ended : sharing.unshare(id, user, now, owner)) {
                        audit.recordDelete(DRIVE_ITEM_SHARE_AGGREGATE, ended);
                        outbox.publish(DRIVE_ITEM_SHARE_AGGREGATE + LmsEvents.DELETED, DRIVE_ITEM_SHARE_AGGREGATE,
                                ended, LmsEvents.payload(ended));
                    }
                }
            }
            for (UUID user : users) {
                if (!current.contains(user)) {
                    UUID shareId = UuidV7.next(clock);
                    sharing.share(shareId, id, user, now, owner);
                    audit.recordCreate(DRIVE_ITEM_SHARE_AGGREGATE, shareId);
                    outbox.publish(DRIVE_ITEM_SHARE_AGGREGATE + LmsEvents.CREATED, DRIVE_ITEM_SHARE_AGGREGATE,
                            shareId, LmsEvents.payload(shareId));
                }
            }
            if (sharing.markShared(id, item.rowVersion(), !users.isEmpty(), now, owner) == 0) {
                throw new PreconditionFailedException("The item changed meanwhile; share it again.");
            }
            audit.recordTransition(AGGREGATE, id, "share", List.of("isShared"), null);
            outbox.publish(AGGREGATE + ".share", AGGREGATE, id, LmsEvents.payload(id));
            DriveItemShareList list = new DriveItemShareList();
            list.setItems(sharing.shares(id).stream().map(s -> driveItemShareRepository.findById(s).orElseThrow())
                    .map(driveItemShareMapper::toDto).toList());
            return list;
        });
    }
}
