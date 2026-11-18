package com.mysms.people.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.people.api.model.Honorific;
import com.mysms.people.api.model.HonorificUpdate;
import com.mysms.people.business.HonorificService;
import com.mysms.people.v1.CreateHonorificRequest;
import com.mysms.people.v1.CreateHonorificResponse;
import com.mysms.people.v1.DeleteHonorificRequest;
import com.mysms.people.v1.DeleteHonorificResponse;
import com.mysms.people.v1.GetHonorificRequest;
import com.mysms.people.v1.GetHonorificResponse;
import com.mysms.people.v1.HonorificServiceGrpc;
import com.mysms.people.v1.ListHonorificsRequest;
import com.mysms.people.v1.ListHonorificsResponse;
import com.mysms.people.v1.UpdateHonorificRequest;
import com.mysms.people.v1.UpdateHonorificResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of people.HonorificService: delegates every RPC to the business service REST uses, so the rules exist
 * once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class HonorificResolver extends HonorificServiceGrpc.HonorificServiceImplBase {

    private final HonorificService service;
    private final ProtoBridge bridge;

    public HonorificResolver(HonorificService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listHonorifics(ListHonorificsRequest request, StreamObserver<ListHonorificsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Honorific> rows = service.listHonorifics(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListHonorificsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getHonorific(GetHonorificRequest request, StreamObserver<GetHonorificResponse> out) {
        Honorific found = service.getHonorific(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetHonorificResponse.newBuilder(), "honorific", found).build());
    }

    @Override
    public void createHonorific(CreateHonorificRequest request, StreamObserver<CreateHonorificResponse> out) {
        Honorific created = service.createHonorific(
                bridge.body(request.getHonorific(), Honorific.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateHonorificResponse.newBuilder(), "honorific", created).build());
    }

    @Override
    public void updateHonorific(UpdateHonorificRequest request, StreamObserver<UpdateHonorificResponse> out) {
        Honorific updated = service.updateHonorific(
                bridge.id(request.getHonorific().getId(), "id"), bridge.requiredIfMatch(request.getHonorific().getEtag()),
                bridge.changes(request.getHonorific(), request.getUpdateMask(), HonorificUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateHonorificResponse.newBuilder(), "honorific", updated).build());
    }

    @Override
    public void deleteHonorific(DeleteHonorificRequest request, StreamObserver<DeleteHonorificResponse> out) {
        service.deleteHonorific(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteHonorificResponse.getDefaultInstance());
    }
}
