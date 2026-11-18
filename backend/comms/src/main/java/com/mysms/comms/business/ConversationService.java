package com.mysms.comms.business;

import com.mysms.authclient.DataScope;
import com.mysms.comms.api.model.Conversation;
import com.mysms.comms.api.model.ConversationMessage;
import com.mysms.comms.api.model.ConversationMessageUpdate;
import com.mysms.comms.api.model.ConversationParticipant;
import com.mysms.comms.api.model.ConversationParticipantUpdate;
import com.mysms.comms.api.model.ConversationUpdate;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.config.CommsEventsConfig;
import com.mysms.comms.domain.ConversationEntity;
import com.mysms.comms.domain.ConversationMessageEntity;
import com.mysms.comms.domain.ConversationParticipantEntity;
import com.mysms.comms.events.CommsEvents;
import com.mysms.comms.mapper.ConversationMapper;
import com.mysms.comms.mapper.ConversationMessageMapper;
import com.mysms.comms.mapper.ConversationParticipantMapper;
import com.mysms.comms.repository.ConversationMessageRepository;
import com.mysms.comms.repository.ConversationParticipantRepository;
import com.mysms.comms.repository.ConversationRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of comms.conversation and of the records that exist only as part of a Conversation
 * (ConversationMessage, ConversationParticipant): audit and domain event commit in the same transaction as every
 * change. Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide
 * grant.
 */
@Service
public class ConversationService extends AppTransactionManager {

    public static final String AGGREGATE = ConversationEntity.TABLE;
    static final String PERMISSION_LIST = "comms.conversation.read";
    static final String PERMISSION_GET = "comms.conversation.read";
    static final String PERMISSION_CREATE = "comms.conversation.create";
    static final String PERMISSION_UPDATE = "comms.conversation.update";
    static final String PERMISSION_DELETE = "comms.conversation.delete";
    /** The contract's ConversationUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "conversationType", "title", "lastMessageAt", "isActive");

    public static final String CONVERSATION_MESSAGE_AGGREGATE = ConversationMessageEntity.TABLE;
    static final String CONVERSATION_MESSAGE_PERMISSION_LIST = "comms.conversation_message.read";
    static final String CONVERSATION_MESSAGE_PERMISSION_GET = "comms.conversation_message.read";
    static final String CONVERSATION_MESSAGE_PERMISSION_CREATE = "comms.conversation_message.create";
    static final String CONVERSATION_MESSAGE_PERMISSION_UPDATE = "comms.conversation_message.update";
    static final String CONVERSATION_MESSAGE_PERMISSION_DELETE = "comms.conversation_message.delete";
    /** The contract's ConversationMessageUpdate fields, in apply order. */
    static final List<String> CONVERSATION_MESSAGE_WRITABLE = List.of("senderUserId", "body", "deletedBySender");

    public static final String CONVERSATION_PARTICIPANT_AGGREGATE = ConversationParticipantEntity.TABLE;
    static final String CONVERSATION_PARTICIPANT_PERMISSION_LIST = "comms.conversation_participant.read";
    static final String CONVERSATION_PARTICIPANT_PERMISSION_GET = "comms.conversation_participant.read";
    static final String CONVERSATION_PARTICIPANT_PERMISSION_CREATE = "comms.conversation_participant.create";
    static final String CONVERSATION_PARTICIPANT_PERMISSION_UPDATE = "comms.conversation_participant.update";
    static final String CONVERSATION_PARTICIPANT_PERMISSION_DELETE = "comms.conversation_participant.delete";
    /** The contract's ConversationParticipantUpdate fields, in apply order. */
    static final List<String> CONVERSATION_PARTICIPANT_WRITABLE = List.of("userId", "lastReadAt");

    private final ConversationRepository repository;
    private final ConversationMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final ConversationMessageRepository conversationMessageRepository;
    private final ConversationMessageMapper conversationMessageMapper;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final ConversationParticipantMapper conversationParticipantMapper;

    public ConversationService(ConversationRepository repository,
            ConversationMapper mapper,
            @Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CommsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CommsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(CommsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            ConversationMessageRepository conversationMessageRepository,
            ConversationMessageMapper conversationMessageMapper,
            ConversationParticipantRepository conversationParticipantRepository,
            ConversationParticipantMapper conversationParticipantMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.conversationMessageRepository = conversationMessageRepository;
        this.conversationMessageMapper = conversationMessageMapper;
        this.conversationParticipantRepository = conversationParticipantRepository;
        this.conversationParticipantMapper = conversationParticipantMapper;
    }

    public Conversation getConversation(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Conversation> listConversations(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ConversationRepository.SORT_FIELDS, ConversationRepository.DEFAULT_ORDER,
                ConversationRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ConversationRepository.FILTER_FIELDS)
                .and(ConversationRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ConversationEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Conversation createConversation(Conversation request, boolean validateOnly) {
        validator.validate(request);
        ConversationEntity entity = mapper.toEntity(request);
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
            ConversationEntity stored = repository.insert(entity);
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
    public Conversation updateConversation(UUID id, String ifMatch, ConversationUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ConversationEntity entity = loadVisible(scope, id);
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
    public void deleteConversation(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            ConversationEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByConversationMessageConversationId(id)) {
                throw new InvalidStateException(
                        "The conversation is still referenced by comms.conversation_message rows; remove those first.");
            }
            if (repository.referencedByConversationParticipantConversationId(id)) {
                throw new InvalidStateException(
                        "The conversation is still referenced by comms.conversation_participant rows; remove those "
                                + "first.");
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
    private ConversationEntity loadVisible(DataScope scope, UUID id) {
        ConversationEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(ConversationEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected ConversationUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(ConversationEntity entity, ConversationUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "conversationType" -> entity.setConversationType(
                        Optional.ofNullable(changes.getConversationType()).map(v -> v.getValue()).orElse(null));
                case "title" -> entity.setTitle(changes.getTitle());
                case "lastMessageAt" -> entity.setLastMessageAt(changes.getLastMessageAt());
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ConversationEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getConversationType() == null) {
            errors.add(new FieldError("conversationType", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ConversationEntity entity) {

    }

    // ---- ConversationMessage ----

    public ConversationMessage getConversationMessage(UUID conversationId, UUID id) {
        return doInReadTransaction(() -> {
            return conversationMessageMapper.toDto(loadVisibleConversationMessage(DataScope.current(
                    CONVERSATION_MESSAGE_PERMISSION_GET), conversationId, id));
        });
    }

    public Page<ConversationMessage> listConversationMessages(UUID conversationId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ConversationMessageRepository.SORT_FIELDS,
                        ConversationMessageRepository.DEFAULT_ORDER,
                ConversationMessageRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ConversationMessageRepository.FILTER_FIELDS)
                .and(ConversationMessageRepository.ofParent(conversationId))
                        .and(ConversationMessageRepository.scope(DataScope.current(CONVERSATION_MESSAGE_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + conversationId, clock);
        return doInReadTransaction(() -> {
            Page<ConversationMessageEntity> rows = conversationMessageRepository.list(page, where);
            return rows.map(conversationMessageMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ConversationMessage createConversationMessage(UUID conversationId, ConversationMessage request,
            boolean validateOnly) {
        validator.validate(request);
        ConversationMessageEntity entity = conversationMessageMapper.toEntity(request);
        entity.setConversationId(conversationId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(CONVERSATION_MESSAGE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!conversationMessageRepository.existsConversationId(entity.getConversationId())) {
                throw new ResourceNotFoundException("comms.conversation", entity.getConversationId());
            }
            if (!scope.canRead(institutionOfConversationMessage(entity))) {
                throw new ResourceNotFoundException("comms.conversation", entity.getConversationId());
            }
            assertValidConversationMessage(entity, true);
            scope.requireWritable(institutionOfConversationMessage(entity));
            assertUniqueConversationMessage(entity);
            if (validateOnly) {
                return conversationMessageMapper.toDto(entity);
            }
            ConversationMessageEntity stored = conversationMessageRepository.insert(entity);
            audit.recordCreate(CONVERSATION_MESSAGE_AGGREGATE, stored.getId());
            outbox.publish(CONVERSATION_MESSAGE_AGGREGATE + CommsEvents.CREATED, CONVERSATION_MESSAGE_AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return conversationMessageMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public ConversationMessage updateConversationMessage(UUID conversationId, UUID id, String ifMatch,
            ConversationMessageUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, CONVERSATION_MESSAGE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CONVERSATION_MESSAGE_PERMISSION_UPDATE);
            ConversationMessageEntity entity = loadVisibleConversationMessage(scope, conversationId, id);
            scope.requireWritable(institutionOfConversationMessage(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyConversationMessage(entity, changes, fields);
            validator.validate(conversationMessageMapper.toDto(entity));
            assertValidConversationMessage(entity, false);
            scope.requireWritable(institutionOfConversationMessage(entity));
            assertUniqueConversationMessage(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return conversationMessageMapper.toDto(entity);
            }
            if (conversationMessageRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(CONVERSATION_MESSAGE_AGGREGATE, id, fields);
            outbox.publish(CONVERSATION_MESSAGE_AGGREGATE + CommsEvents.UPDATED, CONVERSATION_MESSAGE_AGGREGATE, id,
                    CommsEvents.payload(id));
            return conversationMessageMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteConversationMessage(UUID conversationId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CONVERSATION_MESSAGE_PERMISSION_DELETE);
            ConversationMessageEntity entity = loadVisibleConversationMessage(scope, conversationId, id);
            scope.requireWritable(institutionOfConversationMessage(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (conversationMessageRepository.referencedByMessageReceiptMessageId(id)) {
                throw new InvalidStateException(
                        "The conversation message is still referenced by comms.message_receipt rows; remove those "
                                + "first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (conversationMessageRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(CONVERSATION_MESSAGE_AGGREGATE, id);
            outbox.publish(CONVERSATION_MESSAGE_AGGREGATE + CommsEvents.DELETED, CONVERSATION_MESSAGE_AGGREGATE, id,
                    CommsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ConversationMessageEntity loadVisibleConversationMessage(DataScope scope, UUID conversationId, UUID id) {
        ConversationMessageEntity entity = conversationMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CONVERSATION_MESSAGE_AGGREGATE, id));
        if (!scope.canRead(institutionOfConversationMessage(entity)) || !conversationId.equals(entity.getConversationId())) {
            throw new ResourceNotFoundException(CONVERSATION_MESSAGE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the comms.conversation the row belongs to. */
    private Optional<UUID> institutionOfConversationMessage(ConversationMessageEntity entity) {
        return conversationMessageRepository.institutionOf(entity.getConversationId());
    }

    /** Applies the selected ConversationMessageUpdate fields; explicit per field so a mask can clear values. */
    private static void applyConversationMessage(ConversationMessageEntity entity, ConversationMessageUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "senderUserId" -> entity.setSenderUserId(changes.getSenderUserId());
                case "body" -> entity.setBody(changes.getBody());
                case "deletedBySender" -> entity.setDeletedBySender(changes.getDeletedBySender());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidConversationMessage(ConversationMessageEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getConversationId() == null) {
            errors.add(new FieldError("conversationId", "must not be null", "NotNull"));
        }
        if (entity.getSenderUserId() == null) {
            errors.add(new FieldError("senderUserId", "must not be null", "NotNull"));
        }
        if (entity.getBody() == null) {
            errors.add(new FieldError("body", "must not be null", "NotNull"));
        }
        if (!creating && entity.getDeletedBySender() == null) {
            errors.add(new FieldError("deletedBySender", "must not be null", "NotNull"));
        }
        if (entity.getConversationId() != null
                && !conversationMessageRepository.existsConversationId(entity.getConversationId())) {
            errors.add(new FieldError("conversationId", "conversation does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueConversationMessage(ConversationMessageEntity entity) {

    }

    // ---- ConversationParticipant ----

    public ConversationParticipant getConversationParticipant(UUID conversationId, UUID id) {
        return doInReadTransaction(() -> {
            return conversationParticipantMapper.toDto(loadVisibleConversationParticipant(DataScope.current(
                    CONVERSATION_PARTICIPANT_PERMISSION_GET), conversationId, id));
        });
    }

    public Page<ConversationParticipant> listConversationParticipants(UUID conversationId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ConversationParticipantRepository.SORT_FIELDS,
                        ConversationParticipantRepository.DEFAULT_ORDER,
                ConversationParticipantRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ConversationParticipantRepository.FILTER_FIELDS)
                .and(ConversationParticipantRepository.ofParent(conversationId))
                        .and(ConversationParticipantRepository.scope(DataScope.current(
                                CONVERSATION_PARTICIPANT_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + conversationId, clock);
        return doInReadTransaction(() -> {
            Page<ConversationParticipantEntity> rows = conversationParticipantRepository.list(page, where);
            return rows.map(conversationParticipantMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ConversationParticipant createConversationParticipant(UUID conversationId, ConversationParticipant request,
            boolean validateOnly) {
        validator.validate(request);
        ConversationParticipantEntity entity = conversationParticipantMapper.toEntity(request);
        entity.setConversationId(conversationId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(CONVERSATION_PARTICIPANT_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!conversationParticipantRepository.existsConversationId(entity.getConversationId())) {
                throw new ResourceNotFoundException("comms.conversation", entity.getConversationId());
            }
            if (!scope.canRead(institutionOfConversationParticipant(entity))) {
                throw new ResourceNotFoundException("comms.conversation", entity.getConversationId());
            }
            assertValidConversationParticipant(entity, true);
            scope.requireWritable(institutionOfConversationParticipant(entity));
            assertUniqueConversationParticipant(entity);
            if (validateOnly) {
                return conversationParticipantMapper.toDto(entity);
            }
            ConversationParticipantEntity stored = conversationParticipantRepository.insert(entity);
            audit.recordCreate(CONVERSATION_PARTICIPANT_AGGREGATE, stored.getId());
            outbox.publish(CONVERSATION_PARTICIPANT_AGGREGATE + CommsEvents.CREATED, CONVERSATION_PARTICIPANT_AGGREGATE,
                    stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return conversationParticipantMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public ConversationParticipant updateConversationParticipant(UUID conversationId, UUID id, String ifMatch,
            ConversationParticipantUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, CONVERSATION_PARTICIPANT_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CONVERSATION_PARTICIPANT_PERMISSION_UPDATE);
            ConversationParticipantEntity entity = loadVisibleConversationParticipant(scope, conversationId, id);
            scope.requireWritable(institutionOfConversationParticipant(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyConversationParticipant(entity, changes, fields);
            validator.validate(conversationParticipantMapper.toDto(entity));
            assertValidConversationParticipant(entity, false);
            scope.requireWritable(institutionOfConversationParticipant(entity));
            assertUniqueConversationParticipant(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return conversationParticipantMapper.toDto(entity);
            }
            if (conversationParticipantRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(CONVERSATION_PARTICIPANT_AGGREGATE, id, fields);
            outbox.publish(CONVERSATION_PARTICIPANT_AGGREGATE + CommsEvents.UPDATED, CONVERSATION_PARTICIPANT_AGGREGATE, id,
                    CommsEvents.payload(id));
            return conversationParticipantMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteConversationParticipant(UUID conversationId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CONVERSATION_PARTICIPANT_PERMISSION_DELETE);
            ConversationParticipantEntity entity = loadVisibleConversationParticipant(scope, conversationId, id);
            scope.requireWritable(institutionOfConversationParticipant(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (conversationParticipantRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(CONVERSATION_PARTICIPANT_AGGREGATE, id);
            outbox.publish(CONVERSATION_PARTICIPANT_AGGREGATE + CommsEvents.DELETED, CONVERSATION_PARTICIPANT_AGGREGATE, id,
                    CommsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ConversationParticipantEntity loadVisibleConversationParticipant(DataScope scope, UUID conversationId, UUID id) {
        ConversationParticipantEntity entity = conversationParticipantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CONVERSATION_PARTICIPANT_AGGREGATE, id));
        if (!scope.canRead(institutionOfConversationParticipant(entity)) || !conversationId.equals(entity.getConversationId())) {
            throw new ResourceNotFoundException(CONVERSATION_PARTICIPANT_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the comms.conversation the row belongs to. */
    private Optional<UUID> institutionOfConversationParticipant(ConversationParticipantEntity entity) {
        return conversationParticipantRepository.institutionOf(entity.getConversationId());
    }

    /** Applies the selected ConversationParticipantUpdate fields; explicit per field so a mask can clear values. */
    private static void applyConversationParticipant(ConversationParticipantEntity entity, ConversationParticipantUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "userId" -> entity.setUserId(changes.getUserId());
                case "lastReadAt" -> entity.setLastReadAt(changes.getLastReadAt());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidConversationParticipant(ConversationParticipantEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getConversationId() == null) {
            errors.add(new FieldError("conversationId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getConversationId() != null
                && !conversationParticipantRepository.existsConversationId(entity.getConversationId())) {
            errors.add(new FieldError("conversationId", "conversation does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueConversationParticipant(ConversationParticipantEntity entity) {
        if (conversationParticipantRepository.takenConversationIdUserId1(entity)) {
            throw new DuplicateResourceException(
                    "A conversation participant with the same conversation id and user id already exists.");
        }
    }
}
