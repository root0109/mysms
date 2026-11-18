package com.mysms.comms.business;

import com.mysms.authclient.DataScope;
import com.mysms.comms.api.model.NotificationPreference;
import com.mysms.comms.api.model.NotificationPreferenceSet;
import com.mysms.comms.api.model.NotificationPreferenceSetUpdate;
import com.mysms.comms.api.model.NotificationPreferenceUpdate;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.config.CommsEventsConfig;
import com.mysms.comms.domain.NotificationPreferenceEntity;
import com.mysms.comms.domain.NotificationPreferenceSetEntity;
import com.mysms.comms.events.CommsEvents;
import com.mysms.comms.mapper.NotificationPreferenceMapper;
import com.mysms.comms.mapper.NotificationPreferenceSetMapper;
import com.mysms.comms.repository.NotificationPreferenceRepository;
import com.mysms.comms.repository.NotificationPreferenceSetRepository;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
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
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of comms.notification_preference_set and its NotificationPreference records: audit and domain event
 * commit in the same transaction as every change. Sets are owner-only: a tenant permission does not reach another
 * account's settings.
 */
@Service
public class NotificationPreferenceSetService extends AppTransactionManager {

    public static final String AGGREGATE = NotificationPreferenceSetEntity.TABLE;
    static final String PERMISSION_LIST = "comms.notification_preference_set.read";
    static final String PERMISSION_GET = "comms.notification_preference_set.read";
    static final String PERMISSION_CREATE = "comms.notification_preference_set.create";
    static final String PERMISSION_UPDATE = "comms.notification_preference_set.update";
    static final String PERMISSION_DELETE = "comms.notification_preference_set.delete";
    /** The contract's NotificationPreferenceSetUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("userId", "roleId");

    public static final String NOTIFICATION_PREFERENCE_AGGREGATE = NotificationPreferenceEntity.TABLE;
    static final String NOTIFICATION_PREFERENCE_PERMISSION_LIST = "comms.notification_preference.read";
    static final String NOTIFICATION_PREFERENCE_PERMISSION_GET = "comms.notification_preference.read";
    static final String NOTIFICATION_PREFERENCE_PERMISSION_CREATE = "comms.notification_preference.create";
    static final String NOTIFICATION_PREFERENCE_PERMISSION_UPDATE = "comms.notification_preference.update";
    static final String NOTIFICATION_PREFERENCE_PERMISSION_DELETE = "comms.notification_preference.delete";
    /** The contract's NotificationPreferenceUpdate fields, in apply order. */
    static final List<String> NOTIFICATION_PREFERENCE_WRITABLE = List.of("notificationTypeId", "propertyName", "propertyValue");

    private final NotificationPreferenceSetRepository repository;
    private final NotificationPreferenceSetMapper mapper;
    private final NotificationPreferenceSetRules rules;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final NotificationPreferenceMapper notificationPreferenceMapper;
    private final NotificationPreferenceRules notificationPreferenceRules;

    public NotificationPreferenceSetService(NotificationPreferenceSetRepository repository,
            NotificationPreferenceSetMapper mapper,
            NotificationPreferenceSetRules rules,
            @Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CommsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CommsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(CommsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            NotificationPreferenceRepository notificationPreferenceRepository,
            NotificationPreferenceMapper notificationPreferenceMapper,
            NotificationPreferenceRules notificationPreferenceRules) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.rules = rules;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.notificationPreferenceMapper = notificationPreferenceMapper;
        this.notificationPreferenceRules = notificationPreferenceRules;
    }

    public NotificationPreferenceSet getNotificationPreferenceSet(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<NotificationPreferenceSet> listNotificationPreferenceSets(Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, NotificationPreferenceSetRepository.SORT_FIELDS,
                        NotificationPreferenceSetRepository.DEFAULT_ORDER,
                NotificationPreferenceSetRepository.ID_COLUMN);
        SqlFragment filterWhere = FilterParser.parse(filter, NotificationPreferenceSetRepository.FILTER_FIELDS);
        UUID ownerId = CallContextHolder.require().userId().orElseThrow();
        SqlFragment where = filterWhere.and(NotificationPreferenceSetRepository.ownerFilter(ownerId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + ownerId, clock);
        return doInReadTransaction(() -> {
            Page<NotificationPreferenceSetEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public NotificationPreferenceSet createNotificationPreferenceSet(NotificationPreferenceSet request,
            boolean validateOnly) {
        validator.validate(request);
        NotificationPreferenceSetEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        return doInWriteTransaction(() -> {
            assertValid(entity, true);
            rules.beforeWrite(entity);
            assertUnique(entity);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            NotificationPreferenceSetEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + CommsEvents.CREATED, AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public NotificationPreferenceSet updateNotificationPreferenceSet(UUID id, String ifMatch,
            NotificationPreferenceSetUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            NotificationPreferenceSetEntity entity = loadVisible(scope, id);
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            rules.beforeUpdate(entity);
            apply(entity, changes, fields);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            rules.beforeWrite(entity);
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
            outbox.publish(AGGREGATE + CommsEvents.UPDATED, AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteNotificationPreferenceSet(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            NotificationPreferenceSetEntity entity = loadVisible(scope, id);
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            rules.beforeDelete(entity);
            if (repository.referencedByNotificationPreferencePreferenceSetId(id)) {
                throw new InvalidStateException(
                        "The notification preference set is still referenced by comms.notification_preference rows; "
                                + "remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + CommsEvents.DELETED, AGGREGATE, id, CommsEvents.payload(id));
            rules.afterDelete(entity);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private NotificationPreferenceSetEntity loadVisible(DataScope scope, UUID id) {
        NotificationPreferenceSetEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        rules.beforeAccess(entity.getUserId(), id, scope);
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOf(NotificationPreferenceSetEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected NotificationPreferenceSetUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(NotificationPreferenceSetEntity entity, NotificationPreferenceSetUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "userId" -> entity.setUserId(changes.getUserId());
                case "roleId" -> entity.setRoleId(changes.getRoleId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(NotificationPreferenceSetEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(NotificationPreferenceSetEntity entity) {

    }

    // ---- NotificationPreference ----

    public NotificationPreference getNotificationPreference(UUID preferenceSetId, UUID id) {
        return doInReadTransaction(() -> {
            return notificationPreferenceMapper.toDto(loadVisibleNotificationPreference(DataScope.current(
                    NOTIFICATION_PREFERENCE_PERMISSION_GET), preferenceSetId, id));
        });
    }

    public Page<NotificationPreference> listNotificationPreferences(UUID preferenceSetId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, NotificationPreferenceRepository.SORT_FIELDS,
                        NotificationPreferenceRepository.DEFAULT_ORDER,
                NotificationPreferenceRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, NotificationPreferenceRepository.FILTER_FIELDS)
                .and(NotificationPreferenceRepository.ofParent(preferenceSetId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + preferenceSetId, clock);
        return doInReadTransaction(() -> {
            notificationPreferenceRules.beforeAccess(preferenceSetId, DataScope.current(NOTIFICATION_PREFERENCE_PERMISSION_LIST));
            Page<NotificationPreferenceEntity> rows = notificationPreferenceRepository.list(page, where);
            return rows.map(notificationPreferenceMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public NotificationPreference createNotificationPreference(UUID preferenceSetId, NotificationPreference request,
            boolean validateOnly) {
        validator.validate(request);
        NotificationPreferenceEntity entity = notificationPreferenceMapper.toEntity(request);
        entity.setPreferenceSetId(preferenceSetId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        return doInWriteTransaction(() -> {
            if (!notificationPreferenceRepository.existsPreferenceSetId(entity.getPreferenceSetId())) {
                throw new ResourceNotFoundException("comms.notification_preference_set", entity.getPreferenceSetId());
            }
            notificationPreferenceRules.beforeAccess(entity.getPreferenceSetId(),
                    DataScope.current(NOTIFICATION_PREFERENCE_PERMISSION_CREATE));
            assertValidNotificationPreference(entity, true);
            notificationPreferenceRules.beforeWrite(entity);
            assertUniqueNotificationPreference(entity);
            if (validateOnly) {
                return notificationPreferenceMapper.toDto(entity);
            }
            NotificationPreferenceEntity stored = notificationPreferenceRepository.insert(entity);
            audit.recordCreate(NOTIFICATION_PREFERENCE_AGGREGATE, stored.getId());
            outbox.publish(NOTIFICATION_PREFERENCE_AGGREGATE + CommsEvents.CREATED, NOTIFICATION_PREFERENCE_AGGREGATE,
                    stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return notificationPreferenceMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public NotificationPreference updateNotificationPreference(UUID preferenceSetId, UUID id, String ifMatch,
            NotificationPreferenceUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, NOTIFICATION_PREFERENCE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(NOTIFICATION_PREFERENCE_PERMISSION_UPDATE);
            NotificationPreferenceEntity entity = loadVisibleNotificationPreference(scope, preferenceSetId, id);
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            notificationPreferenceRules.beforeUpdate(entity);
            applyNotificationPreference(entity, changes, fields);
            validator.validate(notificationPreferenceMapper.toDto(entity));
            assertValidNotificationPreference(entity, false);
            notificationPreferenceRules.beforeWrite(entity);
            assertUniqueNotificationPreference(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return notificationPreferenceMapper.toDto(entity);
            }
            if (notificationPreferenceRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(NOTIFICATION_PREFERENCE_AGGREGATE, id, fields);
            outbox.publish(NOTIFICATION_PREFERENCE_AGGREGATE + CommsEvents.UPDATED, NOTIFICATION_PREFERENCE_AGGREGATE, id,
                    CommsEvents.payload(id));
            return notificationPreferenceMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteNotificationPreference(UUID preferenceSetId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(NOTIFICATION_PREFERENCE_PERMISSION_DELETE);
            NotificationPreferenceEntity entity = loadVisibleNotificationPreference(scope, preferenceSetId, id);
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            notificationPreferenceRules.beforeDelete(entity);
            entity.onDeletingData(Actor.current(), clock);
            if (notificationPreferenceRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(NOTIFICATION_PREFERENCE_AGGREGATE, id);
            outbox.publish(NOTIFICATION_PREFERENCE_AGGREGATE + CommsEvents.DELETED, NOTIFICATION_PREFERENCE_AGGREGATE, id,
                    CommsEvents.payload(id));
            notificationPreferenceRules.afterDelete(entity);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private NotificationPreferenceEntity loadVisibleNotificationPreference(DataScope scope, UUID preferenceSetId, UUID id) {
        notificationPreferenceRules.beforeAccess(preferenceSetId, scope);
        NotificationPreferenceEntity entity = notificationPreferenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(NOTIFICATION_PREFERENCE_AGGREGATE, id));
        if (!preferenceSetId.equals(entity.getPreferenceSetId())) {
            throw new ResourceNotFoundException(NOTIFICATION_PREFERENCE_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfNotificationPreference(NotificationPreferenceEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected NotificationPreferenceUpdate fields; explicit per field so a mask can clear values. */
    private static void applyNotificationPreference(NotificationPreferenceEntity entity, NotificationPreferenceUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "notificationTypeId" -> entity.setNotificationTypeId(changes.getNotificationTypeId());
                case "propertyName" -> entity.setPropertyName(changes.getPropertyName());
                case "propertyValue" -> entity.setPropertyValue(changes.getPropertyValue());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidNotificationPreference(NotificationPreferenceEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getPreferenceSetId() == null) {
            errors.add(new FieldError("preferenceSetId", "must not be null", "NotNull"));
        }
        if (entity.getPropertyName() == null) {
            errors.add(new FieldError("propertyName", "must not be null", "NotNull"));
        }
        if (entity.getNotificationTypeId() != null
                && !notificationPreferenceRepository.existsNotificationTypeId(entity.getNotificationTypeId())) {
            errors.add(new FieldError("notificationTypeId", "notification type does not exist", "NOT_FOUND"));
        }
        if (entity.getPreferenceSetId() != null
                && !notificationPreferenceRepository.existsPreferenceSetId(entity.getPreferenceSetId())) {
            errors.add(new FieldError("preferenceSetId", "notification preference set does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueNotificationPreference(NotificationPreferenceEntity entity) {

    }
}
