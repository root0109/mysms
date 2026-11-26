package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.Operation;
import com.mysms.learning.lms.business.OperationService;
import com.mysms.lms.v1.CancelOperationRequest;
import com.mysms.lms.v1.CancelOperationResponse;
import com.mysms.lms.v1.GetOperationRequest;
import com.mysms.lms.v1.GetOperationResponse;
import com.mysms.lms.v1.ListOperationsRequest;
import com.mysms.lms.v1.ListOperationsResponse;
import com.mysms.lms.v1.OperationServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.OperationService: the module's long-running operations, polled and cancelled through the same
 * business service as REST. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
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

    /** REST's list takes no filter or sort: the kind, and the page, size and cursor of the pageable. */
    @Override
    public void listOperations(ListOperationsRequest request, StreamObserver<ListOperationsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Operation> rows = service.listOperations(
                bridge.orNull(request.getKind()), page.page(), page.size(), page.cursor());
        ProtoBridge.send(out, bridge.page(ListOperationsResponse.newBuilder(), rows).build());
    }

    /** REST answers 202 with the operation as it now stands; so does this. */
    @Override
    public void cancelOperation(CancelOperationRequest request, StreamObserver<CancelOperationResponse> out) {
        Operation cancelled = service.cancelOperation(bridge.id(request.getOperationId(), "operationId"));
        ProtoBridge.send(out, bridge.reply(CancelOperationResponse.newBuilder(), "result", cancelled).build());
    }
}
