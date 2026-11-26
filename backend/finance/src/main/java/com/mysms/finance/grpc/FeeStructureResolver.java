package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.finance.api.model.FeeStructure;
import com.mysms.finance.api.model.FeeStructureUpdate;
import com.mysms.finance.api.model.Operation;
import com.mysms.finance.business.FeeStructureService;
import com.mysms.finance.v1.ArchiveFeeStructureRequest;
import com.mysms.finance.v1.ArchiveFeeStructureResponse;
import com.mysms.finance.v1.CreateFeeStructureRequest;
import com.mysms.finance.v1.CreateFeeStructureResponse;
import com.mysms.finance.v1.DeleteFeeStructureRequest;
import com.mysms.finance.v1.DeleteFeeStructureResponse;
import com.mysms.finance.v1.FeeStructureServiceGrpc;
import com.mysms.finance.v1.GetFeeStructureRequest;
import com.mysms.finance.v1.GetFeeStructureResponse;
import com.mysms.finance.v1.ListFeeStructuresRequest;
import com.mysms.finance.v1.ListFeeStructuresResponse;
import com.mysms.finance.v1.PublishFeeStructureRequest;
import com.mysms.finance.v1.PublishFeeStructureResponse;
import com.mysms.finance.v1.UpdateFeeStructureRequest;
import com.mysms.finance.v1.UpdateFeeStructureResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of finance.FeeStructureService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class FeeStructureResolver extends FeeStructureServiceGrpc.FeeStructureServiceImplBase {

    private final FeeStructureService service;
    private final ProtoBridge bridge;

    public FeeStructureResolver(FeeStructureService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listFeeStructures(ListFeeStructuresRequest request, StreamObserver<ListFeeStructuresResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<FeeStructure> rows = service.listFeeStructures(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListFeeStructuresResponse.newBuilder(), rows).build());
    }

    @Override
    public void getFeeStructure(GetFeeStructureRequest request, StreamObserver<GetFeeStructureResponse> out) {
        FeeStructure found = service.getFeeStructure(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetFeeStructureResponse.newBuilder(), "fee_structure", found).build());
    }

    @Override
    public void createFeeStructure(CreateFeeStructureRequest request, StreamObserver<CreateFeeStructureResponse> out) {
        FeeStructure created = service.createFeeStructure(
                bridge.body(request.getFeeStructure(), FeeStructure.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateFeeStructureResponse.newBuilder(), "fee_structure", created).build());
    }

    @Override
    public void updateFeeStructure(UpdateFeeStructureRequest request, StreamObserver<UpdateFeeStructureResponse> out) {
        FeeStructure updated = service.updateFeeStructure(
                bridge.id(request.getFeeStructure().getId(), "id"),
                bridge.requiredIfMatch(request.getFeeStructure().getEtag()),
                bridge.changes(request.getFeeStructure(), request.getUpdateMask(), FeeStructureUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateFeeStructureResponse.newBuilder(), "fee_structure", updated).build());
    }

    @Override
    public void deleteFeeStructure(DeleteFeeStructureRequest request, StreamObserver<DeleteFeeStructureResponse> out) {
        service.deleteFeeStructure(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteFeeStructureResponse.getDefaultInstance());
    }

    /** finance.PublishFeeStructure: the fee run's operation, as REST's 202 answers it. */
    @Override
    public void publishFeeStructure(PublishFeeStructureRequest request,
            StreamObserver<PublishFeeStructureResponse> out) {
        Operation run = service.publishFeeStructure(bridge.id(request.getFeeStructureId(), "feeStructureId"));
        ProtoBridge.send(out, bridge.reply(PublishFeeStructureResponse.newBuilder(), "result", run).build());
    }

    /** finance.ArchiveFeeStructure: the etag stands for REST's required If-Match header. */
    @Override
    public void archiveFeeStructure(ArchiveFeeStructureRequest request,
            StreamObserver<ArchiveFeeStructureResponse> out) {
        FeeStructure changed = service.archiveFeeStructure(bridge.id(request.getFeeStructureId(), "feeStructureId"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, bridge.reply(ArchiveFeeStructureResponse.newBuilder(), "fee_structure", changed).build());
    }
}
