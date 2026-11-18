package com.mysms.comms.grpc;

import com.mysms.comms.api.model.FeedbackMessage;
import com.mysms.comms.api.model.FeedbackMessageUpdate;
import com.mysms.comms.api.model.FeedbackThread;
import com.mysms.comms.api.model.FeedbackThreadUpdate;
import com.mysms.comms.business.FeedbackThreadService;
import com.mysms.comms.v1.CloseFeedbackThreadRequest;
import com.mysms.comms.v1.CloseFeedbackThreadResponse;
import com.mysms.comms.v1.CreateFeedbackMessageRequest;
import com.mysms.comms.v1.CreateFeedbackMessageResponse;
import com.mysms.comms.v1.CreateFeedbackThreadRequest;
import com.mysms.comms.v1.CreateFeedbackThreadResponse;
import com.mysms.comms.v1.DeleteFeedbackMessageRequest;
import com.mysms.comms.v1.DeleteFeedbackMessageResponse;
import com.mysms.comms.v1.DeleteFeedbackThreadRequest;
import com.mysms.comms.v1.DeleteFeedbackThreadResponse;
import com.mysms.comms.v1.FeedbackThreadServiceGrpc;
import com.mysms.comms.v1.GetFeedbackMessageRequest;
import com.mysms.comms.v1.GetFeedbackMessageResponse;
import com.mysms.comms.v1.GetFeedbackThreadRequest;
import com.mysms.comms.v1.GetFeedbackThreadResponse;
import com.mysms.comms.v1.ListFeedbackMessagesRequest;
import com.mysms.comms.v1.ListFeedbackMessagesResponse;
import com.mysms.comms.v1.ListFeedbackThreadsRequest;
import com.mysms.comms.v1.ListFeedbackThreadsResponse;
import com.mysms.comms.v1.MarkFeedbackMessageRequest;
import com.mysms.comms.v1.MarkFeedbackMessageResponse;
import com.mysms.comms.v1.ReopenFeedbackThreadRequest;
import com.mysms.comms.v1.ReopenFeedbackThreadResponse;
import com.mysms.comms.v1.UpdateFeedbackMessageRequest;
import com.mysms.comms.v1.UpdateFeedbackMessageResponse;
import com.mysms.comms.v1.UpdateFeedbackThreadRequest;
import com.mysms.comms.v1.UpdateFeedbackThreadResponse;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of comms.FeedbackThreadService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class FeedbackThreadResolver extends FeedbackThreadServiceGrpc.FeedbackThreadServiceImplBase {

    private final FeedbackThreadService service;
    private final ProtoBridge bridge;

    public FeedbackThreadResolver(FeedbackThreadService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listFeedbackThreads(ListFeedbackThreadsRequest request, StreamObserver<ListFeedbackThreadsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<FeedbackThread> rows = service.listFeedbackThreads(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListFeedbackThreadsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getFeedbackThread(GetFeedbackThreadRequest request, StreamObserver<GetFeedbackThreadResponse> out) {
        FeedbackThread found = service.getFeedbackThread(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetFeedbackThreadResponse.newBuilder(), "feedback_thread", found).build());
    }

    @Override
    public void createFeedbackThread(CreateFeedbackThreadRequest request, StreamObserver<CreateFeedbackThreadResponse> out) {
        FeedbackThread created = service.createFeedbackThread(
                bridge.body(request.getFeedbackThread(), FeedbackThread.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateFeedbackThreadResponse.newBuilder(), "feedback_thread", created).build());
    }

    @Override
    public void updateFeedbackThread(UpdateFeedbackThreadRequest request, StreamObserver<UpdateFeedbackThreadResponse> out) {
        FeedbackThread updated = service.updateFeedbackThread(
                bridge.id(request.getFeedbackThread().getId(), "id"),
                bridge.requiredIfMatch(request.getFeedbackThread().getEtag()),
                bridge.changes(request.getFeedbackThread(), request.getUpdateMask(), FeedbackThreadUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateFeedbackThreadResponse.newBuilder(), "feedback_thread", updated).build());
    }

    @Override
    public void deleteFeedbackThread(DeleteFeedbackThreadRequest request, StreamObserver<DeleteFeedbackThreadResponse> out) {
        service.deleteFeedbackThread(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteFeedbackThreadResponse.getDefaultInstance());
    }

    @Override
    public void listFeedbackMessages(ListFeedbackMessagesRequest request, StreamObserver<ListFeedbackMessagesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<FeedbackMessage> rows = service.listFeedbackMessages(
                bridge.id(request.getFeedbackThreadId(), "feedbackThreadId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListFeedbackMessagesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getFeedbackMessage(GetFeedbackMessageRequest request, StreamObserver<GetFeedbackMessageResponse> out) {
        FeedbackMessage found = service.getFeedbackMessage(
                bridge.id(request.getFeedbackThreadId(), "feedbackThreadId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetFeedbackMessageResponse.newBuilder(), "feedback_message", found).build());
    }

    @Override
    public void createFeedbackMessage(CreateFeedbackMessageRequest request, StreamObserver<CreateFeedbackMessageResponse> out) {
        FeedbackMessage created = service.createFeedbackMessage(
                bridge.id(request.getFeedbackThreadId(), "feedbackThreadId"),
                bridge.body(request.getFeedbackMessage(), FeedbackMessage.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateFeedbackMessageResponse.newBuilder(), "feedback_message", created).build());
    }

    @Override
    public void updateFeedbackMessage(UpdateFeedbackMessageRequest request, StreamObserver<UpdateFeedbackMessageResponse> out) {
        FeedbackMessage updated = service.updateFeedbackMessage(
                bridge.id(request.getFeedbackThreadId(), "feedbackThreadId"),
                bridge.id(request.getFeedbackMessage().getId(), "id"),
                bridge.requiredIfMatch(request.getFeedbackMessage().getEtag()),
                bridge.changes(request.getFeedbackMessage(), request.getUpdateMask(), FeedbackMessageUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateFeedbackMessageResponse.newBuilder(), "feedback_message", updated).build());
    }

    @Override
    public void deleteFeedbackMessage(DeleteFeedbackMessageRequest request, StreamObserver<DeleteFeedbackMessageResponse> out) {
        service.deleteFeedbackMessage(
                bridge.id(request.getFeedbackThreadId(), "feedbackThreadId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteFeedbackMessageResponse.getDefaultInstance());
    }

    /** The REST body (the workflow etag, its other fields) is the request without its path ids and idempotency key. */
    @Override
    public void closeFeedbackThread(CloseFeedbackThreadRequest request, StreamObserver<CloseFeedbackThreadResponse> out) {
        FeedbackThread changed = service.closeFeedbackThread(
                bridge.id(request.getFeedbackThreadId(), "feedbackThreadId"),
                bridge.body(request, com.mysms.comms.api.model.CloseFeedbackThreadRequest.class, "feedback_thread_id",
                "request_id"));
        ProtoBridge.send(out, bridge.reply(CloseFeedbackThreadResponse.newBuilder(), "feedback_thread", changed).build());
    }

    /** The REST body (the workflow etag, its other fields) is the request without its path ids and idempotency key. */
    @Override
    public void reopenFeedbackThread(ReopenFeedbackThreadRequest request, StreamObserver<ReopenFeedbackThreadResponse> out) {
        FeedbackThread changed = service.reopenFeedbackThread(
                bridge.id(request.getFeedbackThreadId(), "feedbackThreadId"),
                bridge.body(request, com.mysms.comms.api.model.ReopenFeedbackThreadRequest.class, "feedback_thread_id",
                "request_id"));
        ProtoBridge.send(out, bridge.reply(ReopenFeedbackThreadResponse.newBuilder(), "feedback_thread", changed).build());
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void markFeedbackMessage(MarkFeedbackMessageRequest request, StreamObserver<MarkFeedbackMessageResponse> out) {
        throw new NotImplementedException();
    }
}
