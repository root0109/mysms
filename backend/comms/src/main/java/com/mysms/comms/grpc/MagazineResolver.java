package com.mysms.comms.grpc;

import com.mysms.comms.api.model.Magazine;
import com.mysms.comms.business.MagazineService;
import com.mysms.comms.v1.CreateMagazineRequest;
import com.mysms.comms.v1.CreateMagazineResponse;
import com.mysms.comms.v1.DeleteMagazineRequest;
import com.mysms.comms.v1.DeleteMagazineResponse;
import com.mysms.comms.v1.GetMagazineRequest;
import com.mysms.comms.v1.GetMagazineResponse;
import com.mysms.comms.v1.ListMagazinesRequest;
import com.mysms.comms.v1.ListMagazinesResponse;
import com.mysms.comms.v1.MagazineServiceGrpc;
import com.mysms.comms.v1.MagazineUpdate;
import com.mysms.comms.v1.UpdateMagazineResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of comms.MagazineService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class MagazineResolver extends MagazineServiceGrpc.MagazineServiceImplBase {

    private final MagazineService service;
    private final ProtoBridge bridge;

    public MagazineResolver(MagazineService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listMagazines(ListMagazinesRequest request, StreamObserver<ListMagazinesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Magazine> rows = service.listMagazines(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListMagazinesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getMagazine(GetMagazineRequest request, StreamObserver<GetMagazineResponse> out) {
        Magazine found = service.getMagazine(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetMagazineResponse.newBuilder(), "magazine", found).build());
    }

    @Override
    public void createMagazine(CreateMagazineRequest request, StreamObserver<CreateMagazineResponse> out) {
        Magazine created = service.createMagazine(bridge.body(request.getMagazine(), Magazine.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateMagazineResponse.newBuilder(), "magazine", created).build());
    }

    @Override
    public void updateMagazine(MagazineUpdate request, StreamObserver<UpdateMagazineResponse> out) {
        Magazine updated = service.updateMagazine(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()),
                MagazineProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateMagazineResponse.newBuilder(), "magazine", updated).build());
    }

    @Override
    public void deleteMagazine(DeleteMagazineRequest request, StreamObserver<DeleteMagazineResponse> out) {
        service.deleteMagazine(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteMagazineResponse.getDefaultInstance());
    }
}
