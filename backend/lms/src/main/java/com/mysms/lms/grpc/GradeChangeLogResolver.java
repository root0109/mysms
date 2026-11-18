package com.mysms.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.lms.api.model.GradeChangeLog;
import com.mysms.lms.business.GradeChangeLogService;
import com.mysms.lms.v1.GetGradeChangeLogRequest;
import com.mysms.lms.v1.GetGradeChangeLogResponse;
import com.mysms.lms.v1.GradeChangeLogServiceGrpc;
import com.mysms.lms.v1.ListGradeChangeLogsRequest;
import com.mysms.lms.v1.ListGradeChangeLogsResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.GradeChangeLogService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class GradeChangeLogResolver extends GradeChangeLogServiceGrpc.GradeChangeLogServiceImplBase {

    private final GradeChangeLogService service;
    private final ProtoBridge bridge;

    public GradeChangeLogResolver(GradeChangeLogService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listGradeChangeLogs(ListGradeChangeLogsRequest request, StreamObserver<ListGradeChangeLogsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<GradeChangeLog> rows = service.listGradeChangeLogs(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListGradeChangeLogsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getGradeChangeLog(GetGradeChangeLogRequest request, StreamObserver<GetGradeChangeLogResponse> out) {
        GradeChangeLog found = service.getGradeChangeLog(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetGradeChangeLogResponse.newBuilder(), "grade_change_log", found).build());
    }
}
