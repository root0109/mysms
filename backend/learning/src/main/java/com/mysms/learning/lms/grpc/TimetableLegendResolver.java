package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.TimetableLegend;
import com.mysms.learning.lms.business.TimetableLegendService;
import com.mysms.lms.v1.CreateTimetableLegendRequest;
import com.mysms.lms.v1.CreateTimetableLegendResponse;
import com.mysms.lms.v1.DeleteTimetableLegendRequest;
import com.mysms.lms.v1.DeleteTimetableLegendResponse;
import com.mysms.lms.v1.GetTimetableLegendRequest;
import com.mysms.lms.v1.GetTimetableLegendResponse;
import com.mysms.lms.v1.ListTimetableLegendsRequest;
import com.mysms.lms.v1.ListTimetableLegendsResponse;
import com.mysms.lms.v1.TimetableLegendServiceGrpc;
import com.mysms.lms.v1.TimetableLegendUpdate;
import com.mysms.lms.v1.UpdateTimetableLegendResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.TimetableLegendService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge; operations not served yet answer
 * UNIMPLEMENTED.
 */
@GrpcService
public class TimetableLegendResolver extends TimetableLegendServiceGrpc.TimetableLegendServiceImplBase {

    private final TimetableLegendService service;
    private final ProtoBridge bridge;

    public TimetableLegendResolver(TimetableLegendService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listTimetableLegends(ListTimetableLegendsRequest request, StreamObserver<ListTimetableLegendsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<TimetableLegend> rows = service.listTimetableLegends(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListTimetableLegendsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getTimetableLegend(GetTimetableLegendRequest request, StreamObserver<GetTimetableLegendResponse> out) {
        TimetableLegend found = service.getTimetableLegend(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetTimetableLegendResponse.newBuilder(), "timetable_legend", found).build());
    }

    @Override
    public void createTimetableLegend(CreateTimetableLegendRequest request, StreamObserver<CreateTimetableLegendResponse> out) {
        TimetableLegend created = service.createTimetableLegend(bridge.body(request.getTimetableLegend(), TimetableLegend.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateTimetableLegendResponse.newBuilder(), "timetable_legend", created).build());
    }

    @Override
    public void updateTimetableLegend(TimetableLegendUpdate request, StreamObserver<UpdateTimetableLegendResponse> out) {
        TimetableLegend updated = service.updateTimetableLegend(bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                TimetableLegendProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateTimetableLegendResponse.newBuilder(), "timetable_legend", updated).build());
    }

    @Override
    public void deleteTimetableLegend(DeleteTimetableLegendRequest request, StreamObserver<DeleteTimetableLegendResponse> out) {
        service.deleteTimetableLegend(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteTimetableLegendResponse.getDefaultInstance());
    }
}
