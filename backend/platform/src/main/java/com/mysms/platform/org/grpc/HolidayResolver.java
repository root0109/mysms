package com.mysms.platform.org.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.platform.org.api.model.Holiday;
import com.mysms.platform.org.business.HolidayService;
import com.mysms.org.v1.CreateHolidayRequest;
import com.mysms.org.v1.CreateHolidayResponse;
import com.mysms.org.v1.DeleteHolidayRequest;
import com.mysms.org.v1.DeleteHolidayResponse;
import com.mysms.org.v1.GetHolidayRequest;
import com.mysms.org.v1.GetHolidayResponse;
import com.mysms.org.v1.HolidayServiceGrpc;
import com.mysms.org.v1.HolidayUpdate;
import com.mysms.org.v1.ListHolidaysRequest;
import com.mysms.org.v1.ListHolidaysResponse;
import com.mysms.org.v1.UpdateHolidayResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of org.HolidayService: delegates every RPC to the business service REST uses, so the rules exist once.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class HolidayResolver extends HolidayServiceGrpc.HolidayServiceImplBase {

    private final HolidayService service;
    private final ProtoBridge bridge;

    public HolidayResolver(HolidayService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }


    @Override
    public void listHolidays(ListHolidaysRequest request,
            StreamObserver<ListHolidaysResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Holiday> rows = service.listHolidays(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListHolidaysResponse.newBuilder(), rows).build());
    }

    @Override
    public void getHoliday(GetHolidayRequest request, StreamObserver<GetHolidayResponse> out) {
        Holiday found = service.getHoliday(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetHolidayResponse.newBuilder(), "holiday", found).build());
    }

    @Override
    public void createHoliday(CreateHolidayRequest request, StreamObserver<CreateHolidayResponse> out) {
        Holiday created = service.createHoliday(
                bridge.body(request.getHoliday(), Holiday.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateHolidayResponse.newBuilder(), "holiday", created).build());
    }

    @Override
    public void updateHoliday(HolidayUpdate request, StreamObserver<UpdateHolidayResponse> out) {
        Holiday updated = service.updateHoliday(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                HolidayProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateHolidayResponse.newBuilder(), "holiday", updated).build());
    }

    @Override
    public void deleteHoliday(DeleteHolidayRequest request, StreamObserver<DeleteHolidayResponse> out) {
        service.deleteHoliday(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteHolidayResponse.getDefaultInstance());
    }
}
