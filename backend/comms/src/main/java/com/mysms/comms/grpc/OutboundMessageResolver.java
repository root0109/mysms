package com.mysms.comms.grpc;

import com.mysms.comms.api.model.OutboundMessage;
import com.mysms.comms.business.OutboundMessageService;
import com.mysms.comms.v1.CancelOutboundMessageRequest;
import com.mysms.comms.v1.CancelOutboundMessageResponse;
import com.mysms.comms.v1.GetOutboundMessageRequest;
import com.mysms.comms.v1.GetOutboundMessageResponse;
import com.mysms.comms.v1.ListOutboundMessagesRequest;
import com.mysms.comms.v1.ListOutboundMessagesResponse;
import com.mysms.comms.v1.OutboundMessageServiceGrpc;
import com.mysms.comms.v1.RetryOutboundMessageRequest;
import com.mysms.comms.v1.RetryOutboundMessageResponse;
import com.mysms.comms.v1.SendMessageRequest;
import com.mysms.comms.v1.SendMessageResponse;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of comms.OutboundMessageService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class OutboundMessageResolver extends OutboundMessageServiceGrpc.OutboundMessageServiceImplBase {

    private final OutboundMessageService service;
    private final ProtoBridge bridge;

    public OutboundMessageResolver(OutboundMessageService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listOutboundMessages(ListOutboundMessagesRequest request, StreamObserver<ListOutboundMessagesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<OutboundMessage> rows = service.listOutboundMessages(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListOutboundMessagesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getOutboundMessage(GetOutboundMessageRequest request, StreamObserver<GetOutboundMessageResponse> out) {
        OutboundMessage found = service.getOutboundMessage(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetOutboundMessageResponse.newBuilder(), "outbound_message", found).build());
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void sendMessage(SendMessageRequest request, StreamObserver<SendMessageResponse> out) {
        throw new NotImplementedException();
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void cancelOutboundMessage(CancelOutboundMessageRequest request, StreamObserver<CancelOutboundMessageResponse> out) {
        throw new NotImplementedException();
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void retryOutboundMessage(RetryOutboundMessageRequest request, StreamObserver<RetryOutboundMessageResponse> out) {
        throw new NotImplementedException();
    }
}
