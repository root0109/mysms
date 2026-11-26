package com.mysms.hrms.hr.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.hrms.hr.api.model.LeaveApplication;
import com.mysms.hrms.hr.api.model.LeaveApplicationUpdate;
import com.mysms.hrms.hr.business.LeaveApplicationService;
import com.mysms.hr.v1.ApproveLeaveRequest;
import com.mysms.hr.v1.ApproveLeaveResponse;
import com.mysms.hr.v1.CancelLeaveRequest;
import com.mysms.hr.v1.CancelLeaveResponse;
import com.mysms.hr.v1.CreateLeaveApplicationRequest;
import com.mysms.hr.v1.CreateLeaveApplicationResponse;
import com.mysms.hr.v1.DeleteLeaveApplicationRequest;
import com.mysms.hr.v1.DeleteLeaveApplicationResponse;
import com.mysms.hr.v1.GetLeaveApplicationRequest;
import com.mysms.hr.v1.GetLeaveApplicationResponse;
import com.mysms.hr.v1.LeaveApplicationServiceGrpc;
import com.mysms.hr.v1.ListLeaveApplicationsRequest;
import com.mysms.hr.v1.ListLeaveApplicationsResponse;
import com.mysms.hr.v1.RejectLeaveRequest;
import com.mysms.hr.v1.RejectLeaveResponse;
import com.mysms.hr.v1.UpdateLeaveApplicationRequest;
import com.mysms.hr.v1.UpdateLeaveApplicationResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of hr.LeaveApplicationService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class LeaveApplicationResolver extends LeaveApplicationServiceGrpc.LeaveApplicationServiceImplBase {

    private final LeaveApplicationService leaveApplicationService;
    private final ProtoBridge bridge;

    public LeaveApplicationResolver(LeaveApplicationService leaveApplicationService, ProtoBridge bridge) {
        this.leaveApplicationService = leaveApplicationService;
        this.bridge = bridge;
    }

    @Override
    public void listLeaveApplications(ListLeaveApplicationsRequest request,
            StreamObserver<ListLeaveApplicationsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<LeaveApplication> rows = leaveApplicationService.listLeaveApplications(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListLeaveApplicationsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getLeaveApplication(GetLeaveApplicationRequest request,
            StreamObserver<GetLeaveApplicationResponse> out) {
        LeaveApplication found = leaveApplicationService.getLeaveApplication(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetLeaveApplicationResponse.newBuilder(),
                "leave_application", found).build());
    }

    @Override
    public void createLeaveApplication(CreateLeaveApplicationRequest request,
            StreamObserver<CreateLeaveApplicationResponse> out) {
        LeaveApplication created = leaveApplicationService.createLeaveApplication(
                bridge.body(request.getLeaveApplication(), LeaveApplication.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateLeaveApplicationResponse.newBuilder(),
                "leave_application", created).build());
    }

    @Override
    public void updateLeaveApplication(UpdateLeaveApplicationRequest request,
            StreamObserver<UpdateLeaveApplicationResponse> out) {
        LeaveApplication updated = leaveApplicationService.updateLeaveApplication(
                bridge.id(request.getLeaveApplication().getId(), "id"),
                bridge.requiredIfMatch(request.getLeaveApplication().getEtag()),
                bridge.changes(request.getLeaveApplication(), request.getUpdateMask(), LeaveApplicationUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateLeaveApplicationResponse.newBuilder(),
                "leave_application", updated).build());
    }

    @Override
    public void deleteLeaveApplication(DeleteLeaveApplicationRequest request,
            StreamObserver<DeleteLeaveApplicationResponse> out) {
        leaveApplicationService.deleteLeaveApplication(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteLeaveApplicationResponse.getDefaultInstance());
    }

    @Override
    public void approveLeave(ApproveLeaveRequest request, StreamObserver<ApproveLeaveResponse> out) {
        LeaveApplication changed = leaveApplicationService.approveLeave(
                bridge.id(request.getLeaveApplicationId(), "leaveApplicationId"),
                bridge.body(request,
                        com.mysms.hrms.hr.api.model.ApproveLeaveRequest.class, "leave_application_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ApproveLeaveResponse.newBuilder(), "leave_application", changed).build());
    }

    @Override
    public void rejectLeave(RejectLeaveRequest request, StreamObserver<RejectLeaveResponse> out) {
        LeaveApplication changed = leaveApplicationService.rejectLeave(
                bridge.id(request.getLeaveApplicationId(), "leaveApplicationId"),
                bridge.body(request,
                        com.mysms.hrms.hr.api.model.RejectLeaveRequest.class, "leave_application_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(RejectLeaveResponse.newBuilder(), "leave_application", changed).build());
    }

    @Override
    public void cancelLeave(CancelLeaveRequest request, StreamObserver<CancelLeaveResponse> out) {
        LeaveApplication changed = leaveApplicationService.cancelLeave(
                bridge.id(request.getLeaveApplicationId(), "leaveApplicationId"),
                bridge.body(request,
                        com.mysms.hrms.hr.api.model.CancelLeaveRequest.class, "leave_application_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(CancelLeaveResponse.newBuilder(), "leave_application", changed).build());
    }
}
