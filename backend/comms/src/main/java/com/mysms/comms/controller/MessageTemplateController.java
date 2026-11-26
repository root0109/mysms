package com.mysms.comms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.business.MessageTemplateService;
import com.mysms.comms.api.MessageTemplateApi;
import com.mysms.comms.api.model.MessageTemplate;
import com.mysms.comms.api.model.MessageTemplateUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the MessageTemplate operations; authentication and the permission check already ran in the auth-client
 * filter.
 */
@RestController
public class MessageTemplateController implements MessageTemplateApi {

    private final MessageTemplateService messageTemplateService;

    public MessageTemplateController(MessageTemplateService messageTemplateService) {
        this.messageTemplateService = messageTemplateService;
    }

    @Override
    public ResponseEntity<MessageTemplate> createMessageTemplate(MessageTemplate messageTemplate,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        MessageTemplate created = messageTemplateService.createMessageTemplate(messageTemplate, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/message-templates/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteMessageTemplate(UUID messageTemplateId, String ifMatch) {
        messageTemplateService.deleteMessageTemplate(messageTemplateId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<MessageTemplate> getMessageTemplate(UUID messageTemplateId, String fields) {
        MessageTemplate found = messageTemplateService.getMessageTemplate(messageTemplateId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<MessageTemplate>> listMessageTemplates(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<MessageTemplate> list =
                PagingUtil.pagingInfo(messageTemplateService.listMessageTemplates(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<MessageTemplate> updateMessageTemplate(UUID messageTemplateId, String ifMatch,
            MessageTemplateUpdate messageTemplateUpdate, Boolean validateOnly) {
        MessageTemplate updated =
                messageTemplateService.updateMessageTemplate(messageTemplateId, ifMatch, messageTemplateUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
