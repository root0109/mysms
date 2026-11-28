package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.finance.api.model.AcquisitionSource;
import com.mysms.finance.business.AcquisitionSourceService;
import com.mysms.finance.v1.AcquisitionSourceServiceGrpc;
import com.mysms.finance.v1.AcquisitionSourceUpdate;
import com.mysms.finance.v1.CreateAcquisitionSourceRequest;
import com.mysms.finance.v1.CreateAcquisitionSourceResponse;
import com.mysms.finance.v1.DeleteAcquisitionSourceRequest;
import com.mysms.finance.v1.DeleteAcquisitionSourceResponse;
import com.mysms.finance.v1.GetAcquisitionSourceRequest;
import com.mysms.finance.v1.GetAcquisitionSourceResponse;
import com.mysms.finance.v1.ListAcquisitionSourcesRequest;
import com.mysms.finance.v1.ListAcquisitionSourcesResponse;
import com.mysms.finance.v1.UpdateAcquisitionSourceResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of finance.AcquisitionSourceService: delegates every RPC to the business service REST uses, so the
 * rules exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class AcquisitionSourceResolver extends AcquisitionSourceServiceGrpc.AcquisitionSourceServiceImplBase {

    private final AcquisitionSourceService service;
    private final ProtoBridge bridge;

    public AcquisitionSourceResolver(AcquisitionSourceService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listAcquisitionSources(ListAcquisitionSourcesRequest request,
            StreamObserver<ListAcquisitionSourcesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AcquisitionSource> rows = service.listAcquisitionSources(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAcquisitionSourcesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAcquisitionSource(GetAcquisitionSourceRequest request,
            StreamObserver<GetAcquisitionSourceResponse> out) {
        AcquisitionSource found = service.getAcquisitionSource(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAcquisitionSourceResponse.newBuilder(), "acquisition_source",
                found).build());
    }

    @Override
    public void createAcquisitionSource(CreateAcquisitionSourceRequest request,
            StreamObserver<CreateAcquisitionSourceResponse> out) {
        AcquisitionSource created = service.createAcquisitionSource(
                bridge.body(request.getAcquisitionSource(), AcquisitionSource.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateAcquisitionSourceResponse.newBuilder(), "acquisition_source",
                created).build());
    }

    @Override
    public void updateAcquisitionSource(AcquisitionSourceUpdate request,
            StreamObserver<UpdateAcquisitionSourceResponse> out) {
        AcquisitionSource updated = service.updateAcquisitionSource(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                AcquisitionSourceProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateAcquisitionSourceResponse.newBuilder(), "acquisition_source",
                updated).build());
    }

    @Override
    public void deleteAcquisitionSource(DeleteAcquisitionSourceRequest request,
            StreamObserver<DeleteAcquisitionSourceResponse> out) {
        service.deleteAcquisitionSource(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAcquisitionSourceResponse.getDefaultInstance());
    }
}
