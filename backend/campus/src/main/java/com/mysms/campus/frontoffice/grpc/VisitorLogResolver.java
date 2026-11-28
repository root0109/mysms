package com.mysms.campus.frontoffice.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.campus.frontoffice.api.model.VisitorLog;
import com.mysms.campus.frontoffice.business.VisitorLogService;
import com.mysms.frontoffice.v1.CheckOutVisitorRequest;
import com.mysms.frontoffice.v1.CheckOutVisitorResponse;
import com.mysms.frontoffice.v1.CreateVisitorLogRequest;
import com.mysms.frontoffice.v1.CreateVisitorLogResponse;
import com.mysms.frontoffice.v1.DeleteVisitorLogRequest;
import com.mysms.frontoffice.v1.DeleteVisitorLogResponse;
import com.mysms.frontoffice.v1.GetVisitorLogRequest;
import com.mysms.frontoffice.v1.GetVisitorLogResponse;
import com.mysms.frontoffice.v1.ListVisitorLogsRequest;
import com.mysms.frontoffice.v1.ListVisitorLogsResponse;
import com.mysms.frontoffice.v1.UpdateVisitorLogResponse;
import com.mysms.frontoffice.v1.VisitorLogServiceGrpc;
import com.mysms.frontoffice.v1.VisitorLogUpdate;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of frontoffice.VisitorLogService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class VisitorLogResolver extends VisitorLogServiceGrpc.VisitorLogServiceImplBase {

    private final VisitorLogService visitorLogService;
    private final ProtoBridge bridge;

    public VisitorLogResolver(VisitorLogService visitorLogService, ProtoBridge bridge) {
        this.visitorLogService = visitorLogService;
        this.bridge = bridge;
    }

    @Override
    public void listVisitorLogs(ListVisitorLogsRequest request, StreamObserver<ListVisitorLogsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<VisitorLog> rows = visitorLogService.listVisitorLogs(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListVisitorLogsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getVisitorLog(GetVisitorLogRequest request, StreamObserver<GetVisitorLogResponse> out) {
        VisitorLog found = visitorLogService.getVisitorLog(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetVisitorLogResponse.newBuilder(), "visitor_log", found).build());
    }

    @Override
    public void createVisitorLog(CreateVisitorLogRequest request, StreamObserver<CreateVisitorLogResponse> out) {
        VisitorLog created = visitorLogService.createVisitorLog(
                bridge.body(request.getVisitorLog(), VisitorLog.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateVisitorLogResponse.newBuilder(), "visitor_log", created).build());
    }

    @Override
    public void updateVisitorLog(VisitorLogUpdate request, StreamObserver<UpdateVisitorLogResponse> out) {
        VisitorLog updated = visitorLogService.updateVisitorLog(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                VisitorLogProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateVisitorLogResponse.newBuilder(), "visitor_log", updated).build());
    }

    @Override
    public void deleteVisitorLog(DeleteVisitorLogRequest request, StreamObserver<DeleteVisitorLogResponse> out) {
        visitorLogService.deleteVisitorLog(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteVisitorLogResponse.getDefaultInstance());
    }

    @Override
    public void checkOutVisitor(CheckOutVisitorRequest request, StreamObserver<CheckOutVisitorResponse> out) {
        VisitorLog changed = visitorLogService.checkOutVisitor(bridge.id(request.getVisitorLogId(), "visitorLogId"),
                bridge.body(request, com.mysms.campus.frontoffice.api.model.CheckOutVisitorRequest.class, "visitor_log_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(CheckOutVisitorResponse.newBuilder(), "visitor_log", changed).build());
    }
}
