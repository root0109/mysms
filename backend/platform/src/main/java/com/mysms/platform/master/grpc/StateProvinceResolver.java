package com.mysms.platform.master.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.platform.master.api.model.StateProvince;
import com.mysms.platform.master.api.model.StateProvinceUpdate;
import com.mysms.platform.master.business.StateProvinceService;
import com.mysms.master.v1.CreateStateProvinceRequest;
import com.mysms.master.v1.CreateStateProvinceResponse;
import com.mysms.master.v1.DeleteStateProvinceRequest;
import com.mysms.master.v1.DeleteStateProvinceResponse;
import com.mysms.master.v1.GetStateProvinceRequest;
import com.mysms.master.v1.GetStateProvinceResponse;
import com.mysms.master.v1.ListStateProvincesRequest;
import com.mysms.master.v1.ListStateProvincesResponse;
import com.mysms.master.v1.StateProvinceServiceGrpc;
import com.mysms.master.v1.UpdateStateProvinceRequest;
import com.mysms.master.v1.UpdateStateProvinceResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of master.StateProvinceService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class StateProvinceResolver extends StateProvinceServiceGrpc.StateProvinceServiceImplBase {

    private final StateProvinceService service;
    private final ProtoBridge bridge;

    public StateProvinceResolver(StateProvinceService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }


    @Override
    public void listStateProvinces(ListStateProvincesRequest request,
            StreamObserver<ListStateProvincesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<StateProvince> rows = service.listStateProvinces(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListStateProvincesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getStateProvince(GetStateProvinceRequest request, StreamObserver<GetStateProvinceResponse> out) {
        StateProvince found = service.getStateProvince(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetStateProvinceResponse.newBuilder(), "state_province", found).build());
    }

    @Override
    public void createStateProvince(CreateStateProvinceRequest request,
            StreamObserver<CreateStateProvinceResponse> out) {
        StateProvince created = service.createStateProvince(
                bridge.body(request.getStateProvince(), StateProvince.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateStateProvinceResponse.newBuilder(), "state_province",
                created).build());
    }

    @Override
    public void updateStateProvince(UpdateStateProvinceRequest request,
            StreamObserver<UpdateStateProvinceResponse> out) {
        StateProvince updated = service.updateStateProvince(
                bridge.id(request.getStateProvince().getId(), "id"),
                bridge.requiredIfMatch(request.getStateProvince().getEtag()),
                bridge.changes(request.getStateProvince(), request.getUpdateMask(), StateProvinceUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateStateProvinceResponse.newBuilder(), "state_province",
                updated).build());
    }

    @Override
    public void deleteStateProvince(DeleteStateProvinceRequest request,
            StreamObserver<DeleteStateProvinceResponse> out) {
        service.deleteStateProvince(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteStateProvinceResponse.getDefaultInstance());
    }
}
