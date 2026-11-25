package com.mysms.comms.business;

import com.mysms.authclient.DataScope;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.config.CommsEventsConfig;
import com.mysms.comms.domain.MessageTemplateEntity;
import com.mysms.comms.events.CommsEvents;
import com.mysms.comms.mapper.MessageTemplateMapper;
import com.mysms.comms.repository.MessageTemplateRepository;
import com.mysms.comms.api.model.MessageTemplate;
import com.mysms.comms.api.model.MessageTemplateUpdate;
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
 * Business service of comms.message_template: audit and domain event commit in the same transaction as every change.
 * Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class MessageTemplateService extends AppTransactionManager {

    public static final String AGGREGATE = MessageTemplateEntity.TABLE;
    static final String PERMISSION_LIST = "comms.message_template.read";
    static final String PERMISSION_GET = "comms.message_template.read";
    static final String PERMISSION_CREATE = "comms.message_template.create";
    static final String PERMISSION_UPDATE = "comms.message_template.update";
    static final String PERMISSION_DELETE = "comms.message_template.delete";
    /** The contract's MessageTemplateUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "channel", "code", "name", "subject", "body", "externalTemplateId", "isActive");


    private final MessageTemplateRepository repository;
    private final MessageTemplateMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public MessageTemplateService(MessageTemplateRepository repository, MessageTemplateMapper mapper,
            @Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CommsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CommsEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(CommsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public MessageTemplate getMessageTemplate(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<MessageTemplate> listMessageTemplates(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, MessageTemplateRepository.SORT_FIELDS, MessageTemplateRepository.DEFAULT_ORDER,
                MessageTemplateRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, MessageTemplateRepository.FILTER_FIELDS)
                .and(MessageTemplateRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<MessageTemplateEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public MessageTemplate createMessageTemplate(MessageTemplate request, boolean validateOnly) {
        validator.validate(request);
        MessageTemplateEntity entity = mapper.toEntity(request);
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
            MessageTemplateEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + CommsEvents.CREATED, AGGREGATE, stored.getId(), CommsEvents.template(
                    stored.getId(), stored.getInstitutionId(), Boolean.TRUE.equals(stored.getIsActive()),
                    stored.getRowVersion()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public MessageTemplate updateMessageTemplate(UUID id, String ifMatch, MessageTemplateUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            MessageTemplateEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + CommsEvents.UPDATED, AGGREGATE, id, CommsEvents.template(id,
                    entity.getInstitutionId(), Boolean.TRUE.equals(entity.getIsActive()), entity.getRowVersion()));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteMessageTemplate(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            MessageTemplateEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByOutboundMessageTemplateId(id)) {
                throw new InvalidStateException(
                        "The message template is still referenced by comms.outbound_message rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + CommsEvents.DELETED, AGGREGATE, id, CommsEvents.template(id,
                    entity.getInstitutionId(), false, expected + 1));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private MessageTemplateEntity loadVisible(DataScope scope, UUID id) {
        MessageTemplateEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(MessageTemplateEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected MessageTemplateUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(MessageTemplateEntity entity, MessageTemplateUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "channel" -> entity.setChannel(Optional.ofNullable(changes.getChannel()).map(v -> v.getValue())
                        .orElse(null));
                case "code" -> entity.setCode(changes.getCode());
                case "name" -> entity.setName(changes.getName());
                case "subject" -> entity.setSubject(changes.getSubject());
                case "body" -> entity.setBody(changes.getBody());
                case "externalTemplateId" -> entity.setExternalTemplateId(changes.getExternalTemplateId());
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(MessageTemplateEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getChannel() == null) {
            errors.add(new FieldError("channel", "must not be null", "NotNull"));
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

    private void assertUnique(MessageTemplateEntity entity) {

    }
}
