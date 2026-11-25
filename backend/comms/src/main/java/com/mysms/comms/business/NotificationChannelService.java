package com.mysms.comms.business;

import com.mysms.comms.api.model.SendTestMessageRequest;
import com.mysms.authclient.DataScope;
import com.mysms.comms.api.model.NotificationChannel;
import com.mysms.comms.api.model.NotificationChannelProperty;
import com.mysms.comms.api.model.NotificationChannelPropertyUpdate;
import com.mysms.comms.api.model.NotificationChannelUpdate;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.config.CommsEventsConfig;
import com.mysms.comms.domain.NotificationChannelEntity;
import com.mysms.comms.domain.NotificationChannelPropertyEntity;
import com.mysms.comms.events.CommsEvents;
import com.mysms.comms.mapper.NotificationChannelMapper;
import com.mysms.comms.mapper.NotificationChannelPropertyMapper;
import com.mysms.comms.repository.NotificationChannelPropertyRepository;
import com.mysms.comms.repository.NotificationChannelRepository;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.crypto.FieldCipher;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of comms.notification_channel and of the records that exist only as part of a NotificationChannel
 * (NotificationChannelProperty): audit and domain event commit in the same transaction as every change. Rows outside
 * the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class NotificationChannelService extends AppTransactionManager implements InitializingBean {

    public static final String AGGREGATE = NotificationChannelEntity.TABLE;
    static final String PERMISSION_LIST = "comms.notification_channel.read";
    static final String PERMISSION_GET = "comms.notification_channel.read";
    static final String PERMISSION_CREATE = "comms.notification_channel.create";
    static final String PERMISSION_UPDATE = "comms.notification_channel.update";
    static final String PERMISSION_DELETE = "comms.notification_channel.delete";
    static final String PERMISSION_SEND_TEST_MESSAGE = "comms.notification_channel.send_test_message";
    /** The text of a test message: nothing personal, nothing from the request. */
    static final String TEST_SUBJECT = "MySMS test message";
    static final String TEST_BODY = "This is a test message from your school's MySMS notification settings.";
    /** The contract's NotificationChannelUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("code", "description", "isEnabled", "sortOrder");

    public static final String NOTIFICATION_CHANNEL_PROPERTY_AGGREGATE = NotificationChannelPropertyEntity.TABLE;
    static final String NOTIFICATION_CHANNEL_PROPERTY_PERMISSION_LIST = "comms.notification_channel_property.read";
    static final String NOTIFICATION_CHANNEL_PROPERTY_PERMISSION_GET = "comms.notification_channel_property.read";
    static final String NOTIFICATION_CHANNEL_PROPERTY_PERMISSION_CREATE = "comms.notification_channel_property.create";
    static final String NOTIFICATION_CHANNEL_PROPERTY_PERMISSION_UPDATE = "comms.notification_channel_property.update";
    static final String NOTIFICATION_CHANNEL_PROPERTY_PERMISSION_DELETE = "comms.notification_channel_property.delete";
    /** The contract's NotificationChannelPropertyUpdate fields, in apply order. */
    static final List<String> NOTIFICATION_CHANNEL_PROPERTY_WRITABLE =
            List.of("propertyKey", "propertyValue", "secretValue", "sortOrder", "isMandatory");

    /** The write-only secrets: never in a response, so not validated on the merged model. */
    private static final Set<String> NOTIFICATION_CHANNEL_PROPERTY_SECRETS = Set.of("secretValue");
    private static final String NOTIFICATION_CHANNEL_PROPERTY_SECRET_SECRET_VALUE_ENC =
            "comms.notification_channel_property.secret_value_enc";

    private final NotificationChannelRepository repository;
    private final NotificationChannelMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final NotificationChannelPropertyRepository notificationChannelPropertyRepository;
    private final NotificationChannelPropertyMapper notificationChannelPropertyMapper;
    private final DeliveryChannels channels;
    private final RecipientDirectory directory;
    private final FieldCipher cipher;

    public NotificationChannelService(NotificationChannelRepository repository,
            NotificationChannelMapper mapper,
            @Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CommsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CommsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(CommsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            NotificationChannelPropertyRepository notificationChannelPropertyRepository,
            NotificationChannelPropertyMapper notificationChannelPropertyMapper,
            FieldCipher cipher, DeliveryChannels channels, RecipientDirectory directory) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.notificationChannelPropertyRepository = notificationChannelPropertyRepository;
        this.notificationChannelPropertyMapper = notificationChannelPropertyMapper;
        this.cipher = cipher;
        this.channels = channels;
        this.directory = directory;
    }

    /**
     * comms.SendTestMessage: sends a fixed test text through the channel, only to the caller's own verified e-mail
     * address or mobile number (422 for any other), and only for an administrator of the tenant's channel settings
     * (the permission held tenant-wide, 403 otherwise). A disabled channel is 409; a channel this deployment cannot
     * deliver is 503 NOT_CONFIGURED; a channel that is not e-mail, SMS or push has no test (422).
     */
    public void sendTestMessage(UUID id, SendTestMessageRequest request) {
        DataScope scope = DataScope.current(PERMISSION_SEND_TEST_MESSAGE);
        NotificationChannelEntity channel = doInReadTransaction(() -> {
            NotificationChannelEntity found = loadVisible(scope, id);
            scope.requireWritable(institutionOf(found));
            if (!Boolean.TRUE.equals(found.getIsEnabled())) {
                throw new InvalidStateException("The channel is disabled.");
            }
            return found;
        });
        String name = channel.getCode().substring(channel.getCode().lastIndexOf('.') + 1);
        if (!List.of(EmailChannel.NAME, "sms", "push").contains(name)) {
            throw new ValidationFailedException(List.of(new FieldError("notificationChannelId",
                    "names a channel that has no test message", "NOT_SUPPORTED")));
        }
        DeliveryChannel delivery = channels.require(name);
        String to = request.getTo().trim();
        boolean own = directory.ownVerified(name).filter(address -> address.equalsIgnoreCase(to)).isPresent();
        if (!own) {
            throw new ValidationFailedException(List.of(new FieldError("to",
                    "must be the caller's own verified address or number", "OWN_ADDRESS")));
        }
        delivery.deliver(to, TEST_SUBJECT, TEST_BODY);
        runInWriteTransaction(() -> audit.recordTransition(AGGREGATE, id, "send_test", List.of(), null));
    }

    public NotificationChannel getNotificationChannel(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<NotificationChannel> listNotificationChannels(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, NotificationChannelRepository.SORT_FIELDS,
                        NotificationChannelRepository.DEFAULT_ORDER,
                NotificationChannelRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, NotificationChannelRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<NotificationChannelEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public NotificationChannel createNotificationChannel(NotificationChannel request, boolean validateOnly) {
        validator.validate(request);
        NotificationChannelEntity entity = mapper.toEntity(request);
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
            NotificationChannelEntity stored = repository.insert(entity);
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
    public NotificationChannel updateNotificationChannel(UUID id, String ifMatch, NotificationChannelUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            NotificationChannelEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + CommsEvents.UPDATED, AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteNotificationChannel(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            NotificationChannelEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByNotificationChannelPropertyNotificationChannelId(id)) {
                throw new InvalidStateException(
                        "The notification channel is still referenced by comms.notification_channel_property rows; "
                                + "remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + CommsEvents.DELETED, AGGREGATE, id, CommsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private NotificationChannelEntity loadVisible(DataScope scope, UUID id) {
        NotificationChannelEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOf(NotificationChannelEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected NotificationChannelUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(NotificationChannelEntity entity, NotificationChannelUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "code" -> entity.setCode(changes.getCode());
                case "description" -> entity.setDescription(changes.getDescription());
                case "isEnabled" -> entity.setIsEnabled(changes.getIsEnabled());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(NotificationChannelEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsEnabled() == null) {
            errors.add(new FieldError("isEnabled", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(NotificationChannelEntity entity) {

    }

    // ---- NotificationChannelProperty ----

    /** Refuses to start without a master key: this resource stores secret columns. */
    @Override
    public void afterPropertiesSet() {
        cipher.requireConfigured();
    }

    public NotificationChannelProperty getNotificationChannelProperty(UUID notificationChannelId, UUID id) {
        return doInReadTransaction(() -> {
            return dtoNotificationChannelProperty(loadVisibleNotificationChannelProperty(DataScope.current(
                    NOTIFICATION_CHANNEL_PROPERTY_PERMISSION_GET), notificationChannelId, id));
        });
    }

    public Page<NotificationChannelProperty> listNotificationChannelPropertys(UUID notificationChannelId,
            Integer pageNumber, Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, NotificationChannelPropertyRepository.SORT_FIELDS,
                        NotificationChannelPropertyRepository.DEFAULT_ORDER,
                NotificationChannelPropertyRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, NotificationChannelPropertyRepository.FILTER_FIELDS)
                .and(NotificationChannelPropertyRepository.ofParent(notificationChannelId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + notificationChannelId, clock);
        return doInReadTransaction(() -> {
            Page<NotificationChannelPropertyEntity> rows = notificationChannelPropertyRepository.list(page, where);
            return rows.map(this::dtoNotificationChannelProperty);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public NotificationChannelProperty createNotificationChannelProperty(UUID notificationChannelId,
            NotificationChannelProperty request, boolean validateOnly) {
        validator.validate(request);
        NotificationChannelPropertyEntity entity = notificationChannelPropertyMapper.toEntity(request);
        entity.setNotificationChannelId(notificationChannelId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        sealSecretValueNotificationChannelProperty(entity, request.getSecretValue());
        DataScope scope = DataScope.current(NOTIFICATION_CHANNEL_PROPERTY_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!notificationChannelPropertyRepository.existsNotificationChannelId(entity.getNotificationChannelId())) {
                throw new ResourceNotFoundException("comms.notification_channel", entity.getNotificationChannelId());
            }
            assertValidNotificationChannelProperty(entity, true);
            scope.requireWritable(institutionOfNotificationChannelProperty(entity));
            assertUniqueNotificationChannelProperty(entity);
            if (validateOnly) {
                return dtoNotificationChannelProperty(entity);
            }
            NotificationChannelPropertyEntity stored = notificationChannelPropertyRepository.insert(entity);
            audit.recordCreate(NOTIFICATION_CHANNEL_PROPERTY_AGGREGATE, stored.getId());
            outbox.publish(NOTIFICATION_CHANNEL_PROPERTY_AGGREGATE + CommsEvents.CREATED,
                    NOTIFICATION_CHANNEL_PROPERTY_AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return dtoNotificationChannelProperty(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public NotificationChannelProperty updateNotificationChannelProperty(UUID notificationChannelId, UUID id,
            String ifMatch, NotificationChannelPropertyUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, NOTIFICATION_CHANNEL_PROPERTY_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(NOTIFICATION_CHANNEL_PROPERTY_PERMISSION_UPDATE);
            NotificationChannelPropertyEntity entity = loadVisibleNotificationChannelProperty(scope, notificationChannelId, id);
            scope.requireWritable(institutionOfNotificationChannelProperty(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyNotificationChannelProperty(entity, changes, fields);
            validator.validateExcept(dtoNotificationChannelProperty(entity), NOTIFICATION_CHANNEL_PROPERTY_SECRETS);
            assertValidNotificationChannelProperty(entity, false);
            scope.requireWritable(institutionOfNotificationChannelProperty(entity));
            assertUniqueNotificationChannelProperty(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return dtoNotificationChannelProperty(entity);
            }
            if (notificationChannelPropertyRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(NOTIFICATION_CHANNEL_PROPERTY_AGGREGATE, id, fields);
            outbox.publish(NOTIFICATION_CHANNEL_PROPERTY_AGGREGATE + CommsEvents.UPDATED,
                    NOTIFICATION_CHANNEL_PROPERTY_AGGREGATE, id, CommsEvents.payload(id));
            return dtoNotificationChannelProperty(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteNotificationChannelProperty(UUID notificationChannelId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(NOTIFICATION_CHANNEL_PROPERTY_PERMISSION_DELETE);
            NotificationChannelPropertyEntity entity = loadVisibleNotificationChannelProperty(scope, notificationChannelId, id);
            scope.requireWritable(institutionOfNotificationChannelProperty(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (notificationChannelPropertyRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(NOTIFICATION_CHANNEL_PROPERTY_AGGREGATE, id);
            outbox.publish(NOTIFICATION_CHANNEL_PROPERTY_AGGREGATE + CommsEvents.DELETED,
                    NOTIFICATION_CHANNEL_PROPERTY_AGGREGATE, id, CommsEvents.payload(id));
        });
    }

    /** Stores secretValue encrypted for the row's tenant and column; never the plaintext. */
    private void sealSecretValueNotificationChannelProperty(NotificationChannelPropertyEntity entity, String plaintext) {
        entity.setSecretValueEnc(cipher.encrypt(plaintext, entity.getTenantId(),
                NOTIFICATION_CHANNEL_PROPERTY_SECRET_SECRET_VALUE_ENC));
    }

    /** The response DTO with each secret's display form ("••••1234"); the plaintext itself is never returned. */
    private NotificationChannelProperty dtoNotificationChannelProperty(NotificationChannelPropertyEntity entity) {
        NotificationChannelProperty dto = notificationChannelPropertyMapper.toDto(entity);
        dto.setSecretValueMasked(
                cipher.maskStored(entity.getSecretValueEnc(), entity.getTenantId(),
                        NOTIFICATION_CHANNEL_PROPERTY_SECRET_SECRET_VALUE_ENC));
        return dto;
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private NotificationChannelPropertyEntity loadVisibleNotificationChannelProperty(DataScope scope,
            UUID notificationChannelId, UUID id) {
        NotificationChannelPropertyEntity entity = notificationChannelPropertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(NOTIFICATION_CHANNEL_PROPERTY_AGGREGATE, id));
        if (!scope.canRead(institutionOfNotificationChannelProperty(entity))
                || !notificationChannelId.equals(entity.getNotificationChannelId())) {
            throw new ResourceNotFoundException(NOTIFICATION_CHANNEL_PROPERTY_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfNotificationChannelProperty(NotificationChannelPropertyEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected NotificationChannelPropertyUpdate fields; explicit per field so a mask can clear values. */
    private void applyNotificationChannelProperty(NotificationChannelPropertyEntity entity,
            NotificationChannelPropertyUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "propertyKey" -> entity.setPropertyKey(changes.getPropertyKey());
                case "propertyValue" -> entity.setPropertyValue(changes.getPropertyValue());
                case "secretValue" -> sealSecretValueNotificationChannelProperty(entity, changes.getSecretValue());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                case "isMandatory" -> entity.setIsMandatory(changes.getIsMandatory());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidNotificationChannelProperty(NotificationChannelPropertyEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getNotificationChannelId() == null) {
            errors.add(new FieldError("notificationChannelId", "must not be null", "NotNull"));
        }
        if (entity.getPropertyKey() == null) {
            errors.add(new FieldError("propertyKey", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsMandatory() == null) {
            errors.add(new FieldError("isMandatory", "must not be null", "NotNull"));
        }
        if (entity.getNotificationChannelId() != null
                && !notificationChannelPropertyRepository.existsNotificationChannelId(entity.getNotificationChannelId())) {
            errors.add(new FieldError("notificationChannelId", "notification channel does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueNotificationChannelProperty(NotificationChannelPropertyEntity entity) {
        if (notificationChannelPropertyRepository.takenNotificationChannelIdPropertyKey1(entity)) {
            throw new DuplicateResourceException(
                    "A notification channel property with the same notification channel id and property key already "
                            + "exists.");
        }
    }
}
