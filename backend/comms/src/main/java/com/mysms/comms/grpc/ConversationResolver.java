package com.mysms.comms.grpc;

import com.mysms.comms.api.model.Conversation;
import com.mysms.comms.api.model.ConversationMessage;
import com.mysms.comms.api.model.ConversationMessageUpdate;
import com.mysms.comms.api.model.ConversationParticipant;
import com.mysms.comms.api.model.ConversationParticipantUpdate;
import com.mysms.comms.api.model.ConversationUpdate;
import com.mysms.comms.business.ConversationService;
import com.mysms.comms.v1.ConversationServiceGrpc;
import com.mysms.comms.v1.CreateConversationMessageRequest;
import com.mysms.comms.v1.CreateConversationMessageResponse;
import com.mysms.comms.v1.CreateConversationParticipantRequest;
import com.mysms.comms.v1.CreateConversationParticipantResponse;
import com.mysms.comms.v1.CreateConversationRequest;
import com.mysms.comms.v1.CreateConversationResponse;
import com.mysms.comms.v1.DeleteConversationMessageRequest;
import com.mysms.comms.v1.DeleteConversationMessageResponse;
import com.mysms.comms.v1.DeleteConversationParticipantRequest;
import com.mysms.comms.v1.DeleteConversationParticipantResponse;
import com.mysms.comms.v1.DeleteConversationRequest;
import com.mysms.comms.v1.DeleteConversationResponse;
import com.mysms.comms.v1.GetConversationMessageRequest;
import com.mysms.comms.v1.GetConversationMessageResponse;
import com.mysms.comms.v1.GetConversationParticipantRequest;
import com.mysms.comms.v1.GetConversationParticipantResponse;
import com.mysms.comms.v1.GetConversationRequest;
import com.mysms.comms.v1.GetConversationResponse;
import com.mysms.comms.v1.HideMessageForMeRequest;
import com.mysms.comms.v1.HideMessageForMeResponse;
import com.mysms.comms.v1.ListConversationMessagesRequest;
import com.mysms.comms.v1.ListConversationMessagesResponse;
import com.mysms.comms.v1.ListConversationParticipantsRequest;
import com.mysms.comms.v1.ListConversationParticipantsResponse;
import com.mysms.comms.v1.ListConversationsRequest;
import com.mysms.comms.v1.ListConversationsResponse;
import com.mysms.comms.v1.MarkConversationReadRequest;
import com.mysms.comms.v1.MarkConversationReadResponse;
import com.mysms.comms.v1.UpdateConversationMessageRequest;
import com.mysms.comms.v1.UpdateConversationMessageResponse;
import com.mysms.comms.v1.UpdateConversationParticipantRequest;
import com.mysms.comms.v1.UpdateConversationParticipantResponse;
import com.mysms.comms.v1.UpdateConversationRequest;
import com.mysms.comms.v1.UpdateConversationResponse;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of comms.ConversationService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ConversationResolver extends ConversationServiceGrpc.ConversationServiceImplBase {

    private final ConversationService service;
    private final ProtoBridge bridge;

    public ConversationResolver(ConversationService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listConversations(ListConversationsRequest request, StreamObserver<ListConversationsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Conversation> rows = service.listConversations(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListConversationsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getConversation(GetConversationRequest request, StreamObserver<GetConversationResponse> out) {
        Conversation found = service.getConversation(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetConversationResponse.newBuilder(), "conversation", found).build());
    }

    @Override
    public void createConversation(CreateConversationRequest request, StreamObserver<CreateConversationResponse> out) {
        Conversation created = service.createConversation(
                bridge.body(request.getConversation(), Conversation.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateConversationResponse.newBuilder(), "conversation", created).build());
    }

    @Override
    public void updateConversation(UpdateConversationRequest request, StreamObserver<UpdateConversationResponse> out) {
        Conversation updated = service.updateConversation(
                bridge.id(request.getConversation().getId(), "id"), bridge.requiredIfMatch(request.getConversation().getEtag()),
                bridge.changes(request.getConversation(), request.getUpdateMask(), ConversationUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateConversationResponse.newBuilder(), "conversation", updated).build());
    }

    @Override
    public void deleteConversation(DeleteConversationRequest request, StreamObserver<DeleteConversationResponse> out) {
        service.deleteConversation(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteConversationResponse.getDefaultInstance());
    }

    @Override
    public void listConversationParticipants(ListConversationParticipantsRequest request,
            StreamObserver<ListConversationParticipantsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ConversationParticipant> rows = service.listConversationParticipants(
                bridge.id(request.getConversationId(), "conversationId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListConversationParticipantsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getConversationParticipant(GetConversationParticipantRequest request,
            StreamObserver<GetConversationParticipantResponse> out) {
        ConversationParticipant found = service.getConversationParticipant(
                bridge.id(request.getConversationId(), "conversationId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out,
                bridge.reply(GetConversationParticipantResponse.newBuilder(), "conversation_participant", found).build());
    }

    @Override
    public void createConversationParticipant(CreateConversationParticipantRequest request,
            StreamObserver<CreateConversationParticipantResponse> out) {
        ConversationParticipant created = service.createConversationParticipant(
                bridge.id(request.getConversationId(), "conversationId"),
                bridge.body(request.getConversationParticipant(), ConversationParticipant.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreateConversationParticipantResponse.newBuilder(), "conversation_participant", created).build());
    }

    @Override
    public void updateConversationParticipant(UpdateConversationParticipantRequest request,
            StreamObserver<UpdateConversationParticipantResponse> out) {
        ConversationParticipant updated = service.updateConversationParticipant(
                bridge.id(request.getConversationId(), "conversationId"),
                bridge.id(request.getConversationParticipant().getId(), "id"),
                bridge.requiredIfMatch(request.getConversationParticipant().getEtag()),
                bridge.changes(request.getConversationParticipant(), request.getUpdateMask(),
                ConversationParticipantUpdate.class), bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdateConversationParticipantResponse.newBuilder(), "conversation_participant", updated).build());
    }

    @Override
    public void deleteConversationParticipant(DeleteConversationParticipantRequest request,
            StreamObserver<DeleteConversationParticipantResponse> out) {
        service.deleteConversationParticipant(
                bridge.id(request.getConversationId(), "conversationId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteConversationParticipantResponse.getDefaultInstance());
    }

    @Override
    public void listConversationMessages(ListConversationMessagesRequest request,
            StreamObserver<ListConversationMessagesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ConversationMessage> rows = service.listConversationMessages(
                bridge.id(request.getConversationId(), "conversationId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListConversationMessagesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getConversationMessage(GetConversationMessageRequest request,
            StreamObserver<GetConversationMessageResponse> out) {
        ConversationMessage found = service.getConversationMessage(
                bridge.id(request.getConversationId(), "conversationId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetConversationMessageResponse.newBuilder(), "conversation_message", found).build());
    }

    @Override
    public void createConversationMessage(CreateConversationMessageRequest request,
            StreamObserver<CreateConversationMessageResponse> out) {
        ConversationMessage created = service.createConversationMessage(
                bridge.id(request.getConversationId(), "conversationId"),
                bridge.body(request.getConversationMessage(), ConversationMessage.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreateConversationMessageResponse.newBuilder(), "conversation_message", created).build());
    }

    @Override
    public void updateConversationMessage(UpdateConversationMessageRequest request,
            StreamObserver<UpdateConversationMessageResponse> out) {
        ConversationMessage updated = service.updateConversationMessage(
                bridge.id(request.getConversationId(), "conversationId"),
                bridge.id(request.getConversationMessage().getId(), "id"),
                bridge.requiredIfMatch(request.getConversationMessage().getEtag()),
                bridge.changes(request.getConversationMessage(), request.getUpdateMask(), ConversationMessageUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdateConversationMessageResponse.newBuilder(), "conversation_message", updated).build());
    }

    @Override
    public void deleteConversationMessage(DeleteConversationMessageRequest request,
            StreamObserver<DeleteConversationMessageResponse> out) {
        service.deleteConversationMessage(
                bridge.id(request.getConversationId(), "conversationId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteConversationMessageResponse.getDefaultInstance());
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void markConversationRead(MarkConversationReadRequest request, StreamObserver<MarkConversationReadResponse> out) {
        throw new NotImplementedException();
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void hideMessageForMe(HideMessageForMeRequest request, StreamObserver<HideMessageForMeResponse> out) {
        throw new NotImplementedException();
    }
}
