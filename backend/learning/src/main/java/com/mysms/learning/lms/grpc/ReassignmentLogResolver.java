package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.ReassignmentLog;
import com.mysms.learning.lms.business.ReassignmentLogService;
import com.mysms.lms.v1.GetReassignmentLogRequest;
import com.mysms.lms.v1.GetReassignmentLogResponse;
import com.mysms.lms.v1.ListReassignmentLogsRequest;
import com.mysms.lms.v1.ListReassignmentLogsResponse;
import com.mysms.lms.v1.ReassignmentLogServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.ReassignmentLogService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ReassignmentLogResolver extends ReassignmentLogServiceGrpc.ReassignmentLogServiceImplBase {

    private final ReassignmentLogService service;
    private final ProtoBridge bridge;

    public ReassignmentLogResolver(ReassignmentLogService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listReassignmentLogs(ListReassignmentLogsRequest request, StreamObserver<ListReassignmentLogsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ReassignmentLog> rows = service.listReassignmentLogs(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListReassignmentLogsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getReassignmentLog(GetReassignmentLogRequest request, StreamObserver<GetReassignmentLogResponse> out) {
        ReassignmentLog found = service.getReassignmentLog(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetReassignmentLogResponse.newBuilder(), "reassignment_log", found).build());
    }
}
