package com.mysms.careers.grpc;

import com.mysms.careers.api.model.FunctionArea;
import com.mysms.careers.api.model.FunctionAreaUpdate;
import com.mysms.careers.business.FunctionAreaService;
import com.mysms.careers.v1.CreateFunctionAreaRequest;
import com.mysms.careers.v1.CreateFunctionAreaResponse;
import com.mysms.careers.v1.DeleteFunctionAreaRequest;
import com.mysms.careers.v1.DeleteFunctionAreaResponse;
import com.mysms.careers.v1.FunctionAreaServiceGrpc;
import com.mysms.careers.v1.GetFunctionAreaRequest;
import com.mysms.careers.v1.GetFunctionAreaResponse;
import com.mysms.careers.v1.ListFunctionAreasRequest;
import com.mysms.careers.v1.ListFunctionAreasResponse;
import com.mysms.careers.v1.UpdateFunctionAreaRequest;
import com.mysms.careers.v1.UpdateFunctionAreaResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of careers.FunctionAreaService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class FunctionAreaResolver extends FunctionAreaServiceGrpc.FunctionAreaServiceImplBase {

    private final FunctionAreaService service;
    private final ProtoBridge bridge;

    public FunctionAreaResolver(FunctionAreaService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listFunctionAreas(ListFunctionAreasRequest request, StreamObserver<ListFunctionAreasResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<FunctionArea> rows = service.listFunctionAreas(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListFunctionAreasResponse.newBuilder(), rows).build());
    }

    @Override
    public void getFunctionArea(GetFunctionAreaRequest request, StreamObserver<GetFunctionAreaResponse> out) {
        FunctionArea found = service.getFunctionArea(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetFunctionAreaResponse.newBuilder(), "function_area", found).build());
    }

    @Override
    public void createFunctionArea(CreateFunctionAreaRequest request, StreamObserver<CreateFunctionAreaResponse> out) {
        FunctionArea created = service.createFunctionArea(
                bridge.body(request.getFunctionArea(), FunctionArea.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateFunctionAreaResponse.newBuilder(), "function_area", created).build());
    }

    @Override
    public void updateFunctionArea(UpdateFunctionAreaRequest request, StreamObserver<UpdateFunctionAreaResponse> out) {
        FunctionArea updated = service.updateFunctionArea(
                bridge.id(request.getFunctionArea().getId(), "id"),
                bridge.requiredIfMatch(request.getFunctionArea().getEtag()),
                bridge.changes(request.getFunctionArea(), request.getUpdateMask(), FunctionAreaUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateFunctionAreaResponse.newBuilder(), "function_area", updated).build());
    }

    @Override
    public void deleteFunctionArea(DeleteFunctionAreaRequest request, StreamObserver<DeleteFunctionAreaResponse> out) {
        service.deleteFunctionArea(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteFunctionAreaResponse.getDefaultInstance());
    }
}
