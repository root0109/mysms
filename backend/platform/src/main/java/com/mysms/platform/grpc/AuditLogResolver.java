package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.platform.api.model.AuditLog;
import com.mysms.platform.business.AuditLogService;
import com.mysms.platform.v1.AuditLogServiceGrpc;
import com.mysms.platform.v1.GetAuditLogRequest;
import com.mysms.platform.v1.GetAuditLogResponse;
import com.mysms.platform.v1.ListAuditLogsRequest;
import com.mysms.platform.v1.ListAuditLogsResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of platform.AuditLogService: delegates every RPC to the business service REST uses, so the rules exist
 * once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class AuditLogResolver extends AuditLogServiceGrpc.AuditLogServiceImplBase {

    private final AuditLogService service;
    private final ProtoBridge bridge;

    public AuditLogResolver(AuditLogService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listAuditLogs(ListAuditLogsRequest request, StreamObserver<ListAuditLogsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AuditLog> rows = service.listAuditLogs(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAuditLogsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAuditLog(GetAuditLogRequest request, StreamObserver<GetAuditLogResponse> out) {
        AuditLog found = service.getAuditLog(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAuditLogResponse.newBuilder(), "audit_log", found).build());
    }
}
