package com.mysms.students.careers.grpc;

import com.mysms.students.careers.api.model.Industry;
import com.mysms.students.careers.business.IndustryService;
import com.mysms.careers.v1.CreateIndustryRequest;
import com.mysms.careers.v1.CreateIndustryResponse;
import com.mysms.careers.v1.DeleteIndustryRequest;
import com.mysms.careers.v1.DeleteIndustryResponse;
import com.mysms.careers.v1.GetIndustryRequest;
import com.mysms.careers.v1.GetIndustryResponse;
import com.mysms.careers.v1.IndustryServiceGrpc;
import com.mysms.careers.v1.IndustryUpdate;
import com.mysms.careers.v1.ListIndustriesRequest;
import com.mysms.careers.v1.ListIndustriesResponse;
import com.mysms.careers.v1.UpdateIndustryResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of careers.IndustryService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class IndustryResolver extends IndustryServiceGrpc.IndustryServiceImplBase {

    private final IndustryService service;
    private final ProtoBridge bridge;

    public IndustryResolver(IndustryService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listIndustries(ListIndustriesRequest request, StreamObserver<ListIndustriesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Industry> rows = service.listIndustrys(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListIndustriesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getIndustry(GetIndustryRequest request, StreamObserver<GetIndustryResponse> out) {
        Industry found = service.getIndustry(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetIndustryResponse.newBuilder(), "industry", found).build());
    }

    @Override
    public void createIndustry(CreateIndustryRequest request, StreamObserver<CreateIndustryResponse> out) {
        Industry created = service.createIndustry(
                bridge.body(request.getIndustry(), Industry.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateIndustryResponse.newBuilder(), "industry", created).build());
    }

    @Override
    public void updateIndustry(IndustryUpdate request, StreamObserver<UpdateIndustryResponse> out) {
        Industry updated = service.updateIndustry(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                IndustryProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateIndustryResponse.newBuilder(), "industry", updated).build());
    }

    @Override
    public void deleteIndustry(DeleteIndustryRequest request, StreamObserver<DeleteIndustryResponse> out) {
        service.deleteIndustry(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteIndustryResponse.getDefaultInstance());
    }
}
