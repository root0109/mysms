package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.GradingScheme;
import com.mysms.academics.assessment.business.GradingSchemeService;
import com.mysms.assessment.v1.CreateGradingSchemeRequest;
import com.mysms.assessment.v1.CreateGradingSchemeResponse;
import com.mysms.assessment.v1.DeleteGradingSchemeRequest;
import com.mysms.assessment.v1.DeleteGradingSchemeResponse;
import com.mysms.assessment.v1.GetGradingSchemeRequest;
import com.mysms.assessment.v1.GetGradingSchemeResponse;
import com.mysms.assessment.v1.GradingSchemeServiceGrpc;
import com.mysms.assessment.v1.GradingSchemeUpdate;
import com.mysms.assessment.v1.ListGradingSchemesRequest;
import com.mysms.assessment.v1.ListGradingSchemesResponse;
import com.mysms.assessment.v1.UpdateGradingSchemeResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of assessment.GradingSchemeService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class GradingSchemeResolver extends GradingSchemeServiceGrpc.GradingSchemeServiceImplBase {

    private final GradingSchemeService service;
    private final ProtoBridge bridge;

    public GradingSchemeResolver(GradingSchemeService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listGradingSchemes(ListGradingSchemesRequest request,
            StreamObserver<ListGradingSchemesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<GradingScheme> rows = service.listGradingSchemes(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListGradingSchemesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getGradingScheme(GetGradingSchemeRequest request, StreamObserver<GetGradingSchemeResponse> out) {
        GradingScheme found = service.getGradingScheme(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetGradingSchemeResponse.newBuilder(), "grading_scheme", found).build());
    }

    @Override
    public void createGradingScheme(CreateGradingSchemeRequest request,
            StreamObserver<CreateGradingSchemeResponse> out) {
        GradingScheme created = service.createGradingScheme(
                bridge.body(request.getGradingScheme(), GradingScheme.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateGradingSchemeResponse.newBuilder(),
                "grading_scheme", created).build());
    }

    @Override
    public void updateGradingScheme(GradingSchemeUpdate request,
            StreamObserver<UpdateGradingSchemeResponse> out) {
        GradingScheme updated = service.updateGradingScheme(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                GradingSchemeProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateGradingSchemeResponse.newBuilder(),
                "grading_scheme", updated).build());
    }

    @Override
    public void deleteGradingScheme(DeleteGradingSchemeRequest request,
            StreamObserver<DeleteGradingSchemeResponse> out) {
        service.deleteGradingScheme(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteGradingSchemeResponse.getDefaultInstance());
    }
}
