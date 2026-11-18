package com.mysms.academics.grpc;

import com.mysms.academics.api.model.Batch;
import com.mysms.academics.api.model.BatchUpdate;
import com.mysms.academics.business.BatchService;
import com.mysms.academics.v1.BatchServiceGrpc;
import com.mysms.academics.v1.CreateBatchRequest;
import com.mysms.academics.v1.CreateBatchResponse;
import com.mysms.academics.v1.DeleteBatchRequest;
import com.mysms.academics.v1.DeleteBatchResponse;
import com.mysms.academics.v1.GetBatchRequest;
import com.mysms.academics.v1.GetBatchResponse;
import com.mysms.academics.v1.ListBatchesRequest;
import com.mysms.academics.v1.ListBatchesResponse;
import com.mysms.academics.v1.UpdateBatchRequest;
import com.mysms.academics.v1.UpdateBatchResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of academics.BatchService: delegates every RPC to the business service REST uses, so the rules exist
 * once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class BatchResolver extends BatchServiceGrpc.BatchServiceImplBase {

    private final BatchService service;
    private final ProtoBridge bridge;

    public BatchResolver(BatchService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listBatches(ListBatchesRequest request, StreamObserver<ListBatchesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Batch> rows = service.listBatchs(page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()),
                page.sort());
        ProtoBridge.send(out, bridge.page(ListBatchesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getBatch(GetBatchRequest request, StreamObserver<GetBatchResponse> out) {
        Batch found = service.getBatch(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetBatchResponse.newBuilder(), "batch", found).build());
    }

    @Override
    public void createBatch(CreateBatchRequest request, StreamObserver<CreateBatchResponse> out) {
        Batch created = service.createBatch(bridge.body(request.getBatch(), Batch.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateBatchResponse.newBuilder(), "batch", created).build());
    }

    @Override
    public void updateBatch(UpdateBatchRequest request, StreamObserver<UpdateBatchResponse> out) {
        Batch updated = service.updateBatch(bridge.id(request.getBatch().getId(), "id"),
                bridge.requiredIfMatch(request.getBatch().getEtag()),
                bridge.changes(request.getBatch(), request.getUpdateMask(), BatchUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateBatchResponse.newBuilder(), "batch", updated).build());
    }

    @Override
    public void deleteBatch(DeleteBatchRequest request, StreamObserver<DeleteBatchResponse> out) {
        service.deleteBatch(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteBatchResponse.getDefaultInstance());
    }
}
