package com.mysms.comms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.business.OutboundMessageService;
import com.mysms.comms.api.OutboundMessageApi;
import com.mysms.comms.api.model.CancelOutboundMessageRequest;
import com.mysms.comms.api.model.OutboundMessage;
import com.mysms.comms.api.model.RetryOutboundMessageRequest;
import com.mysms.comms.api.model.SendMessageRequest;
import com.mysms.comms.api.model.SendResult;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the OutboundMessage operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/comms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class OutboundMessageController implements OutboundMessageApi {

    private final OutboundMessageService outboundMessageService;

    public OutboundMessageController(OutboundMessageService outboundMessageService) {
        this.outboundMessageService = outboundMessageService;
    }

    @Override
    public ResponseEntity<OutboundMessage> cancelOutboundMessage(UUID outboundMessageId,
            CancelOutboundMessageRequest cancelOutboundMessageRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<OutboundMessage> getOutboundMessage(UUID outboundMessageId, String fields) {
        OutboundMessage found = outboundMessageService.getOutboundMessage(outboundMessageId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<OutboundMessage>> listOutboundMessages(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<OutboundMessage> list =
                PagingUtil.pagingInfo(outboundMessageService.listOutboundMessages(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<OutboundMessage> retryOutboundMessage(UUID outboundMessageId,
            RetryOutboundMessageRequest retryOutboundMessageRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<SendResult> sendMessage(UUID idempotencyKey,
            SendMessageRequest sendMessageRequest) {
        throw new NotImplementedException();
    }
}
