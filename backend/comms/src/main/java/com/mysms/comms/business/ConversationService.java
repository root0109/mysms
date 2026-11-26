package com.mysms.comms.business;

import java.time.temporal.ChronoUnit;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import com.mysms.comms.domain.MessageReceiptEntity;
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
import com.mysms.comms.repository.MemberDirectory;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
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

    public static final String CONVERSATION_MESSAGE_AGGREGATE = ConversationMessageEntity.TABLE;
    static final String CONVERSATION_MESSAGE_PERMISSION_LIST = "comms.conversation_message.read";
    static final String PERMISSION_MARK_CONVERSATION_READ = "comms.conversation.mark_conversation_read";
    static final String PERMISSION_HIDE_MESSAGE_FOR_ME = "comms.conversation_message.hide_message_for_me";
    static final String CONVERSATION_MESSAGE_PERMISSION_GET = "comms.conversation_message.read";
    static final String CONVERSATION_MESSAGE_PERMISSION_CREATE = "comms.conversation_message.create";
    static final String CONVERSATION_MESSAGE_PERMISSION_UPDATE = "comms.conversation_message.update";
    static final String CONVERSATION_MESSAGE_PERMISSION_DELETE = "comms.conversation_message.delete";

    public static final String CONVERSATION_PARTICIPANT_AGGREGATE = ConversationParticipantEntity.TABLE;
    static final String CONVERSATION_PARTICIPANT_PERMISSION_LIST = "comms.conversation_participant.read";
    static final String CONVERSATION_PARTICIPANT_PERMISSION_GET = "comms.conversation_participant.read";
    static final String CONVERSATION_PARTICIPANT_PERMISSION_CREATE = "comms.conversation_participant.create";
    static final String CONVERSATION_PARTICIPANT_PERMISSION_UPDATE = "comms.conversation_participant.update";
    static final String CONVERSATION_PARTICIPANT_PERMISSION_DELETE = "comms.conversation_participant.delete";

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
    private final MemberDirectory members;

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
            ConversationParticipantMapper conversationParticipantMapper,
            MemberDirectory members) {
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
        this.members = members;
    }

    /** The conversation, to whoever opened it and its participants only (404 to anyone else). */
    public Conversation getConversation(UUID id) {
        DataScope scope = DataScope.current(PERMISSION_GET);
        return doInReadTransaction(() -> mapper.toDto(loadVisible(scope, id)));
    }

    /** The conversations the caller opened or takes part in. */
    public Page<Conversation> listConversations(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ConversationRepository.SORT_FIELDS, ConversationRepository.DEFAULT_ORDER,
                ConversationRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ConversationRepository.FILTER_FIELDS)
                .and(ConversationRepository.scope(DataScope.current(PERMISSION_LIST)))
                .and(ConversationRepository.memberOf(OwnRows.caller()));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + OwnRows.caller(), clock);
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
     * Partial update under If-Match by the conversation's opener (403 for another participant) (§7.2: a field left out
     * is kept, a null one cleared, a value set); the merged row must validate and stay in the caller's scope.
     */
    public Conversation updateConversation(UUID id, String ifMatch, ConversationUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ConversationEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            requireOpener(entity);
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
            outbox.publish(AGGREGATE + CommsEvents.UPDATED, AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /**
     * Soft delete under If-Match by the conversation's opener (403 for another participant); refused while live rows
     * of this module still reference the row.
     */
    public void deleteConversation(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            ConversationEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            requireOpener(entity);
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

    /**
     * comms.MarkConversationRead: the caller's read mark in the conversation moves to now. Only a participant's own
     * mark moves; the conversation of someone who is not a participant is 404.
     */
    public void markConversationRead(UUID id) {
        DataScope scope = DataScope.current(PERMISSION_MARK_CONVERSATION_READ);
        UUID caller = OwnRows.caller();
        runInWriteTransaction(() -> {
            loadVisible(scope, id);
            ConversationParticipantEntity participant = conversationParticipantRepository.findParticipant(id, caller)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (conversationParticipantRepository.markRead(participant.getId(), participant.getRowVersion(), now(),
                    Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The participant changed since it was read.");
            }
            audit.recordTransition(CONVERSATION_PARTICIPANT_AGGREGATE, participant.getId(), "mark_read",
                    List.of("lastReadAt"), null);
            outbox.publish(CONVERSATION_PARTICIPANT_AGGREGATE + ".mark_read", CONVERSATION_PARTICIPANT_AGGREGATE,
                    participant.getId(), CommsEvents.payload(participant.getId()));
        });
    }

    /**
     * comms.HideMessageForMe: the message disappears from the caller's own view of the conversation only — the
     * sender's flag for a message they sent, the caller's receipt otherwise. Only participants reach a message (404
     * otherwise); hiding it again changes nothing.
     */
    public void hideMessageForMe(UUID messageId) {
        DataScope scope = DataScope.current(PERMISSION_HIDE_MESSAGE_FOR_ME);
        UUID caller = OwnRows.caller();
        runInWriteTransaction(() -> {
            ConversationMessageEntity message = conversationMessageRepository.findById(messageId)
                    .orElseThrow(() -> new ResourceNotFoundException(CONVERSATION_MESSAGE_AGGREGATE, messageId));
            ConversationEntity conversation = repository.findById(message.getConversationId())
                    .filter(found -> scope.canRead(institutionOf(found))).orElse(null);
            if (conversation == null
                    || conversationParticipantRepository.findParticipant(conversation.getId(), caller).isEmpty()) {
                throw new ResourceNotFoundException(CONVERSATION_MESSAGE_AGGREGATE, messageId);
            }
            UUID actor = Actor.current().orElse(null);
            if (caller.equals(message.getSenderUserId())) {
                if (Boolean.TRUE.equals(message.getDeletedBySender())) {
                    return;
                }
                if (conversationMessageRepository.hideForSender(messageId, message.getRowVersion(), now(), actor) == 0) {
                    throw new PreconditionFailedException("The message changed since it was read.");
                }
                hidden(CONVERSATION_MESSAGE_AGGREGATE, messageId, "deletedBySender");
                return;
            }
            MessageReceiptEntity receipt = conversationMessageRepository.findReceipt(messageId, caller).orElse(null);
            if (receipt == null) {
                MessageReceiptEntity created = MessageReceiptEntity.builder().messageId(messageId).userId(caller)
                        .deletedAtByRecipient(now()).tenantId(conversation.getTenantId()).build();
                created.onCreatingData(Actor.current(), clock);
                conversationMessageRepository.insertReceipt(created);
                audit.recordCreate(MessageReceiptEntity.TABLE, created.getId());
                hidden(MessageReceiptEntity.TABLE, created.getId(), "deletedAtByRecipient");
            } else if (receipt.getDeletedAtByRecipient() == null) {
                if (conversationMessageRepository.hideReceipt(receipt.getId(), receipt.getRowVersion(), now(),
                        actor) == 0) {
                    throw new PreconditionFailedException("The receipt changed since it was read.");
                }
                hidden(MessageReceiptEntity.TABLE, receipt.getId(), "deletedAtByRecipient");
            }
        });
    }

    private void hidden(String aggregate, UUID id, String field) {
        audit.recordTransition(aggregate, id, "hide_for_me", List.of(field), null);
        outbox.publish(aggregate + ".hide_for_me", aggregate, id, CommsEvents.payload(id));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    /**
     * The live conversation, if the caller may see it: outside the caller's scope, or opened by someone else without
     * the caller taking part, it is 404.
     */
    private ConversationEntity loadVisible(DataScope scope, UUID id) {
        ConversationEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        requireMember(id);
        return entity;
    }

    /** 403 unless the caller opened the conversation: only the opener changes it and who takes part. */
    private static void requireOpener(ConversationEntity conversation) {
        if (!OwnRows.caller().equals(conversation.getCreatedBy())) {
            throw new PermissionDeniedException("Only the conversation's opener changes it or who takes part in it.");
        }
    }

    /** 404 when the conversation exists and the caller neither opened it nor takes part in it. */
    private void requireMember(UUID conversationId) {
        if (repository.findById(conversationId).isPresent() && !repository.admits(conversationId, OwnRows.caller())) {
            throw new ResourceNotFoundException(AGGREGATE, conversationId);
        }
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(ConversationEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(ConversationEntity entity, ConversationUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("conversationType", changes.getConversationType(),
                v -> entity.setConversationType(v == null ? null : v.getValue()));
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("lastMessageAt", changes.getLastMessageAt(), entity::setLastMessageAt);
        patch.apply("isActive", changes.getIsActive(), entity::setIsActive);
        return patch.applied();
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
        DataScope scope = DataScope.current(CONVERSATION_MESSAGE_PERMISSION_GET);
        return doInReadTransaction(() -> conversationMessageMapper.toDto(loadVisibleConversationMessage(scope,
                conversationId, id)));
    }

    public Page<ConversationMessage> listConversationMessages(UUID conversationId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ConversationMessageRepository.SORT_FIELDS,
                        ConversationMessageRepository.DEFAULT_ORDER,
                ConversationMessageRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ConversationMessageRepository.FILTER_FIELDS)
                .and(ConversationMessageRepository.ofParent(conversationId))
                        .and(ConversationMessageRepository.scope(DataScope.current(CONVERSATION_MESSAGE_PERMISSION_LIST)))
                .and(ConversationMessageRepository.notHiddenFrom(OwnRows.caller()));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort,
                filter + "|" + conversationId + "|" + OwnRows.caller(), clock);
        return doInReadTransaction(() -> {
            requireMember(conversationId);
            Page<ConversationMessageEntity> rows = conversationMessageRepository.list(page, where);
            return rows.map(conversationMessageMapper::toDto);
        });
    }

    /**
     * Sends the caller's message into a conversation they opened or take part in (404 otherwise); with validateOnly
     * every check runs and nothing is written.
     */
    public ConversationMessage createConversationMessage(UUID conversationId, ConversationMessage request,
            boolean validateOnly) {
        validator.validate(request);
        ConversationMessageEntity entity = conversationMessageMapper.toEntity(request);
        entity.setConversationId(conversationId);
        entity.setSenderUserId(OwnRows.caller());
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
            requireMember(conversationId);
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
     * Partial update of the caller's own message under If-Match (anyone else's is 404) (§7.2: a field left out is kept,
     * a null one cleared, a value set); the merged row must validate and stay in the caller's scope.
     */
    public ConversationMessage updateConversationMessage(UUID conversationId, UUID id, String ifMatch,
            ConversationMessageUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CONVERSATION_MESSAGE_PERMISSION_UPDATE);
            ConversationMessageEntity entity = loadVisibleConversationMessage(scope, conversationId, id);
            OwnRows.require(entity.getSenderUserId(), CONVERSATION_MESSAGE_AGGREGATE, id);
            scope.requireWritable(institutionOfConversationMessage(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyConversationMessage(entity, changes);
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

    /**
     * Soft delete of the caller's own message under If-Match (anyone else's is 404; HideMessageForMe hides it from one
     * participant); refused while live rows of this module still reference the row.
     */
    public void deleteConversationMessage(UUID conversationId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CONVERSATION_MESSAGE_PERMISSION_DELETE);
            ConversationMessageEntity entity = loadVisibleConversationMessage(scope, conversationId, id);
            OwnRows.require(entity.getSenderUserId(), CONVERSATION_MESSAGE_AGGREGATE, id);
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
        requireMember(conversationId);
        return entity;
    }

    /** The institution of the comms.conversation the row belongs to. */
    private Optional<UUID> institutionOfConversationMessage(ConversationMessageEntity entity) {
        return conversationMessageRepository.institutionOf(entity.getConversationId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyConversationMessage(ConversationMessageEntity entity, ConversationMessageUpdate changes) {
        Patches patch = new Patches();
        patch.apply("body", changes.getBody(), entity::setBody);
        patch.apply("deletedBySender", changes.getDeletedBySender(), entity::setDeletedBySender);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidConversationMessage(ConversationMessageEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getConversationId() == null) {
            errors.add(new FieldError("conversationId", "must not be null", "NotNull"));
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
        DataScope scope = DataScope.current(CONVERSATION_PARTICIPANT_PERMISSION_GET);
        return doInReadTransaction(() -> conversationParticipantMapper.toDto(loadVisibleConversationParticipant(scope,
                conversationId, id)));
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
            requireMember(conversationId);
            Page<ConversationParticipantEntity> rows = conversationParticipantRepository.list(page, where);
            return rows.map(conversationParticipantMapper::toDto);
        });
    }

    /**
     * The opener adds someone (403 for another participant): an account of the conversation's school (of any school of
     * the tenant for a tenant-level conversation), as comms knows them from sis's and hr's records (422 otherwise);
     * with validateOnly every check runs and nothing is written.
     */
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
            requireMember(conversationId);
            assertValidConversationParticipant(entity, true);
            scope.requireWritable(institutionOfConversationParticipant(entity));
            requireOpener(repository.findById(conversationId)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, conversationId)));
            if (members.accountsIn(List.of(entity.getUserId()),
                    institutionOfConversationParticipant(entity).orElse(null)).isEmpty()) {
                throw new ValidationFailedException(List.of(new FieldError("userId",
                        "is not an account of the conversation's school", "NOT_FOUND")));
            }
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
     * A participant moves their own read mark (lastReadAt) under If-Match; another's row is 404 and the account a row
     * names never changes (422).
     */
    public ConversationParticipant updateConversationParticipant(UUID conversationId, UUID id, String ifMatch,
            ConversationParticipantUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CONVERSATION_PARTICIPANT_PERMISSION_UPDATE);
            ConversationParticipantEntity entity = loadVisibleConversationParticipant(scope, conversationId, id);
            OwnRows.require(entity.getUserId(), CONVERSATION_PARTICIPANT_AGGREGATE, id);
            scope.requireWritable(institutionOfConversationParticipant(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            UUID account = entity.getUserId();
            List<String> fields = applyConversationParticipant(entity, changes);
            if (!account.equals(entity.getUserId())) {
                throw new ValidationFailedException(List.of(new FieldError("userId",
                        "a participant's account never changes; add the other person instead", "IMMUTABLE")));
            }
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

    /** A participant leaves (their own row) or the opener removes someone, under If-Match; anyone else's is 404. */
    public void deleteConversationParticipant(UUID conversationId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CONVERSATION_PARTICIPANT_PERMISSION_DELETE);
            ConversationParticipantEntity entity = loadVisibleConversationParticipant(scope, conversationId, id);
            boolean opener = repository.findById(conversationId)
                    .map(found -> OwnRows.caller().equals(found.getCreatedBy())).orElse(false);
            if (!opener) {
                OwnRows.require(entity.getUserId(), CONVERSATION_PARTICIPANT_AGGREGATE, id);
            }
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
        requireMember(conversationId);
        return entity;
    }

    /** The institution of the comms.conversation the row belongs to. */
    private Optional<UUID> institutionOfConversationParticipant(ConversationParticipantEntity entity) {
        return conversationParticipantRepository.institutionOf(entity.getConversationId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyConversationParticipant(ConversationParticipantEntity entity,
            ConversationParticipantUpdate changes) {
        Patches patch = new Patches();
        patch.apply("userId", changes.getUserId(), entity::setUserId);
        patch.apply("lastReadAt", changes.getLastReadAt(), entity::setLastReadAt);
        return patch.applied();
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
