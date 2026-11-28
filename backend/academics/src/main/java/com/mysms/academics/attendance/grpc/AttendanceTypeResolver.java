package com.mysms.academics.attendance.grpc;

import com.mysms.academics.attendance.api.model.AttendanceType;
import com.mysms.academics.attendance.business.AttendanceTypeService;
import com.mysms.attendance.v1.AttendanceTypeServiceGrpc;
import com.mysms.attendance.v1.AttendanceTypeUpdate;
import com.mysms.attendance.v1.CreateAttendanceTypeRequest;
import com.mysms.attendance.v1.CreateAttendanceTypeResponse;
import com.mysms.attendance.v1.DeleteAttendanceTypeRequest;
import com.mysms.attendance.v1.DeleteAttendanceTypeResponse;
import com.mysms.attendance.v1.GetAttendanceTypeRequest;
import com.mysms.attendance.v1.GetAttendanceTypeResponse;
import com.mysms.attendance.v1.ListAttendanceTypesRequest;
import com.mysms.attendance.v1.ListAttendanceTypesResponse;
import com.mysms.attendance.v1.UpdateAttendanceTypeResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of attendance.AttendanceTypeService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class AttendanceTypeResolver extends AttendanceTypeServiceGrpc.AttendanceTypeServiceImplBase {

    private final AttendanceTypeService service;
    private final ProtoBridge bridge;

    public AttendanceTypeResolver(AttendanceTypeService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listAttendanceTypes(ListAttendanceTypesRequest request, StreamObserver<ListAttendanceTypesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AttendanceType> rows = service.listAttendanceTypes(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAttendanceTypesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAttendanceType(GetAttendanceTypeRequest request, StreamObserver<GetAttendanceTypeResponse> out) {
        AttendanceType found = service.getAttendanceType(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAttendanceTypeResponse.newBuilder(), "attendance_type", found).build());
    }

    @Override
    public void createAttendanceType(CreateAttendanceTypeRequest request, StreamObserver<CreateAttendanceTypeResponse> out) {
        AttendanceType created = service.createAttendanceType(bridge.body(request.getAttendanceType(), AttendanceType.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateAttendanceTypeResponse.newBuilder(), "attendance_type", created).build());
    }

    @Override
    public void updateAttendanceType(AttendanceTypeUpdate request, StreamObserver<UpdateAttendanceTypeResponse> out) {
        AttendanceType updated = service.updateAttendanceType(bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                AttendanceTypeProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateAttendanceTypeResponse.newBuilder(), "attendance_type", updated).build());
    }

    @Override
    public void deleteAttendanceType(DeleteAttendanceTypeRequest request, StreamObserver<DeleteAttendanceTypeResponse> out) {
        service.deleteAttendanceType(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAttendanceTypeResponse.getDefaultInstance());
    }
}
