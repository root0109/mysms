package com.mysms.campus.frontoffice.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.campus.frontoffice.api.model.CallLog;
import com.mysms.campus.frontoffice.business.CallLogService;
import com.mysms.frontoffice.v1.CallLogServiceGrpc;
import com.mysms.frontoffice.v1.CallLogUpdate;
import com.mysms.frontoffice.v1.CreateCallLogRequest;
import com.mysms.frontoffice.v1.CreateCallLogResponse;
import com.mysms.frontoffice.v1.DeleteCallLogRequest;
import com.mysms.frontoffice.v1.DeleteCallLogResponse;
import com.mysms.frontoffice.v1.GetCallLogRequest;
import com.mysms.frontoffice.v1.GetCallLogResponse;
import com.mysms.frontoffice.v1.ListCallLogsRequest;
import com.mysms.frontoffice.v1.ListCallLogsResponse;
import com.mysms.frontoffice.v1.UpdateCallLogResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of frontoffice.CallLogService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class CallLogResolver extends CallLogServiceGrpc.CallLogServiceImplBase {

    private final CallLogService callLogService;
    private final ProtoBridge bridge;

    public CallLogResolver(CallLogService callLogService, ProtoBridge bridge) {
        this.callLogService = callLogService;
        this.bridge = bridge;
    }

    @Override
    public void listCallLogs(ListCallLogsRequest request, StreamObserver<ListCallLogsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CallLog> rows = callLogService.listCallLogs(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCallLogsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCallLog(GetCallLogRequest request, StreamObserver<GetCallLogResponse> out) {
        CallLog found = callLogService.getCallLog(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCallLogResponse.newBuilder(), "call_log", found).build());
    }

    @Override
    public void createCallLog(CreateCallLogRequest request, StreamObserver<CreateCallLogResponse> out) {
        CallLog created = callLogService.createCallLog(
                bridge.body(request.getCallLog(), CallLog.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCallLogResponse.newBuilder(), "call_log", created).build());
    }

    @Override
    public void updateCallLog(CallLogUpdate request, StreamObserver<UpdateCallLogResponse> out) {
        CallLog updated = callLogService.updateCallLog(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()),
                CallLogProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCallLogResponse.newBuilder(), "call_log", updated).build());
    }

    @Override
    public void deleteCallLog(DeleteCallLogRequest request, StreamObserver<DeleteCallLogResponse> out) {
        callLogService.deleteCallLog(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCallLogResponse.getDefaultInstance());
    }
}
