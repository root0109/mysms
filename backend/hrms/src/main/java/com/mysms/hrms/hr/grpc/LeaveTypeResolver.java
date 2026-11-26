package com.mysms.hrms.hr.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.hrms.hr.api.model.LeaveType;
import com.mysms.hrms.hr.api.model.LeaveTypeUpdate;
import com.mysms.hrms.hr.business.LeaveTypeService;
import com.mysms.hr.v1.CreateLeaveTypeRequest;
import com.mysms.hr.v1.CreateLeaveTypeResponse;
import com.mysms.hr.v1.DeleteLeaveTypeRequest;
import com.mysms.hr.v1.DeleteLeaveTypeResponse;
import com.mysms.hr.v1.GetLeaveTypeRequest;
import com.mysms.hr.v1.GetLeaveTypeResponse;
import com.mysms.hr.v1.LeaveTypeServiceGrpc;
import com.mysms.hr.v1.ListLeaveTypesRequest;
import com.mysms.hr.v1.ListLeaveTypesResponse;
import com.mysms.hr.v1.UpdateLeaveTypeRequest;
import com.mysms.hr.v1.UpdateLeaveTypeResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of hr.LeaveTypeService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class LeaveTypeResolver extends LeaveTypeServiceGrpc.LeaveTypeServiceImplBase {

    private final LeaveTypeService leaveTypeService;
    private final ProtoBridge bridge;

    public LeaveTypeResolver(LeaveTypeService leaveTypeService, ProtoBridge bridge) {
        this.leaveTypeService = leaveTypeService;
        this.bridge = bridge;
    }

    @Override
    public void listLeaveTypes(ListLeaveTypesRequest request, StreamObserver<ListLeaveTypesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<LeaveType> rows = leaveTypeService.listLeaveTypes(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListLeaveTypesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getLeaveType(GetLeaveTypeRequest request, StreamObserver<GetLeaveTypeResponse> out) {
        LeaveType found = leaveTypeService.getLeaveType(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetLeaveTypeResponse.newBuilder(), "leave_type", found).build());
    }

    @Override
    public void createLeaveType(CreateLeaveTypeRequest request, StreamObserver<CreateLeaveTypeResponse> out) {
        LeaveType created = leaveTypeService.createLeaveType(
                bridge.body(request.getLeaveType(), LeaveType.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateLeaveTypeResponse.newBuilder(), "leave_type", created).build());
    }

    @Override
    public void updateLeaveType(UpdateLeaveTypeRequest request, StreamObserver<UpdateLeaveTypeResponse> out) {
        LeaveType updated = leaveTypeService.updateLeaveType(
                bridge.id(request.getLeaveType().getId(), "id"),
                bridge.requiredIfMatch(request.getLeaveType().getEtag()),
                bridge.changes(request.getLeaveType(), request.getUpdateMask(), LeaveTypeUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateLeaveTypeResponse.newBuilder(), "leave_type", updated).build());
    }

    @Override
    public void deleteLeaveType(DeleteLeaveTypeRequest request, StreamObserver<DeleteLeaveTypeResponse> out) {
        leaveTypeService.deleteLeaveType(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteLeaveTypeResponse.getDefaultInstance());
    }
}
