package com.mysms.comms.grpc;

import com.mysms.comms.api.model.MailboxMessage;
import com.mysms.comms.business.MailboxMessageService;
import com.mysms.comms.v1.ComposeMailRequest;
import com.mysms.comms.v1.ComposeMailResponse;
import com.mysms.comms.v1.DeleteMailboxMessageRequest;
import com.mysms.comms.v1.DeleteMailboxMessageResponse;
import com.mysms.comms.v1.GetMailboxMessageRequest;
import com.mysms.comms.v1.GetMailboxMessageResponse;
import com.mysms.comms.v1.ListMailboxMessagesRequest;
import com.mysms.comms.v1.ListMailboxMessagesResponse;
import com.mysms.comms.v1.MailboxMessageServiceGrpc;
import com.mysms.comms.v1.MarkMailReadRequest;
import com.mysms.comms.v1.MarkMailReadResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of comms.MailboxMessageService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class MailboxMessageResolver extends MailboxMessageServiceGrpc.MailboxMessageServiceImplBase {

    private final MailboxMessageService service;
    private final ProtoBridge bridge;

    public MailboxMessageResolver(MailboxMessageService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listMailboxMessages(ListMailboxMessagesRequest request, StreamObserver<ListMailboxMessagesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<MailboxMessage> rows = service.listMailboxMessages(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListMailboxMessagesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getMailboxMessage(GetMailboxMessageRequest request, StreamObserver<GetMailboxMessageResponse> out) {
        MailboxMessage found = service.getMailboxMessage(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetMailboxMessageResponse.newBuilder(), "mailbox_message", found).build());
    }

    @Override
    public void deleteMailboxMessage(DeleteMailboxMessageRequest request, StreamObserver<DeleteMailboxMessageResponse> out) {
        service.deleteMailboxMessage(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteMailboxMessageResponse.getDefaultInstance());
    }

    @Override
    public void composeMail(ComposeMailRequest request, StreamObserver<ComposeMailResponse> out) {
        MailboxMessage sent = service.composeMail(bridge.body(request,
                com.mysms.comms.api.model.ComposeMailRequest.class, "request_id"));
        ProtoBridge.send(out, bridge.reply(ComposeMailResponse.newBuilder(), "mailbox_message", sent).build());
    }

    @Override
    public void markMailRead(MarkMailReadRequest request, StreamObserver<MarkMailReadResponse> out) {
        MailboxMessage read = service.markMailRead(bridge.id(request.getMailboxMessageId(), "mailboxMessageId"));
        ProtoBridge.send(out, bridge.reply(MarkMailReadResponse.newBuilder(), "mailbox_message", read).build());
    }
}
