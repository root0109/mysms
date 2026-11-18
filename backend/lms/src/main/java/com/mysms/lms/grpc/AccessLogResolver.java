package com.mysms.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.lms.api.model.AccessLog;
import com.mysms.lms.business.AccessLogService;
import com.mysms.lms.v1.AccessLogServiceGrpc;
import com.mysms.lms.v1.GetAccessLogRequest;
import com.mysms.lms.v1.GetAccessLogResponse;
import com.mysms.lms.v1.ListAccessLogsRequest;
import com.mysms.lms.v1.ListAccessLogsResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/** gRPC resolver of lms.AccessLogService: delegates every RPC to the same business service as REST. */
@GrpcService
public class AccessLogResolver extends AccessLogServiceGrpc.AccessLogServiceImplBase {

    private final AccessLogService service;
    private final ProtoBridge bridge;

    public AccessLogResolver(AccessLogService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listAccessLogs(ListAccessLogsRequest request, StreamObserver<ListAccessLogsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AccessLog> rows = service.listAccessLogs(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAccessLogsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAccessLog(GetAccessLogRequest request, StreamObserver<GetAccessLogResponse> out) {
        AccessLog found = service.getAccessLog(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAccessLogResponse.newBuilder(), "access_log", found).build());
    }
}
