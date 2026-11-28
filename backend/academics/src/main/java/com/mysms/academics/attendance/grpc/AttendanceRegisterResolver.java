package com.mysms.academics.attendance.grpc;

import com.mysms.academics.attendance.api.model.AttendanceRegister;
import com.mysms.academics.attendance.business.AttendanceRegisterService;
import com.mysms.attendance.v1.AttendanceRegisterServiceGrpc;
import com.mysms.attendance.v1.AttendanceRegisterUpdate;
import com.mysms.attendance.v1.CreateAttendanceRegisterRequest;
import com.mysms.attendance.v1.CreateAttendanceRegisterResponse;
import com.mysms.attendance.v1.DeleteAttendanceRegisterRequest;
import com.mysms.attendance.v1.DeleteAttendanceRegisterResponse;
import com.mysms.attendance.v1.GetAttendanceRegisterRequest;
import com.mysms.attendance.v1.GetAttendanceRegisterResponse;
import com.mysms.attendance.v1.ListAttendanceRegistersRequest;
import com.mysms.attendance.v1.ListAttendanceRegistersResponse;
import com.mysms.attendance.v1.UpdateAttendanceRegisterResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of attendance.AttendanceRegisterService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class AttendanceRegisterResolver extends AttendanceRegisterServiceGrpc.AttendanceRegisterServiceImplBase {

    private final AttendanceRegisterService service;
    private final ProtoBridge bridge;

    public AttendanceRegisterResolver(AttendanceRegisterService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listAttendanceRegisters(ListAttendanceRegistersRequest request,
            StreamObserver<ListAttendanceRegistersResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AttendanceRegister> rows = service.listAttendanceRegisters(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAttendanceRegistersResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAttendanceRegister(GetAttendanceRegisterRequest request, StreamObserver<GetAttendanceRegisterResponse> out) {
        AttendanceRegister found = service.getAttendanceRegister(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAttendanceRegisterResponse.newBuilder(), "attendance_register", found).build());
    }

    @Override
    public void createAttendanceRegister(CreateAttendanceRegisterRequest request,
            StreamObserver<CreateAttendanceRegisterResponse> out) {
        AttendanceRegister created = service.createAttendanceRegister(
                bridge.body(request.getAttendanceRegister(), AttendanceRegister.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreateAttendanceRegisterResponse.newBuilder(), "attendance_register", created).build());
    }

    @Override
    public void updateAttendanceRegister(AttendanceRegisterUpdate request,
            StreamObserver<UpdateAttendanceRegisterResponse> out) {
        AttendanceRegister updated = service.updateAttendanceRegister(bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                AttendanceRegisterProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdateAttendanceRegisterResponse.newBuilder(), "attendance_register", updated).build());
    }

    @Override
    public void deleteAttendanceRegister(DeleteAttendanceRegisterRequest request,
            StreamObserver<DeleteAttendanceRegisterResponse> out) {
        service.deleteAttendanceRegister(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAttendanceRegisterResponse.getDefaultInstance());
    }
}
