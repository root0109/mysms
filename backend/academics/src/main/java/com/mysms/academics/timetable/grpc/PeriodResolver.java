package com.mysms.academics.timetable.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.academics.timetable.api.model.Period;
import com.mysms.academics.timetable.business.PeriodService;
import com.mysms.timetable.v1.CreatePeriodRequest;
import com.mysms.timetable.v1.CreatePeriodResponse;
import com.mysms.timetable.v1.DeletePeriodRequest;
import com.mysms.timetable.v1.DeletePeriodResponse;
import com.mysms.timetable.v1.GetPeriodRequest;
import com.mysms.timetable.v1.GetPeriodResponse;
import com.mysms.timetable.v1.ListPeriodsRequest;
import com.mysms.timetable.v1.ListPeriodsResponse;
import com.mysms.timetable.v1.PeriodServiceGrpc;
import com.mysms.timetable.v1.PeriodUpdate;
import com.mysms.timetable.v1.UpdatePeriodResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of timetable.PeriodService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class PeriodResolver extends PeriodServiceGrpc.PeriodServiceImplBase {

    private final PeriodService service;
    private final ProtoBridge bridge;

    public PeriodResolver(PeriodService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listPeriods(ListPeriodsRequest request, StreamObserver<ListPeriodsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Period> rows = service.listPeriods(page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()),
                page.sort());
        ProtoBridge.send(out, bridge.page(ListPeriodsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPeriod(GetPeriodRequest request, StreamObserver<GetPeriodResponse> out) {
        Period found = service.getPeriod(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPeriodResponse.newBuilder(), "period", found).build());
    }

    @Override
    public void createPeriod(CreatePeriodRequest request, StreamObserver<CreatePeriodResponse> out) {
        Period created = service.createPeriod(bridge.body(request.getPeriod(), Period.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreatePeriodResponse.newBuilder(), "period", created).build());
    }

    @Override
    public void updatePeriod(PeriodUpdate request, StreamObserver<UpdatePeriodResponse> out) {
        Period updated = service.updatePeriod(bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                PeriodProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdatePeriodResponse.newBuilder(), "period", updated).build());
    }

    @Override
    public void deletePeriod(DeletePeriodRequest request, StreamObserver<DeletePeriodResponse> out) {
        service.deletePeriod(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePeriodResponse.getDefaultInstance());
    }
}
