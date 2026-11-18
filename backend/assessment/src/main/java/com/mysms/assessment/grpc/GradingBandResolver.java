package com.mysms.assessment.grpc;

import com.mysms.assessment.api.model.GradingBand;
import com.mysms.assessment.api.model.GradingBandUpdate;
import com.mysms.assessment.business.GradingBandService;
import com.mysms.assessment.v1.CreateGradingBandRequest;
import com.mysms.assessment.v1.CreateGradingBandResponse;
import com.mysms.assessment.v1.DeleteGradingBandRequest;
import com.mysms.assessment.v1.DeleteGradingBandResponse;
import com.mysms.assessment.v1.GetGradingBandRequest;
import com.mysms.assessment.v1.GetGradingBandResponse;
import com.mysms.assessment.v1.GradingBandServiceGrpc;
import com.mysms.assessment.v1.ListGradingBandsRequest;
import com.mysms.assessment.v1.ListGradingBandsResponse;
import com.mysms.assessment.v1.UpdateGradingBandRequest;
import com.mysms.assessment.v1.UpdateGradingBandResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of assessment.GradingBandService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class GradingBandResolver extends GradingBandServiceGrpc.GradingBandServiceImplBase {

    private final GradingBandService gradingBandService;
    private final ProtoBridge bridge;

    public GradingBandResolver(GradingBandService gradingBandService, ProtoBridge bridge) {
        this.gradingBandService = gradingBandService;
        this.bridge = bridge;
    }

    @Override
    public void listGradingBands(ListGradingBandsRequest request, StreamObserver<ListGradingBandsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<GradingBand> rows = gradingBandService.listGradingBands(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListGradingBandsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getGradingBand(GetGradingBandRequest request, StreamObserver<GetGradingBandResponse> out) {
        GradingBand found = gradingBandService.getGradingBand(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetGradingBandResponse.newBuilder(), "grading_band", found).build());
    }

    @Override
    public void createGradingBand(CreateGradingBandRequest request, StreamObserver<CreateGradingBandResponse> out) {
        GradingBand created = gradingBandService.createGradingBand(
                bridge.body(request.getGradingBand(), GradingBand.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateGradingBandResponse.newBuilder(), "grading_band", created).build());
    }

    @Override
    public void updateGradingBand(UpdateGradingBandRequest request, StreamObserver<UpdateGradingBandResponse> out) {
        GradingBand updated = gradingBandService.updateGradingBand(
                bridge.id(request.getGradingBand().getId(), "id"),
                bridge.requiredIfMatch(request.getGradingBand().getEtag()),
                bridge.changes(request.getGradingBand(), request.getUpdateMask(), GradingBandUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateGradingBandResponse.newBuilder(), "grading_band", updated).build());
    }

    @Override
    public void deleteGradingBand(DeleteGradingBandRequest request, StreamObserver<DeleteGradingBandResponse> out) {
        gradingBandService.deleteGradingBand(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteGradingBandResponse.getDefaultInstance());
    }
}
