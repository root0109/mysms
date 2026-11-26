package com.mysms.platform.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.platform.api.model.StorageQuota;
import com.mysms.platform.api.model.StorageQuotaUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ConflictException;
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
import com.mysms.platform.domain.StorageQuotaEntity;
import com.mysms.platform.events.PlatformEvents;
import com.mysms.platform.mapper.StorageQuotaMapper;
import com.mysms.platform.repository.StorageQuotaRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of platform.storage_quota: audit and domain event in the same transaction as every change; rows
 * outside the caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class StorageQuotaService extends AppTransactionManager {

    public static final String AGGREGATE = StorageQuotaEntity.TABLE;
    static final String PERMISSION_LIST = "platform.storage_quota.read";
    static final String PERMISSION_GET = "platform.storage_quota.read";
    static final String PERMISSION_CREATE = "platform.storage_quota.create";
    static final String PERMISSION_UPDATE = "platform.storage_quota.update";
    static final String PERMISSION_DELETE = "platform.storage_quota.delete";
    /** 409 code of an upload or copy that would exceed the uploader's enforced quota. */
    public static final String QUOTA_EXCEEDED = "QUOTA_EXCEEDED";
    private static final List<String> USED_BYTES = List.of("usedBytes");


    private final StorageQuotaRepository repository;
    private final StorageQuotaMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public StorageQuotaService(StorageQuotaRepository repository, StorageQuotaMapper mapper,
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

    public StorageQuota getStorageQuota(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<StorageQuota> listStorageQuotas(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, StorageQuotaRepository.SORT_FIELDS, StorageQuotaRepository.DEFAULT_ORDER,
                StorageQuotaRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, StorageQuotaRepository.FILTER_FIELDS)
                .and(StorageQuotaRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<StorageQuotaEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public StorageQuota createStorageQuota(StorageQuota request, boolean validateOnly) {
        validator.validate(request);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkInt64("quotaBytes", request.getQuotaBytes(), wire);
        WireValues.checkInt64("usedBytes", request.getUsedBytes(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        StorageQuotaEntity entity = mapper.toEntity(request);
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
            StorageQuotaEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + PlatformEvents.CREATED, AGGREGATE, stored.getId(),
                    PlatformEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. A field the request leaves out is kept, a null one is cleared and a
     * value is set (§7.2). The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public StorageQuota updateStorageQuota(UUID id, String ifMatch, StorageQuotaUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkInt64("quotaBytes", Patches.value(changes.getQuotaBytes()), wire);
        WireValues.checkInt64("usedBytes", Patches.value(changes.getUsedBytes()), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            StorageQuotaEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + PlatformEvents.UPDATED, AGGREGATE, id, PlatformEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteStorageQuota(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            StorageQuotaEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + PlatformEvents.DELETED, AGGREGATE, id, PlatformEvents.payload(id));
        });
    }

    /**
     * platform.GetMyStorageUsage: the caller's own quota and the bytes their files take; 404 when no quota is set for
     * them (storage is then limited only by the tenant's plan).
     */
    public StorageQuota getMyStorageUsage() {
        UUID caller = AuthContext.require().userId();
        return doInReadTransaction(() -> mapper.toDto(repository.findForUser(caller)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, caller))));
    }

    /**
     * Counts a file's bytes against its uploader's quota, in the caller's transaction (the file workflow runs it as it
     * makes the file available): 409 QUOTA_EXCEEDED when an enforced quota would be exceeded; nothing to count when
     * the uploader has no quota. The quota change is audited like any update.
     */
    public void spend(UUID user, long bytes) {
        OffsetDateTime now = now();
        runInWriteTransaction(() -> {
            if (repository.findForUser(user).isEmpty()) {
                return;
            }
            List<UUID> changed = repository.spend(user, bytes, now, Actor.current().orElse(null));
            if (changed.isEmpty()) {
                throw new ConflictException(QUOTA_EXCEEDED, "The file would exceed your storage quota.");
            }
            changed.forEach(id -> audit.recordUpdate(AGGREGATE, id, USED_BYTES));
        });
    }

    /** Whether {@code bytes} more still fit the user's enforced quota (the early check before an upload starts). */
    public boolean fits(UUID user, long bytes) {
        return doInReadTransaction(() -> repository.findForUser(user)
                .map(q -> !Boolean.TRUE.equals(q.getIsActive()) || q.getUsedBytes() + bytes <= q.getQuotaBytes())
                .orElse(true));
    }

    /** Gives a deleted file's bytes back to its uploader's quota, in the caller's transaction. */
    public void release(UUID user, long bytes) {
        OffsetDateTime now = now();
        runInWriteTransaction(() -> repository.release(user, bytes, now, Actor.current().orElse(null))
                .forEach(id -> audit.recordUpdate(AGGREGATE, id, USED_BYTES)));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private StorageQuotaEntity loadVisible(DataScope scope, UUID id) {
        StorageQuotaEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(StorageQuotaEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(StorageQuotaEntity entity, StorageQuotaUpdate changes) {
        Patches patch = new Patches();
        patch.apply("userId", changes.getUserId(), entity::setUserId);
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("quotaBytes", changes.getQuotaBytes(), v -> entity.setQuotaBytes(WireValues.int64(v)));
        patch.apply("usedBytes", changes.getUsedBytes(), v -> entity.setUsedBytes(WireValues.int64(v)));
        patch.apply("isActive", changes.getIsActive(), entity::setIsActive);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(StorageQuotaEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getQuotaBytes() == null) {
            errors.add(new FieldError("quotaBytes", "must not be null", "NotNull"));
        }
        if (!creating && entity.getUsedBytes() == null) {
            errors.add(new FieldError("usedBytes", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(StorageQuotaEntity entity) {

    }
}
