package com.mysms.comms.business;

import com.mysms.authclient.DataScope;
import com.mysms.comms.api.model.NotificationCohortSetting;
import com.mysms.comms.api.model.NotificationCohortSettingUpdate;
import com.mysms.comms.api.model.NotificationRule;
import com.mysms.comms.api.model.NotificationRuleUpdate;
import com.mysms.comms.api.model.NotificationType;
import com.mysms.comms.api.model.NotificationTypeProperty;
import com.mysms.comms.api.model.NotificationTypePropertyUpdate;
import com.mysms.comms.api.model.NotificationTypeUpdate;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.config.CommsEventsConfig;
import com.mysms.comms.domain.NotificationCohortSettingEntity;
import com.mysms.comms.domain.NotificationRuleEntity;
import com.mysms.comms.domain.NotificationTypeEntity;
import com.mysms.comms.domain.NotificationTypePropertyEntity;
import com.mysms.comms.events.CommsEvents;
import com.mysms.comms.mapper.NotificationCohortSettingMapper;
import com.mysms.comms.mapper.NotificationRuleMapper;
import com.mysms.comms.mapper.NotificationTypeMapper;
import com.mysms.comms.mapper.NotificationTypePropertyMapper;
import com.mysms.comms.repository.NotificationCohortSettingRepository;
import com.mysms.comms.repository.NotificationRuleRepository;
import com.mysms.comms.repository.NotificationTypePropertyRepository;
import com.mysms.comms.repository.NotificationTypeRepository;
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
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of comms.notification_type (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a NotificationType
 * are served here too — NotificationCohortSetting, NotificationRule, NotificationTypeProperty.
 */
@Service
public class NotificationTypeService extends AppTransactionManager {

    public static final String AGGREGATE = NotificationTypeEntity.TABLE;
    /** The contract's NotificationTypeUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("code", "description", "moduleCode", "roleCode", "isEnabled", "popupEnabled", "teacherPopupEnabled",
            "validParameters");

    public static final String NOTIFICATION_COHORT_SETTING_AGGREGATE = NotificationCohortSettingEntity.TABLE;
    static final String NOTIFICATION_COHORT_SETTING_PERMISSION_LIST = "comms.notification_cohort_setting.read";
    static final String NOTIFICATION_COHORT_SETTING_PERMISSION_GET = "comms.notification_cohort_setting.read";
    static final String NOTIFICATION_COHORT_SETTING_PERMISSION_CREATE = "comms.notification_cohort_setting.create";
    static final String NOTIFICATION_COHORT_SETTING_PERMISSION_UPDATE = "comms.notification_cohort_setting.update";
    static final String NOTIFICATION_COHORT_SETTING_PERMISSION_DELETE = "comms.notification_cohort_setting.delete";
    /** The contract's NotificationCohortSettingUpdate fields, in apply order. */
    static final List<String> NOTIFICATION_COHORT_SETTING_WRITABLE = List.of("cohortId", "popupEnabled");

    public static final String NOTIFICATION_RULE_AGGREGATE = NotificationRuleEntity.TABLE;
    static final String NOTIFICATION_RULE_PERMISSION_LIST = "comms.notification_rule.read";
    static final String NOTIFICATION_RULE_PERMISSION_GET = "comms.notification_rule.read";
    static final String NOTIFICATION_RULE_PERMISSION_CREATE = "comms.notification_rule.create";
    static final String NOTIFICATION_RULE_PERMISSION_UPDATE = "comms.notification_rule.update";
    static final String NOTIFICATION_RULE_PERMISSION_DELETE = "comms.notification_rule.delete";
    /** The contract's NotificationRuleUpdate fields, in apply order. */
    static final List<String> NOTIFICATION_RULE_WRITABLE =
            List.of("roleId", "permissionId", "isEnabled", "emailSubjectTemplate", "emailBodyTemplate", "smsTemplate");

    public static final String NOTIFICATION_TYPE_PROPERTY_AGGREGATE = NotificationTypePropertyEntity.TABLE;
    /** The contract's NotificationTypePropertyUpdate fields, in apply order. */
    static final List<String> NOTIFICATION_TYPE_PROPERTY_WRITABLE = List.of("moduleCode", "propertyValue");

    private final NotificationTypeRepository repository;
    private final NotificationTypeMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final NotificationCohortSettingRepository notificationCohortSettingRepository;
    private final NotificationCohortSettingMapper notificationCohortSettingMapper;
    private final NotificationRuleRepository notificationRuleRepository;
    private final NotificationRuleMapper notificationRuleMapper;
    private final NotificationTypePropertyRepository notificationTypePropertyRepository;
    private final NotificationTypePropertyMapper notificationTypePropertyMapper;

    public NotificationTypeService(NotificationTypeRepository repository,
            NotificationTypeMapper mapper,
            @Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CommsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CommsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(CommsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            NotificationCohortSettingRepository notificationCohortSettingRepository,
            NotificationCohortSettingMapper notificationCohortSettingMapper,
            NotificationRuleRepository notificationRuleRepository,
            NotificationRuleMapper notificationRuleMapper,
            NotificationTypePropertyRepository notificationTypePropertyRepository,
            NotificationTypePropertyMapper notificationTypePropertyMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.notificationCohortSettingRepository = notificationCohortSettingRepository;
        this.notificationCohortSettingMapper = notificationCohortSettingMapper;
        this.notificationRuleRepository = notificationRuleRepository;
        this.notificationRuleMapper = notificationRuleMapper;
        this.notificationTypePropertyRepository = notificationTypePropertyRepository;
        this.notificationTypePropertyMapper = notificationTypePropertyMapper;
    }

    public NotificationType getNotificationType(UUID id) {
        return doInGlobalReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.global(), id));
        });
    }

    public Page<NotificationType> listNotificationTypes(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, NotificationTypeRepository.SORT_FIELDS,
                        NotificationTypeRepository.DEFAULT_ORDER,
                NotificationTypeRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, NotificationTypeRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInGlobalReadTransaction(() -> {
            Page<NotificationTypeEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public NotificationType createNotificationType(NotificationType request, boolean validateOnly) {
        validator.validate(request);
        NotificationTypeEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        DataScope scope = DataScope.global();
        return doInGlobalWriteTransaction(() -> {
            try (CallContextHolder.Scope global = CallContextHolder.withoutTenant()) {
                assertValid(entity, true);
                scope.requireWritable(institutionOf(entity));
                assertUnique(entity);
                if (validateOnly) {
                    return mapper.toDto(entity);
                }
                NotificationTypeEntity stored = repository.insert(entity);
                audit.recordCreate(AGGREGATE, stored.getId());
                outbox.publish(AGGREGATE + CommsEvents.CREATED, AGGREGATE, stored.getId(),
                        eventPayload(stored, stored.getRowVersion()));
                return mapper.toDto(stored);

            }
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public NotificationType updateNotificationType(UUID id, String ifMatch, NotificationTypeUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInGlobalWriteTransaction(() -> {
            try (CallContextHolder.Scope global = CallContextHolder.withoutTenant()) {
                DataScope scope = DataScope.global();
                NotificationTypeEntity entity = loadVisible(scope, id);
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
                outbox.publish(AGGREGATE + CommsEvents.UPDATED, AGGREGATE, id, eventPayload(entity, expected + 1));
                return mapper.toDto(entity);

            }
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteNotificationType(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInGlobalWriteTransaction(() -> {
            try (CallContextHolder.Scope global = CallContextHolder.withoutTenant()) {
                DataScope scope = DataScope.global();
                NotificationTypeEntity entity = loadVisible(scope, id);
                scope.requireWritable(institutionOf(entity));
                if (entity.getRowVersion() != expected) {
                    throw new PreconditionFailedException("If-Match does not match the current ETag.");
                }
                if (repository.referencedByNotificationTypePropertyNotificationTypeId(id)) {
                    throw new InvalidStateException(
                            "The notification type is still referenced by comms.notification_type_property rows; remove "
                                    + "those first.");
                }
                entity.onDeletingData(Actor.current(), clock);
                if (repository.softDelete(entity, expected) == 0) {
                    throw new PreconditionFailedException("If-Match does not match the current ETag.");
                }
                audit.recordDelete(AGGREGATE, id);
                outbox.publish(AGGREGATE + CommsEvents.DELETED, AGGREGATE, id, eventPayload(entity, expected + 1));
            }
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private NotificationTypeEntity loadVisible(DataScope scope, UUID id) {
        NotificationTypeEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOf(NotificationTypeEntity entity) {
        return Optional.empty();
    }

    /**
     * Applies the selected NotificationTypeUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void apply(NotificationTypeEntity entity, NotificationTypeUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "code" -> entity.setCode(changes.getCode());
                case "description" -> entity.setDescription(changes.getDescription());
                case "moduleCode" -> entity.setModuleCode(changes.getModuleCode());
                case "roleCode" -> entity.setRoleCode(changes.getRoleCode());
                case "isEnabled" -> entity.setIsEnabled(changes.getIsEnabled());
                case "popupEnabled" -> entity.setPopupEnabled(changes.getPopupEnabled());
                case "teacherPopupEnabled" -> entity.setTeacherPopupEnabled(changes.getTeacherPopupEnabled());
                case "validParameters" -> entity.setValidParameters(changes.getValidParameters());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(NotificationTypeEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
        }
        if (entity.getDescription() == null) {
            errors.add(new FieldError("description", "must not be null", "NotNull"));
        }
        if (entity.getModuleCode() == null) {
            errors.add(new FieldError("moduleCode", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsEnabled() == null) {
            errors.add(new FieldError("isEnabled", "must not be null", "NotNull"));
        }
        if (!creating && entity.getPopupEnabled() == null) {
            errors.add(new FieldError("popupEnabled", "must not be null", "NotNull"));
        }
        if (!creating && entity.getTeacherPopupEnabled() == null) {
            errors.add(new FieldError("teacherPopupEnabled", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(NotificationTypeEntity entity) {
        if (repository.takenCodeCode(entity)) {
            throw new DuplicateResourceException("A notification type with the same code already exists.");
        }
    }

    /** The event of a change to a global row: its id and the row version the change produced. */
    private static Map<String, Object> eventPayload(NotificationTypeEntity e, int version) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", e.getId().toString());
        payload.put("rowVersion", version);
        return payload;
    }

    // ---- NotificationCohortSetting: a record of the NotificationType aggregate ----

    public NotificationCohortSetting getNotificationCohortSetting(UUID notificationTypeId, UUID id) {
        return doInReadTransaction(() -> {
            return notificationCohortSettingMapper.toDto(loadVisibleNotificationCohortSetting(DataScope.current(
                    NOTIFICATION_COHORT_SETTING_PERMISSION_GET), notificationTypeId, id));
        });
    }

    public Page<NotificationCohortSetting> listNotificationCohortSettings(UUID notificationTypeId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, NotificationCohortSettingRepository.SORT_FIELDS,
                        NotificationCohortSettingRepository.DEFAULT_ORDER,
                NotificationCohortSettingRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, NotificationCohortSettingRepository.FILTER_FIELDS)
                .and(NotificationCohortSettingRepository.ofParent(notificationTypeId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + notificationTypeId, clock);
        return doInReadTransaction(() -> {
            Page<NotificationCohortSettingEntity> rows = notificationCohortSettingRepository.list(page, where);
            return rows.map(notificationCohortSettingMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public NotificationCohortSetting createNotificationCohortSetting(UUID notificationTypeId,
            NotificationCohortSetting request, boolean validateOnly) {
        validator.validate(request);
        NotificationCohortSettingEntity entity = notificationCohortSettingMapper.toEntity(request);
        entity.setNotificationTypeId(notificationTypeId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(NOTIFICATION_COHORT_SETTING_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!notificationCohortSettingRepository.existsNotificationTypeId(entity.getNotificationTypeId())) {
                throw new ResourceNotFoundException("comms.notification_type", entity.getNotificationTypeId());
            }
            assertValidNotificationCohortSetting(entity, true);
            scope.requireWritable(institutionOfNotificationCohortSetting(entity));
            assertUniqueNotificationCohortSetting(entity);
            if (validateOnly) {
                return notificationCohortSettingMapper.toDto(entity);
            }
            NotificationCohortSettingEntity stored = notificationCohortSettingRepository.insert(entity);
            audit.recordCreate(NOTIFICATION_COHORT_SETTING_AGGREGATE, stored.getId());
            outbox.publish(NOTIFICATION_COHORT_SETTING_AGGREGATE + CommsEvents.CREATED,
                    NOTIFICATION_COHORT_SETTING_AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return notificationCohortSettingMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public NotificationCohortSetting updateNotificationCohortSetting(UUID notificationTypeId, UUID id, String ifMatch,
            NotificationCohortSettingUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, NOTIFICATION_COHORT_SETTING_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(NOTIFICATION_COHORT_SETTING_PERMISSION_UPDATE);
            NotificationCohortSettingEntity entity = loadVisibleNotificationCohortSetting(scope, notificationTypeId, id);
            scope.requireWritable(institutionOfNotificationCohortSetting(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyNotificationCohortSetting(entity, changes, fields);
            validator.validate(notificationCohortSettingMapper.toDto(entity));
            assertValidNotificationCohortSetting(entity, false);
            scope.requireWritable(institutionOfNotificationCohortSetting(entity));
            assertUniqueNotificationCohortSetting(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return notificationCohortSettingMapper.toDto(entity);
            }
            if (notificationCohortSettingRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(NOTIFICATION_COHORT_SETTING_AGGREGATE, id, fields);
            outbox.publish(NOTIFICATION_COHORT_SETTING_AGGREGATE + CommsEvents.UPDATED,
                    NOTIFICATION_COHORT_SETTING_AGGREGATE, id, CommsEvents.payload(id));
            return notificationCohortSettingMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteNotificationCohortSetting(UUID notificationTypeId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(NOTIFICATION_COHORT_SETTING_PERMISSION_DELETE);
            NotificationCohortSettingEntity entity = loadVisibleNotificationCohortSetting(scope, notificationTypeId, id);
            scope.requireWritable(institutionOfNotificationCohortSetting(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (notificationCohortSettingRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(NOTIFICATION_COHORT_SETTING_AGGREGATE, id);
            outbox.publish(NOTIFICATION_COHORT_SETTING_AGGREGATE + CommsEvents.DELETED,
                    NOTIFICATION_COHORT_SETTING_AGGREGATE, id, CommsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private NotificationCohortSettingEntity loadVisibleNotificationCohortSetting(DataScope scope, UUID notificationTypeId,
            UUID id) {
        NotificationCohortSettingEntity entity = notificationCohortSettingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(NOTIFICATION_COHORT_SETTING_AGGREGATE, id));
        if (!scope.canRead(institutionOfNotificationCohortSetting(entity))
                || !notificationTypeId.equals(entity.getNotificationTypeId())) {
            throw new ResourceNotFoundException(NOTIFICATION_COHORT_SETTING_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOfNotificationCohortSetting(NotificationCohortSettingEntity entity) {
        return Optional.empty();
    }

    /**
     * Applies the selected NotificationCohortSettingUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyNotificationCohortSetting(NotificationCohortSettingEntity entity,
            NotificationCohortSettingUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "cohortId" -> entity.setCohortId(changes.getCohortId());
                case "popupEnabled" -> entity.setPopupEnabled(changes.getPopupEnabled());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidNotificationCohortSetting(NotificationCohortSettingEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCohortId() == null) {
            errors.add(new FieldError("cohortId", "must not be null", "NotNull"));
        }
        if (entity.getNotificationTypeId() == null) {
            errors.add(new FieldError("notificationTypeId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getPopupEnabled() == null) {
            errors.add(new FieldError("popupEnabled", "must not be null", "NotNull"));
        }
        if (entity.getNotificationTypeId() != null
                && !notificationCohortSettingRepository.existsNotificationTypeId(entity.getNotificationTypeId())) {
            errors.add(new FieldError("notificationTypeId", "notification type does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueNotificationCohortSetting(NotificationCohortSettingEntity entity) {
        if (notificationCohortSettingRepository.takenCohortIdNotificationTypeId1(entity)) {
            throw new DuplicateResourceException(
                    "A notification cohort setting with the same cohort id and notification type id already exists.");
        }
    }

    // ---- NotificationRule: a record of the NotificationType aggregate ----

    public NotificationRule getNotificationRule(UUID notificationTypeId, UUID id) {
        return doInReadTransaction(() -> {
            return notificationRuleMapper.toDto(loadVisibleNotificationRule(DataScope.current(
                    NOTIFICATION_RULE_PERMISSION_GET), notificationTypeId, id));
        });
    }

    public Page<NotificationRule> listNotificationRules(UUID notificationTypeId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, NotificationRuleRepository.SORT_FIELDS,
                        NotificationRuleRepository.DEFAULT_ORDER,
                NotificationRuleRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, NotificationRuleRepository.FILTER_FIELDS)
                .and(NotificationRuleRepository.ofParent(notificationTypeId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + notificationTypeId, clock);
        return doInReadTransaction(() -> {
            Page<NotificationRuleEntity> rows = notificationRuleRepository.list(page, where);
            return rows.map(notificationRuleMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public NotificationRule createNotificationRule(UUID notificationTypeId, NotificationRule request,
            boolean validateOnly) {
        validator.validate(request);
        NotificationRuleEntity entity = notificationRuleMapper.toEntity(request);
        entity.setNotificationTypeId(notificationTypeId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(NOTIFICATION_RULE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!notificationRuleRepository.existsNotificationTypeId(entity.getNotificationTypeId())) {
                throw new ResourceNotFoundException("comms.notification_type", entity.getNotificationTypeId());
            }
            assertValidNotificationRule(entity, true);
            scope.requireWritable(institutionOfNotificationRule(entity));
            assertUniqueNotificationRule(entity);
            if (validateOnly) {
                return notificationRuleMapper.toDto(entity);
            }
            NotificationRuleEntity stored = notificationRuleRepository.insert(entity);
            audit.recordCreate(NOTIFICATION_RULE_AGGREGATE, stored.getId());
            outbox.publish(NOTIFICATION_RULE_AGGREGATE + CommsEvents.CREATED, NOTIFICATION_RULE_AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return notificationRuleMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public NotificationRule updateNotificationRule(UUID notificationTypeId, UUID id, String ifMatch,
            NotificationRuleUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, NOTIFICATION_RULE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(NOTIFICATION_RULE_PERMISSION_UPDATE);
            NotificationRuleEntity entity = loadVisibleNotificationRule(scope, notificationTypeId, id);
            scope.requireWritable(institutionOfNotificationRule(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyNotificationRule(entity, changes, fields);
            validator.validate(notificationRuleMapper.toDto(entity));
            assertValidNotificationRule(entity, false);
            scope.requireWritable(institutionOfNotificationRule(entity));
            assertUniqueNotificationRule(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return notificationRuleMapper.toDto(entity);
            }
            if (notificationRuleRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(NOTIFICATION_RULE_AGGREGATE, id, fields);
            outbox.publish(NOTIFICATION_RULE_AGGREGATE + CommsEvents.UPDATED, NOTIFICATION_RULE_AGGREGATE, id,
                    CommsEvents.payload(id));
            return notificationRuleMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteNotificationRule(UUID notificationTypeId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(NOTIFICATION_RULE_PERMISSION_DELETE);
            NotificationRuleEntity entity = loadVisibleNotificationRule(scope, notificationTypeId, id);
            scope.requireWritable(institutionOfNotificationRule(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (notificationRuleRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(NOTIFICATION_RULE_AGGREGATE, id);
            outbox.publish(NOTIFICATION_RULE_AGGREGATE + CommsEvents.DELETED, NOTIFICATION_RULE_AGGREGATE, id,
                    CommsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private NotificationRuleEntity loadVisibleNotificationRule(DataScope scope, UUID notificationTypeId, UUID id) {
        NotificationRuleEntity entity = notificationRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(NOTIFICATION_RULE_AGGREGATE, id));
        if (!scope.canRead(institutionOfNotificationRule(entity)) || !notificationTypeId.equals(entity.getNotificationTypeId())) {
            throw new ResourceNotFoundException(NOTIFICATION_RULE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOfNotificationRule(NotificationRuleEntity entity) {
        return Optional.empty();
    }

    /**
     * Applies the selected NotificationRuleUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyNotificationRule(NotificationRuleEntity entity, NotificationRuleUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "roleId" -> entity.setRoleId(changes.getRoleId());
                case "permissionId" -> entity.setPermissionId(changes.getPermissionId());
                case "isEnabled" -> entity.setIsEnabled(changes.getIsEnabled());
                case "emailSubjectTemplate" -> entity.setEmailSubjectTemplate(changes.getEmailSubjectTemplate());
                case "emailBodyTemplate" -> entity.setEmailBodyTemplate(changes.getEmailBodyTemplate());
                case "smsTemplate" -> entity.setSmsTemplate(changes.getSmsTemplate());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidNotificationRule(NotificationRuleEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getRoleId() == null) {
            errors.add(new FieldError("roleId", "must not be null", "NotNull"));
        }
        if (entity.getNotificationTypeId() == null) {
            errors.add(new FieldError("notificationTypeId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsEnabled() == null) {
            errors.add(new FieldError("isEnabled", "must not be null", "NotNull"));
        }
        if (entity.getNotificationTypeId() != null
                && !notificationRuleRepository.existsNotificationTypeId(entity.getNotificationTypeId())) {
            errors.add(new FieldError("notificationTypeId", "notification type does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueNotificationRule(NotificationRuleEntity entity) {

    }

    // ---- NotificationTypeProperty: a record of the NotificationType aggregate ----

    public NotificationTypeProperty getNotificationTypeProperty(UUID notificationTypeId, UUID id) {
        return doInGlobalReadTransaction(() -> {
            return notificationTypePropertyMapper.toDto(loadVisibleNotificationTypeProperty(DataScope.global(),
                    notificationTypeId, id));
        });
    }

    public Page<NotificationTypeProperty> listNotificationTypePropertys(UUID notificationTypeId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, NotificationTypePropertyRepository.SORT_FIELDS,
                        NotificationTypePropertyRepository.DEFAULT_ORDER,
                NotificationTypePropertyRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, NotificationTypePropertyRepository.FILTER_FIELDS)
                .and(NotificationTypePropertyRepository.ofParent(notificationTypeId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + notificationTypeId, clock);
        return doInGlobalReadTransaction(() -> {
            Page<NotificationTypePropertyEntity> rows = notificationTypePropertyRepository.list(page, where);
            return rows.map(notificationTypePropertyMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public NotificationTypeProperty createNotificationTypeProperty(UUID notificationTypeId,
            NotificationTypeProperty request, boolean validateOnly) {
        validator.validate(request);
        NotificationTypePropertyEntity entity = notificationTypePropertyMapper.toEntity(request);
        entity.setNotificationTypeId(notificationTypeId);
        entity.onCreatingData(Actor.current(), clock);
        DataScope scope = DataScope.global();
        return doInGlobalWriteTransaction(() -> {
            try (CallContextHolder.Scope global = CallContextHolder.withoutTenant()) {
                if (!notificationTypePropertyRepository.existsNotificationTypeId(entity.getNotificationTypeId())) {
                    throw new ResourceNotFoundException("comms.notification_type", entity.getNotificationTypeId());
                }
                assertValidNotificationTypeProperty(entity, true);
                scope.requireWritable(institutionOfNotificationTypeProperty(entity));
                assertUniqueNotificationTypeProperty(entity);
                if (validateOnly) {
                    return notificationTypePropertyMapper.toDto(entity);
                }
                NotificationTypePropertyEntity stored = notificationTypePropertyRepository.insert(entity);
                audit.recordCreate(NOTIFICATION_TYPE_PROPERTY_AGGREGATE, stored.getId());
                outbox.publish(NOTIFICATION_TYPE_PROPERTY_AGGREGATE + CommsEvents.CREATED,
                        NOTIFICATION_TYPE_PROPERTY_AGGREGATE, stored.getId(),
                        eventPayloadNotificationTypeProperty(stored, stored.getRowVersion()));
                return notificationTypePropertyMapper.toDto(stored);

            }
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public NotificationTypeProperty updateNotificationTypeProperty(UUID notificationTypeId, UUID id, String ifMatch,
            NotificationTypePropertyUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, NOTIFICATION_TYPE_PROPERTY_WRITABLE);
        return doInGlobalWriteTransaction(() -> {
            try (CallContextHolder.Scope global = CallContextHolder.withoutTenant()) {
                DataScope scope = DataScope.global();
                NotificationTypePropertyEntity entity = loadVisibleNotificationTypeProperty(scope, notificationTypeId, id);
                scope.requireWritable(institutionOfNotificationTypeProperty(entity));
                if (entity.getRowVersion() != expected) {
                    throw new PreconditionFailedException("If-Match does not match the current ETag.");
                }
                applyNotificationTypeProperty(entity, changes, fields);
                validator.validate(notificationTypePropertyMapper.toDto(entity));
                assertValidNotificationTypeProperty(entity, false);
                scope.requireWritable(institutionOfNotificationTypeProperty(entity));
                assertUniqueNotificationTypeProperty(entity);
                entity.onUpdatingData(Actor.current(), clock);
                if (validateOnly) {
                    return notificationTypePropertyMapper.toDto(entity);
                }
                if (notificationTypePropertyRepository.update(entity, expected) == 0) {
                    throw new PreconditionFailedException("If-Match does not match the current ETag.");
                }
                entity.setRowVersion(expected + 1);
                audit.recordUpdate(NOTIFICATION_TYPE_PROPERTY_AGGREGATE, id, fields);
                outbox.publish(NOTIFICATION_TYPE_PROPERTY_AGGREGATE + CommsEvents.UPDATED,
                        NOTIFICATION_TYPE_PROPERTY_AGGREGATE, id, eventPayloadNotificationTypeProperty(entity, expected + 1));
                return notificationTypePropertyMapper.toDto(entity);

            }
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteNotificationTypeProperty(UUID notificationTypeId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInGlobalWriteTransaction(() -> {
            try (CallContextHolder.Scope global = CallContextHolder.withoutTenant()) {
                DataScope scope = DataScope.global();
                NotificationTypePropertyEntity entity = loadVisibleNotificationTypeProperty(scope, notificationTypeId, id);
                scope.requireWritable(institutionOfNotificationTypeProperty(entity));
                if (entity.getRowVersion() != expected) {
                    throw new PreconditionFailedException("If-Match does not match the current ETag.");
                }
                entity.onDeletingData(Actor.current(), clock);
                if (notificationTypePropertyRepository.softDelete(entity, expected) == 0) {
                    throw new PreconditionFailedException("If-Match does not match the current ETag.");
                }
                audit.recordDelete(NOTIFICATION_TYPE_PROPERTY_AGGREGATE, id);
                outbox.publish(NOTIFICATION_TYPE_PROPERTY_AGGREGATE + CommsEvents.DELETED,
                        NOTIFICATION_TYPE_PROPERTY_AGGREGATE, id, eventPayloadNotificationTypeProperty(entity, expected + 1));
            }
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private NotificationTypePropertyEntity loadVisibleNotificationTypeProperty(DataScope scope, UUID notificationTypeId,
            UUID id) {
        NotificationTypePropertyEntity entity = notificationTypePropertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(NOTIFICATION_TYPE_PROPERTY_AGGREGATE, id));
        if (!scope.canRead(institutionOfNotificationTypeProperty(entity))
                || !notificationTypeId.equals(entity.getNotificationTypeId())) {
            throw new ResourceNotFoundException(NOTIFICATION_TYPE_PROPERTY_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOfNotificationTypeProperty(NotificationTypePropertyEntity entity) {
        return Optional.empty();
    }

    /**
     * Applies the selected NotificationTypePropertyUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyNotificationTypeProperty(NotificationTypePropertyEntity entity,
            NotificationTypePropertyUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "moduleCode" -> entity.setModuleCode(changes.getModuleCode());
                case "propertyValue" -> entity.setPropertyValue(changes.getPropertyValue());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidNotificationTypeProperty(NotificationTypePropertyEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getNotificationTypeId() == null) {
            errors.add(new FieldError("notificationTypeId", "must not be null", "NotNull"));
        }
        if (entity.getPropertyValue() == null) {
            errors.add(new FieldError("propertyValue", "must not be null", "NotNull"));
        }
        if (entity.getNotificationTypeId() != null
                && !notificationTypePropertyRepository.existsNotificationTypeId(entity.getNotificationTypeId())) {
            errors.add(new FieldError("notificationTypeId", "notification type does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueNotificationTypeProperty(NotificationTypePropertyEntity entity) {

    }

    /** The event of a change to a global row: its id and the row version the change produced. */
    private static Map<String, Object> eventPayloadNotificationTypeProperty(NotificationTypePropertyEntity e, int version) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", e.getId().toString());
        payload.put("rowVersion", version);
        return payload;
    }
}
