package com.mysms.comms.grpc;

import com.mysms.comms.api.model.Operation;
import com.mysms.comms.api.model.CalendarExport;
import com.mysms.comms.business.CalendarExportService;
import com.mysms.comms.v1.CalendarExportServiceGrpc;
import com.mysms.comms.v1.DeleteCalendarExportRequest;
import com.mysms.comms.v1.DeleteCalendarExportResponse;
import com.mysms.comms.v1.ExportCalendarRequest;
import com.mysms.comms.v1.ExportCalendarResponse;
import com.mysms.comms.v1.GetCalendarExportRequest;
import com.mysms.comms.v1.GetCalendarExportResponse;
import com.mysms.comms.v1.ListCalendarExportsRequest;
import com.mysms.comms.v1.ListCalendarExportsResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of comms.CalendarExportService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class CalendarExportResolver extends CalendarExportServiceGrpc.CalendarExportServiceImplBase {

    private final CalendarExportService service;
    private final ProtoBridge bridge;

    public CalendarExportResolver(CalendarExportService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listCalendarExports(ListCalendarExportsRequest request, StreamObserver<ListCalendarExportsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CalendarExport> rows = service.listCalendarExports(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCalendarExportsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCalendarExport(GetCalendarExportRequest request, StreamObserver<GetCalendarExportResponse> out) {
        CalendarExport found = service.getCalendarExport(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCalendarExportResponse.newBuilder(), "calendar_export", found).build());
    }

    @Override
    public void deleteCalendarExport(DeleteCalendarExportRequest request, StreamObserver<DeleteCalendarExportResponse> out) {
        service.deleteCalendarExport(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCalendarExportResponse.getDefaultInstance());
    }

    @Override
    public void exportCalendar(ExportCalendarRequest request, StreamObserver<ExportCalendarResponse> out) {
        Operation accepted = service.exportCalendar(bridge.body(request,
                com.mysms.comms.api.model.ExportCalendarRequest.class, "request_id"));
        ProtoBridge.send(out, bridge.reply(ExportCalendarResponse.newBuilder(), "result", accepted).build());
    }
}
