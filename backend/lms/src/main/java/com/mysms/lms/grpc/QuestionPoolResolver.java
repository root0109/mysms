package com.mysms.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.lms.api.model.QuestionPool;
import com.mysms.lms.api.model.QuestionPoolItem;
import com.mysms.lms.api.model.QuestionPoolItemUpdate;
import com.mysms.lms.api.model.QuestionPoolUpdate;
import com.mysms.lms.business.QuestionPoolService;
import com.mysms.lms.v1.CreateQuestionPoolItemRequest;
import com.mysms.lms.v1.CreateQuestionPoolItemResponse;
import com.mysms.lms.v1.CreateQuestionPoolRequest;
import com.mysms.lms.v1.CreateQuestionPoolResponse;
import com.mysms.lms.v1.DeleteQuestionPoolItemRequest;
import com.mysms.lms.v1.DeleteQuestionPoolItemResponse;
import com.mysms.lms.v1.DeleteQuestionPoolRequest;
import com.mysms.lms.v1.DeleteQuestionPoolResponse;
import com.mysms.lms.v1.GetQuestionPoolItemRequest;
import com.mysms.lms.v1.GetQuestionPoolItemResponse;
import com.mysms.lms.v1.GetQuestionPoolRequest;
import com.mysms.lms.v1.GetQuestionPoolResponse;
import com.mysms.lms.v1.ListQuestionPoolItemsRequest;
import com.mysms.lms.v1.ListQuestionPoolItemsResponse;
import com.mysms.lms.v1.ListQuestionPoolsRequest;
import com.mysms.lms.v1.ListQuestionPoolsResponse;
import com.mysms.lms.v1.QuestionPoolServiceGrpc;
import com.mysms.lms.v1.UpdateQuestionPoolItemRequest;
import com.mysms.lms.v1.UpdateQuestionPoolItemResponse;
import com.mysms.lms.v1.UpdateQuestionPoolRequest;
import com.mysms.lms.v1.UpdateQuestionPoolResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.QuestionPoolService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class QuestionPoolResolver extends QuestionPoolServiceGrpc.QuestionPoolServiceImplBase {

    private final QuestionPoolService service;
    private final ProtoBridge bridge;

    public QuestionPoolResolver(QuestionPoolService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listQuestionPools(ListQuestionPoolsRequest request, StreamObserver<ListQuestionPoolsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<QuestionPool> rows = service.listQuestionPools(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListQuestionPoolsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getQuestionPool(GetQuestionPoolRequest request, StreamObserver<GetQuestionPoolResponse> out) {
        QuestionPool found = service.getQuestionPool(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetQuestionPoolResponse.newBuilder(), "question_pool", found).build());
    }

    @Override
    public void createQuestionPool(CreateQuestionPoolRequest request, StreamObserver<CreateQuestionPoolResponse> out) {
        QuestionPool created = service.createQuestionPool(
                bridge.body(request.getQuestionPool(), QuestionPool.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateQuestionPoolResponse.newBuilder(), "question_pool", created).build());
    }

    @Override
    public void updateQuestionPool(UpdateQuestionPoolRequest request, StreamObserver<UpdateQuestionPoolResponse> out) {
        QuestionPool updated = service.updateQuestionPool(
                bridge.id(request.getQuestionPool().getId(), "id"),
                bridge.requiredIfMatch(request.getQuestionPool().getEtag()),
                bridge.changes(request.getQuestionPool(), request.getUpdateMask(), QuestionPoolUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateQuestionPoolResponse.newBuilder(), "question_pool", updated).build());
    }

    @Override
    public void deleteQuestionPool(DeleteQuestionPoolRequest request, StreamObserver<DeleteQuestionPoolResponse> out) {
        service.deleteQuestionPool(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteQuestionPoolResponse.getDefaultInstance());
    }

    @Override
    public void listQuestionPoolItems(ListQuestionPoolItemsRequest request,
            StreamObserver<ListQuestionPoolItemsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<QuestionPoolItem> rows = service.listQuestionPoolItems(
                bridge.id(request.getQuestionPoolId(), "questionPoolId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListQuestionPoolItemsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getQuestionPoolItem(GetQuestionPoolItemRequest request,
            StreamObserver<GetQuestionPoolItemResponse> out) {
        QuestionPoolItem found = service.getQuestionPoolItem(
                bridge.id(request.getQuestionPoolId(), "questionPoolId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(
                GetQuestionPoolItemResponse.newBuilder(), "question_pool_item", found).build());
    }

    @Override
    public void createQuestionPoolItem(CreateQuestionPoolItemRequest request,
            StreamObserver<CreateQuestionPoolItemResponse> out) {
        QuestionPoolItem created = service.createQuestionPoolItem(
                bridge.id(request.getQuestionPoolId(), "questionPoolId"),
                bridge.body(request.getQuestionPoolItem(), QuestionPoolItem.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateQuestionPoolItemResponse.newBuilder(), "question_pool_item", created).build());
    }

    @Override
    public void updateQuestionPoolItem(UpdateQuestionPoolItemRequest request,
            StreamObserver<UpdateQuestionPoolItemResponse> out) {
        QuestionPoolItem updated = service.updateQuestionPoolItem(
                bridge.id(request.getQuestionPoolId(), "questionPoolId"),
                bridge.id(request.getQuestionPoolItem().getId(), "id"),
                bridge.requiredIfMatch(request.getQuestionPoolItem().getEtag()),
                bridge.changes(request.getQuestionPoolItem(), request.getUpdateMask(), QuestionPoolItemUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateQuestionPoolItemResponse.newBuilder(), "question_pool_item", updated).build());
    }

    @Override
    public void deleteQuestionPoolItem(DeleteQuestionPoolItemRequest request,
            StreamObserver<DeleteQuestionPoolItemResponse> out) {
        service.deleteQuestionPoolItem(
                bridge.id(request.getQuestionPoolId(), "questionPoolId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteQuestionPoolItemResponse.getDefaultInstance());
    }
}
