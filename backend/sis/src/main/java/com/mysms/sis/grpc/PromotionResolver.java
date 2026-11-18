package com.mysms.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.sis.api.model.Operation;
import com.mysms.sis.api.model.Promotion;
import com.mysms.sis.api.model.PromotionList;
import com.mysms.sis.api.model.PromotionUpdate;
import com.mysms.sis.business.PromotionService;
import com.mysms.sis.business.PromotionWorkflowService;
import com.mysms.sis.v1.ApplyPromotionsRequest;
import com.mysms.sis.v1.ApplyPromotionsResponse;
import com.mysms.sis.v1.ApprovePromotionRequest;
import com.mysms.sis.v1.ApprovePromotionResponse;
import com.mysms.sis.v1.CreatePromotionRequest;
import com.mysms.sis.v1.CreatePromotionResponse;
import com.mysms.sis.v1.DeletePromotionRequest;
import com.mysms.sis.v1.DeletePromotionResponse;
import com.mysms.sis.v1.GetPromotionRequest;
import com.mysms.sis.v1.GetPromotionResponse;
import com.mysms.sis.v1.ListPromotionsRequest;
import com.mysms.sis.v1.ListPromotionsResponse;
import com.mysms.sis.v1.PromotionServiceGrpc;
import com.mysms.sis.v1.ProposePromotionsRequest;
import com.mysms.sis.v1.ProposePromotionsResponse;
import com.mysms.sis.v1.RejectPromotionRequest;
import com.mysms.sis.v1.RejectPromotionResponse;
import com.mysms.sis.v1.UpdatePromotionRequest;
import com.mysms.sis.v1.UpdatePromotionResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of sis.PromotionService: delegates every RPC to the business service REST uses, so the rules exist
 * once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class PromotionResolver extends PromotionServiceGrpc.PromotionServiceImplBase {

    private final PromotionService promotionService;
    private final PromotionWorkflowService workflowService;
    private final ProtoBridge bridge;

    public PromotionResolver(PromotionService promotionService, PromotionWorkflowService workflowService, ProtoBridge bridge) {
        this.promotionService = promotionService;
        this.workflowService = workflowService;
        this.bridge = bridge;
    }

    @Override
    public void listPromotions(ListPromotionsRequest request, StreamObserver<ListPromotionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Promotion> rows = promotionService.listPromotions(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPromotionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPromotion(GetPromotionRequest request, StreamObserver<GetPromotionResponse> out) {
        Promotion found = promotionService.getPromotion(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPromotionResponse.newBuilder(), "promotion", found).build());
    }

    @Override
    public void createPromotion(CreatePromotionRequest request, StreamObserver<CreatePromotionResponse> out) {
        Promotion created = promotionService.createPromotion(
                bridge.body(request.getPromotion(), Promotion.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreatePromotionResponse.newBuilder(), "promotion", created).build());
    }

    @Override
    public void updatePromotion(UpdatePromotionRequest request, StreamObserver<UpdatePromotionResponse> out) {
        Promotion updated = promotionService.updatePromotion(
                bridge.id(request.getPromotion().getId(), "id"), bridge.requiredIfMatch(request.getPromotion().getEtag()),
                bridge.changes(request.getPromotion(), request.getUpdateMask(), PromotionUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdatePromotionResponse.newBuilder(), "promotion", updated).build());
    }

    @Override
    public void deletePromotion(DeletePromotionRequest request, StreamObserver<DeletePromotionResponse> out) {
        promotionService.deletePromotion(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePromotionResponse.getDefaultInstance());
    }

    @Override
    public void proposePromotions(ProposePromotionsRequest request, StreamObserver<ProposePromotionsResponse> out) {
        PromotionList proposed = workflowService.proposePromotions(
                bridge.body(request, com.mysms.sis.api.model.ProposePromotionsRequest.class, "request_id"));
        ProtoBridge.send(out, bridge.reply(ProposePromotionsResponse.newBuilder(), "items",
                proposed.getItems()).build());
    }

    @Override
    public void approvePromotion(ApprovePromotionRequest request, StreamObserver<ApprovePromotionResponse> out) {
        Promotion changed = promotionService.approvePromotion(
                bridge.id(request.getPromotionId(), "promotionId"),
                bridge.body(request, com.mysms.sis.api.model.ApprovePromotionRequest.class, "promotion_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ApprovePromotionResponse.newBuilder(), "promotion", changed).build());
    }

    @Override
    public void rejectPromotion(RejectPromotionRequest request, StreamObserver<RejectPromotionResponse> out) {
        Promotion changed = promotionService.rejectPromotion(
                bridge.id(request.getPromotionId(), "promotionId"),
                bridge.body(request, com.mysms.sis.api.model.RejectPromotionRequest.class, "promotion_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(RejectPromotionResponse.newBuilder(), "promotion", changed).build());
    }

    @Override
    public void applyPromotions(ApplyPromotionsRequest request, StreamObserver<ApplyPromotionsResponse> out) {
        Operation operation = workflowService.applyPromotions(
                bridge.body(request, com.mysms.sis.api.model.ApplyPromotionsRequest.class, "request_id"));
        ProtoBridge.send(out, bridge.reply(ApplyPromotionsResponse.newBuilder(), "result", operation).build());
    }
}
