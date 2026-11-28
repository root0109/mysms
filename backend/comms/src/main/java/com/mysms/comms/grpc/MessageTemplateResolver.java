package com.mysms.comms.grpc;

import com.mysms.comms.api.model.MessageTemplate;
import com.mysms.comms.business.MessageTemplateService;
import com.mysms.comms.v1.CreateMessageTemplateRequest;
import com.mysms.comms.v1.CreateMessageTemplateResponse;
import com.mysms.comms.v1.DeleteMessageTemplateRequest;
import com.mysms.comms.v1.DeleteMessageTemplateResponse;
import com.mysms.comms.v1.GetMessageTemplateRequest;
import com.mysms.comms.v1.GetMessageTemplateResponse;
import com.mysms.comms.v1.ListMessageTemplatesRequest;
import com.mysms.comms.v1.ListMessageTemplatesResponse;
import com.mysms.comms.v1.MessageTemplateServiceGrpc;
import com.mysms.comms.v1.MessageTemplateUpdate;
import com.mysms.comms.v1.UpdateMessageTemplateResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of comms.MessageTemplateService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class MessageTemplateResolver extends MessageTemplateServiceGrpc.MessageTemplateServiceImplBase {

    private final MessageTemplateService service;
    private final ProtoBridge bridge;

    public MessageTemplateResolver(MessageTemplateService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listMessageTemplates(ListMessageTemplatesRequest request, StreamObserver<ListMessageTemplatesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<MessageTemplate> rows = service.listMessageTemplates(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListMessageTemplatesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getMessageTemplate(GetMessageTemplateRequest request, StreamObserver<GetMessageTemplateResponse> out) {
        MessageTemplate found = service.getMessageTemplate(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetMessageTemplateResponse.newBuilder(), "message_template", found).build());
    }

    @Override
    public void createMessageTemplate(CreateMessageTemplateRequest request, StreamObserver<CreateMessageTemplateResponse> out) {
        MessageTemplate created = service.createMessageTemplate(
                bridge.body(request.getMessageTemplate(), MessageTemplate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateMessageTemplateResponse.newBuilder(), "message_template", created).build());
    }

    @Override
    public void updateMessageTemplate(MessageTemplateUpdate request, StreamObserver<UpdateMessageTemplateResponse> out) {
        MessageTemplate updated = service.updateMessageTemplate(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                MessageTemplateProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateMessageTemplateResponse.newBuilder(), "message_template", updated).build());
    }

    @Override
    public void deleteMessageTemplate(DeleteMessageTemplateRequest request, StreamObserver<DeleteMessageTemplateResponse> out) {
        service.deleteMessageTemplate(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteMessageTemplateResponse.getDefaultInstance());
    }
}
