package com.mysms.comms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.business.MailboxMessageService;
import com.mysms.comms.api.MailboxMessageApi;
import com.mysms.comms.api.model.ComposeMailRequest;
import com.mysms.comms.api.model.MailboxMessage;
import com.mysms.core.web.ReadMask;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the MailboxMessage operations; authentication and the permission check already ran in the auth-client
 * filter.
 */
@RestController
public class MailboxMessageController implements MailboxMessageApi {

    private final MailboxMessageService mailboxMessageService;

    public MailboxMessageController(MailboxMessageService mailboxMessageService) {
        this.mailboxMessageService = mailboxMessageService;
    }

    @Override
    public ResponseEntity<MailboxMessage> composeMail(ComposeMailRequest composeMailRequest,
            UUID idempotencyKey) {
        return ResponseEntity.ok(mailboxMessageService.composeMail(composeMailRequest));
    }

    @Override
    public ResponseEntity<Void> deleteMailboxMessage(UUID mailboxMessageId, String ifMatch) {
        mailboxMessageService.deleteMailboxMessage(mailboxMessageId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<MailboxMessage> getMailboxMessage(UUID mailboxMessageId, String fields) {
        MailboxMessage found = mailboxMessageService.getMailboxMessage(mailboxMessageId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<MailboxMessage>> listMailboxMessages(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<MailboxMessage> list =
                PagingUtil.pagingInfo(mailboxMessageService.listMailboxMessages(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<MailboxMessage> markMailRead(UUID mailboxMessageId, Object body,
            UUID idempotencyKey) {
        MailboxMessage read = mailboxMessageService.markMailRead(mailboxMessageId);
        return ResponseEntity.ok().eTag(read.getEtag()).body(read);
    }
}
