package com.mysms.iam.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.iam.api.model.Operation;
import com.mysms.iam.business.OperationService;
import com.mysms.iam.v1.CancelOperationRequest;
import com.mysms.iam.v1.CancelOperationResponse;
import com.mysms.iam.v1.GetOperationRequest;
import com.mysms.iam.v1.GetOperationResponse;
import com.mysms.iam.v1.ListOperationsRequest;
import com.mysms.iam.v1.ListOperationsResponse;
import com.mysms.iam.v1.OperationServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of iam.OperationService: the long-running operations started in iam (a user
 * import), delegated to the same business service as REST's controller. REST's 202 on a cancellation is the
 * operation in {@code result}, as on every gRPC answer.
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
        Operation operation = service.getOperation(bridge.id(request.getOperationId(), "operationId"));
        ProtoBridge.send(out, bridge.reply(GetOperationResponse.newBuilder(), "result", operation).build());
    }

    @Override
    public void listOperations(ListOperationsRequest request, StreamObserver<ListOperationsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Operation> rows = service.listOperations(bridge.orNull(request.getKind()), page.page(), page.size(),
                page.cursor());
        ProtoBridge.send(out, bridge.page(ListOperationsResponse.newBuilder(), rows).build());
    }

    @Override
    public void cancelOperation(CancelOperationRequest request, StreamObserver<CancelOperationResponse> out) {
        Operation operation = service.cancelOperation(bridge.id(request.getOperationId(), "operationId"));
        ProtoBridge.send(out, bridge.reply(CancelOperationResponse.newBuilder(), "result", operation).build());
    }
}
