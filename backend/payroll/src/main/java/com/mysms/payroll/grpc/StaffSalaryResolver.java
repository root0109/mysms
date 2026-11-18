package com.mysms.payroll.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.payroll.api.model.StaffSalary;
import com.mysms.payroll.api.model.StaffSalaryComponent;
import com.mysms.payroll.api.model.StaffSalaryComponentUpdate;
import com.mysms.payroll.api.model.StaffSalaryUpdate;
import com.mysms.payroll.business.StaffSalaryService;
import com.mysms.payroll.v1.CreateStaffSalaryComponentRequest;
import com.mysms.payroll.v1.CreateStaffSalaryComponentResponse;
import com.mysms.payroll.v1.CreateStaffSalaryRequest;
import com.mysms.payroll.v1.CreateStaffSalaryResponse;
import com.mysms.payroll.v1.DeleteStaffSalaryComponentRequest;
import com.mysms.payroll.v1.DeleteStaffSalaryComponentResponse;
import com.mysms.payroll.v1.DeleteStaffSalaryRequest;
import com.mysms.payroll.v1.DeleteStaffSalaryResponse;
import com.mysms.payroll.v1.GetStaffSalaryComponentRequest;
import com.mysms.payroll.v1.GetStaffSalaryComponentResponse;
import com.mysms.payroll.v1.GetStaffSalaryRequest;
import com.mysms.payroll.v1.GetStaffSalaryResponse;
import com.mysms.payroll.v1.ListStaffSalariesRequest;
import com.mysms.payroll.v1.ListStaffSalariesResponse;
import com.mysms.payroll.v1.ListStaffSalaryComponentsRequest;
import com.mysms.payroll.v1.ListStaffSalaryComponentsResponse;
import com.mysms.payroll.v1.StaffSalaryServiceGrpc;
import com.mysms.payroll.v1.UpdateStaffSalaryComponentRequest;
import com.mysms.payroll.v1.UpdateStaffSalaryComponentResponse;
import com.mysms.payroll.v1.UpdateStaffSalaryRequest;
import com.mysms.payroll.v1.UpdateStaffSalaryResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of payroll.StaffSalaryService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class StaffSalaryResolver extends StaffSalaryServiceGrpc.StaffSalaryServiceImplBase {

    private final StaffSalaryService staffSalaryService;
    private final ProtoBridge bridge;

    public StaffSalaryResolver(StaffSalaryService staffSalaryService, ProtoBridge bridge) {
        this.staffSalaryService = staffSalaryService;
        this.bridge = bridge;
    }

    @Override
    public void listStaffSalaries(ListStaffSalariesRequest request, StreamObserver<ListStaffSalariesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<StaffSalary> rows = staffSalaryService.listStaffSalarys(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListStaffSalariesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getStaffSalary(GetStaffSalaryRequest request, StreamObserver<GetStaffSalaryResponse> out) {
        StaffSalary found = staffSalaryService.getStaffSalary(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetStaffSalaryResponse.newBuilder(), "staff_salary", found).build());
    }

    @Override
    public void createStaffSalary(CreateStaffSalaryRequest request, StreamObserver<CreateStaffSalaryResponse> out) {
        StaffSalary created = staffSalaryService.createStaffSalary(
                bridge.body(request.getStaffSalary(), StaffSalary.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateStaffSalaryResponse.newBuilder(), "staff_salary", created).build());
    }

    @Override
    public void updateStaffSalary(UpdateStaffSalaryRequest request, StreamObserver<UpdateStaffSalaryResponse> out) {
        StaffSalary updated = staffSalaryService.updateStaffSalary(
                bridge.id(request.getStaffSalary().getId(), "id"),
                bridge.requiredIfMatch(request.getStaffSalary().getEtag()),
                bridge.changes(request.getStaffSalary(), request.getUpdateMask(), StaffSalaryUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateStaffSalaryResponse.newBuilder(), "staff_salary", updated).build());
    }

    @Override
    public void deleteStaffSalary(DeleteStaffSalaryRequest request, StreamObserver<DeleteStaffSalaryResponse> out) {
        staffSalaryService.deleteStaffSalary(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteStaffSalaryResponse.getDefaultInstance());
    }

    @Override
    public void listStaffSalaryComponents(ListStaffSalaryComponentsRequest request,
            StreamObserver<ListStaffSalaryComponentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<StaffSalaryComponent> rows = staffSalaryService.listStaffSalaryComponents(
                bridge.id(request.getStaffSalaryId(), "staffSalaryId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListStaffSalaryComponentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getStaffSalaryComponent(GetStaffSalaryComponentRequest request,
            StreamObserver<GetStaffSalaryComponentResponse> out) {
        StaffSalaryComponent found = staffSalaryService.getStaffSalaryComponent(
                bridge.id(request.getStaffSalaryId(), "staffSalaryId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetStaffSalaryComponentResponse.newBuilder(),
                "staff_salary_component", found).build());
    }

    @Override
    public void createStaffSalaryComponent(CreateStaffSalaryComponentRequest request,
            StreamObserver<CreateStaffSalaryComponentResponse> out) {
        StaffSalaryComponent created = staffSalaryService.createStaffSalaryComponent(
                bridge.id(request.getStaffSalaryId(), "staffSalaryId"),
                bridge.body(request.getStaffSalaryComponent(), StaffSalaryComponent.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateStaffSalaryComponentResponse.newBuilder(),
                "staff_salary_component", created).build());
    }

    @Override
    public void updateStaffSalaryComponent(UpdateStaffSalaryComponentRequest request,
            StreamObserver<UpdateStaffSalaryComponentResponse> out) {
        StaffSalaryComponent updated = staffSalaryService.updateStaffSalaryComponent(
                bridge.id(request.getStaffSalaryId(), "staffSalaryId"),
                bridge.id(request.getStaffSalaryComponent().getId(), "id"),
                bridge.requiredIfMatch(request.getStaffSalaryComponent().getEtag()),
                bridge.changes(request.getStaffSalaryComponent(), request.getUpdateMask(),
                        StaffSalaryComponentUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateStaffSalaryComponentResponse.newBuilder(),
                "staff_salary_component", updated).build());
    }

    @Override
    public void deleteStaffSalaryComponent(DeleteStaffSalaryComponentRequest request,
            StreamObserver<DeleteStaffSalaryComponentResponse> out) {
        staffSalaryService.deleteStaffSalaryComponent(
                bridge.id(request.getStaffSalaryId(), "staffSalaryId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteStaffSalaryComponentResponse.getDefaultInstance());
    }
}
