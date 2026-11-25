package com.mysms.comms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.business.ConversationService;
import com.mysms.comms.api.ConversationApi;
import com.mysms.comms.api.model.Conversation;
import com.mysms.comms.api.model.ConversationMessage;
import com.mysms.comms.api.model.ConversationMessageUpdate;
import com.mysms.comms.api.model.ConversationParticipant;
import com.mysms.comms.api.model.ConversationParticipantUpdate;
import com.mysms.comms.api.model.ConversationUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Conversation operations; authentication and the permission check already ran in the auth-client
 * filter.
 */
@RestController
public class ConversationController implements ConversationApi {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @Override
    public ResponseEntity<Conversation> createConversation(Conversation conversation, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Conversation created = conversationService.createConversation(conversation, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/conversations/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<ConversationMessage> createConversationMessage(UUID conversationId,
            ConversationMessage conversationMessage, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ConversationMessage created =
                conversationService.createConversationMessage(conversationId, conversationMessage, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/comms/conversations/" + conversationId + "/messages/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<ConversationParticipant> createConversationParticipant(UUID conversationId,
            ConversationParticipant conversationParticipant, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ConversationParticipant created =
                conversationService.createConversationParticipant(conversationId, conversationParticipant,
                        dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/comms/conversations/" + conversationId + "/participants/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteConversation(UUID conversationId, String ifMatch) {
        conversationService.deleteConversation(conversationId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteConversationMessage(UUID conversationId, UUID conversationMessageId,
            String ifMatch) {
        conversationService.deleteConversationMessage(conversationId, conversationMessageId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteConversationParticipant(UUID conversationId,
            UUID conversationParticipantId, String ifMatch) {
        conversationService.deleteConversationParticipant(conversationId, conversationParticipantId,
                ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Conversation> getConversation(UUID conversationId, String fields) {
        Conversation found = conversationService.getConversation(conversationId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ConversationMessage> getConversationMessage(UUID conversationId,
            UUID conversationMessageId, String fields) {
        ConversationMessage found =
                conversationService.getConversationMessage(conversationId, conversationMessageId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ConversationParticipant> getConversationParticipant(UUID conversationId,
            UUID conversationParticipantId, String fields) {
        ConversationParticipant found =
                conversationService.getConversationParticipant(conversationId, conversationParticipantId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<Void> hideMessageForMe(UUID messageId, Object body, UUID idempotencyKey) {
        conversationService.hideMessageForMe(messageId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PagingInfo<ConversationMessage>> listConversationMessages(UUID conversationId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ConversationMessage> list =
                PagingUtil.pagingInfo(conversationService.listConversationMessages(conversationId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ConversationParticipant>> listConversationParticipants(UUID conversationId,
            Integer page, Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ConversationParticipant> list =
                PagingUtil.pagingInfo(conversationService.listConversationParticipants(conversationId, page, size,
                        cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Conversation>> listConversations(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<Conversation> list =
                PagingUtil.pagingInfo(conversationService.listConversations(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Void> markConversationRead(UUID conversationId, Object body,
            UUID idempotencyKey) {
        conversationService.markConversationRead(conversationId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Conversation> updateConversation(UUID conversationId, String ifMatch,
            ConversationUpdate conversationUpdate, String updateMask, Boolean validateOnly) {
        Conversation updated =
                conversationService.updateConversation(conversationId, ifMatch, conversationUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ConversationMessage> updateConversationMessage(UUID conversationId,
            UUID conversationMessageId, String ifMatch, ConversationMessageUpdate conversationMessageUpdate,
            String updateMask, Boolean validateOnly) {
        ConversationMessage updated =
                conversationService.updateConversationMessage(conversationId, conversationMessageId, ifMatch,
                        conversationMessageUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ConversationParticipant> updateConversationParticipant(UUID conversationId,
            UUID conversationParticipantId, String ifMatch, ConversationParticipantUpdate conversationParticipantUpdate,
            String updateMask, Boolean validateOnly) {
        ConversationParticipant updated =
                conversationService.updateConversationParticipant(conversationId, conversationParticipantId,
                        ifMatch, conversationParticipantUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
