package com.mysms.attendance.grpc;

import com.mysms.attendance.api.model.StaffAttendance;
import com.mysms.attendance.api.model.StaffAttendanceUpdate;
import com.mysms.attendance.business.StaffAttendanceService;
import com.mysms.attendance.v1.CreateStaffAttendanceRequest;
import com.mysms.attendance.v1.CreateStaffAttendanceResponse;
import com.mysms.attendance.v1.GetStaffAttendanceRequest;
import com.mysms.attendance.v1.GetStaffAttendanceResponse;
import com.mysms.attendance.v1.ListStaffAttendancesRequest;
import com.mysms.attendance.v1.ListStaffAttendancesResponse;
import com.mysms.attendance.v1.StaffAttendanceServiceGrpc;
import com.mysms.attendance.v1.UpdateStaffAttendanceRequest;
import com.mysms.attendance.v1.UpdateStaffAttendanceResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of attendance.StaffAttendanceService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class StaffAttendanceResolver extends StaffAttendanceServiceGrpc.StaffAttendanceServiceImplBase {

    private final StaffAttendanceService service;
    private final ProtoBridge bridge;

    public StaffAttendanceResolver(StaffAttendanceService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listStaffAttendances(ListStaffAttendancesRequest request, StreamObserver<ListStaffAttendancesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<StaffAttendance> rows = service.listStaffAttendances(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListStaffAttendancesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getStaffAttendance(GetStaffAttendanceRequest request, StreamObserver<GetStaffAttendanceResponse> out) {
        StaffAttendance found = service.getStaffAttendance(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetStaffAttendanceResponse.newBuilder(), "staff_attendance", found).build());
    }

    @Override
    public void createStaffAttendance(CreateStaffAttendanceRequest request, StreamObserver<CreateStaffAttendanceResponse> out) {
        StaffAttendance created = service.createStaffAttendance(bridge.body(request.getStaffAttendance(), StaffAttendance.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateStaffAttendanceResponse.newBuilder(), "staff_attendance", created).build());
    }

    @Override
    public void updateStaffAttendance(UpdateStaffAttendanceRequest request, StreamObserver<UpdateStaffAttendanceResponse> out) {
        StaffAttendance updated = service.updateStaffAttendance(bridge.id(request.getStaffAttendance().getId(), "id"),
                bridge.requiredIfMatch(request.getStaffAttendance().getEtag()),
                bridge.changes(request.getStaffAttendance(), request.getUpdateMask(), StaffAttendanceUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateStaffAttendanceResponse.newBuilder(), "staff_attendance", updated).build());
    }
}
