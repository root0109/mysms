package com.mysms.campus.facility.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.campus.facility.api.model.Facility;
import com.mysms.campus.facility.api.model.FacilityUpdate;
import com.mysms.campus.facility.business.FacilityService;
import com.mysms.facility.v1.CreateFacilityRequest;
import com.mysms.facility.v1.CreateFacilityResponse;
import com.mysms.facility.v1.DeleteFacilityRequest;
import com.mysms.facility.v1.DeleteFacilityResponse;
import com.mysms.facility.v1.FacilityServiceGrpc;
import com.mysms.facility.v1.GetFacilityRequest;
import com.mysms.facility.v1.GetFacilityResponse;
import com.mysms.facility.v1.ListFacilitiesRequest;
import com.mysms.facility.v1.ListFacilitiesResponse;
import com.mysms.facility.v1.UpdateFacilityRequest;
import com.mysms.facility.v1.UpdateFacilityResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of facility.FacilityService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class FacilityResolver extends FacilityServiceGrpc.FacilityServiceImplBase {

    private final FacilityService service;
    private final ProtoBridge bridge;

    public FacilityResolver(FacilityService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listFacilities(ListFacilitiesRequest request, StreamObserver<ListFacilitiesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Facility> rows = service.listFacilitys(page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()),
                page.sort());
        ProtoBridge.send(out, bridge.page(ListFacilitiesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getFacility(GetFacilityRequest request, StreamObserver<GetFacilityResponse> out) {
        Facility found = service.getFacility(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetFacilityResponse.newBuilder(), "facility", found).build());
    }

    @Override
    public void createFacility(CreateFacilityRequest request, StreamObserver<CreateFacilityResponse> out) {
        Facility created = service.createFacility(bridge.body(request.getFacility(), Facility.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateFacilityResponse.newBuilder(), "facility", created).build());
    }

    @Override
    public void updateFacility(UpdateFacilityRequest request, StreamObserver<UpdateFacilityResponse> out) {
        Facility updated = service.updateFacility(bridge.id(request.getFacility().getId(), "id"),
                bridge.requiredIfMatch(request.getFacility().getEtag()),
                bridge.changes(request.getFacility(), request.getUpdateMask(), FacilityUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateFacilityResponse.newBuilder(), "facility", updated).build());
    }

    @Override
    public void deleteFacility(DeleteFacilityRequest request, StreamObserver<DeleteFacilityResponse> out) {
        service.deleteFacility(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteFacilityResponse.getDefaultInstance());
    }
}
