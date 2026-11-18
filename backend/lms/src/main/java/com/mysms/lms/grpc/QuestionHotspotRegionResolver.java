package com.mysms.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.lms.api.model.QuestionHotspotRegion;
import com.mysms.lms.api.model.QuestionHotspotRegionUpdate;
import com.mysms.lms.business.QuestionHotspotRegionService;
import com.mysms.lms.v1.CreateQuestionHotspotRegionRequest;
import com.mysms.lms.v1.CreateQuestionHotspotRegionResponse;
import com.mysms.lms.v1.DeleteQuestionHotspotRegionRequest;
import com.mysms.lms.v1.DeleteQuestionHotspotRegionResponse;
import com.mysms.lms.v1.GetQuestionHotspotRegionRequest;
import com.mysms.lms.v1.GetQuestionHotspotRegionResponse;
import com.mysms.lms.v1.ListQuestionHotspotRegionsRequest;
import com.mysms.lms.v1.ListQuestionHotspotRegionsResponse;
import com.mysms.lms.v1.QuestionHotspotRegionServiceGrpc;
import com.mysms.lms.v1.UpdateQuestionHotspotRegionRequest;
import com.mysms.lms.v1.UpdateQuestionHotspotRegionResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.QuestionHotspotRegionService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class QuestionHotspotRegionResolver extends QuestionHotspotRegionServiceGrpc.QuestionHotspotRegionServiceImplBase {

    private final QuestionHotspotRegionService service;
    private final ProtoBridge bridge;

    public QuestionHotspotRegionResolver(QuestionHotspotRegionService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listQuestionHotspotRegions(ListQuestionHotspotRegionsRequest request,
            StreamObserver<ListQuestionHotspotRegionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<QuestionHotspotRegion> rows = service.listQuestionHotspotRegions(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListQuestionHotspotRegionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getQuestionHotspotRegion(GetQuestionHotspotRegionRequest request,
            StreamObserver<GetQuestionHotspotRegionResponse> out) {
        QuestionHotspotRegion found = service.getQuestionHotspotRegion(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(
                GetQuestionHotspotRegionResponse.newBuilder(), "question_hotspot_region", found).build());
    }

    @Override
    public void createQuestionHotspotRegion(CreateQuestionHotspotRegionRequest request,
            StreamObserver<CreateQuestionHotspotRegionResponse> out) {
        QuestionHotspotRegion created = service.createQuestionHotspotRegion(
                bridge.body(request.getQuestionHotspotRegion(), QuestionHotspotRegion.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateQuestionHotspotRegionResponse.newBuilder(), "question_hotspot_region", created).build());
    }

    @Override
    public void updateQuestionHotspotRegion(UpdateQuestionHotspotRegionRequest request,
            StreamObserver<UpdateQuestionHotspotRegionResponse> out) {
        QuestionHotspotRegion updated = service.updateQuestionHotspotRegion(
                bridge.id(request.getQuestionHotspotRegion().getId(), "id"),
                bridge.requiredIfMatch(request.getQuestionHotspotRegion().getEtag()),
                bridge.changes(request.getQuestionHotspotRegion(), request.getUpdateMask(),
                        QuestionHotspotRegionUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateQuestionHotspotRegionResponse.newBuilder(), "question_hotspot_region", updated).build());
    }

    @Override
    public void deleteQuestionHotspotRegion(DeleteQuestionHotspotRegionRequest request,
            StreamObserver<DeleteQuestionHotspotRegionResponse> out) {
        service.deleteQuestionHotspotRegion(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteQuestionHotspotRegionResponse.getDefaultInstance());
    }
}
