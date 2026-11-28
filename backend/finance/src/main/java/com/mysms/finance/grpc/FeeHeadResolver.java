package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.finance.api.model.FeeHead;
import com.mysms.finance.business.FeeHeadService;
import com.mysms.finance.v1.CreateFeeHeadRequest;
import com.mysms.finance.v1.CreateFeeHeadResponse;
import com.mysms.finance.v1.DeleteFeeHeadRequest;
import com.mysms.finance.v1.DeleteFeeHeadResponse;
import com.mysms.finance.v1.FeeHeadServiceGrpc;
import com.mysms.finance.v1.FeeHeadUpdate;
import com.mysms.finance.v1.GetFeeHeadRequest;
import com.mysms.finance.v1.GetFeeHeadResponse;
import com.mysms.finance.v1.ListFeeHeadsRequest;
import com.mysms.finance.v1.ListFeeHeadsResponse;
import com.mysms.finance.v1.UpdateFeeHeadResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of finance.FeeHeadService: delegates every RPC to the business service REST uses, so the rules exist
 * once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class FeeHeadResolver extends FeeHeadServiceGrpc.FeeHeadServiceImplBase {

    private final FeeHeadService service;
    private final ProtoBridge bridge;

    public FeeHeadResolver(FeeHeadService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listFeeHeads(ListFeeHeadsRequest request, StreamObserver<ListFeeHeadsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<FeeHead> rows = service.listFeeHeads(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListFeeHeadsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getFeeHead(GetFeeHeadRequest request, StreamObserver<GetFeeHeadResponse> out) {
        FeeHead found = service.getFeeHead(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetFeeHeadResponse.newBuilder(), "fee_head", found).build());
    }

    @Override
    public void createFeeHead(CreateFeeHeadRequest request, StreamObserver<CreateFeeHeadResponse> out) {
        FeeHead created = service.createFeeHead(
                bridge.body(request.getFeeHead(), FeeHead.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateFeeHeadResponse.newBuilder(), "fee_head", created).build());
    }

    @Override
    public void updateFeeHead(FeeHeadUpdate request, StreamObserver<UpdateFeeHeadResponse> out) {
        FeeHead updated = service.updateFeeHead(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                FeeHeadProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateFeeHeadResponse.newBuilder(), "fee_head", updated).build());
    }

    @Override
    public void deleteFeeHead(DeleteFeeHeadRequest request, StreamObserver<DeleteFeeHeadResponse> out) {
        service.deleteFeeHead(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteFeeHeadResponse.getDefaultInstance());
    }
}
