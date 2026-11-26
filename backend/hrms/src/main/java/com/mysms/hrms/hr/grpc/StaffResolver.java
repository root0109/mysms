package com.mysms.hrms.hr.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.hrms.hr.api.model.LeaveBalanceSummary;
import com.mysms.hrms.hr.api.model.Staff;
import com.mysms.hrms.hr.api.model.StaffDepartment;
import com.mysms.hrms.hr.api.model.StaffDepartmentUpdate;
import com.mysms.hrms.hr.api.model.StaffUpdate;
import com.mysms.hrms.hr.business.StaffService;
import com.mysms.hr.v1.ChangeStaffStatusRequest;
import com.mysms.hr.v1.ChangeStaffStatusResponse;
import com.mysms.hr.v1.CreateStaffDepartmentRequest;
import com.mysms.hr.v1.CreateStaffDepartmentResponse;
import com.mysms.hr.v1.CreateStaffPortalAccountRequest;
import com.mysms.hr.v1.CreateStaffPortalAccountResponse;
import com.mysms.hr.v1.CreateStaffRequest;
import com.mysms.hr.v1.CreateStaffResponse;
import com.mysms.hr.v1.DeleteStaffDepartmentRequest;
import com.mysms.hr.v1.DeleteStaffDepartmentResponse;
import com.mysms.hr.v1.DeleteStaffRequest;
import com.mysms.hr.v1.DeleteStaffResponse;
import com.mysms.hr.v1.GetStaffDepartmentRequest;
import com.mysms.hr.v1.GetStaffDepartmentResponse;
import com.mysms.hr.v1.GetStaffLeaveBalanceRequest;
import com.mysms.hr.v1.GetStaffLeaveBalanceResponse;
import com.mysms.hr.v1.GetStaffRequest;
import com.mysms.hr.v1.GetStaffResponse;
import com.mysms.hr.v1.ListStaffDepartmentsRequest;
import com.mysms.hr.v1.ListStaffDepartmentsResponse;
import com.mysms.hr.v1.ListStaffRequest;
import com.mysms.hr.v1.ListStaffResponse;
import com.mysms.hr.v1.StaffServiceGrpc;
import com.mysms.hr.v1.UpdateStaffDepartmentRequest;
import com.mysms.hr.v1.UpdateStaffDepartmentResponse;
import com.mysms.hr.v1.UpdateStaffRequest;
import com.mysms.hr.v1.UpdateStaffResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of hr.StaffService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class StaffResolver extends StaffServiceGrpc.StaffServiceImplBase {

    private final StaffService staffService;
    private final ProtoBridge bridge;

    public StaffResolver(StaffService staffService, ProtoBridge bridge) {
        this.staffService = staffService;
        this.bridge = bridge;
    }

    @Override
    public void listStaff(ListStaffRequest request, StreamObserver<ListStaffResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Staff> rows = staffService.listStaffs(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListStaffResponse.newBuilder(), rows).build());
    }

    @Override
    public void getStaff(GetStaffRequest request, StreamObserver<GetStaffResponse> out) {
        Staff found = staffService.getStaff(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetStaffResponse.newBuilder(), "staff", found).build());
    }

    @Override
    public void createStaff(CreateStaffRequest request, StreamObserver<CreateStaffResponse> out) {
        Staff created = staffService.createStaff(
                bridge.body(request.getStaff(), Staff.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateStaffResponse.newBuilder(), "staff", created).build());
    }

    @Override
    public void updateStaff(UpdateStaffRequest request, StreamObserver<UpdateStaffResponse> out) {
        Staff updated = staffService.updateStaff(
                bridge.id(request.getStaff().getId(), "id"), bridge.requiredIfMatch(request.getStaff().getEtag()),
                bridge.changes(request.getStaff(), request.getUpdateMask(), StaffUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateStaffResponse.newBuilder(), "staff", updated).build());
    }

    @Override
    public void deleteStaff(DeleteStaffRequest request, StreamObserver<DeleteStaffResponse> out) {
        staffService.deleteStaff(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteStaffResponse.getDefaultInstance());
    }

    @Override
    public void listStaffDepartments(ListStaffDepartmentsRequest request,
            StreamObserver<ListStaffDepartmentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<StaffDepartment> rows = staffService.listStaffDepartments(
                bridge.id(request.getStaffId(), "staffId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListStaffDepartmentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getStaffDepartment(GetStaffDepartmentRequest request,
            StreamObserver<GetStaffDepartmentResponse> out) {
        StaffDepartment found = staffService.getStaffDepartment(
                bridge.id(request.getStaffId(), "staffId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetStaffDepartmentResponse.newBuilder(),
                "staff_department", found).build());
    }

    @Override
    public void createStaffDepartment(CreateStaffDepartmentRequest request,
            StreamObserver<CreateStaffDepartmentResponse> out) {
        StaffDepartment created = staffService.createStaffDepartment(
                bridge.id(request.getStaffId(), "staffId"),
                bridge.body(request.getStaffDepartment(), StaffDepartment.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateStaffDepartmentResponse.newBuilder(),
                "staff_department", created).build());
    }

    @Override
    public void updateStaffDepartment(UpdateStaffDepartmentRequest request,
            StreamObserver<UpdateStaffDepartmentResponse> out) {
        StaffDepartment updated = staffService.updateStaffDepartment(
                bridge.id(request.getStaffId(), "staffId"), bridge.id(request.getStaffDepartment().getId(), "id"),
                bridge.requiredIfMatch(request.getStaffDepartment().getEtag()),
                bridge.changes(request.getStaffDepartment(), request.getUpdateMask(), StaffDepartmentUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateStaffDepartmentResponse.newBuilder(),
                "staff_department", updated).build());
    }

    @Override
    public void deleteStaffDepartment(DeleteStaffDepartmentRequest request,
            StreamObserver<DeleteStaffDepartmentResponse> out) {
        staffService.deleteStaffDepartment(
                bridge.id(request.getStaffId(), "staffId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteStaffDepartmentResponse.getDefaultInstance());
    }

    @Override
    public void changeStaffStatus(ChangeStaffStatusRequest request, StreamObserver<ChangeStaffStatusResponse> out) {
        Staff changed = staffService.changeStaffStatus(
                bridge.id(request.getStaffId(), "staffId"),
                bridge.body(request,
                        com.mysms.hrms.hr.api.model.ChangeStaffStatusRequest.class, "staff_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ChangeStaffStatusResponse.newBuilder(), "staff", changed).build());
    }

    @Override
    public void createStaffPortalAccount(CreateStaffPortalAccountRequest request,
            StreamObserver<CreateStaffPortalAccountResponse> out) {
        Staff changed = staffService.createStaffPortalAccount(
                bridge.id(request.getStaffId(), "staffId"),
                bridge.body(request,
                        com.mysms.hrms.hr.api.model.CreateStaffPortalAccountRequest.class, "staff_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(CreateStaffPortalAccountResponse.newBuilder(), "staff", changed).build());
    }

    /** The academic year is required as on REST (its query parameter): unset, it is 400 before the service runs. */
    @Override
    public void getStaffLeaveBalance(GetStaffLeaveBalanceRequest request,
            StreamObserver<GetStaffLeaveBalanceResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<LeaveBalanceSummary> rows = staffService.getStaffLeaveBalance(
                bridge.id(request.getStaffId(), "staffId"), bridge.id(request.getAcademicYearId(), "academicYearId"),
                page.page(), page.size(), page.cursor());
        ProtoBridge.send(out, bridge.page(GetStaffLeaveBalanceResponse.newBuilder(), rows).build());
    }
}
