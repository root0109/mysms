package com.mysms.hrms.timeattendance.grpc;

import com.mysms.hrms.timeattendance.api.model.StaffAttendance;
import com.mysms.hrms.timeattendance.business.StaffAttendanceService;
import com.mysms.time_attendance.v1.CreateStaffAttendanceRequest;
import com.mysms.time_attendance.v1.CreateStaffAttendanceResponse;
import com.mysms.time_attendance.v1.GetStaffAttendanceRequest;
import com.mysms.time_attendance.v1.GetStaffAttendanceResponse;
import com.mysms.time_attendance.v1.ListStaffAttendancesRequest;
import com.mysms.time_attendance.v1.ListStaffAttendancesResponse;
import com.mysms.time_attendance.v1.StaffAttendanceServiceGrpc;
import com.mysms.time_attendance.v1.StaffAttendanceUpdate;
import com.mysms.time_attendance.v1.UpdateStaffAttendanceResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of time_attendance.StaffAttendanceService: delegates every RPC to the same business service as REST.
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
    public void updateStaffAttendance(StaffAttendanceUpdate request, StreamObserver<UpdateStaffAttendanceResponse> out) {
        StaffAttendance updated = service.updateStaffAttendance(bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                StaffAttendanceProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateStaffAttendanceResponse.newBuilder(), "staff_attendance", updated).build());
    }
}
