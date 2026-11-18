package com.mysms.payroll.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.payroll.api.model.Operation;
import com.mysms.payroll.business.OperationService;
import com.mysms.payroll.v1.CancelOperationRequest;
import com.mysms.payroll.v1.CancelOperationResponse;
import com.mysms.payroll.v1.GetOperationRequest;
import com.mysms.payroll.v1.GetOperationResponse;
import com.mysms.payroll.v1.ListOperationsRequest;
import com.mysms.payroll.v1.ListOperationsResponse;
import com.mysms.payroll.v1.OperationServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of payroll.OperationService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class OperationResolver extends OperationServiceGrpc.OperationServiceImplBase {

    private final OperationService operationService;
    private final ProtoBridge bridge;

    public OperationResolver(OperationService operationService, ProtoBridge bridge) {
        this.operationService = operationService;
        this.bridge = bridge;
    }

    @Override
    public void getOperation(GetOperationRequest request, StreamObserver<GetOperationResponse> out) {
        Operation found = operationService.getOperation(bridge.id(request.getOperationId(), "operationId"));
        ProtoBridge.send(out, bridge.reply(GetOperationResponse.newBuilder(), "result", found).build());
    }

    @Override
    public void listOperations(ListOperationsRequest request, StreamObserver<ListOperationsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Operation> rows = operationService.listOperations(
                bridge.orNull(request.getKind()), page.page(), page.size(), page.cursor());
        ProtoBridge.send(out, bridge.page(ListOperationsResponse.newBuilder(), rows).build());
    }

    @Override
    public void cancelOperation(CancelOperationRequest request, StreamObserver<CancelOperationResponse> out) {
        Operation changed = operationService.cancelOperation(bridge.id(request.getOperationId(), "operationId"));
        ProtoBridge.send(out, bridge.reply(CancelOperationResponse.newBuilder(), "result", changed).build());
    }
}
