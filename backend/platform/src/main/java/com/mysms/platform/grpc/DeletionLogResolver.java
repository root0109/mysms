package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.platform.api.model.DeletionLog;
import com.mysms.platform.business.DeletionLogService;
import com.mysms.platform.v1.DeletionLogServiceGrpc;
import com.mysms.platform.v1.GetDeletionLogRequest;
import com.mysms.platform.v1.GetDeletionLogResponse;
import com.mysms.platform.v1.ListDeletionLogsRequest;
import com.mysms.platform.v1.ListDeletionLogsResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of platform.DeletionLogService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class DeletionLogResolver extends DeletionLogServiceGrpc.DeletionLogServiceImplBase {

    private final DeletionLogService service;
    private final ProtoBridge bridge;

    public DeletionLogResolver(DeletionLogService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listDeletionLogs(ListDeletionLogsRequest request, StreamObserver<ListDeletionLogsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<DeletionLog> rows = service.listDeletionLogs(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListDeletionLogsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getDeletionLog(GetDeletionLogRequest request, StreamObserver<GetDeletionLogResponse> out) {
        DeletionLog found = service.getDeletionLog(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetDeletionLogResponse.newBuilder(), "deletion_log", found).build());
    }
}
