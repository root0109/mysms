package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.finance.api.model.Operation;
import com.mysms.finance.business.OperationService;
import com.mysms.finance.v1.CancelOperationRequest;
import com.mysms.finance.v1.CancelOperationResponse;
import com.mysms.finance.v1.GetOperationRequest;
import com.mysms.finance.v1.GetOperationResponse;
import com.mysms.finance.v1.ListOperationsRequest;
import com.mysms.finance.v1.ListOperationsResponse;
import com.mysms.finance.v1.OperationServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of finance.OperationService: finance's long-running operations (fee runs, fee structure publications),
 * delegated to the same business service as REST's controller. Thin: the service call, DTO → proto; authentication, the
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class OperationResolver extends OperationServiceGrpc.OperationServiceImplBase {

    private final OperationService service;
    private final ProtoBridge bridge;

    public OperationResolver(OperationService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void getOperation(GetOperationRequest request, StreamObserver<GetOperationResponse> out) {
        Operation found = service.getOperation(bridge.id(request.getOperationId(), "operationId"));
        ProtoBridge.send(out, bridge.reply(GetOperationResponse.newBuilder(), "result", found).build());
    }

    @Override
    public void listOperations(ListOperationsRequest request, StreamObserver<ListOperationsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Operation> rows = service.listOperations(
                bridge.orNull(request.getKind()), page.page(), page.size(), page.cursor());
        ProtoBridge.send(out, bridge.page(ListOperationsResponse.newBuilder(), rows).build());
    }

    @Override
    public void cancelOperation(CancelOperationRequest request, StreamObserver<CancelOperationResponse> out) {
        Operation changed = service.cancelOperation(bridge.id(request.getOperationId(), "operationId"));
        ProtoBridge.send(out, bridge.reply(CancelOperationResponse.newBuilder(), "result", changed).build());
    }
}
